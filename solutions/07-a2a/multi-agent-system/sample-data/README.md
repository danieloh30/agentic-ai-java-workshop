# Sample data — log screenshot for the vision upload

The incident detail panel has a **Choose File** input (`accept="image/*"`). When you
attach an image and process an incident, the file is base64-encoded and sent to the
multimodal model (`gpt-4o`) via `IncidentLogAnalysisAgent`, which reads the image and
folds its observations into the incident report before triage/impact/resolution run.

## Try it

1. Start the remote A2A agent (`../remote-a2a-agent`, port 8888) and this app (port 8080).
2. Open http://localhost:8080 and select incident **#1** —
   `payment-gateway / checkout-api`, *"Intermittent 503 errors during peak hours"* (OPEN).
3. Click **Choose File**, pick `checkout-api-503-incident.png`, then process the incident.
4. Watch the report get enriched with what the model sees in the screenshot
   (HikariCP pool exhaustion, upstream payment-processor timeouts, circuit breaker OPEN,
   503s under peak load) — which then drives the downstream escalation decision.

## Files

- `checkout-api-503-incident.png` — a mock observability log console (real, readable text
  so the vision model can actually parse it). Matches incident #1.
- `checkout-api-503-incident.html` — the source used to render the PNG. Re-render with:

  ```bash
  "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
    --headless --disable-gpu --hide-scrollbars --force-device-scale-factor=2 \
    --window-size=1332,565 \
    --screenshot=checkout-api-503-incident.png \
    "file://$PWD/checkout-api-503-incident.html"
  ```

Edit the HTML (service name, error lines, metrics) to create screenshots for other
incidents in `import.sql`.
