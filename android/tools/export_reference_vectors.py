#!/usr/bin/env python3
"""Export deterministic reference cases from the existing Python engine.

This script imports the root calculator read-only; it never edits the existing
Streamlit/macOS source. It is run from the repository root or with an explicit
output path while preparing Android parity tests.
"""
from __future__ import annotations

import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from prowrap_calculations import calculate_repair  # noqa: E402


BASE = {
    "customer": "PROTAP",
    "location": "Turkey",
    "report_no": "24-152",
    "od": 457.2,
    "wall": 9.53,
    "pressure": 50.0,
    "temp": 40.0,
    "defect_type": "Corrosion",
    "defect_loc": "External",
    "length": 100.0,
    "rem_wall": 4.5,
    "yield_strength": 359.0,
    "design_factor": 0.72,
    "design_life": 20.0,
    "force_3_layers": False,
    "internal_corrosion_rate": 0.0,
    "installation_temp": 20.0,
    "component_type": "Straight",
    "cyclic_derating_factor": 1.0,
    "axial_load_case": 0,
    "cloth_width_mm": 300.0,
}


def case(name: str, **overrides):
    inputs = {**BASE, **overrides}
    expected_errors = []
    try:
        result = calculate_repair(**inputs)
        expected = {
            key: result[key]
            for key in (
                "calc_method_thick", "calc_method_overlap", "p_steel_capacity",
                "p_composite_design", "t_required", "num_plies", "final_thickness",
                "iso_length", "num_bands", "proc_length", "cloth_width_mm",
                "optimized_sqm", "epoxy_kg",
            )
        }
    except ValueError as exc:
        expected = {}
        expected_errors = str(exc).splitlines()
    return {"name": name, "inputs": inputs, "expected": expected, "expectedErrors": expected_errors}


def export(path: Path):
    vectors = [
        case("baseline_external_corrosion_300mm"),
        case("baseline_external_corrosion_250mm", cloth_width_mm=250.0),
        case("type_b_leak_two_year_cap", defect_type="Leak", design_life=10.0),
        case("internal_corrosion_end_of_life", defect_loc="Internal", internal_corrosion_rate=0.1),
        case("dent_type_a", defect_type="Dent"),
        case("crack_type_b", defect_type="Crack", length=25.0),
        case("invalid_width_equal_overlap", cloth_width_mm=50.0),
        case("invalid_input_negative_od", od=-1.0),
    ]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(vectors, indent=2, sort_keys=True) + "\n", encoding="utf-8")


if __name__ == "__main__":
    output = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("android/app/src/test/resources/reference_vectors.json")
    export(output)
    print(output)
