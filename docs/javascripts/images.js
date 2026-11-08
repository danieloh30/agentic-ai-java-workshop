/* Keep linked screenshots in the guide, including after Material instant navigation. */
(() => {
  const dialog = document.createElement("dialog");
  if (typeof dialog.showModal !== "function") return;
  dialog.className = "workshop-image-dialog";
  dialog.setAttribute("aria-label", "Expanded screenshot");

  const toolbar = document.createElement("div");
  toolbar.className = "workshop-image-toolbar";
  const title = document.createElement("span");
  const close = document.createElement("button");
  close.type = "button";
  close.textContent = "Close image ×";
  close.addEventListener("click", () => dialog.close());
  toolbar.append(title, close);

  const content = document.createElement("div");
  content.className = "workshop-image-content";
  const expanded = document.createElement("img");
  content.append(expanded);
  dialog.append(toolbar, content);
  document.body.append(dialog);

  let returnFocus;
  dialog.addEventListener("close", () => {
    document.body.classList.remove("workshop-image-open");
    expanded.removeAttribute("src");
    if (returnFocus?.isConnected) returnFocus.focus({ preventScroll: true });
  });
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) dialog.close();
  });

  document.addEventListener("click", (event) => {
    if (event.defaultPrevented || event.button !== 0 || event.ctrlKey ||
        event.metaKey || event.shiftKey || event.altKey) return;
    const link = event.target.closest?.(".md-content a[href]");
    const image = link?.querySelector("img");
    if (!image || link.hasAttribute("download")) return;
    const url = new URL(link.href, document.baseURI);
    if (url.origin !== location.origin || !/\.(png|jpe?g|webp|gif|avif|svg)$/i.test(url.pathname)) return;

    event.preventDefault();
    returnFocus = link;
    expanded.src = url.href;
    expanded.alt = image.alt;
    title.textContent = image.alt || "Screenshot";
    document.body.classList.add("workshop-image-open");
    dialog.showModal();
    content.scrollTop = 0;
    close.focus();
  });

  // Native dialogs handle Escape and keep keyboard focus inside the viewer.
  if (typeof document$ !== "undefined") {
    document$.subscribe(() => {
      if (dialog.open) dialog.close();
    });
  }
})();
