#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$REPO_ROOT"

LAB_BASE="lab/src/main/java/com/incidentmanagement/agentic"
EX08_BASE="solutions/08-quarkus-flow/lab/src/main/java/com/incidentmanagement/agentic"
EX09_BASE="solutions/09-plan-execute/lab/src/main/java/com/incidentmanagement/agentic"
EX10_BASE="solutions/10-memory-tiering/lab/src/main/java/com/incidentmanagement"
EX11_BASE="solutions/11-consensus-voting/lab/src/main/java/com/incidentmanagement/agentic"
EX12_BASE="solutions/12-event-driven/lab/src/main/java/com/incidentmanagement"

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

EX09_FILES=(
    "$EX09_BASE/agents/PlannerAgent.java"
    "$EX09_BASE/workflow/PlanExecuteFlow.java"
)

EX10_FILES=(
    "$EX10_BASE/agentic/agents/IncidentAssistant.java"
    "$EX10_BASE/memory/PersistentChatMemoryStore.java"
)

EX11_FILES=(
    "$EX11_BASE/agents/CostVoter.java"
    "$EX11_BASE/workflow/ConsensusWorkflow.java"
)

EX12_FILES=(
    "$EX12_BASE/agentic/workflow/IncidentResolutionWorkflow.java"
    "$EX12_BASE/messaging/IncidentEventConsumer.java"
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
    echo "Usage: $0 [all|lab|ex08|ex09|ex10|ex11|ex12]"
    echo ""
    echo "  all   Reset root lab (Ex 1-4) and Exercise 08, 09, 10, 11, 12 labs (default)"
    echo "  lab   Reset root lab only (Exercises 1-4)"
    echo "  ex08  Reset Exercise 08 lab only"
    echo "  ex09  Reset Exercise 09 lab only"
    echo "  ex10  Reset Exercise 10 lab only"
    echo "  ex11  Reset Exercise 11 lab only"
    echo "  ex12  Reset Exercise 12 lab only"
    exit 1
}

TARGET="${1:-all}"

echo "Resetting lab stubs to original TODO state..."
echo ""

case "$TARGET" in
    all)
        reset_files "lab/ (Exercises 1-4)" "${LAB_FILES[@]}"
        reset_files "solutions/08-quarkus-flow/lab/ (Exercise 8)" "${EX08_FILES[@]}"
        reset_files "solutions/09-plan-execute/lab/ (Exercise 9)" "${EX09_FILES[@]}"
        reset_files "solutions/10-memory-tiering/lab/ (Exercise 10)" "${EX10_FILES[@]}"
        reset_files "solutions/11-consensus-voting/lab/ (Exercise 11)" "${EX11_FILES[@]}"
        reset_files "solutions/12-event-driven/lab/ (Exercise 12)" "${EX12_FILES[@]}"
        ;;
    lab)
        reset_files "lab/ (Exercises 1-4)" "${LAB_FILES[@]}"
        ;;
    ex08)
        reset_files "solutions/08-quarkus-flow/lab/ (Exercise 8)" "${EX08_FILES[@]}"
        ;;
    ex09)
        reset_files "solutions/09-plan-execute/lab/ (Exercise 9)" "${EX09_FILES[@]}"
        ;;
    ex10)
        reset_files "solutions/10-memory-tiering/lab/ (Exercise 10)" "${EX10_FILES[@]}"
        ;;
    ex11)
        reset_files "solutions/11-consensus-voting/lab/ (Exercise 11)" "${EX11_FILES[@]}"
        ;;
    ex12)
        reset_files "solutions/12-event-driven/lab/ (Exercise 12)" "${EX12_FILES[@]}"
        ;;
    *)
        usage
        ;;
esac

cleanup_artifacts

echo ""
echo "Done. Hot reload will pick up the changes automatically."
