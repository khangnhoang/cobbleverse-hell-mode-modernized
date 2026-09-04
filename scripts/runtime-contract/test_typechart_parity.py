#!/usr/bin/env python3
"""
Test Type Chart Parity between Showdown (in Cobblemon jar) and companion mod's typechart_gen9.json.

Verifies that all 18x18 = 324 directed type interactions in
companion-mod/src/main/resources/typechart_gen9.json
are 100% identical to Cobblemon's bundled Pokémon Showdown type chart.
"""

import io
import json
import os
import re
import sys
import zipfile
from pathlib import Path

STANDARD_18_TYPES = [
    "normal", "fire", "water", "grass", "electric", "ice",
    "fighting", "poison", "ground", "flying", "psychic", "bug",
    "rock", "ghost", "dragon", "dark", "steel", "fairy"
]

def find_cobblemon_jar() -> Path:
    candidates = []
    env_dir = os.environ.get("COBBLEVERSE_MODS_DIR")
    if env_dir:
        candidates.append(Path(env_dir))
    candidates.append(Path(r"c:\Users\khang\curseforge\minecraft\Instances\COBBLEVERSE - Pokemon Adventure [Cobblemon]\mods"))

    for c in candidates:
        if c.exists():
            for jar in c.glob("Cobblemon-fabric-*.jar"):
                return jar
    raise FileNotFoundError(f"Cobblemon jar not found in candidate paths: {[str(c) for c in candidates]}")

def parse_showdown_typechart(cobblemon_jar: Path) -> dict:
    with zipfile.ZipFile(cobblemon_jar, 'r') as z:
        sz_bytes = z.read("data/cobblemon/showdown.zip")
        with zipfile.ZipFile(io.BytesIO(sz_bytes)) as sz:
            js_code = sz.read("data/typechart.js").decode("utf-8")

    # Showdown damageTaken code mapping:
    # 0 = 1.0 (neutral)
    # 1 = 2.0 (super-effective)
    # 2 = 0.5 (resisted)
    # 3 = 0.0 (immune)
    code_to_mult = {0: 1.0, 1: 2.0, 2: 0.5, 3: 0.0}

    matrix = {att: {df: 1.0 for df in STANDARD_18_TYPES} for att in STANDARD_18_TYPES}

    pattern = re.compile(r'(\w+):\s*\{[^{}]*damageTaken:\s*\{([^{}]+)\}', re.MULTILINE | re.DOTALL)
    for match in pattern.finditer(js_code):
        defending = match.group(1).lower()
        if defending not in STANDARD_18_TYPES:
            continue
        dt_block = match.group(2)
        for kv in re.finditer(r'(\w+):\s*(\d+)', dt_block):
            attacking = kv.group(1).lower()
            if attacking not in STANDARD_18_TYPES:
                continue
            code = int(kv.group(2))
            matrix[attacking][defending] = code_to_mult.get(code, 1.0)

    return matrix

def main():
    repo_root = Path(__file__).resolve().parent.parent.parent
    production_json_path = repo_root / "companion-mod" / "src" / "main" / "resources" / "typechart_gen9.json"

    if not production_json_path.exists():
        print(f"FAIL: Production type chart not found at {production_json_path}", file=sys.stderr)
        sys.exit(1)

    with open(production_json_path, "r", encoding="utf-8") as f:
        prod_chart = json.load(f)

    cobblemon_jar = find_cobblemon_jar()
    print(f"Found Cobblemon jar: {cobblemon_jar}")
    showdown_chart = parse_showdown_typechart(cobblemon_jar)

    mismatches = []
    total_pairs = 0

    for att in STANDARD_18_TYPES:
        for df in STANDARD_18_TYPES:
            total_pairs += 1
            expected = showdown_chart.get(att, {}).get(df, 1.0)
            actual = prod_chart.get(att, {}).get(df)

            if actual is None:
                mismatches.append(f"Missing pair in production JSON: ({att} -> {df})")
            elif abs(actual - expected) > 1e-6:
                mismatches.append(
                    f"Mismatch on ({att} -> {df}): Showdown={expected}, Production={actual}"
                )

    if mismatches:
        print(f"FAIL: {len(mismatches)} / {total_pairs} type matchups diverged!", file=sys.stderr)
        for m in mismatches[:20]:
            print(f"  - {m}", file=sys.stderr)
        if len(mismatches) > 20:
            print(f"  ... and {len(mismatches) - 20} more", file=sys.stderr)
        sys.exit(1)

    print(f"SUCCESS: 100% parity verified across all {total_pairs} directed type interactions ({cobblemon_jar.name} <-> typechart_gen9.json).")
    sys.exit(0)

if __name__ == "__main__":
    main()
