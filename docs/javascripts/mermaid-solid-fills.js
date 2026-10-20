/* Adjust each SVG before insertion so solid fills retain the renderer's
   original hand-drawn outlines, including in the expanded diagram view. */
(() => {
  function install() {
    const mermaid = window.mermaid;
    if (typeof mermaid?.render !== "function" || mermaid.render.workshopSolidFills) return;

    const render = mermaid.render;
    async function renderWithSolidFills(...args) {
      const result = await render.apply(this, args);
      // Mermaid's HTML labels can contain <br> tags. Parse as HTML, just as
      // Material does, to preserve multiline labels and their following nodes.
      const container = document.createElement("div");
      container.innerHTML = result.svg;
      const svg = container.querySelector("svg");
      if (!svg) return result;

      // Rough.js emits a hatch path followed by its boundary path. Its hatch
      // stroke color is the requested fill color. Mermaid 12 can make that
      // stroke thinner than the outline, so compare colors rather than widths.
      svg.querySelectorAll(".rough-node g, .cluster[data-look='handDrawn'] > g")
        .forEach((group) => {
          const paths = Array.from(group.children).filter(
            (child) => child.localName === "path"
          );
          if (paths.length !== 2) return;
          const [hatch, outline] = paths;
          const fill = hatch.getAttribute("stroke");
          if (hatch.getAttribute("fill") !== "none" || !fill || fill === "none" ||
              outline.getAttribute("fill") !== "none" ||
              outline.getAttribute("stroke") === fill) return;

          // Each outline segment is drawn twice. Join the first trace of each
          // segment into a closed fill, keeping the original outline untouched.
          const segments = outline.getAttribute("d").match(/M[^M]*/g);
          if (!segments) return;
          const background = document.createElementNS("http://www.w3.org/2000/svg", "path");
          background.setAttribute("class", "workshop-solid-fill");
          background.setAttribute("d", segments.filter((_, index) => index % 2 === 0)
            .map((segment, index) => index === 0 ? segment : segment.replace(/^M/, "L"))
            .join(" ") + " Z");
          background.setAttribute("fill", fill);
          background.setAttribute("stroke", "none");
          group.insertBefore(background, outline);
          hatch.remove();
        });

      return { ...result, svg: svg.outerHTML };
    }
    renderWithSolidFills.workshopSolidFills = true;
    mermaid.render = renderWithSolidFills;
  }

  // Capture the dynamically loaded Mermaid script's load event before
  // Material's loader starts rendering. Also handle an already loaded script.
  document.addEventListener("load", (event) => {
    if (event.target.tagName === "SCRIPT") install();
  }, true);
  install();
})();
