/* Render outside Material's Mermaid shadow roots so diagrams can expand and
   retain the same paper palette in light and dark site modes. */
(() => {
  let sequence = 0;
  let pending = Promise.resolve();

  mermaid.initialize({
    startOnLoad: false,
    securityLevel: "strict",
    look: "handDrawn",
    theme: "base",
    themeVariables: { fontFamily: "Roboto, sans-serif", lineColor: "#4A4035" },
    flowchart: { htmlLabels: false, curve: "basis" },
  });

  function addFrame(element, source) {
    const frame = document.createElement("div");
    frame.className = "workshop-diagram";
    frame.dataset.direction = /(?:flowchart|graph)\s+(LR|RL)/.test(source)
      || /sequenceDiagram/.test(source) ? "horizontal" : "vertical";
    element.before(frame);
    const toolbar = document.createElement("div");
    toolbar.className = "workshop-diagram-toolbar";
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = "Expand diagram";
    button.setAttribute("aria-expanded", "false");
    button.setAttribute("aria-controls", element.id);
    toolbar.append(button);
    frame.append(toolbar, element);

    const dialog = document.createElement("dialog");
    dialog.className = "workshop-diagram-dialog";
    dialog.setAttribute("aria-label", "Expanded diagram");
    frame.after(dialog);
    let returnFocus;
    function close() {
      if (!dialog.open) return;
      dialog.close();
    }
    dialog.addEventListener("close", () => {
      frame.append(toolbar, element);
      button.textContent = "Expand diagram";
      button.setAttribute("aria-expanded", "false");
      returnFocus?.focus({ preventScroll: true });
    });
    dialog.addEventListener("click", event => {
      if (event.target === dialog) close();
    });
    button.addEventListener("click", () => {
      if (dialog.open) return close();
      returnFocus = document.activeElement;
      dialog.append(toolbar, element);
      dialog.dataset.direction = frame.dataset.direction;
      button.textContent = "Close diagram";
      button.setAttribute("aria-expanded", "true");
      dialog.showModal();
      button.focus();
    });
  }

  async function renderDiagrams() {
    for (const element of document.querySelectorAll(".workshop-mermaid:not([data-rendered])")) {
      if (!element.isConnected) continue;
      const source = element.textContent;
      const id = `workshop-diagram-${++sequence}`;
      element.id = `${id}-content`;
      element.dataset.rendered = "pending";
      try {
        const result = await mermaid.render(id, source);
        if (!element.isConnected) continue;
        element.innerHTML = result.svg;
        element.dataset.rendered = "true";
        result.bindFunctions?.(element);
        addFrame(element, source);
      } catch (error) {
        element.textContent = "Diagram could not be rendered. Reload this page to retry.";
        element.classList.add("workshop-diagram-error");
        element.setAttribute("role", "alert");
        console.error("Unable to render diagram", error);
      }
    }
  }

  function schedule() {
    pending = pending.catch(console.error).then(renderDiagrams);
  }
  if (typeof document$ !== "undefined") document$.subscribe(schedule);
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", schedule, { once: true });
  } else schedule();
})();
