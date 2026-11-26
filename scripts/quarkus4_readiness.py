#!/usr/bin/env python3
"""Probe the isolated Quarkus 4 migration; never change or merge the main baseline."""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
from urllib.request import Request, urlopen
import xml.etree.ElementTree as ET

CENTRAL = "https://repo.maven.apache.org/maven2/"
PROJECTS = ("part2/lab", "part2/solution")
VERSION = re.compile(r"^(\d+(?:\.\d+){1,3})(?:[.-](Alpha|Beta|CR|RC|Final)(\d*))?$", re.I)
BLOCKERS = ("io/quarkus/jackson/ObjectMapperCustomizer", "io.quarkus.jackson.ObjectMapperCustomizer")


def version_key(value):
    match = VERSION.fullmatch(value)
    if not match:
        raise ValueError(f"Unsupported release version: {value}")
    numbers = tuple(map(int, match[1].split(".")))
    qualifier = (match[2] or "final").lower()
    return numbers + (0,) * (4 - len(numbers)), {"alpha": 0, "beta": 1, "cr": 2, "rc": 2, "final": 3}[qualifier], int(match[3] or 0)


def stable(value):
    return version_key(value)[1] == 3


def fetch(relative):
    request = Request(CENTRAL + relative, headers={"User-Agent": "agentic-ai-java-workshop-readiness"})
    for attempt in range(3):
        try:
            with urlopen(request, timeout=30) as response:
                return response.read().decode("utf-8")
        except OSError:
            if attempt == 2:
                raise
            time.sleep(attempt + 1)


def releases(xml):
    values = []
    for item in ET.fromstring(xml).findall("./versioning/versions/version"):
        if item.text and VERSION.fullmatch(item.text):
            values.append(item.text)
    return sorted(set(values), key=version_key, reverse=True)


def resolve_versions(read=fetch):
    quarkus = next(v for v in releases(read("io/quarkus/platform/quarkus-bom/maven-metadata.xml"))
                   if stable(v) and version_key(v)[0][0] == 4)
    mcp = next(v for v in releases(read("io/quarkiverse/mcp/quarkus-mcp-server-http/maven-metadata.xml")) if stable(v))
    candidates = [v for v in releases(read("io/quarkiverse/langchain4j/quarkus-langchain4j-bom/maven-metadata.xml"))
                  if version_key(v)[0][0] >= 2]
    # Prefer a stable compatible extension, even when a newer preview exists.
    candidates = [v for v in candidates if stable(v)] + [v for v in candidates if not stable(v)]
    for candidate in candidates:
        pom = ET.fromstring(read(f"io/quarkiverse/langchain4j/quarkus-langchain4j-parent/{candidate}/quarkus-langchain4j-parent-{candidate}.pom"))
        baseline = pom.find("{*}properties/{*}quarkus.version")
        if baseline is not None and VERSION.fullmatch(baseline.text or ""):
            if version_key(baseline.text)[0][0] == 4 and version_key(baseline.text) <= version_key(quarkus):
                return {"quarkus": quarkus, "mcp_server": mcp, "langchain4j": candidate,
                        "langchain4j_quarkus_baseline": baseline.text, "uses_prerelease": not stable(candidate)}
    raise ValueError("No released LangChain4j extension targeting the selected Quarkus 4 family was found")


def prepare(migration_root, versions, output):
    if migration_root.resolve() == Path(__file__).resolve().parents[1]:
        raise ValueError("Use a separate migration checkout; refusing to modify the monitor checkout")
    candidates = []
    for project in PROJECTS:
        pom = migration_root / project / "pom.xml"
        text = pom.read_text()
        platform = ET.fromstring(text).find("{*}properties/{*}quarkus.platform.version")
        if platform is None or not platform.text.startswith("4."):
            raise ValueError(f"{project} is not a Quarkus 4 migration checkout; refusing to change it")
        for tag, value in (("quarkus.platform.version", versions["quarkus"]),
                           ("quarkus-langchain4j.version", versions["langchain4j"])):
            text, count = re.subn(rf"(<{re.escape(tag)}>)[^<]+(</{re.escape(tag)}>)",
                                  lambda m: m[1] + value + m[2], text)
            if count != 1:
                raise ValueError(f"Expected one {tag} in {project}")
        text, count = re.subn(r"(<artifactId>quarkus-mcp-server-http</artifactId>\s*<version>)[^<]+(</version>)",
                              lambda m: m[1] + versions["mcp_server"] + m[2], text)
        if count != 1:
            raise ValueError(f"Expected one explicit MCP Server dependency in {project}")
        if list((migration_root / project / "target/surefire-reports").glob("TEST-*.xml")):
            raise ValueError(f"Use a fresh migration checkout: stale test reports exist in {project}")
        candidates.append((pom, text, project))
    # Validate both projects before writing either candidate POM.
    for pom, text, project in candidates:
        pom.write_text(text)
        (output / (project.replace("/", "-") + "-pom.xml")).write_text(text)


def test_counts(project, destination):
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    evidence_test_passed = False
    reports = list((project / "target/surefire-reports").glob("TEST-*.xml"))
    destination.mkdir(parents=True, exist_ok=True)
    for path in reports:
        suite = ET.parse(path).getroot()
        values = {key: int(suite.get(key, "0")) for key in totals}
        for key, value in values.items():
            totals[key] += value
        if suite.get("name", "").endswith("DeploymentEvidenceClientTest"):
            evidence_test_passed = values["tests"] > 0 and all(values[key] == 0 for key in ("failures", "errors", "skipped"))
        shutil.copy2(path, destination / path.name)
    return totals, evidence_test_passed


def classify(returncode, log, counts, evidence_passed):
    if returncode == 0:
        if counts["tests"] > 0 and counts["failures"] == counts["errors"] == counts["skipped"] == 0 and evidence_passed:
            return "compatible"
        return "error"  # A green Maven exit with missing/skipped tests is not compatibility evidence.
    if returncode == 124 or any(term in log for term in (
            "Could not resolve host", "Could not transfer artifact", "Could not resolve dependencies",
            "Could not find a valid Docker environment", "Could not connect to Ryuk")):
        return "error"
    if any(term in log for term in BLOCKERS):
        return "blocked"
    return "regression"


def probe_project(root, project, output, runner=subprocess.run):
    name = project.replace("/", "-")
    log_path = output / f"{name}.log"
    environment = dict(os.environ, OPENAI_API_KEY="scripted-readiness-key",
                       QUARKUS_LANGCHAIN4J_OPENAI_BASE_URL="http://127.0.0.1:1/v1")
    command = ["./mvnw", "-B", "-ntp", "-U", "test"]
    with log_path.open("w") as log:
        try:
            completed = runner(command, cwd=root / project, env=environment,
                               stdout=log, stderr=subprocess.STDOUT, timeout=900, check=False)
            code = completed.returncode
        except subprocess.TimeoutExpired:
            code = 124
            log.write("\nReadiness test timed out after 900 seconds\n")
    counts, evidence_passed = test_counts(root / project, output / name / "surefire-reports")
    status = classify(code, log_path.read_text(), counts, evidence_passed)
    return {"project": project, "status": status, "maven_exit_code": code,
            "counts": counts, "evidence_discovery_test_passed": evidence_passed, "log": log_path.name}


def aggregate(projects):
    for status in ("error", "regression", "blocked"):
        if any(project["status"] == status for project in projects):
            return status
    return "compatible" if len(projects) == len(PROJECTS) else "error"


def publish(report, output):
    (output / "report.json").write_text(json.dumps(report, indent=2) + "\n")
    lines = [f"## Quarkus 4 MCP compatibility: {report['status'].upper()}", "",
             "This checks the Part 2 MCP blocker. It does not approve the full workshop upgrade or merge PR #418.", "",
             f"Migration commit: `{report.get('migration_sha', 'not supplied')}`", ""]
    for key, value in report.get("versions", {}).items():
        lines.append(f"- {key}: `{value}`")
    if report.get("versions", {}).get("uses_prerelease"):
        lines += ["", "A release-candidate/preview extension is in use; passing tests do not establish a stable release stack."]
    for project in report.get("projects", []):
        lines += ["", f"- **{project['project']}: {project['status'].upper()}** — {project['counts']}; log: `{project['log']}`"]
    if report.get("error"):
        lines += ["", f"Monitor error: {report['error']}"]
    lines += ["", "Download the workflow artifact for candidate POMs, Maven logs, versions, and JUnit XML reports.", ""]
    summary = "\n".join(lines)
    (output / "summary.md").write_text(summary)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as target:
            target.write(summary)
    print(summary)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("resolve", "probe"))
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--migration-root", type=Path)
    parser.add_argument("--migration-sha", default="not supplied")
    parser.add_argument("--allow-known-blocker", action="store_true")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    report = {"status": "error", "checked_at": datetime.now(timezone.utc).isoformat(),
              "migration_sha": args.migration_sha, "projects": []}
    try:
        if args.mode == "resolve":
            versions = resolve_versions()
            (args.output / "versions.json").write_text(json.dumps(versions, indent=2) + "\n")
            print(json.dumps(versions, indent=2))
            return 0
        if args.migration_root is None:
            raise ValueError("--migration-root is required for probe")
        report["versions"] = json.loads((args.output / "versions.json").read_text())
        prepare(args.migration_root, report["versions"], args.output)
        for project in PROJECTS:
            report["projects"].append(probe_project(args.migration_root, project, args.output))
        report["status"] = aggregate(report["projects"])
    except Exception as failure:
        report["error"] = f"{type(failure).__name__}: {failure}"
    publish(report, args.output)
    if report["status"] == "compatible" or (args.allow_known_blocker and report["status"] == "blocked"):
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
