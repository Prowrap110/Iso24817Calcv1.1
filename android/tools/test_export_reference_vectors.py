import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from export_reference_vectors import export


class ExportReferenceVectorsTest(unittest.TestCase):
    def test_schema_and_boundary_cases(self):
        with tempfile.TemporaryDirectory() as tmp:
            output = Path(tmp) / "vectors.json"
            export(output)
            vectors = json.loads(output.read_text())
        self.assertGreaterEqual(len(vectors), 6)
        for vector in vectors:
            self.assertTrue({"name", "inputs", "expected", "expectedErrors"}.issubset(vector))
        baseline = next(v for v in vectors if v["name"] == "baseline_external_corrosion_300mm")
        self.assertEqual(baseline["inputs"]["cloth_width_mm"], 300.0)
        invalid = next(v for v in vectors if v["name"] == "invalid_width_equal_overlap")
        self.assertTrue(any("cloth width" in error.lower() for error in invalid["expectedErrors"]))


if __name__ == "__main__":
    unittest.main()
