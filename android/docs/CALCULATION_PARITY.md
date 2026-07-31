# Android calculation parity

The reference vectors are exported from the existing root `prowrap_calculations.calculate_repair`
function by `tools/export_reference_vectors.py`. The Android engine follows the same units and route
conditions: millimetres, MPa internally, bar at the form boundary, kilograms of epoxy, and square
metres of saturated cloth.

| Android output | Python source | Unit | Comparison |
| --- | --- | --- | --- |
| `wallLossRatio` | `wall_loss_ratio` | fraction | `1e-9` relative |
| `calcMethodThickness` | `calc_method_thick` | text | exact |
| `steelCapacityMpa` | `p_steel_capacity` | MPa | `1e-9` relative |
| `requiredThicknessMm` | `t_required` | mm | `1e-9` relative |
| `numPlies`, `numBands` | `num_plies`, `num_bands` | count | exact |
| `isoLengthMm`, `procurementLengthMm` | `iso_length`, `proc_length` | mm | `1e-9` relative |
| `optimizedSquareMeters`, `epoxyKg` | `optimized_sqm`, `epoxy_kg` | m², kg | `1e-9` relative |

The fixed qualified stitch overlap is 50 mm. `clothWidthMm` is an independent user input and is
used only for axial band procurement; changing it must not change structural ply calculations.
