const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const { test } = require("node:test");
const { runInNewContext } = require("node:vm");

function viewer() {
  let navigate;
  const classes = new Set();
  const document = {
    baseURI: "https://workshop.example/exercise/",
    listeners: {},
    body: {
      append(element) { document.dialog = element; },
      classList: { add: (name) => classes.add(name), remove: (name) => classes.delete(name) },
    },
    addEventListener(name, listener) { this.listeners[name] = listener; },
    createElement(tag) {
      return {
        tag, children: [], listeners: {}, open: false,
        append(...elements) { this.children.push(...elements); },
        setAttribute() {},
        removeAttribute(name) { delete this[name]; },
        addEventListener(name, listener) { this.listeners[name] = listener; },
        showModal() { this.open = true; },
        close() { this.open = false; this.listeners.close(); },
        focus() { document.activeElement = this; },
      };
    },
  };
  runInNewContext(readFileSync(resolve(__dirname, "../docs/javascripts/images.js"), "utf8"), {
    document, URL, location: { origin: "https://workshop.example" },
    document$: { subscribe(callback) { navigate = callback; } },
  });
  function click(href = "https://workshop.example/images/result.png", options = {}) {
    const thumbnail = { alt: "Example recommendation" };
    const link = {
      href, isConnected: true,
      querySelector: () => thumbnail,
      hasAttribute: () => false,
      focus() { document.activeElement = this; },
    };
    const event = {
      button: 0, defaultPrevented: false,
      target: { closest: () => link },
      preventDefault() { this.defaultPrevented = true; },
      ...options,
    };
    document.listeners.click(event);
    return { event, link };
  }
  const dialog = document.dialog;
  return { document, dialog, click, classes, navigate: () => navigate(),
    closeButton: dialog.children[0].children[1],
    image: dialog.children[1].children[0] };
}

test("screenshot opens in place; close clears the image and restores focus", () => {
  const v = viewer();
  const { event, link } = v.click();
  assert.equal(event.defaultPrevented, true);
  assert.equal(v.dialog.open, true);
  assert.equal(v.image.src, link.href);
  assert.equal(v.image.alt, "Example recommendation");
  assert.equal(v.document.activeElement, v.closeButton);
  assert.equal(v.classes.has("workshop-image-open"), true);
  v.closeButton.listeners.click();
  assert.equal(v.dialog.open, false);
  assert.equal(v.image.src, undefined);
  assert.equal(v.document.activeElement, link);
  assert.equal(v.classes.has("workshop-image-open"), false);
});

test("clicking outside closes; clicking the image keeps the viewer open", () => {
  const v = viewer();
  v.click();
  v.dialog.listeners.click({ target: v.image });
  assert.equal(v.dialog.open, true);
  v.dialog.listeners.click({ target: v.dialog });
  assert.equal(v.dialog.open, false);
});

test("normal links and modified screenshot clicks retain browser behavior", () => {
  const v = viewer();
  for (const option of ["ctrlKey", "metaKey", "shiftKey", "altKey"]) {
    assert.equal(v.click(undefined, { [option]: true }).event.defaultPrevented, false);
  }
  for (const href of ["https://other.example/image.png", "https://workshop.example/next/"]) {
    assert.equal(v.click(href).event.defaultPrevented, false);
  }
  assert.equal(v.dialog.open, false);
});

test("instant navigation closes the viewer and screenshots still open afterward", () => {
  const v = viewer();
  const { link } = v.click();
  link.isConnected = false;
  v.navigate();
  assert.equal(v.dialog.open, false);
  assert.equal(v.classes.has("workshop-image-open"), false);
  v.click("https://workshop.example/images/next.png");
  assert.equal(v.dialog.open, true);
  assert.match(v.image.src, /next.png$/);
});
