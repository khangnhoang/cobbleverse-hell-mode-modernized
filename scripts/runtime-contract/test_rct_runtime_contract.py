#!/usr/bin/env python3
"""
Offline Runtime Contract Test for RCTMod, RCTAPI, and Cobblemon Bytecode Invariants.

Verifies that all classes, methods, descriptors, constructors, and bytecode instructions
assumed by the dynamic lead selection implementation exist exactly as expected
in the local runtime jars and companion mod mixin/refmap metadata.
"""

import json
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
        proc = subprocess.run(['javap', '-v', '-p', '-c', str(tmp_path)], capture_output=True, text=True, check=True)
        return proc.stdout
    finally:
        if tmp_path.exists():
            tmp_path.unlink()

def main():
    repo_root = Path(__file__).resolve().parent.parent.parent
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

    # 1. Scoped RCTMod.makeBattle inspection
    rctmod_javap = get_class_javap(rctmod_jar, "com/gitlab/srcmc/rctmod/api/RCTMod.class")
    mb_pattern = re.compile(r'public boolean makeBattle\(.*?\);.*?(?=\n  public |\n  private |\Z)', re.DOTALL)
    mb_match = mb_pattern.search(rctmod_javap)
    mb_code = mb_match.group(0) if mb_match else ""

    checks.append(("RCTMod.makeBattle method extracted", bool(mb_code)))

    if mb_code:
        # Exact descriptor
        has_descriptor = "descriptor: (Lcom/gitlab/srcmc/rctmod/world/entities/TrainerMob;Lnet/minecraft/class_1657;)Z" in mb_code
        checks.append(("RCTMod.makeBattle exact JVM descriptor (TrainerMob, class_1657)Z", has_descriptor))

        # TrainerRegistry.getById invocation
        has_get_by_id = "TrainerRegistry.getById:(Ljava/lang/String;Ljava/lang/Class;)Lcom/gitlab/srcmc/rctapi/api/trainer/Trainer;" in mb_code
        checks.append(("RCTMod.makeBattle invokes TrainerRegistry.getById", has_get_by_id))

        # Lookup is specifically TrainerNPC (ldc TrainerNPC before getById, followed by checkcast TrainerNPC)
        npc_lookup_pattern = re.compile(
            r'ldc\s+#\d+\s+//\s+class\s+com/gitlab/srcmc/rctapi/api/trainer/TrainerNPC'
            r'.*?invokevirtual\s+#\d+\s+//\s+Method\s+com/gitlab/srcmc/rctapi/api/trainer/TrainerRegistry\.getById'
            r'.*?checkcast\s+#\d+\s+//\s+class\s+com/gitlab/srcmc/rctapi/api/trainer/TrainerNPC',
            re.DOTALL
        )
        checks.append(("RCTMod.makeBattle lookup targets TrainerNPC (not TrainerPlayer)", bool(npc_lookup_pattern.search(mb_code))))

        # Cast and store into slot 5
        store_slot5_pattern = re.compile(
            r'checkcast\s+#\d+\s+//\s+class\s+com/gitlab/srcmc/rctapi/api/trainer/TrainerNPC\s+'
            r'\d+:\s+astore\s+5\b'
        )
        checks.append(("RCTMod.makeBattle stores TrainerNPC into local slot 5 (astore 5)", bool(store_slot5_pattern.search(mb_code))))

        # LocalVariableTable verifies slot 5 is TrainerNPC trNPC
        has_lvt_slot5 = bool(re.search(r'5\s+trNPC\s+Lcom/gitlab/srcmc/rctapi/api/trainer/TrainerNPC;', mb_code))
        checks.append(("RCTMod.makeBattle LocalVariableTable maps slot 5 to trNPC", has_lvt_slot5))

        # Downstream slot 5 usages:
        # a) setEntity
        has_set_entity = bool(re.search(r'aload\s+5\s+\d+:\s+aload_1\s+\d+:\s+invokevirtual.*?TrainerNPC\.setEntity', mb_code))
        checks.append(("Downstream local slot 5 participates in TrainerNPC.setEntity", has_set_entity))

        # b) startBattle
        has_start_battle = bool(re.search(r'aload\s+5\s+\d+:\s+invokestatic.*?List\.of.*?startBattle', mb_code, re.DOTALL))
        checks.append(("Downstream local slot 5 participates in BattleManager.startBattle", has_start_battle))

        # c) registerWinCommands
        has_win_cmd = bool(re.search(r'aload\s+5\s+\d+:\s+aload_1\s+\d+:\s+invokestatic.*?Map\.of.*?registerWinCommands', mb_code, re.DOTALL))
        checks.append(("Downstream local slot 5 participates in TBCSCompat.registerWinCommands", has_win_cmd))

    # 2. Companion mod mixin and refmap inspection
    mixins_json_path = repo_root / "companion-mod" / "src" / "main" / "resources" / "rct_legendary_rule.mixins.json"
    has_mixin_entry = False
    if mixins_json_path.exists():
        with open(mixins_json_path, "r", encoding="utf-8") as mf:
            mixins_data = json.load(mf)
            has_mixin_entry = "RCTModMakeBattleMixin" in mixins_data.get("mixins", [])
    checks.append(("rct_legendary_rule.mixins.json declares RCTModMakeBattleMixin", has_mixin_entry))

    # Inspect refmap from built jar first, fallback to build directories
    refmap_data = None
    refmap_source_desc = None
    built_jars = [p for p in (repo_root / "companion-mod" / "build" / "libs").glob("rct-legendary-rule-companion-*.jar")
                  if not p.name.endswith("-sources.jar") and not p.name.endswith("-dev.jar")]
    if built_jars:
        built_jar = built_jars[0]
        try:
            with zipfile.ZipFile(built_jar, 'r') as z:
                if "rct-legendary-rule-companion-refmap.json" in z.namelist():
                    refmap_data = json.loads(z.read("rct-legendary-rule-companion-refmap.json").decode("utf-8"))
                    refmap_source_desc = f"built JAR ({built_jar.name})"
        except Exception as e:
            print(f"Warning: Failed to read refmap from {built_jar}: {e}", file=sys.stderr)

    if refmap_data is None:
        refmap_candidates = [
            repo_root / "companion-mod" / "build" / "classes" / "java" / "main" / "rct-legendary-rule-companion-refmap.json",
            repo_root / "companion-mod" / "build" / "resources" / "main" / "rct-legendary-rule-companion-refmap.json",
        ]
        refmap_path = next((p for p in refmap_candidates if p.exists()), None)
        if refmap_path:
            with open(refmap_path, "r", encoding="utf-8") as rf:
                refmap_data = json.load(rf)
                refmap_source_desc = f"build tree ({refmap_path.relative_to(repo_root)})"

    refmap_present = refmap_data is not None
    checks.append((f"Generated refmap exists in {refmap_source_desc or 'built JAR / build output'}", refmap_present))

    mixin_key = "com/cobbleverse/legendaryrule/mixin/RCTModMakeBattleMixin"
    source_method = "makeBattle(Lcom/gitlab/srcmc/rctmod/world/entities/TrainerMob;Lnet/minecraft/entity/player/PlayerEntity;)Z"
    expected_target = "Lcom/gitlab/srcmc/rctmod/api/RCTMod;makeBattle(Lcom/gitlab/srcmc/rctmod/world/entities/TrainerMob;Lnet/minecraft/class_1657;)Z"

    if refmap_data is not None:
        mappings = refmap_data.get("mappings", {}).get(mixin_key, {})
        has_mixin = mixin_key in refmap_data.get("mappings", {})
        checks.append(("Generated refmap declares RCTModMakeBattleMixin entry", has_mixin))

        has_source_method = source_method in mappings
        checks.append(("Generated refmap maps source makeBattle method key", has_source_method))

        actual_target = mappings.get(source_method)
        target_matches = (actual_target == expected_target)
        checks.append((f"Generated refmap target equals expected intermediary ({expected_target})", target_matches))

        # Check data section for named:intermediary
        inter_mappings = refmap_data.get("data", {}).get("named:intermediary", {}).get(mixin_key, {})
        data_target = inter_mappings.get(source_method)
        checks.append((f"Generated refmap named:intermediary target equals expected intermediary", data_target == expected_target))
    else:
        checks.append(("Generated refmap declares RCTModMakeBattleMixin entry", False))
        checks.append(("Generated refmap maps source makeBattle method key", False))
        checks.append((f"Generated refmap target equals expected intermediary ({expected_target})", False))
        checks.append(("Generated refmap named:intermediary target equals expected intermediary", False))

    # 3. RCTAPI: TrainerNPC copy constructor and getTeam
    trainer_npc_javap = get_class_javap(rctapi_jar, "com/gitlab/srcmc/rctapi/api/trainer/TrainerNPC.class")
    has_copy_ctor = ("TrainerNPC(com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC)" in trainer_npc_javap or
                     "<init>(Lcom/gitlab/srcmc/rctapi/api/trainer/TrainerNPC;)V" in trainer_npc_javap)
    has_get_team = ("getTeam()" in trainer_npc_javap and "com.cobblemon.mod.common.pokemon.Pokemon[]" in trainer_npc_javap)
    checks.append(("TrainerNPC copy constructor TrainerNPC(TrainerNPC)", has_copy_ctor))
    checks.append(("TrainerNPC.getTeam() returning Pokemon[]", has_get_team))

    # 4. RCTAPI: BattleManager.startBattle
    battle_mgr_javap = get_class_javap(rctapi_jar, "com/gitlab/srcmc/rctapi/api/battle/BattleManager.class")
    has_start_battle = "startBattle(" in battle_mgr_javap
    checks.append(("BattleManager.startBattle", has_start_battle))

    # 5. Cobblemon: Pokemon methods
    pokemon_javap = get_class_javap(cobblemon_jar, "com/cobblemon/mod/common/pokemon/Pokemon.class")
    checks.append(("Pokemon.isFainted()", "isFainted()" in pokemon_javap))
    checks.append(("Pokemon.getSpecies()", "getSpecies()" in pokemon_javap))
    checks.append(("Pokemon.getForm()", "getForm()" in pokemon_javap))
    checks.append(("Pokemon.getAspects()", "getAspects()" in pokemon_javap))
    checks.append(("Pokemon.getPrimaryType()", "getPrimaryType()" in pokemon_javap))
    checks.append(("Pokemon.getSecondaryType()", "getSecondaryType()" in pokemon_javap))

    # 6. Cobblemon: Species, FormData, ElementalType getName()
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
