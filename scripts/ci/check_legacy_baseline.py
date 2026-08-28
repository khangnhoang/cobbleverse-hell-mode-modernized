#!/usr/bin/env python3
"""
Lightweight safeguard against accidental changes to the preserved legacy Hell Mode baseline.
The directory '!Doctors HELL MODE DOUBLE BATTLE EVERYTHING/' must remain immutable as an
authoritative imported reference baseline.
"""

import os
import sys
import hashlib
import subprocess

LEGACY_DIR_NAME = "!Doctors HELL MODE DOUBLE BATTLE EVERYTHING"
MANIFEST_FILE_NAME = "legacy_baseline.sha256"

def compute_baseline_digest(legacy_path):
    h = hashlib.sha256()
    file_count = 0
    for root, dirs, files in os.walk(legacy_path):
        dirs.sort()
        for f in sorted(files):
            rel_path = os.path.relpath(os.path.join(root, f), legacy_path).replace("\\", "/")
            full_path = os.path.join(root, f)
            with open(full_path, "rb") as content_file:
                content = content_file.read()
            file_count += 1
            h.update(f"{rel_path}:{len(content)}:".encode("utf-8"))
            h.update(content)
    return h.hexdigest(), file_count

def load_expected_digest(manifest_path):
    if not os.path.exists(manifest_path):
        raise FileNotFoundError(f"Legacy baseline manifest not found: {manifest_path}")
    with open(manifest_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split()
            if len(parts) >= 1:
                return parts[0]
    raise ValueError(f"No digest found in manifest file: {manifest_path}")

def check_git_cleanliness(repo_root):
    try:
        res = subprocess.run(
            ["git", "status", "--porcelain", "--", LEGACY_DIR_NAME],
            cwd=repo_root,
            capture_output=True,
            text=True
        )
        if res.returncode == 0 and res.stdout.strip():
            print("ERROR: Git detected uncommitted changes in the legacy baseline folder:")
            print(res.stdout.strip())
            return False
    except Exception:
        # If git is not present in runtime environment, checksum validation suffices
        pass
    return True

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    repo_root = os.path.abspath(os.path.join(script_dir, "..", ".."))
    legacy_path = os.path.join(repo_root, LEGACY_DIR_NAME)
    manifest_path = os.path.join(script_dir, MANIFEST_FILE_NAME)

    print(f"Checking legacy baseline immutability: {LEGACY_DIR_NAME}")

    if not os.path.exists(legacy_path):
        print(f"ERROR: Legacy baseline folder missing: {legacy_path}")
        sys.exit(1)

    # 1. Check working tree cleanliness
    if not check_git_cleanliness(repo_root):
        print("\nAborting: Working tree has modifications in legacy baseline folder.")
        sys.exit(1)

    # 2. Check deterministic content digest
    expected_digest = load_expected_digest(manifest_path)
    actual_digest, file_count = compute_baseline_digest(legacy_path)

    if actual_digest != expected_digest:
        print("=" * 70)
        print("ERROR: LEGACY BASELINE INTEGRITY CHECK FAILED!")
        print("=" * 70)
        print(f"Expected Digest: {expected_digest}")
        print(f"Actual Digest:   {actual_digest}")
        print(f"Files Scanned:   {file_count}")
        print("\nThe directory '!Doctors HELL MODE DOUBLE BATTLE EVERYTHING' is intended to")
        print("remain the preserved imported Doctor baseline.")
        print("Modernized trainer modifications belong in separate output packages (Phase C/D),")
        print("not in this legacy reference folder.")
        print("\nIf an intentional baseline replacement was performed, update")
        print(f"'{os.path.relpath(manifest_path, repo_root)}' with documented rationale.")
        print("=" * 70)
        sys.exit(1)

    print(f"PASS: Legacy baseline verified ({file_count} files, digest {actual_digest[:16]}...)")

if __name__ == "__main__":
    main()
