import importlib.util
import json
import subprocess
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory


MODULE_PATH = Path(__file__).with_name("inspect-body-models.py")
SPEC = importlib.util.spec_from_file_location("inspect_body_models", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class InspectBodyModelsTest(unittest.TestCase):
    def run_verifier(self, entries):
        with TemporaryDirectory() as directory:
            repo = Path(directory)
            manifest_path = repo / "app/src/main/assets/body/model-manifest.json"
            manifest_path.parent.mkdir(parents=True)
            manifest_path.write_text(json.dumps({"entries": entries}))
            result = subprocess.run(
                ["python3", str(MODULE_PATH), str(repo)],
                capture_output=True,
                text=True,
                check=False,
            )
        self.assertNotEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(result.stderr, "", result.stderr)
        report = json.loads(result.stdout)
        self.assertFalse(report["valid"])
        self.assertEqual(report["authorization"]["releaseGate"], "blocked")
        return report

    def test_malformed_manifest_entries_emit_blocked_json_without_traceback(self):
        for entries in (None, [None], ["not an object"]):
            with self.subTest(entries=entries):
                report = self.run_verifier(entries)
                self.assertTrue(report["errors"])

    def test_non_integer_manifest_bytes_emit_blocked_json_without_traceback(self):
        report = self.run_verifier([
            {
                "gender": "MALE",
                "assetPath": "body/male/body.glb",
                "sha256": "0" * 64,
                "bytes": "730028",
            }
        ])
        self.assertTrue(any("bytes" in error for error in report["errors"]))

    def test_asset_paths_reject_empty_dot_parent_backslash_and_scheme_segments(self):
        invalid_paths = [
            "body//male.glb",
            "body/./male.glb",
            "body/../male.glb",
            "body\\male.glb",
            "https://example.test/male.glb",
            "http:body/male.glb",
        ]

        for path in invalid_paths:
            self.assertFalse(MODULE.is_relative_asset_path(path), path)

    def test_asset_paths_accept_normalized_relative_path(self):
        self.assertTrue(MODULE.is_relative_asset_path("body/male/body.glb"))
