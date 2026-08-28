import os
import sys
import unittest
import json

# Ensure scripts/compat-audit is on path to import pure functions from audit.py
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPT_DIR not in sys.path:
    sys.path.insert(0, SCRIPT_DIR)

from audit import (
    clean_identifier,
    resolve_item_namespace,
    derive_canonical_held_item,
    classify_species_aspect
)

class TestCanonicalAuditReports(unittest.TestCase):
    """
    CI-safe unit and regression test suite.
    Validates repository-owned artifacts: generated reports, schema invariants,
    canonical mappings, and deterministic helper logic.
    Does NOT require the external Cobbleverse instance.
    """
    @classmethod
    def setUpClass(cls):
        cls.repo_root = os.path.abspath(os.path.join(SCRIPT_DIR, "..", ".."))
        cls.reports_dir = os.path.join(cls.repo_root, "reports", "compat-audit")

    def test_reports_exist(self):
        expected_reports = [
            "current-baseline.json",
            "trainer-inventory.json",
            "held-items.json",
            "species.json",
            "moves.json",
            "abilities.json",
            "aspects.json",
            "gimmicks.json",
            "multi-held-items.json",
            "summary.md"
        ]
        for r in expected_reports:
            p = os.path.join(self.reports_dir, r)
            self.assertTrue(os.path.exists(p), f"Report missing: {r}")

    def test_reports_parse_valid_json(self):
        json_reports = [
            "current-baseline.json",
            "trainer-inventory.json",
            "held-items.json",
            "species.json",
            "moves.json",
            "abilities.json",
            "aspects.json",
            "gimmicks.json",
            "multi-held-items.json"
        ]
        for r in json_reports:
            p = os.path.join(self.reports_dir, r)
            with open(p, "r", encoding="utf-8") as f:
                data = json.load(f)
            self.assertIsInstance(data, dict, f"Report {r} root is not a JSON object")

    def test_held_items_canonical_resolution(self):
        p = os.path.join(self.reports_dir, "held-items.json")
        with open(p, "r", encoding="utf-8") as f:
            data = json.load(f)

        items = data["items"]
        # Exact valid items
        self.assertEqual(items["life_orb"]["status"], "VALID_EXACT")
        self.assertEqual(items["life_orb"]["canonical_replacement"], "cobblemon:life_orb")
        self.assertEqual(items["mega_showdown:garchompite"]["status"], "VALID_EXACT")

        # Hyphenated Z-Crystals
        self.assertEqual(items["mega_showdown:darkinium-z"]["status"], "INVALID_UNIQUE_CANONICAL_MATCH")
        self.assertEqual(items["mega_showdown:darkinium-z"]["canonical_replacement"], "mega_showdown:darkinium_z")

        # Missing underscores
        self.assertEqual(items["mega_showdown:blueorb"]["status"], "INVALID_UNIQUE_CANONICAL_MATCH")
        self.assertEqual(items["mega_showdown:blueorb"]["canonical_replacement"], "mega_showdown:blue_orb")
        self.assertEqual(items["mega_showdown:steelmemory"]["status"], "INVALID_UNIQUE_CANONICAL_MATCH")
        self.assertEqual(items["mega_showdown:steelmemory"]["canonical_replacement"], "mega_showdown:steel_memory")

        # Namespace typo
        self.assertEqual(items["megas_showdown:wellspring_mask"]["status"], "INVALID_UNIQUE_CANONICAL_MATCH")
        self.assertEqual(items["megas_showdown:wellspring_mask"]["canonical_replacement"], "mega_showdown:wellspring_mask")

        # Unqualified bare items
        self.assertEqual(items["charcoal"]["status"], "INVALID_UNIQUE_CANONICAL_MATCH")
        self.assertEqual(items["charcoal"]["canonical_replacement"], "minecraft:charcoal")
        self.assertEqual(items["booster_energy"]["status"], "INVALID_UNIQUE_CANONICAL_MATCH")
        self.assertEqual(items["booster_energy"]["canonical_replacement"], "mega_showdown:booster_energy")

    def test_held_items_invariants(self):
        p = os.path.join(self.reports_dir, "held-items.json")
        with open(p, "r", encoding="utf-8") as f:
            data = json.load(f)

        summary = data["summary"]
        self.assertEqual(summary["invalid_ambiguous_count"], 0)
        self.assertEqual(summary["invalid_no_match_count"], 0)

        for raw, info in data["items"].items():
            status = info["status"]
            canon = info["canonical_replacement"]
            if status in ("VALID_EXACT", "INVALID_UNIQUE_CANONICAL_MATCH"):
                self.assertIsNotNone(canon, f"Item {raw} with status {status} has None canonical replacement")
                self.assertIn(":", canon, f"Item {raw} replacement {canon} is not fully qualified")
            elif status in ("INVALID_AMBIGUOUS", "INVALID_NO_MATCH"):
                self.assertIsNone(canon, f"Item {raw} with status {status} must have None canonical replacement")

    def test_species_all_valid(self):
        p = os.path.join(self.reports_dir, "species.json")
        with open(p, "r", encoding="utf-8") as f:
            data = json.load(f)
        self.assertEqual(data["summary"]["invalid_count"], 0)
        self.assertEqual(data["summary"]["valid_exact_count"], 784)

    def test_invalid_gimmicks(self):
        p = os.path.join(self.reports_dir, "gimmicks.json")
        with open(p, "r", encoding="utf-8") as f:
            data = json.load(f)
        self.assertEqual(data["summary"]["invalid_gimmick_keys_count"], 2)
        invalid_keys = [rec["invalid_key"] for rec in data["invalid_gimmick_usages"]]
        self.assertEqual(invalid_keys, ["mega", "mega"])

    def test_aspects_calyrex_and_necrozma(self):
        p = os.path.join(self.reports_dir, "aspects.json")
        with open(p, "r", encoding="utf-8") as f:
            data = json.load(f)
        combos = data["aspect_combinations"]

        # Calyrex ice syntax fix
        self.assertEqual(combos["calyrex::ice"]["status"], "INVALID_UNIQUE_CANONICAL_MATCH")
        self.assertEqual(combos["calyrex::ice"]["canonical_replacement"], "ice-rider")

        # Necrozma dusk_mane syntax fix
        self.assertEqual(combos["necrozma::dusk_mane"]["status"], "INVALID_UNIQUE_CANONICAL_MATCH")
        self.assertEqual(combos["necrozma::dusk_mane"]["canonical_replacement"], "dusk-fusion")

        # Sevii invalid form
        self.assertEqual(combos["mantine::sevii"]["status"], "INVALID_NO_MATCH")
        self.assertIsNone(combos["mantine::sevii"]["canonical_replacement"])

    def test_aspects_invariants(self):
        p = os.path.join(self.reports_dir, "aspects.json")
        with open(p, "r", encoding="utf-8") as f:
            data = json.load(f)
        for key, info in data["aspect_combinations"].items():
            st = info["status"]
            canon = info["canonical_replacement"]
            if st == "INVALID_NO_MATCH":
                self.assertIsNone(canon, f"Aspect {key} marked INVALID_NO_MATCH must have None replacement")
            elif st in ("VALID_EXACT", "INVALID_UNIQUE_CANONICAL_MATCH"):
                self.assertIsNotNone(canon, f"Aspect {key} marked {st} must have non-None replacement")

    def test_trainer_inventory_invariants(self):
        p = os.path.join(self.reports_dir, "trainer-inventory.json")
        with open(p, "r", encoding="utf-8") as f:
            data = json.load(f)
        summary = data["summary"]
        shared = summary["shared_trainer_ids_count"]
        missing = summary["missing_from_hell_count"]
        obsolete = summary["obsolete_in_hell_count"]
        total_baseline = summary["total_effective_baseline_trainers"]
        total_legacy = summary["total_legacy_hell_trainers"]

        self.assertEqual(shared + missing, total_baseline)
        self.assertEqual(shared + obsolete, total_legacy)

    def test_pure_audit_functions(self):
        """Test pure helper functions from audit.py in isolation."""
        # 1. clean_identifier
        self.assertEqual(clean_identifier("mega_showdown:waterium-z"), "wateriumz")
        self.assertEqual(clean_identifier("cobblemon:life_orb"), "lifeorb")

        # 2. resolve_item_namespace
        self.assertEqual(resolve_item_namespace("leftovers"), "cobblemon:leftovers")
        self.assertEqual(resolve_item_namespace("minecraft:charcoal"), "minecraft:charcoal")
        self.assertEqual(resolve_item_namespace("mega_showdown:lucarionite"), "mega_showdown:lucarionite")

        # 3. derive_canonical_held_item
        registered = {"cobblemon:life_orb", "mega_showdown:waterium_z", "mega_showdown:blue_orb", "minecraft:charcoal"}
        ms_items = {"mega_showdown:waterium_z", "mega_showdown:blue_orb"}
        vanilla = {"minecraft:charcoal"}

        # Exact match
        st, repl, _ = derive_canonical_held_item("life_orb", registered, ms_items, vanilla)
        self.assertEqual(st, "VALID_EXACT")
        self.assertEqual(repl, "cobblemon:life_orb")

        # Hyphen fix
        st, repl, _ = derive_canonical_held_item("mega_showdown:waterium-z", registered, ms_items, vanilla)
        self.assertEqual(st, "INVALID_UNIQUE_CANONICAL_MATCH")
        self.assertEqual(repl, "mega_showdown:waterium_z")

        # Missing underscore fix
        st, repl, _ = derive_canonical_held_item("mega_showdown:blueorb", registered, ms_items, vanilla)
        self.assertEqual(st, "INVALID_UNIQUE_CANONICAL_MATCH")
        self.assertEqual(repl, "mega_showdown:blue_orb")

        # Bare vanilla item fix
        st, repl, _ = derive_canonical_held_item("charcoal", registered, ms_items, vanilla)
        self.assertEqual(st, "INVALID_UNIQUE_CANONICAL_MATCH")
        self.assertEqual(repl, "minecraft:charcoal")

        # No match
        st, repl, _ = derive_canonical_held_item("nonexistent_item_xyz", registered, ms_items, vanilla)
        self.assertEqual(st, "INVALID_NO_MATCH")
        self.assertIsNone(repl)

        # 4. classify_species_aspect
        valid_asps = {"ice-rider", "shadow-rider"}
        # Exact match
        st, repl, _ = classify_species_aspect("calyrex", "ice-rider", valid_asps)
        self.assertEqual(st, "VALID_EXACT")
        self.assertEqual(repl, "ice-rider")

        # Syntax discrepancy
        st, repl, _ = classify_species_aspect("calyrex", "ice", valid_asps)
        self.assertEqual(st, "INVALID_UNIQUE_CANONICAL_MATCH")
        self.assertEqual(repl, "ice-rider")

        # Sevii invalid form
        st, repl, _ = classify_species_aspect("mantine", "sevii", set())
        self.assertEqual(st, "INVALID_NO_MATCH")
        self.assertIsNone(repl)

if __name__ == "__main__":
    unittest.main()
