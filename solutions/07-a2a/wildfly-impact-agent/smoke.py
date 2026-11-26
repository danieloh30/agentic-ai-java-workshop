#!/usr/bin/env python3
"""Real WildFly/CDI/A2A round trip with a local scripted OpenAI endpoint; no API key needed."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

ROOT = Path(__file__).resolve().parent
requests = []


class ScriptedModel(BaseHTTPRequestHandler):
    def do_POST(self):
        payload = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
        requests.append(payload)
        result = {'id': 'local-impact', 'object': 'chat.completion', 'created': 1,
                  'model': 'gpt-4o', 'choices': [{'index': 0, 'finish_reason': 'stop',
                  'message': {'role': 'assistant', 'content':
                  'Estimated Impact: $36,000\nJustification: Revenue-critical checkout outage. Duration is assumed; P1 SLA is at risk.'}}],
                  'usage': {'prompt_tokens': 10, 'completion_tokens': 10, 'total_tokens': 20}}
        body = json.dumps(result).encode()
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *_):
        pass


def main():
    engine = os.environ.get('CONTAINER_ENGINE')
    if not engine:
        engine = next((e for e in ('podman', 'docker') if shutil.which(e)
                       and subprocess.run([e, 'info'], stdout=subprocess.DEVNULL,
                                          stderr=subprocess.DEVNULL).returncode == 0), None)
    if not engine:
        raise SystemExit('Start Podman or Docker first.')
    server = ThreadingHTTPServer(('0.0.0.0', 0), ScriptedModel)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    host = 'host.containers.internal' if engine == 'podman' else 'host.docker.internal'
    env = dict(os.environ, CONTAINER_ENGINE=engine, OPENAI_API_KEY='local-scripted-key',
               OPENAI_BASE_URL=f'http://{host}:{server.server_port}/v1',
               WF_CONTAINER_NAME=f'apex-wildfly-impact-smoke-{os.getpid()}', WF_PORT=os.environ.get('WF_SMOKE_PORT', '18889'))
    try:
        subprocess.run(['bash', str(ROOT / 'run.sh'), 'up'], env=env, cwd=ROOT, check=True)
        subprocess.run([str(ROOT.parent / 'multi-agent-system/mvnw'), '-B', '-ntp', '-f', str(ROOT / 'pom.xml'),
                        'verify', f'-Dwildfly.url=http://localhost:{env["WF_PORT"]}/'], env=env, cwd=ROOT, check=True)
        # Use the existing Quarkus caller's full dependency graph as a second compatibility check.
        subprocess.run([str(ROOT.parent / 'multi-agent-system/mvnw'), '-B', '-ntp', '-f',
                        str(ROOT.parent / 'multi-agent-system/pom.xml'), 'test', '-Dtest=WildFlyInteropTest',
                        f'-Dwildfly.url=http://localhost:{env["WF_PORT"]}/'], env=env, cwd=ROOT, check=True)
        assert requests, 'The WildFly CDI agent never called the scripted model'
        prompt = json.dumps(requests[-1]['messages'])
        for expected in ('Apex Systems', 'checkout-api', 'payment-gateway', 'P1', 'Customer checkout is unavailable.'):
            assert expected in prompt, f'Model prompt lost {expected}'
        print('PASS: WildFly CDI agent, multiline inputs, model call, and LangChain4j A2A client interoperated.')
    except Exception:
        log_path = ROOT / 'target/smoke-server.log'
        with log_path.open('w') as log:
            subprocess.run([engine, 'logs', env['WF_CONTAINER_NAME']], stdout=log, stderr=subprocess.STDOUT)
        print(f'WildFly diagnostics: {log_path}')
        raise
    finally:
        subprocess.run([engine, 'stop', env['WF_CONTAINER_NAME']], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        server.shutdown()
        server.server_close()


if __name__ == '__main__':
    main()
