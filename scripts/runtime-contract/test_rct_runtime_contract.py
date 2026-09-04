#!/usr/bin/env python3
"""
Offline Runtime Contract Test for RCTMod, RCTAPI, and Cobblemon Bytecode Invariants.

Verifies that all classes, methods, descriptors, constructors, and bytecode instructions
assumed by the dynamic lead selection implementation exist exactly as expected
in the local runtime jars.
"""

import os
import re
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

def find_mods_dir() -> Path:
    candidates = []
    env_dir = os.environ.get("COBBLEVERSE_MODS_DIR")
    if env_dir:
        candidates.append(Path(env_dir))
    candidates.append(Path(r"c:\Users\khang\curseforge\minecraft\Instances\COBBLEVERSE - Pokemon Adventure [Cobblemon]\mods"))

    for c in candidates:
        if c.exists():
            return c
    raise FileNotFoundError(f"Mods directory not found in candidate paths: {[str(c) for c in candidates]}")

def get_class_javap(jar_path: Path, class_internal_path: str) -> str:
    with zipfile.ZipFile(jar_path, 'r') as z:
        class_bytes = z.read(class_internal_path)

    with tempfile.NamedTemporaryFile(suffix='.class', delete=False) as tmp:
        tmp.write(class_bytes)
        tmp_path = Path(tmp.name)

    try:
        proc = subprocess.run(['javap', '-v', '-p', str(tmp_path)], capture_output=True, text=True, check=True)
        return proc.stdout
    finally:
        if tmp_path.exists():
            tmp_path.unlink()

def main():
    mods_dir = find_mods_dir()
    print(f"Using mods directory: {mods_dir}")

    rctmod_jars = list(mods_dir.glob("rctmod-fabric-*.jar"))
    rctapi_jars = list(mods_dir.glob("rctapi-fabric-*.jar"))
    cobblemon_jars = list(mods_dir.glob("Cobblemon-fabric-*.jar"))

    if not rctmod_jars:
        print("FAIL: rctmod jar not found", file=sys.stderr)
        sys.exit(1)
    if not rctapi_jars:
        print("FAIL: rctapi jar not found", file=sys.stderr)
        sys.exit(1)
    if not cobblemon_jars:
        print("FAIL: Cobblemon jar not found", file=sys.stderr)
        sys.exit(1)

    rctmod_jar = rctmod_jars[0]
    rctapi_jar = rctapi_jars[0]
    cobblemon_jar = cobblemon_jars[0]

    print(f"Inspecting RCTMod jar: {rctmod_jar.name}")
    print(f"Inspecting RCTAPI jar: {rctapi_jar.name}")
    print(f"Inspecting Cobblemon jar: {cobblemon_jar.name}")

    checks = []

    # 1. RCTMod: makeBattle signature and STORE instruction
    rctmod_javap = get_class_javap(rctmod_jar, "com/gitlab/srcmc/rctmod/api/RCTMod.class")
    has_makebattle = ("makeBattle(com.gitlab.srcmc.rctmod.world.entities.TrainerMob" in rctmod_javap and
                      ("net.minecraft.class_1657" in rctmod_javap or "net.minecraft.entity.player.PlayerEntity" in rctmod_javap))
    has_trainer_store = bool(re.search(r'astore(_|\s+)5\b', rctmod_javap))
    checks.append(("RCTMod.makeBattle method signature", has_makebattle))
    checks.append(("RCTMod.makeBattle local variable slot 5 STORE (astore 5)", has_trainer_store))

    # 2. RCTAPI: TrainerNPC copy constructor and getTeam
    trainer_npc_javap = get_class_javap(rctapi_jar, "com/gitlab/srcmc/rctapi/api/trainer/TrainerNPC.class")
    has_copy_ctor = ("TrainerNPC(com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC)" in trainer_npc_javap or
                     "<init>(Lcom/gitlab/srcmc/rctapi/api/trainer/TrainerNPC;)V" in trainer_npc_javap)
    has_get_team = ("getTeam()" in trainer_npc_javap and "com.cobblemon.mod.common.pokemon.Pokemon[]" in trainer_npc_javap)
    checks.append(("TrainerNPC copy constructor TrainerNPC(TrainerNPC)", has_copy_ctor))
    checks.append(("TrainerNPC.getTeam() returning Pokemon[]", has_get_team))

    # 3. RCTAPI: BattleManager.startBattle
    battle_mgr_javap = get_class_javap(rctapi_jar, "com/gitlab/srcmc/rctapi/api/battle/BattleManager.class")
    has_start_battle = "startBattle(" in battle_mgr_javap
    checks.append(("BattleManager.startBattle", has_start_battle))

    # 4. Cobblemon: Pokemon methods
    pokemon_javap = get_class_javap(cobblemon_jar, "com/cobblemon/mod/common/pokemon/Pokemon.class")
    checks.append(("Pokemon.isFainted()", "isFainted()" in pokemon_javap))
    checks.append(("Pokemon.getSpecies()", "getSpecies()" in pokemon_javap))
    checks.append(("Pokemon.getForm()", "getForm()" in pokemon_javap))
    checks.append(("Pokemon.getAspects()", "getAspects()" in pokemon_javap))
    checks.append(("Pokemon.getPrimaryType()", "getPrimaryType()" in pokemon_javap))
    checks.append(("Pokemon.getSecondaryType()", "getSecondaryType()" in pokemon_javap))

    # 5. Cobblemon: Species, FormData, ElementalType getName()
    species_javap = get_class_javap(cobblemon_jar, "com/cobblemon/mod/common/pokemon/Species.class")
    form_javap = get_class_javap(cobblemon_jar, "com/cobblemon/mod/common/pokemon/FormData.class")
    type_javap = get_class_javap(cobblemon_jar, "com/cobblemon/mod/common/api/types/ElementalType.class")
    checks.append(("Species.getName()", "getName()" in species_javap))
    checks.append(("FormData.getName()", "getName()" in form_javap))
    checks.append(("ElementalType.getName()", "getName()" in type_javap))

    # Evaluate checks
    failed = False
    for desc, passed in checks:
        if passed:
            print(f"  [PASS] {desc}")
        else:
            print(f"  [FAIL] {desc}", file=sys.stderr)
            failed = True

    if failed:
        print("\nFAIL: Runtime bytecode contract checks failed!", file=sys.stderr)
        sys.exit(1)
    else:
        print("\nSUCCESS: All runtime bytecode contract checks PASSED.")
        sys.exit(0)

if __name__ == "__main__":
    main()
