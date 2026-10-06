import importlib.util
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("inspect-body-models.py")
SPEC = importlib.util.spec_from_file_location("inspect_body_models", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class InspectBodyModelsTest(unittest.TestCase):
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
