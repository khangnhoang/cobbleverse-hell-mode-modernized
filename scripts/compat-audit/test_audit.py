import os
import sys
import unittest
import json
import tempfile
import shutil

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, "..", ".."))

# Add scripts directories to path for direct testing of pure functions
sys.path.append(SCRIPT_DIR)
sys.path.append(os.path.join(REPO_ROOT, "scripts", "ci"))

from audit import (
    clean_identifier,
    resolve_item_namespace,
    derive_canonical_held_item,
    classify_species_aspect,
)
from validate_repo import validate_future_pack

class TestCanonicalAuditReports(unittest.TestCase):
    """
    CI-Safe verification suite.
    Runs entirely inside the repository without requiring external modpack binaries.
    Verifies that generated audit reports and classification invariants remain intact.
    """
    @classmethod
    def setUpClass(cls):
        cls.repo_root = REPO_ROOT
        cls.reports_dir = os.path.join(cls.repo_root, "reports", "compat-audit")

    def test_reports_exist(self):
        expected = [
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
        for f in expected:
            p = os.path.join(self.reports_dir, f)
            self.assertTrue(os.path.exists(p), f"Report missing: {f}")

    def test_reports_parse_valid_json(self):
        for f in os.listdir(self.reports_dir):
            if f.endswith(".json"):
                p = os.path.join(self.reports_dir, f)
                with open(p, "r", encoding="utf-8") as jf:
                    data = json.load(jf)
                    self.assertIsInstance(data, dict, f"Report root is not a dict: {f}")

    def test_held_items_canonical_resolution(self):
        p = os.path.join(self.reports_dir, "held-items.json")
        with open(p, "r", encoding="utf-8") as f:
            data = json.load(f)
        items = data["items"]

        # Hyphenated Z-Crystals
        self.assertEqual(items["mega_showdown:waterium-z"]["status"], "INVALID_UNIQUE_CANONICAL_MATCH")
        self.assertEqual(items["mega_showdown:waterium-z"]["canonical_replacement"], "mega_showdown:waterium_z")
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
        usages = data["invalid_gimmick_usages"]
        self.assertEqual(len(usages), 2)
        files = {u["trainer_file"] for u in usages}
        self.assertIn("team_rocket_admin_apollo.json", files)
        self.assertIn("team_rocket_giovanni.json", files)

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
            if st in ("INVALID_NO_MATCH", "INVALID_AMBIGUOUS"):
                self.assertIsNone(canon, f"Aspect {key} marked {st} must have None replacement")
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

    def test_aspect_ambiguity_safety(self):
        """
        Verify that aspect matching is strictly ambiguity-safe:
        - Exact match always wins
        - Single normalized/substring candidate resolves to INVALID_UNIQUE_CANONICAL_MATCH
        - Multiple candidates resolve to INVALID_AMBIGUOUS with None replacement
        - Zero candidates resolve to INVALID_NO_MATCH
        """
        # Exact match wins even if other aspects contain it as substring
        valid_asps = {"sun", "sunny-form", "sunshine"}
        st, repl, _ = classify_species_aspect("cherrim", "sun", valid_asps)
        self.assertEqual(st, "VALID_EXACT")
        self.assertEqual(repl, "sun")

        # Unique fuzzy/substring match succeeds
        valid_unique = {"single-strike-style", "something-else"}
        st, repl, _ = classify_species_aspect("urshifu", "single-strike", valid_unique)
        self.assertEqual(st, "INVALID_UNIQUE_CANONICAL_MATCH")
        self.assertEqual(repl, "single-strike-style")

        # Multiple candidates trigger INVALID_AMBIGUOUS and return None replacement
        valid_ambiguous = {"rotom-heat-appliance", "rotom-heat-special"}
        st, repl, _ = classify_species_aspect("rotom", "heat", valid_ambiguous)
        self.assertEqual(st, "INVALID_AMBIGUOUS")
        self.assertIsNone(repl)

        # Zero candidates triggers INVALID_NO_MATCH
        valid_none = {"form-a", "form-b"}
        st, repl, _ = classify_species_aspect("pikachu", "unrecognized-form", valid_none)
        self.assertEqual(st, "INVALID_NO_MATCH")
        self.assertIsNone(repl)

    def test_validate_future_pack_regression(self):
        """
        Tests validate_future_pack fail-closed behavior:
        - When pack/ is absent, skips cleanly without error
        - When pack/ exists but has missing or invalid pack.mcmeta, returns False
        - When pack/ exists but has missing trainers dir or invalid trainer JSON, returns False
        - When pack/ has valid structure and valid trainer JSON, returns True
        """
        with tempfile.TemporaryDirectory() as temp_dir:
            # 1. pack/ does not exist -> returns True (clean skip)
            self.assertTrue(validate_future_pack(temp_dir))

            pack_path = os.path.join(temp_dir, "pack")
            os.makedirs(pack_path)

            # 2. pack/ exists but missing pack.mcmeta -> returns False
            self.assertFalse(validate_future_pack(temp_dir))

            # Add pack.mcmeta
            mcmeta = {"pack": {"pack_format": 48, "description": "Test Pack"}}
            with open(os.path.join(pack_path, "pack.mcmeta"), "w") as f:
                json.dump(mcmeta, f)

            # 3. pack/ exists with pack.mcmeta but missing trainers dir -> returns False
            self.assertFalse(validate_future_pack(temp_dir))

            # Add trainers dir
            trainers_dir = os.path.join(pack_path, "data", "rctmod", "trainers")
            os.makedirs(trainers_dir)

            # 4. pack/ trainers dir has 0 files -> returns False
            self.assertFalse(validate_future_pack(temp_dir))

            # 5. Add malformed trainer JSON -> returns False
            bad_trainer = os.path.join(trainers_dir, "bad.json")
            with open(bad_trainer, "w") as f:
                f.write("{invalid json")
            self.assertFalse(validate_future_pack(temp_dir))

            # 6. Add valid trainer JSON -> returns True
            good_trainer = {
                "team": [
                    {"species": "pikachu", "level": 50}
                ]
            }
            with open(bad_trainer, "w") as f:
                json.dump(good_trainer, f)
            self.assertTrue(validate_future_pack(temp_dir))

if __name__ == "__main__":
    unittest.main()
