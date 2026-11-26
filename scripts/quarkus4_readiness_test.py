import json
from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

import quarkus4_readiness as monitor


def metadata(*versions):
    return '<metadata><versioning><versions>' + ''.join(f'<version>{v}</version>' for v in versions) + '</versions></versioning></metadata>'


class ReadinessTest(unittest.TestCase):
    def resolver(self, extensions, baselines):
        def read(path):
            if path.startswith('io/quarkus/platform/'):
                return metadata('3.40.1.2', '4.0.1', '4.1.0.CR1', '5.0.0')
            if path.startswith('io/quarkiverse/mcp/'):
                return metadata('2.0.4', '2.1.0', '2.2.0.CR1')
            if path.endswith('maven-metadata.xml'):
                return metadata(*extensions)
            version = path.split('/')[-2]
            return f'<project><properties><quarkus.version>{baselines[version]}</quarkus.version></properties></project>'
        return monitor.resolve_versions(read)

    def test_selects_latest_stable_compatible_stack_not_quarkus5_or_preview(self):
        result = self.resolver(['2.0.0.CR1', '2.0.0', '2.0.1', '2.1.0.Beta1', '3.0.0'],
                               {'2.0.0': '4.0.0', '2.0.1': '4.0.0', '3.0.0': '5.0.0'})
        self.assertEqual(('4.0.1', '2.1.0', '2.0.1'),
                         (result['quarkus'], result['mcp_server'], result['langchain4j']))
        self.assertFalse(result['uses_prerelease'])

    def test_preview_is_explicit_when_no_stable_quarkus4_extension_exists(self):
        result = self.resolver(['1.15.1', '2.0.0.CR1'], {'2.0.0.CR1': '4.0.0'})
        self.assertEqual('2.0.0.CR1', result['langchain4j'])
        self.assertTrue(result['uses_prerelease'])

    def test_no_compatible_extension_is_an_error(self):
        with self.assertRaisesRegex(ValueError, 'No released'):
            self.resolver(['3.0.0'], {'3.0.0': '5.0.0'})

    def test_missing_or_skipped_evidence_tests_cannot_report_compatible(self):
        good = {'tests': 23, 'failures': 0, 'errors': 0, 'skipped': 0}
        self.assertEqual('compatible', monitor.classify(0, '', good, True))
        self.assertEqual('error', monitor.classify(0, '', good, False))
        self.assertEqual('error', monitor.classify(0, '', dict(good, skipped=1), True))
        self.assertEqual('error', monitor.classify(0, '', dict(good, tests=0), True))

    def test_known_blocker_does_not_hide_network_errors_or_new_regressions(self):
        counts = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
        marker = 'Caused by: java.lang.ClassNotFoundException: io.quarkus.jackson.ObjectMapperCustomizer'
        self.assertEqual('blocked', monitor.classify(1, marker, counts, False))
        self.assertEqual('error', monitor.classify(1, marker + '\nCould not transfer artifact', counts, False))
        self.assertEqual('regression', monitor.classify(1, 'AssertionFailedError: protocol mismatch', counts, False))
        self.assertEqual('error', monitor.classify(124, '', counts, False))

    def test_one_passing_project_does_not_clear_the_blocker(self):
        self.assertEqual('blocked', monitor.aggregate([{'status': 'compatible'}, {'status': 'blocked'}]))
        self.assertEqual('error', monitor.aggregate([{'status': 'compatible'}]))
        self.assertEqual('compatible', monitor.aggregate([{'status': 'compatible'}, {'status': 'compatible'}]))

    def test_refuses_v3_checkout_before_modifying_either_pom(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            original = '<project><properties><quarkus.platform.version>3.40.1.2</quarkus.platform.version></properties></project>'
            for project in monitor.PROJECTS:
                path = root / project / 'pom.xml'
                path.parent.mkdir(parents=True)
                path.write_text(original)
            with self.assertRaisesRegex(ValueError, 'refusing'):
                monitor.prepare(root, {}, root)
            self.assertTrue(all((root / project / 'pom.xml').read_text() == original for project in monitor.PROJECTS))

    def test_refuses_monitor_checkout(self):
        with tempfile.TemporaryDirectory() as folder:
            with self.assertRaisesRegex(ValueError, 'monitor checkout'):
                monitor.prepare(Path(monitor.__file__).resolve().parents[1], {}, Path(folder))

    def test_suite_reports_must_include_successful_real_mcp_discovery(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            reports = root / 'target/surefire-reports'
            reports.mkdir(parents=True)
            (reports / 'TEST-evidence.xml').write_text('<testsuite name="com.incidentmanagement.evidence.DeploymentEvidenceClientTest" tests="3" failures="0" errors="0" skipped="0"/>')
            counts, evidence = monitor.test_counts(root, root / 'archive')
            self.assertTrue(evidence)
            self.assertEqual(3, counts['tests'])
            self.assertTrue((root / 'archive/TEST-evidence.xml').exists())

    def test_both_projects_are_probed_even_when_first_is_blocked(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / 'versions.json').write_text(json.dumps({'quarkus': '4.0.1'}))
            def probe(_, project, __):
                return {'project': project, 'status': 'blocked', 'counts': {}, 'log': 'fixture.log'}
            with patch.object(monitor, 'prepare'), patch.object(monitor, 'probe_project', side_effect=probe) as calls:
                with patch('sys.argv', ['monitor', 'probe', '--migration-root', str(root / 'migration'),
                                        '--output', str(root), '--allow-known-blocker']):
                    with redirect_stdout(StringIO()):
                        self.assertEqual(0, monitor.main())
                self.assertEqual(list(monitor.PROJECTS), [call.args[1] for call in calls.call_args_list])
            report = json.loads((root / 'report.json').read_text())
            self.assertEqual('blocked', report['status'])
            self.assertEqual(2, len(report['projects']))


if __name__ == '__main__':
    unittest.main()
