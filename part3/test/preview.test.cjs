const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const { test } = require("node:test");
const { runInNewContext } = require("node:vm");

class Element {
  children = [];
  className = "";
  value = "";
  disabled = false;
  text = "";
  set textContent(value) {
    this.children = [];
    this.text = value;
  }
  get textContent() {
    return this.text + this.children.map((child) => child.textContent).join(" ");
  }
  replaceChildren() {
    this.children = [];
    this.text = "";
  }
  append(...children) {
    this.children.push(...children);
  }
  click() {
    return this.onclick();
  }
}

for (const variant of ["lab", "solution"]) {
  test(`${variant}: failed P1 preview clears P2 result and allows a successful retry`, async () => {
    const elements = new Map();
    const get = (id) => {
      if (!elements.has(id)) elements.set(id, new Element());
      return elements.get(id);
    };
    let finishRequest;
    const requests = [];
    runInNewContext(
      readFileSync(resolve(__dirname, `../${variant}/src/main/resources/META-INF/resources/app.js`), "utf8"),
      {
        document: { getElementById: get, createElement: () => new Element(), querySelectorAll: () => [] },
        crypto: { randomUUID: () => "test-request" },
        setInterval: () => {},
        fetch: async (url, options) => {
          if (url !== "/api/preview") return { ok: true, json: async () => [] };
          requests.push(JSON.parse(options.body));
          return new Promise((resolve) => { finishRequest = resolve; });
        },
      },
    );
    // Let initial dashboard loads finish before simulating a user's request.
    await new Promise(setImmediate);
    get("preview-result").textContent = "INVESTIGATE: Previous checkout evidence";
    get("notice").textContent = "Previous request passed";
    get("priority").value = "P1";
    get("report").value = "Complete authentication failure.";

    const rejected = get("preview").click();
    assert.equal(get("preview").disabled, true);
    assert.match(get("preview-result").textContent, /Generating/);
    assert.doesNotMatch(get("preview-result").textContent, /checkout/);
    assert.equal(get("notice").textContent, "");
    assert.deepEqual(requests[0], { priority: "P1", report: "Complete authentication failure." });
    finishRequest({ ok: false, status: 422, text: async () => "Recommendation rejected: evidence must be copied exactly from the operator report." });
    await rejected;
    assert.match(get("notice").textContent, /HTTP 422: Recommendation rejected/);
    assert.equal(get("notice").className, "error");
    assert.match(get("preview-result").textContent, /No recommendation accepted/);
    assert.doesNotMatch(get("preview-result").textContent, /checkout|INVESTIGATE/);
    assert.equal(get("preview").disabled, false);

    const retry = get("preview").click();
    finishRequest({ ok: true, json: async () => ({ action: "ESCALATE", rationale: "Authentication failure", evidence: ["Complete authentication failure."], verificationChecks: ["Check synthetic logins"], requiresApproval: true }) });
    await retry;
    assert.match(get("preview-result").textContent, /ESCALATE/);
    assert.match(get("preview-result").textContent, /Complete authentication failure/);
    assert.doesNotMatch(get("preview-result").textContent, /checkout|No recommendation accepted/);
    assert.equal(get("preview-result").className, "");
    assert.equal(get("notice").className, "success");
    assert.equal(get("preview").disabled, false);
  });
}
