"use strict";
const $ = (id) => document.getElementById(id);
const node = (tag, text, cls) => {
  const e = document.createElement(tag);
  if (text !== undefined) e.textContent = text;
  if (cls) e.className = cls;
  return e;
};
async function api(path, body) {
  const response = await fetch(
    "/api" + path,
    body === undefined
      ? {}
      : {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        },
  );
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`HTTP ${response.status}: ${text.slice(0, 400)}`);
  }
  return response.json();
}
function notice(text, error = false) {
  $("notice").textContent = text;
  $("notice").className = error ? "error" : "success";
}
async function action(button, task) {
  button.disabled = true;
  try {
    await task();
  } catch (e) {
    notice(e.message, true);
  } finally {
    button.disabled = false;
  }
}
function renderProposal(target, p) {
  target.replaceChildren();
  target.append(node("span", p.action, "badge"), node("p", p.rationale));
  for (const [title, items] of [
    ["Report evidence", p.evidence],
    ["Operator verification to perform", p.verificationChecks],
  ]) {
    target.append(node("h3", title));
    const list = node("ul");
    for (const text of items || []) list.append(node("li", text));
    target.append(list);
  }
  target.append(
    node("p", "Human approval required: " + p.requiresApproval, "muted"),
  );
}
for (const button of document.querySelectorAll("[data-tab]"))
  button.onclick = () => {
    for (const other of document.querySelectorAll("[data-tab]"))
      other.setAttribute("aria-selected", String(other === button));
    for (const section of document.querySelectorAll(".workspace"))
      section.hidden = section.id !== button.dataset.tab;
  };
$("preview").onclick = () =>
  action($("preview"), async () => {
    const p = await api("/preview", {
      priority: $("priority").value,
      report: $("report").value,
    });
    renderProposal($("preview-result"), p);
    notice(
      "Recommendation passed the contract and exact-evidence checks. Review its meaning before approval.",
    );
  });
$("new-id").onclick = () => {
  $("request-id").value = crypto.randomUUID();
};
$("new-id").click();
$("queue").onclick = () =>
  action($("queue"), async () => {
    const r = await api("/responses", {
      requestId: $("request-id").value,
      incidentId: Number($("incident").value),
      report: $("response-report").value,
    });
    notice(`HTTP 202 · ${r.id} · ${r.status}`);
    await loadResponses();
  });
async function loadResponses() {
  const rows = await api("/responses");
  $("responses").replaceChildren();
  if (!rows.length) {
    $("responses").textContent = "No responses yet.";
    return;
  }
  for (const r of rows) {
    const card = node("div", undefined, "result");
    card.append(
      node("span", r.status, "badge"),
      node("h3", r.id),
      node(
        "p",
        `Incident #${r.incidentId} · Planning attempts: ${r.attempts}`,
        "muted",
      ),
    );
    if (r.proposal) {
      const p = node("div");
      renderProposal(p, JSON.parse(r.proposal));
      card.append(p, node("p", "Proposal SHA-256: " + r.proposalHash, "muted"));
    }
    const details = node("details"),
      summary = node("summary", "Audit history");
    details.append(summary, node("pre", r.audit));
    card.append(details);
    if (r.status === "WAITING_APPROVAL") {
      const buttons = node("div", undefined, "actions");
      for (const approve of [true, false]) {
        const b = node(
          "button",
          approve ? "Approve proposal" : "Reject proposal",
          approve ? "primary" : "",
        );
        b.onclick = () =>
          action(b, async () => {
            await api(`/responses/${r.id}/decision`, {
              proposalHash: r.proposalHash,
              approve,
            });
            notice("Decision saved. No infrastructure changes were executed.");
            await loadResponses();
          });
        buttons.append(b);
      }
      card.append(buttons);
    }
    if (["FAILED", "INTERRUPTED"].includes(r.status)) {
      const retry = node("button", "Retry planning with this request ID");
      retry.onclick = () =>
        action(retry, async () => {
          await api(`/responses/${r.id}/retry`, {});
          await loadResponses();
        });
      card.append(retry);
    }
    $("responses").append(card);
  }
}
$("refresh").onclick = () => action($("refresh"), loadResponses);
async function loadEvaluations() {
  const runs = await api("/evaluations");
  $("evaluations").replaceChildren();
  if (!runs.length) {
    $("evaluations").textContent = "No completed runs yet.";
    return;
  }
  for (const run of runs) {
    const card = node("div", undefined, "result");
    card.append(
      node("h3", `${run.label} · Run #${run.id}`),
      node(
        "p",
        `${run.rubric} · ${run.model} · ${run.elapsedMillis} ms`,
        "muted",
      ),
      node("p", "Dataset: " + run.datasetHash, "muted"),
      node("p", "Skill revision: " + run.skillHash, "muted"),
    );
    for (const sample of JSON.parse(run.results)) {
      const details = node("details");
      details.append(
        node(
          "summary",
          `${sample.id} · contract ${sample.contractPass ? "PASS" : "FAIL"} · evidence ${sample.grounded ? "PASS" : "FAIL"} · action ${sample.expectedAction ? "PASS" : "FAIL"}`,
        ),
        node(
          "p",
          `Judge: groundedness ${sample.judge.groundedness}/5, actionability ${sample.judge.actionability}/5`,
        ),
        node("p", sample.judge.reason),
      );
      const output = node("div");
      renderProposal(output, sample.output);
      details.append(output);
      card.append(details);
    }
    $("evaluations").append(card);
  }
}
$("run-evaluation").onclick = () =>
  action($("run-evaluation"), async () => {
    notice(
      "Live evaluation running. Keep this page open; model calls may take a minute.",
    );
    await api("/evaluations", { label: $("run-label").value });
    await loadEvaluations();
    notice(
      "Completed evaluation saved. Compare outputs and judge reasons with the same dataset, rubric, and model.",
    );
  });
$("refresh-evaluations").onclick = () =>
  action($("refresh-evaluations"), loadEvaluations);
async function init() {
  try {
    for (const i of await api("/incidents")) {
      const option = node(
        "option",
        `#${i.id} ${i.system} / ${i.service} (${i.priority})`,
      );
      option.value = i.id;
      $("incident").append(option);
    }
    await loadResponses();
    await loadEvaluations();
  } catch (e) {
    notice(e.message, true);
  }
}
init();
// Poll saved state only. Polling never invokes the model or retries a request.
setInterval(() => {
  if (!$("respond").hidden) loadResponses().catch(() => {});
}, 4000);
