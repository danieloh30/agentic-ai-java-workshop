#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$REPO_ROOT"

LAB_BASE="lab/src/main/java/com/incidentmanagement/agentic"
EX08_BASE="solutions/08-quarkus-flow/lab/src/main/java/com/incidentmanagement/agentic"

LAB_FILES=(
    "$LAB_BASE/agents/TriageAgent.java"
    "$LAB_BASE/tools/TriageTool.java"
    "$LAB_BASE/agents/DiagnosticAgent.java"
    "$LAB_BASE/agents/IncidentAnalysisAgent.java"
    "$LAB_BASE/workflow/IncidentAnalysisWorkflow.java"
    "$LAB_BASE/agents/ImpactAgent.java"
    "$LAB_BASE/agents/EscalationAgent.java"
    "$LAB_BASE/agents/ResolutionAgent.java"
    "$LAB_BASE/agents/IncidentSupervisorAgent.java"
    "$LAB_BASE/workflow/IncidentProcessingWorkflow.java"
)

EX08_FILES=(
    "$EX08_BASE/agents/ReportDrafterAgent.java"
    "$EX08_BASE/agents/ReportCriticAgent.java"
    "$EX08_BASE/workflow/IncidentReportFlow.java"
)

reset_files() {
    local label="$1"
    shift
    local files=("$@")
    local count=0

    for f in "${files[@]}"; do
        git checkout origin/main -- "$f" 2>/dev/null && count=$((count + 1)) || echo "  SKIP  $f (not found on origin/main)"
    done
    git reset HEAD -- "${files[@]}" > /dev/null 2>&1 || true
    echo "  Reset $count file(s) in $label"
}

cleanup_artifacts() {
    local count=0
    while IFS= read -r -d '' f; do
        rm -f "$f" && count=$((count + 1))
    done < <(find solutions/ lab/ \( -name ".mcp.json" -o -name "AGENTS.md" -o -name "CLAUDE.md" \) -print0 | \
             grep -z -v -e "lab/AGENTS.md")
    if [ "$count" -gt 0 ]; then
        echo "  Removed $count artifact(s) (.mcp.json, AGENTS.md, CLAUDE.md)"
    fi
}

usage() {
    echo "Usage: $0 [all|lab|ex08]"
    echo ""
    echo "  all   Reset both root lab (Ex 1-4) and Exercise 08 lab (default)"
    echo "  lab   Reset root lab only (Exercises 1-4)"
    echo "  ex08  Reset Exercise 08 lab only"
    exit 1
}

TARGET="${1:-all}"

echo "Resetting lab stubs to original TODO state..."
echo ""

case "$TARGET" in
    all)
        reset_files "lab/ (Exercises 1-4)" "${LAB_FILES[@]}"
        reset_files "solutions/08-quarkus-flow/lab/ (Exercise 8)" "${EX08_FILES[@]}"
        ;;
    lab)
        reset_files "lab/ (Exercises 1-4)" "${LAB_FILES[@]}"
        ;;
    ex08)
        reset_files "solutions/08-quarkus-flow/lab/ (Exercise 8)" "${EX08_FILES[@]}"
        ;;
    *)
        usage
        ;;
esac

cleanup_artifacts

echo ""
echo "Done. Hot reload will pick up the changes automatically."
