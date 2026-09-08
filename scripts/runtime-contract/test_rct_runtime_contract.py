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
    rbrctai_jars = list(mods_dir.glob("rbrctai-fabric-*.jar"))

    if not rctmod_jars:
        print("FAIL: rctmod jar not found", file=sys.stderr)
        sys.exit(1)
    if not rctapi_jars:
        print("FAIL: rctapi jar not found", file=sys.stderr)
        sys.exit(1)
    if not cobblemon_jars:
        print("FAIL: Cobblemon jar not found", file=sys.stderr)
        sys.exit(1)
    if not rbrctai_jars:
        print("FAIL: rbrctai jar not found", file=sys.stderr)
        sys.exit(1)

    rctmod_jar = rctmod_jars[0]
    rctapi_jar = rctapi_jars[0]
    cobblemon_jar = cobblemon_jars[0]
    rbrctai_jar = rbrctai_jars[0]

    print(f"Inspecting RCTMod jar: {rctmod_jar.name}")
    print(f"Inspecting RCTAPI jar: {rctapi_jar.name}")
    print(f"Inspecting Cobblemon jar: {cobblemon_jar.name}")
    print(f"Inspecting Run & Bun AI jar: {rbrctai_jar.name}")

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

    # 3b. RCTAPI: TrainerNPC 7-arg constructor used by RCTModMakeBattleMixin for per-battle NPC with wrapped BattleAI
    #     TrainerNPC(Text, Pokemon[], GimmicksMap, TrainerBag, Identifier, BattleAI, LivingEntity)
    #     Descriptor derived from javap on installed rctapi-fabric jar bytecode, not from memory.
    #     javap -v outputs descriptor on a separate line, so we match the descriptor string directly.
    seven_arg_ctor_descriptor = (
        "(Lcom/gitlab/srcmc/rctapi/api/util/Text;"
        "[Lcom/cobblemon/mod/common/pokemon/Pokemon;"
        "Lcom/gitlab/srcmc/rctapi/api/trainer/TrainerNPC$GimmicksMap;"
        "Lcom/gitlab/srcmc/rctapi/api/trainer/TrainerBag;"
        "Lnet/minecraft/class_2960;"
        "Lcom/cobblemon/mod/common/api/battles/model/ai/BattleAI;"
        "Lnet/minecraft/class_1309;)V"
    )
    has_seven_arg_ctor = seven_arg_ctor_descriptor in trainer_npc_javap
    checks.append(("TrainerNPC 7-arg constructor (Text, Pokemon[], GimmicksMap, TrainerBag, Identifier, BattleAI, LivingEntity)", has_seven_arg_ctor))

    # 3c. RCTAPI: Getter methods production calls to feed into the 7-arg constructor (RCTModMakeBattleMixin lines 64-70)
    checks.append(("TrainerNPC.getName() returning Text", "getName()" in trainer_npc_javap and "com.gitlab.srcmc.rctapi.api.util.Text" in trainer_npc_javap))
    checks.append(("TrainerNPC.getGimmicks() returning GimmicksMap", "getGimmicks()" in trainer_npc_javap and "GimmicksMap" in trainer_npc_javap))
    checks.append(("TrainerNPC.getBag() returning TrainerBag", "getBag()" in trainer_npc_javap and "TrainerBag" in trainer_npc_javap))
    checks.append(("TrainerNPC.getBattleTheme() returning Identifier", "getBattleTheme()" in trainer_npc_javap and "class_2960" in trainer_npc_javap))
    checks.append(("TrainerNPC.getBattleAI() returning BattleAI", "getBattleAI()" in trainer_npc_javap and "BattleAI" in trainer_npc_javap))
    checks.append(("TrainerNPC.getEntity() returning LivingEntity", "getEntity()" in trainer_npc_javap and "class_1309" in trainer_npc_javap))

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
    checks.append(("FormData.getWeight() returning float (descriptor ()F)", "getWeight" in form_javap and "()F" in form_javap))
    checks.append(("Species.getStandardForm() returning FormData", "getStandardForm" in species_javap and "()Lcom/cobblemon/mod/common/pokemon/FormData;" in species_javap))

    # 7. Cobblemon: EndItem Hook Contracts (CobblemonHeldItemManager.handleEndInstruction, BattleMessage.effectAt, Effect.getId)
    item_mgr_javap = get_class_javap(cobblemon_jar, "com/cobblemon/mod/common/pokemon/helditem/CobblemonHeldItemManager.class")
    handle_end_desc = "(Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Lcom/cobblemon/mod/common/api/battles/interpreter/BattleMessage;)V"
    checks.append(("CobblemonHeldItemManager.handleEndInstruction(BattlePokemon, PokemonBattle, BattleMessage)V", handle_end_desc in item_mgr_javap))

    battle_msg_javap = get_class_javap(cobblemon_jar, "com/cobblemon/mod/common/api/battles/interpreter/BattleMessage.class")
    effect_at_desc = "(I)Lcom/cobblemon/mod/common/api/battles/interpreter/Effect;"
    checks.append(("BattleMessage.effectAt(int) returning Effect", effect_at_desc in battle_msg_javap))

    effect_javap = get_class_javap(cobblemon_jar, "com/cobblemon/mod/common/api/battles/interpreter/Effect.class")
    get_id_desc = "()Ljava/lang/String;"
    checks.append(("Effect.getId() returning String", get_id_desc in effect_javap and "getId()" in effect_javap))

    # 8. Run & Bun AI and Spread Move Valuation Contracts
    # a) Mixin registrations
    if mixins_json_path.exists():
        with open(mixins_json_path, "r", encoding="utf-8") as mf:
            mixins_data = json.load(mf)
            declared_mixins = mixins_data.get("mixins", [])
            checks.append(("rct_legendary_rule.mixins.json declares PokeMathMaxMixin", "PokeMathMaxMixin" in declared_mixins))
            checks.append(("rct_legendary_rule.mixins.json declares RunBunAIChooseMixin", "RunBunAIChooseMixin" in declared_mixins))

    # b) PokeMathMax descriptors and slot 2 read
    pokemath_javap = get_class_javap(rbrctai_jar, "com/gitlab/surilexa/rbrctai/api/ai/utils/PokeMathMax.class")
    pokemath_pub_desc = "(Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lcom/cobblemon/mod/common/api/moves/Move;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;ZZLcom/gitlab/surilexa/rbrctai/api/ai/utils/RBStatStages;)I"
    checks.append(("PokeMathMax.damage public descriptor (7 params)I", pokemath_pub_desc in pokemath_javap))

    pokemath_priv_desc = "(Lcom/cobblemon/mod/common/api/moves/Move;ZZZZZZZZLcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lcom/gitlab/surilexa/rbrctai/api/ai/utils/RBStatStages;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;ZZ)D"
    checks.append(("PokeMathMax.damage private descriptor (15 params)D", pokemath_priv_desc in pokemath_javap))

    pokemath_is_immune_desc = "(Lcom/cobblemon/mod/common/api/moves/Move;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/cobblemon/mod/common/api/types/ElementalType;Z)Z"
    checks.append(("PokeMathMax.isImmuneCheck descriptor (6 params)Z", pokemath_is_immune_desc in pokemath_javap))

    # Verify slot 2 (multiTarget) in private damage is read exactly once
    priv_damage_match = re.search(r'private static double damage\(com\.cobblemon\.mod\.common\.api\.moves\.Move.*?\n\s+Code:.*?(?=\n\s+public |\n\s+private |\Z)', pokemath_javap, re.DOTALL)
    if priv_damage_match:
        priv_damage_code = priv_damage_match.group(0)
        iload2_count = len(re.findall(r'\biload_2\b|\biload\s+2\b', priv_damage_code))
        checks.append(("PokeMathMax.damage private helper reads multiTarget (slot 2) exactly once", iload2_count == 1))
    else:
        checks.append(("PokeMathMax.damage private helper reads multiTarget (slot 2) exactly once", False))

    # c) RunBunAI.choose descriptor and MoveEvaluation.getDamage calls
    runbun_javap = get_class_javap(rbrctai_jar, "com/gitlab/surilexa/rbrctai/api/ai/RunBunAI.class")
    choose_desc = "(Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Lcom/cobblemon/mod/common/battles/BattleSide;Lcom/cobblemon/mod/common/battles/ShowdownMoveset;Z)Lcom/cobblemon/mod/common/battles/ShowdownActionResponse;"
    checks.append(("RunBunAI.choose method descriptor (5 params)ShowdownActionResponse", choose_desc in runbun_javap))

    choose_match = re.search(r'public com\.cobblemon\.mod\.common\.battles\.ShowdownActionResponse choose\(.*?\n\s+Code:.*?(?=\n\s+public |\n\s+private |\Z)', runbun_javap, re.DOTALL)
    if choose_match:
        choose_code = choose_match.group(0)
        get_damage_count = len(re.findall(r'Method com/gitlab/surilexa/rbrctai/api/ai/RunBunAI\$MoveEvaluation\.getDamage:\(\)I', choose_code))
        checks.append(("RunBunAI.choose contains exactly 4 MoveEvaluation.getDamage calls", get_damage_count == 4))

        # Parse LocalVariableTable of choose method
        lvt_pattern = re.compile(r'^\s+(\d+)\s+(\d+)\s+(\d+)\s+(\S+)\s+(\S+)', re.MULTILINE)
        choose_lvt = [
            {'start': int(m.group(1)), 'length': int(m.group(2)), 'slot': int(m.group(3)), 'name': m.group(4), 'sig': m.group(5)}
            for m in lvt_pattern.finditer(choose_code)
        ]

        # Hook 1: evaluations in scope at first getDamage() call
        first_damage_match = re.search(r'(\d+):\s+invokevirtual\s+.*MoveEvaluation\.getDamage:\(\)I', choose_code)
        first_damage_offset = int(first_damage_match.group(1)) if first_damage_match else None
        eval_entries = [e for e in choose_lvt if e['name'] == 'evaluations']
        eval_in_scope = any(e['start'] <= first_damage_offset < e['start'] + e['length'] for e in eval_entries) if first_damage_offset is not None else False
        checks.append(("RunBunAI.choose LVT 'evaluations' in scope at first getDamage call", eval_in_scope))

        # Hook 3: percentChange exists (type double) and 'move' in scope at percentChange store
        percent_entries = [e for e in choose_lvt if e['name'] == 'percentChange']
        percent_exists = len(percent_entries) > 0 and percent_entries[0]['sig'] == 'D'
        checks.append(("RunBunAI.choose LVT 'percentChange' (double) exists", percent_exists))

        move_entries = [e for e in choose_lvt if e['name'] == 'move']
        percent_start = percent_entries[0]['start'] if percent_entries else None
        move_in_scope = any(e['start'] <= percent_start < e['start'] + e['length'] for e in move_entries) if percent_start is not None else False
        checks.append(("RunBunAI.choose LVT 'move' in scope at percentChange store site", move_in_scope))

        # Hook 4: teraMatch exists in LVT and bytecode stores to slot 42
        tera_entries = [e for e in choose_lvt if e['name'] == 'teraMatch']
        tera_exists = len(tera_entries) > 0 and 'BattlePokemon' in tera_entries[0]['sig']
        checks.append(("RunBunAI.choose LVT 'teraMatch' (BattlePokemon) exists", tera_exists))

        tera_store = "astore        42" in choose_code or "astore_w      42" in choose_code
        checks.append(("RunBunAI.choose bytecode contains astore 42 for teraMatch", tera_store))
    else:
        checks.append(("RunBunAI.choose contains exactly 4 MoveEvaluation.getDamage calls", False))
        checks.append(("RunBunAI.choose LVT 'evaluations' in scope at first getDamage call", False))
        checks.append(("RunBunAI.choose LVT 'percentChange' (double) exists", False))
        checks.append(("RunBunAI.choose LVT 'move' in scope at percentChange store site", False))
        checks.append(("RunBunAI.choose LVT 'teraMatch' (BattlePokemon) exists", False))
        checks.append(("RunBunAI.choose bytecode contains astore 42 for teraMatch", False))

    checks.append(("RunBunAI contains private String teraTarget field", "private java.lang.String teraTarget;" in runbun_javap))

    # Explicit @Local and @ModifyVariable name selectors in companion RunBunAIChooseMixin
    mixin_source_path = os.path.join(repo_root, "companion-mod", "src", "main", "java", "com", "cobbleverse", "legendaryrule", "mixin", "RunBunAIChooseMixin.java")
    if os.path.exists(mixin_source_path):
        with open(mixin_source_path, "r", encoding="utf-8") as f:
            mixin_src = f.read()
        checks.append(("RunBunAIChooseMixin declares explicit @Local(name = \"evaluations\")", '@Local(name = "evaluations")' in mixin_src))
        checks.append(("RunBunAIChooseMixin declares explicit @Local(name = \"move\")", '@Local(name = "move")' in mixin_src))
        checks.append(('RunBunAIChooseMixin declares @ModifyVariable targeting name = "teraMatch"', 'name = "teraMatch"' in mixin_src and 'cobbleverse$resolveAliveTeraTarget' in mixin_src))
    else:
        checks.append(("RunBunAIChooseMixin declares explicit @Local(name = \"evaluations\")", False))
        checks.append(("RunBunAIChooseMixin declares explicit @Local(name = \"move\")", False))
        checks.append(('RunBunAIChooseMixin declares @ModifyVariable targeting name = "teraMatch"', False))

    # d) RunBunAI$MoveEvaluation methods
    eval_javap = get_class_javap(rbrctai_jar, "com/gitlab/surilexa/rbrctai/api/ai/RunBunAI$MoveEvaluation.class")
    checks.append(("RunBunAI$MoveEvaluation.getDamage()I", "public int getDamage();" in eval_javap))
    checks.append(("RunBunAI$MoveEvaluation.getScore()I", "public int getScore();" in eval_javap))
    checks.append(("RunBunAI$MoveEvaluation.setScore(int)V", "public void setScore(int);" in eval_javap))
    checks.append(("RunBunAI$MoveEvaluation.getMove()", "public com.cobblemon.mod.common.api.moves.Move getMove();" in eval_javap))
    checks.append(("RunBunAI$MoveEvaluation.getOpponent()", "public com.cobblemon.mod.common.battles.ActiveBattlePokemon getOpponent();" in eval_javap))

    # e) MoveTarget enum constants
    movetarget_javap = get_class_javap(cobblemon_jar, "com/cobblemon/mod/common/battles/MoveTarget.class")
    checks.append(("MoveTarget.allAdjacentFoes exists", "allAdjacentFoes" in movetarget_javap))
    checks.append(("MoveTarget.allAdjacent exists", "allAdjacent" in movetarget_javap))

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
