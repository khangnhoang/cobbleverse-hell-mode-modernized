import os
import sys
import unittest
import subprocess

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, "..", ".."))

class TestLocalIntegrationAudit(unittest.TestCase):
    """
    Authoritative local integration test.
    Requires the actual installed Cobbleverse instance.
    Runs the full audit.py against mod JARs and verifies deterministic report outputs.
    Skipped when COBBLEVERSE_INSTANCE_PATH is not configured or path does not exist.
    """
    @classmethod
    def setUpClass(cls):
        cls.instance_path = os.environ.get("COBBLEVERSE_INSTANCE_PATH")
        if not cls.instance_path:
            raise unittest.SkipTest(
                "COBBLEVERSE_INSTANCE_PATH environment variable not set. "
                "Local integration audit requires the external installed modpack. "
                "Set COBBLEVERSE_INSTANCE_PATH to run this test."
            )
        if not os.path.exists(cls.instance_path):
            raise unittest.SkipTest(
                f"Cobbleverse instance not found at '{cls.instance_path}'. "
                "Local integration audit requires an existing installed modpack directory."
            )

    def test_external_reference_binaries_exist(self):
        mods_dir = os.path.join(self.instance_path, "mods")
        dp_dir = os.path.join(self.instance_path, "datapacks")

        expected_files = [
            os.path.join(mods_dir, "Cobblemon-fabric-1.7.3+1.21.1.jar"),
            os.path.join(mods_dir, "mega_showdown-fabric-1.8.4+1.7.3+1.21.1.jar"),
            os.path.join(mods_dir, "rctmod-fabric-1.21.1-0.18.1-beta.jar"),
            os.path.join(dp_dir, "COBBLEVERSE-RCT-DP-v20.zip")
        ]
        for f in expected_files:
            self.assertTrue(os.path.exists(f), f"Reference binary missing: {f}")

    def test_full_audit_regeneration_determinism(self):
        audit_script = os.path.join(REPO_ROOT, "scripts", "compat-audit", "audit.py")
        res = subprocess.run(
            [sys.executable, audit_script, "--instance", self.instance_path],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True
        )
        self.assertEqual(res.returncode, 0, f"audit.py failed:\n{res.stderr}\n{res.stdout}")

        reports_dir = os.path.join(REPO_ROOT, "reports", "compat-audit")
        git_diff = subprocess.run(
            ["git", "diff", "--stat", reports_dir],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True
        )
        self.assertEqual(
            git_diff.stdout.strip(),
            "",
            f"Audit output is not deterministic across runs! Diff:\n{git_diff.stdout}"
        )

if __name__ == "__main__":
    unittest.main()
