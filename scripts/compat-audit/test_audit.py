import os
import sys
import unittest
import json
import subprocess

class TestCanonicalAudit(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
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

    def test_deterministic_output(self):
        # Running the audit again should produce byte-identical JSON outputs
        audit_script = os.path.join(self.repo_root, "scripts", "compat-audit", "audit.py")
        res = subprocess.run([sys.executable, audit_script], capture_output=True, text=True)
        self.assertEqual(res.returncode, 0)
        
        git_diff = subprocess.run(["git", "diff", "--stat", self.reports_dir], cwd=self.repo_root, capture_output=True, text=True)
        self.assertEqual(git_diff.stdout.strip(), "", "Audit output is not deterministic across runs!")

if __name__ == "__main__":
    unittest.main()
