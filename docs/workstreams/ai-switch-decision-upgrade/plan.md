# AI Switch Decision Upgrade — Candidate Plan

> Trạng thái: Corrected Candidate Plan — Ready for independent Plan Re-review
> Planner: P
> Ngày discovery: 2026-09-11
> Repository: `khangnhoang/cobbleverse-hell-mode-modernized`
> Baseline: `8161e99` trên branch `feat/ai-switch-decision-upgrade`
> Owner Source Package: `C:\Users\khang\.codex\attachments\5db7860f-e164-4cdf-9786-751e326b1c0c\pasted-text.txt`

## 1. Status, authority, and ownership

Đây là plan ứng viên cho workstream, được lập sau discovery độc lập của Planner P. Path này phù hợp convention trong `docs/workstreams/README.md:12-19`: `plan.md` là canonical specification của workstream.

Plan này chưa được freeze bởi Main, chưa được implementation, và chưa phải owner acceptance. Main Controller chịu trách nhiệm hash candidate plan, chuyển sang Plan Reviewer, và chỉ cho phép implementation sau khi plan được review/accept theo workflow.

Các authority bị loại khỏi phase này:

- Không chọn model từ `C:\VocaSpace\docs\native-multi-agent\plan.md`; `gpt-5.6-luna` với reasoning `max` là execution configuration do Main chỉ định, không phải plan semantics hay routing input.
- Không commit, push, tạo/cập nhật PR, merge, rebase, xóa branch, deploy, hoặc sửa production behavior trong lượt Planner này.
- Fresh Planner session là bắt buộc (`fork_context=false`); không gọi thêm agent và không tạo workflow-local review artifact trong repository.

## 2. Owner intent and scope

### Owner intent

Sửa quyết định switch của `RunBunAI` ở đúng hai boundary đã được chứng minh trong dependency bytecode:

1. loại false veto của Gate 2 khi các move non-fail thực tế không gây damage, đồng thời cho phép switch khi active đang chịu critical threat và chỉ còn offensive pressure thấp;
2. chấm candidate theo survivability có tính combined lethal trong Doubles, type hiệu dụng của dynamic move, defensive ability immunity có đầy đủ runtime guards, và giữ các native special case cần thiết.

Mục tiêu là giữ nguyên các native gate không liên quan, dùng lại native damage/ability semantics, và làm cho correction có thể kiểm chứng ở Layer 2–4 trước khi xin Layer 5 canary.

### Out of scope

- Mọi production/code/test implementation trong phase discovery này.
- Thay đổi `RunBunAI`, `PokeMathMax`, `RBTypeChart`, hoặc upstream JAR.
- Thay đổi `DynamicMoveResolver`, `PokeMathMaxMixin`, hay `RedirectAbilityGuard` nếu implementation chưa chứng minh các owner hiện hữu thiếu semantic cần thiết.
- Q1 partner redirection protection; đây là boundary khác và không cần để giải quyết bảy findings.
- Thay đổi trainer JSON, battle rules, datapack, model mapping, workflow governance, hoặc runtime behavior ngoài các symbol và injection seam được liệt kê ở plan này.
- `@Overwrite` toàn bộ `RunBunAI.isSwitching`; chỉ xem xét sau một upstream bytecode drift được chứng minh và một plan correction riêng.

## 3. Discovery boundary and repository baseline

### Repository evidence

- `git status --short --branch` tại discovery: `## feat/ai-switch-decision-upgrade...origin/main`; không có modified/untracked entry.
- `git log -1 --oneline`: `8161e99 Merge pull request #26 from khangnhoang/feat/kanto-koga-poison-sun-team`.
- Candidate path chưa tồn tại trước discovery; workstream convention được xác nhận bởi `docs/workstreams/README.md:12-19`.
- `companion-mod/src/main/resources/rct_legendary_rule.mixins.json:6-12` hiện chỉ đăng ký các mixin hiện hữu, trong đó có `RunBunAIChooseMixin`; chưa có `IsSwitchingOverrideMixin` hoặc `SwitchCandidateScoringMixin`.

### Dependency and bytecode evidence

Discovery dùng đúng local runtime artifacts:

- `rbrctai-fabric-1.21.1-0.15.4-beta.jar`.
- `rctapi-fabric-1.21.1-0.15.2-beta.jar`.
- `Cobblemon-fabric-1.7.3+1.21.1.jar`.
- Gradle `compileClasspath` resolve `io.github.llamalad7:mixinextras-fabric:0.4.1`; đây là version thực tế của build hiện tại, không phải version được ghi trong docs.

Các contract quan trọng đã được đọc bằng `javap -p -s`/`javap -p -c -l`, không suy đoán:

- `RunBunAI.isSwitching(List<RunBunAI.MoveEvaluation>, List<BattlePokemon>, BattlePokemon, List<ActiveBattlePokemon>, ActiveBattlePokemon, RBStatStages, boolean)`.
- `RunBunAI.isOHKO(List<Move>, BattlePokemon, BattlePokemon, ActiveBattlePokemon, RBStatStages)`.
- `RunBunAI.is2HKO(...)` và `RunBunAI.highestPercentDamageMove(BattlePokemon, BattlePokemon, ActiveBattlePokemon, RBStatStages)` đều nhận `ActiveBattlePokemon`.
- `RunBunAI$MoveEvaluation` có `getMove()`, `getOpponent()`, `getDamage()`, `getScore()`, `setScore(int)`, và `getStages()`.
- `PokeMathMax.damage(BattlePokemon, BattlePokemon, Move, ActiveBattlePokemon, boolean, boolean, RBStatStages)`, `isSuppressed(BattlePokemon)`, `ignoreAbilities(BattlePokemon)`, `hasAbility(...)`, và `isImmuneCheck(...)` tồn tại đúng với các descriptor đã dùng trong source/mixin hiện hữu.
- `BattleStates.getTransformationOrEffected(BattlePokemon)` tồn tại trong `rctapi` với descriptor `(BattlePokemon)Pokemon`.

### Baseline verification evidence

- `companion-mod` targeted baseline tests đã chạy thành công:

  ```text
  .\gradlew test --tests "com.cobbleverse.legendaryrule.strategy.dynamic.DynamicMoveResolverTest" --tests "com.cobbleverse.legendaryrule.strategy.guard.RedirectAbilityGuardTest" --tests "com.cobbleverse.legendaryrule.strategy.spread.RunBunAICallSiteContractTest"
  BUILD SUCCESSFUL in 8s
  4 actionable tasks: 2 executed, 2 up-to-date
  ```

- Runtime bytecode contract hiện hữu đã chạy bằng interpreter Python 3.11.6 đã cài trong workspace host:

  ```text
  C:\Users\khang\.pyenv\pyenv-win\versions\3.11.6\python.exe scripts/runtime-contract/test_rct_runtime_contract.py
  SUCCESS: All runtime bytecode contract checks PASSED.
  ```

  Đây chỉ là baseline cho các contract hiện hữu; nó chưa kiểm tra bảy correction mới.

## 4. Facts, assumptions, conflicts, and open questions

| Loại | Evidence / quyết định |
|---|---|
| Fact | `RunBunAI.isSwitching` có Gate 1 `Stream.anyMatch` tại bytecode offset 11; Gate 2 tính `totalMoves - failCount <= badMovesNeeded` và store boolean tại offset 82 (`istore 12`). Fail predicate native là `score <= -5`; low-score lambda là `score >= 6`. |
| Fact | Native `isSwitching` gọi `RunBunAI.isOHKO(oppMoves, oppBattlePokemon, partyCandidate, activeBattlePokemon, battleStatStages)`: `activeBattlePokemon` được truyền thẳng vào `PokeMathMax.damage(...)`. |
| Fact | `RunBunAI.choose` chỉ chọn `slotOpponent` từ `allOpponentActiveBattlePokemon`, có thêm một call cho side thứ hai tùy `battleTurn % 2`; native `switchScore` được khởi tạo bằng `istore 32` tại offset 930, LVT span bắt đầu offset 932, rồi đi vào `switchingScores.put(...)`: source line 453 bắt đầu tại offset 1609 và `Map.put` invocation là offset 1618. Không có vòng cộng damage của mọi opponent. |
| Fact | `DynamicMoveResolver.resolveEffectiveMove(Move, BattlePokemon, ActiveBattlePokemon, boolean)` là canonical resolver; Weather Ball được resolve theo context ở `DynamicMoveResolver.java:160-200`. `PokeMathMaxMixin.java:50` và `:76` đã dùng resolver này trước damage/immunity. |
| Fact | `RedirectAbilityGuard.java:101-124` dùng `PokeMathMax.ignoreAbilities(attacker)` và `PokeMathMax.hasAbility(...)`; Ability Shield được kiểm tra riêng trước Mold Breaker bypass. Đây là pattern guard hiện hữu cần tái sử dụng, không nhân bản. |
| Fact | `RunBunAICallSiteContractTest.java:48-49` assert `RunBunAI$MoveEvaluation.getOpponent()` có return type `ActiveBattlePokemon`; `SpreadMoveValuationContext.java:71-76` đọc wrapper này rồi lấy `getBattlePokemon().getMaxHealth()`. `javap -p -s` cũng ghi field `opponent` và descriptor `()Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;`. |
| Fact | `WeatherAccuracyValuationStrategy.java:397-405` đã dùng pattern association `ActiveBattlePokemon` identity trước, rồi underlying `BattlePokemon` identity/UUID equality; đây là repository precedent cho mapping evaluation → eligible target. |
| Fact | `RunBunAI.isSwitching` native loop lọc incoming opponent theo `target != null`, `!target.isGone()`, `target.getBattlePokemon() != null`, rồi lấy `oppBP.getMoveSet().getMoves()` trước khi gọi `isOHKO(oppMoveSet, oppBP, ally, activeBattlePokemon, battleStatStages)`; không có bộ lọc `maxHP > 0` ở boundary này. |
| Fact | `PokeMathMax.isSuppressed` raw-dereference `actor.battle`, `battle.getActivePokemon()`, `getEffectedPokemon().getAbility().getName()`, và `getHeldItemManager().showdownId(...)`; mỗi non-null active `BattlePokemon` trong iterable cũng phải có raw effected ability/name. `ignoreAbilities` gọi lại `isSuppressed` rồi đọc raw ability/name; `damage` đọc move category và raw attacker ability trước private path. |
| Fact | `RBTypeChart.getEffectiveness(ElementalType, ElementalType, BattlePokemon)` có các ability immunity branch, nhưng không phải authority duy nhất cho suppression/Mold Breaker; native damage/immunity path mới là semantic authority. |
| Fact | MixinExtras 0.4.1 `WrapOperationInjector.getEffectiveArgTypes` thêm owner/receiver cho non-static method invocation; `Operation.call(Object...)` nhận target arguments. Vì vậy target `Stream.anyMatch(Predicate)Z` có handler arguments `Stream`, `Predicate`, `Operation`. |
| Assumption | `isUnderCriticalThreat` phải giữ đúng context native: mỗi opponent dùng `opp.getBattlePokemon()` làm attacker, `self` làm defender, và dùng cùng `activeBattlePokemon`/`RBStatStages` được truyền vào `isSwitching`. Opponent null/gone được bỏ qua như native; danh sách eligible rỗng trả về `false` để không tạo threat ảo. |
| Assumption | “Lethal” dùng native `RunBunAI.isOHKO` cho single-opponent result; combined Doubles cộng các `D_i` tuyệt đối do cùng native `PokeMathMax.damage` tính theo hướng opponent → candidate. Đây là bounded heuristic, không phải mô phỏng toàn bộ multi-hit/secondary effects hoặc tương tác Sturdy/Focus Sash giữa nhiều hit. |
| Conflict | `docs/architecture/companion-mod.md:17` ghi MixinExtras `0.5.0`, Gradle `compileClasspath` resolve `0.4.1`, còn log cũ `companion-mod/run/logs/latest.log:54,72,86` ghi runtime khác (`mixinextras 0.5.0`, Sponge Mixin `0.17.0`). Chỉ classpath của build và một fresh `runServer` log được xem là current evidence; không dùng log cũ để kết luận. |
| Decision | Vì native output là `Map<BattlePokemon, Integer>`, `SwitchCandidateScorer` dùng packed lexicographic `int`: `tierRank → coverageTotal → defenseRank → nativeSwitchScore`. Constants và maximum được chốt ở §6.2; mọi phép tính dùng `long`, kiểm tra bound trước cast, và native score ngoài `[-1,10]` là contract failure. |
| Decision | Low pressure dùng `max(getDamage() / getOpponent().getBattlePokemon().getMaxHealth())` trên evaluations có `ActiveBattlePokemon` target-associated hợp lệ; compare strict `< 0.20`, không sum target và không dùng current HP/self HP. Invalid/empty denominator không tạo evidence để mở FIX B. |
| Decision | All-Tier-2 là acceptance hợp lệ: native map vẫn chọn candidate theo coverage, rồi defensive severity, rồi native score; nếu toàn bộ key giống nhau thì giữ native iteration/tie behavior. Không có “no switch” mới được thêm vào scorer. |
| Open Question | Không còn open question cần Owner Decision cho candidate này. Dependency SHA/classpath drift, thay đổi LVT/descriptor, hoặc native helper contract drift là stop condition cần discovery mới, không phải giả định để implementation tiếp tục. |

## 5. Adjudication of the seven findings

| # | Verdict | Evidence và symbol | Causal impact | Smallest correction và semantic owner |
|---|---|---|---|---|
| 1 | **Confirmed** | Dependency `RunBunAI.isSwitching` offset 21–82 tính `totalMoves - failCount <= badMovesNeeded`; Gate 1 `anyMatch` ở offset 11 là call khác. Owner Source Package `pasted-text.txt:L44,L65-L149` cũng chỉ Hook 1 vào Gate 1. | Status move có score `>= 6` bị tính như non-fail trong Gate 2, nên Hook 1 không thể sửa false veto kiểu “1 damaging/3 status”. | Thêm Hook 2 bằng selector executable ở §6.1; `DeadMatchupDetector` dùng công thức target-specific ở §6.1 và chỉ điều chỉnh `hasLowScore` theo `getScore()`/`getDamage()` và critical-threat/low-pressure policy. Giữ native true path và các random/HP gate. Owner policy: `DeadMatchupDetector`; bytecode boundary: `IsSwitchingOverrideMixin`. |
| 2 | **Confirmed** | `RunBunAI.isOHKO` descriptor chính xác là `(List<Move>, BattlePokemon, BattlePokemon, ActiveBattlePokemon, RBStatStages)Z`; bytecode gọi `PokeMathMax.damage(..., activeBattlePokemon, false, false, stages)`. Owner Source Package `pasted-text.txt:L164-L180` đã nêu context nhưng API proposed cần giữ đúng signature. | Bỏ `activeBattlePokemon` làm mất slot/weather/context cần cho damage và có thể làm threat detector khác native `isSwitching`. | `DeadMatchupDetector.isUnderCriticalThreat(BattlePokemon self, List<ActiveBattlePokemon> opponents, ActiveBattlePokemon activeBattlePokemon, RBStatStages stages)`; reuse native `RunBunAI.isOHKO`, không reimplement damage. Owner adapter: `DeadMatchupDetector`; upstream contract owner: `RunBunAI.isOHKO`. |
| 3 | **Confirmed** | `RunBunAI.choose` bytecode/LVT chỉ có `slotOpponent` và một call second-side theo `battleTurn % 2`; không có aggregate all-opponent damage. Owner Source Package `pasted-text.txt:L251-L270` yêu cầu combined lethal. | Doubles candidate nhận 60% + 60% vẫn có thể bị xếp an toàn nếu từng hit không phải OHKO. | `SwitchCandidateScorer.isSurvivable(...)` duyệt mọi eligible opponent theo `ActiveBattlePokemon` list, lấy highest effective one-hit damage từng opponent, loại Tier 1 nếu bất kỳ single hit hoặc tổng damage `>= current HP`. Cách aggregate và saturating boundary được chốt ở §6.2. Owner: `SwitchCandidateScorer`; damage semantics: `PokeMathMax`/existing mixin. |
| 4 | **Confirmed** | Owner Source Package `pasted-text.txt:L272` ghi Tier 2 chỉ rank bằng offensive coverage và loại defensive tie-break. Native map chỉ giữ một `Integer`, nên omission là thật. | Khi không có Tier 1, candidate chết nhanh và candidate còn cơ hội sống đều bị coi như nhau nếu offensive score bằng nhau. | Dùng packed key cụ thể: `tierRank → coverageTotal → defenseRank → nativeSwitchScore`; công thức, bounds, overflow proof, native-bonus precedence và all-Tier-2 acceptance được chốt ở §6.2. Thay score tại invocation argument của `switchingScores.put`, sau toàn bộ native `switchScore` bonus, không thay tại `istore 32` khởi tạo. Owner policy/representation: `SwitchCandidateScorer`; bytecode boundary: `SwitchCandidateScoringMixin`; không còn Owner Decision mở cho blocker này. |
| 5 | **Confirmed** | Owner table `pasted-text.txt:L297-L328,L346-L359` đọc raw effected ability và chỉ mô tả guard. Dependency `BattleStates.getTransformationOrEffected(...)`, `PokeMathMax.isSuppressed/hasAbility/ignoreAbilities`, `PokeMathMax.isImmuneCheck`, và pattern `RedirectAbilityGuard.java:101-124` chứng minh các guard phải đi cùng lookup. `RBTypeChart` còn có `dryskin`/`wellbakedbody`, không có trong table attachment. | Raw/incomplete table có thể đánh dấu immune khi ability bị suppression, hoặc bỏ qua transformed ability; Mold Breaker family có thể bị chặn sai; thiếu entry native tạo false damage. `BattleStates.getTransformationOrEffected` cũng dereference actor/battle/state nên thiếu context phải có behavior riêng. | Guard sequence và missing-context result được chốt ở §6.2: context gate → effective ability/table hint → existing suppression/bypass/shield guards → chỉ active table immunity mới được short-circuit; mọi case khác dùng native damage, còn unknown là unsafe chứ không phải immune. Owner: `SwitchCandidateScorer`; data owner: `AbilityImmunityTable`; guard authority: existing `PokeMathMax`/`RedirectAbilityGuard` semantics. |
| 6 | **Refuted as a defect in the current package; retained as an implementation invariant** | Owner Source Package `pasted-text.txt:L276-L291` đã yêu cầu `DynamicMoveResolver`; repository `DynamicMoveResolver.java:63-88,160-200` canonicalizes Weather Ball; `PokeMathMaxMixin.java:50,76` dùng effective move. Không có scorer Phase 2 hiện tại để audit. | Nếu scorer mới dùng raw `move.getType()`, Weather Ball và các dynamic moves sẽ bị prefilter sai trước damage. | Không sửa resolver hiện hữu. Mọi type-first call phải dùng canonical 4-argument resolver với context; không loại move chỉ vì raw type. Type chart là prefilter; damage/immunity native là final check. Owner: `SwitchCandidateScorer`, semantic resolver: `DynamicMoveResolver`. |
| 7 | **Refuted as a defect in the supplied Hook 1 signature; runtime application remains a verification item** | Owner Source Package `pasted-text.txt:L65-L89` đã không thêm `@Local`. MixinExtras 0.4.1 injector chứng minh handler nhận receiver `Stream`, argument `Predicate`, rồi `Operation`; dependency call target là `Stream.anyMatch(Predicate)Z` ở `isSwitching` offset 11. | Thêm `@Local` không cần thiết làm handler fragile; nếu target signature đổi thì mixin apply phải fail loud thay vì âm thầm dùng local sai. | Giữ handler tối thiểu `Stream<MoveEvaluation>, Predicate<MoveEvaluation>, Operation<Boolean>`, thêm explicit `require=1`, và gọi `original.call(stream, newPredicate)`. Owner boundary: `IsSwitchingOverrideMixin`; transformed application được xác nhận riêng ở Layer 4. Không thêm `@Overwrite` fallback trong scope này. |

## 6. Detailed correction design

### 6.1 Phase 1 — Gate 1/Gate 2 correction

Planned files:

- Create `companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/switchai/DeadMatchupDetector.java`.
- Create `companion-mod/src/main/java/com/cobbleverse/legendaryrule/mixin/IsSwitchingOverrideMixin.java`.
- Create `companion-mod/src/test/java/com/cobbleverse/legendaryrule/strategy/switchai/DeadMatchupDetectorTest.java`.
- Modify `companion-mod/src/main/resources/rct_legendary_rule.mixins.json` to register the new mixin only after the source compiles.

`DeadMatchupDetector` contract:

- `isLowOffensivePressure(List<RunBunAI.MoveEvaluation> evaluations, List<ActiveBattlePokemon> opponents)` phải dùng target-specific native-derived ratio. Xây tập `eligibleTargets` từ `opponents`: `target != null`, `!target.isGone()`, `target.getBattlePokemon() != null`, và `target.getBattlePokemon().getMaxHealth() > 0`. `MoveEvaluation.getOpponent()` là `ActiveBattlePokemon`; một evaluation chỉ hợp lệ khi wrapper đó là cùng object với `target`, hoặc cả hai có underlying `BattlePokemon` non-null và là cùng object hay có `UUID` non-null bằng nhau. Không so sánh wrapper trực tiếp với `BattlePokemon`, và không ghép một evaluation với target ngoài `opponents`.
- Với mỗi evaluation hợp lệ, lấy `damage = max(0, evaluation.getDamage())`, `evaluationOpponent = evaluation.getOpponent()`, rồi `targetMaxHP = evaluationOpponent.getBattlePokemon().getMaxHealth()`. Tính `ratio = (double) damage / (double) targetMaxHP`; aggregate là `maxRatio = max(maxRatio, ratio)` trên mọi target/evaluation, không sum giữa targets và không chia cho `self`/current HP. `lowPressure = maxRatio < 0.20` (strict; đúng `0.20` là không thấp). `getOpponent()` descriptor phải được giữ đúng `()Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;`.
- `evaluations == null/empty`, `opponents == null/empty`, evaluation/evaluationOpponent null, evaluation target không association, hoặc mọi target-associated denominator `maxHP <= 0` đều trả `false` vì không có evidence đo được để mở FIX B. Nếu có ít nhất một target-associated evaluation với `maxHP > 0` nhưng toàn bộ damage bằng `0` (status/non-damaging), `maxRatio = 0` và trả `true`; FIX A vẫn là đường chính cho toàn status moveset. Một target hợp lệ có nhiều evaluation chỉ góp giá trị lớn nhất của chính target đó vào `maxRatio`; nhiều targets được aggregate bằng global max, không phải tổng.
- `isUnderCriticalThreat(BattlePokemon self, List<ActiveBattlePokemon> opponents, ActiveBattlePokemon activeBattlePokemon, RBStatStages stages)` phải mirror native opponent filtering và gọi `RunBunAI.isOHKO` với descriptor đã xác minh.
- Critical threat chỉ true khi có ít nhất một eligible opponent và mọi eligible opponent đều có OHKO; opponent null/gone bị bỏ qua.

`IsSwitchingOverrideMixin` contract:

- Full target descriptor của cả hai hook là:

  ```text
  isSwitching(Ljava/util/List;Ljava/util/List;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Ljava/util/List;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/gitlab/surilexa/rbrctai/api/ai/utils/RBStatStages;Z)Z
  ```

- Hook 1 dùng `@WrapOperation(method = <full descriptor>, at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;anyMatch(Ljava/util/function/Predicate;)Z"), require = 1, remap = false)`; handler không có `@Local` và có đúng `Stream<RunBunAI.MoveEvaluation> stream`, `Predicate<RunBunAI.MoveEvaluation> nativePredicate`, `Operation<Boolean> original`. Handler forward `original.call(stream, newPredicate)` và chỉ thay predicate cho status/non-damaging policy.
- Hook 1 thay predicate chỉ để status/non-damaging evaluation không tạo score veto; native predicate vẫn được giữ cho damaging evaluation. `original.call(...)` là fallback/native path.
- Hook 2 dùng selector executable, không dùng raw offset làm selector:

  ```java
  @ModifyVariable(
      method = "isSwitching(Ljava/util/List;Ljava/util/List;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Ljava/util/List;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/gitlab/surilexa/rbrctai/api/ai/utils/RBStatStages;Z)Z",
      at = @At(value = "STORE", opcode = Opcodes.ISTORE),
      name = "hasLowScore",
      index = 12,
      ordinal = 0,
      require = 1,
      remap = false
  )
  ```

  Evidence contract của selector là LVT `hasLowScore` slot 12, type `Z`, một store `istore 12` tại bytecode offset 82. Handler return/input contract là `private static boolean adjustHasLowScore(boolean nativeHasLowScore, ...)`; bind bằng exact name/index/type: `@Local(name="evaluations", index=0, argsOnly=true) List<RunBunAI.MoveEvaluation>`, `@Local(name="self", index=2, argsOnly=true) BattlePokemon`, `@Local(name="opponents", index=3, argsOnly=true) List<ActiveBattlePokemon>`, `@Local(name="activeBattlePokemon", index=4, argsOnly=true) ActiveBattlePokemon`, và `@Local(name="battleStatStages", index=5, argsOnly=true) RBStatStages`. Không capture thêm local không có trong contract này.
- Hook 2 giữ native `nativeHasLowScore == true`; nếu native false, áp dụng lần lượt FIX A (mọi non-fail evaluation đều non-damaging) và FIX B (`criticalThreat && lowPressure`). Không thay đổi random gate, HP gate, party traversal, hoặc `RunBunAI.isSwitching` upstream bytecode.
- Layer 3 raw assertion phải fail nếu full descriptor, LVT name/type/slot, `istore 12`, hoặc selector annotations (`STORE` + `ISTORE` + name/index/ordinal + `require=1`) không khớp exact `rbrctai-fabric-1.21.1-0.15.4-beta.jar`.

### 6.2 Phase 2 — Candidate survivability and scoring

Planned files:

- Create `companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/switchai/AbilityImmunityTable.java`.
- Create `companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/switchai/SwitchCandidateScorer.java`.
- Create `companion-mod/src/main/java/com/cobbleverse/legendaryrule/mixin/SwitchCandidateScoringMixin.java`.
- Create `companion-mod/src/test/java/com/cobbleverse/legendaryrule/strategy/switchai/AbilityImmunityTableTest.java`.
- Create `companion-mod/src/test/java/com/cobbleverse/legendaryrule/strategy/switchai/SwitchCandidateScorerTest.java`.
- Modify the same mixin config to register `SwitchCandidateScoringMixin`.

`AbilityImmunityTable` remains data-only. Its verified minimum mapping must include:

| Ability id | Immune type |
|---|---|
| `stormdrain`, `waterabsorb`, `dryskin` | Water |
| `voltabsorb`, `lightningrod`, `motordrive` | Electric |
| `flashfire`, `wellbakedbody` | Fire |
| `sapsipper` | Grass |
| `levitate`, `eartheater` | Ground |

`SwitchCandidateScorer` owns a private result carrier `DamageEvidence(known, damage)` plus the singleton `DamageEvidence.UNKNOWN`: a known result carries the non-negative native damage integer (table immunity is known damage `0`), while UNKNOWN has `known=false` and carries no usable numeric damage. UNKNOWN is excluded from coverage and makes the candidate Tier 2/unsafe; it is never coerced to immune or to numeric damage `0`. This carrier is an implementation detail of `SwitchCandidateScorer`, not a new shared API or file.

Caller rules:

- Before any native ability helper, perform the verified raw-dereference precondition check. `candidate`, `opponentBattlePokemon`, `activeBattlePokemon`, `activeBattlePokemon.getBattlePokemon()`, and `stages` must be non-null; both Pokémon must have non-null `getActor()` and `getActor().getBattle()`; both raw `getEffectedPokemon()`, raw ability, and raw ability name must be non-null; and both `getHeldItemManager()` values must be non-null because the native helpers call `showdownId(...)`. Before `PokeMathMax.damage`, `move` and `move.getDamageCategory()`/its name must also be non-null. The candidate battle’s `getActivePokemon()` iterable used by native `isSuppressed` must be non-null; a null active wrapper or null underlying `BattlePokemon` is skipped exactly as native does, while every non-null underlying active `BattlePokemon` must have non-null raw effected Pokémon, ability, and ability name before the helper is called. These are preconditions, not replacement guard logic.
- If any precondition is missing, return `DamageEvidence.UNKNOWN`, classify the candidate Tier 2/unsafe, do not call `BattleStates.getTransformationOrEffected`, `PokeMathMax.isSuppressed`, `PokeMathMax.ignoreAbilities`, or `PokeMathMax.damage`, and do not use the table to infer immunity. If a native helper throws or returns an incomplete effective object after the precondition check, do not catch/coerce/reimplement it: propagate a deterministic contract failure so Layer 3/4 stops, while missing data remains the deterministic `UNKNOWN` path.
- After the raw precondition gate, resolve both sides through the existing `BattleStates.getTransformationOrEffected(candidate)` and `BattleStates.getTransformationOrEffected(opponentBattlePokemon)` APIs. If either effective `Pokemon`, ability, or ability name is null, return `DamageEvidence.UNKNOWN` without a table shortcut and without calling `PokeMathMax.damage`; this is the missing-context behavior because the verified native immunity/damage path dereferences those values. Do not replace this dependency API with a raw `getEffectedPokemon()` lookup.
- Preserve native guard authority and order: (1) call `PokeMathMax.isSuppressed(candidate)`; its verified native implementation gives the candidate’s own `Ability Shield` precedence over Neutralizing Gas-style suppression, and true makes table immunity inactive; (2) call `PokeMathMax.ignoreAbilities(opponentBattlePokemon)`; this is suppression-aware and covers the attacker’s Mold Breaker/Teravolt/Turboblaze family; (3) read the candidate’s held-item id only through the existing `HeldItemManager.showdownId(candidate)` and recognize exactly `abilityshield`; (4) only an effective candidate table match with suppression false and either no attacker bypass or the Ability Shield exception may short-circuit resolved move damage to `0`. No new suppression/ignore implementation is allowed.
- Ability Shield therefore has precedence at the existing native/pattern bypass boundary, while `PokeMathMax.isSuppressed` remains the authority for whether the candidate’s own shield neutralizes suppression. This preserves `PokeMathMax.isSuppressed`, `PokeMathMax.isImmuneCheck`, and `RedirectAbilityGuard.java:117-122`; the scorer must not reorder or duplicate those APIs.
- For every non-short-circuited move with complete effective-ability context, call `PokeMathMax.damage(opponentBattlePokemon, candidate, resolvedMove, activeBattlePokemon, false, false, stages)`. The existing `PokeMathMaxMixin`/native damage path is final authority for non-table immunities and actual damage; a table hint must never convert an inactive/suppressed/bypassed ability into `0`. If the dependency returns an unexpected result, preserve the native result and record the guard case in verification rather than silently overriding it.
- Resolve every move before type comparison with `DynamicMoveResolver.resolveEffectiveMove(move, attacker, activeBattlePokemon, false)`. `false` is deliberate: native `isOHKO`/`highestPercentDamageMove` scoring calls `PokeMathMax.damage` with `predictTera=false`; the scorer does not invent a second Tera-prediction context.

`SwitchCandidateScorer` contract:

- `score(BattlePokemon candidate, List<ActiveBattlePokemon> opponents, ActiveBattlePokemon activeBattlePokemon, RBStatStages stages, BattlePokemon currentActive, int nativeSwitchScore)` is the only policy entrypoint used by the scoring mixin. The last argument is the untouched native `switchScore` value at the proven replacement seam; this preserves native bonuses without reimplementing them.
- Define coverage targets once from `opponents`: `target != null`, `!target.isGone()`, `target.getBattlePokemon() != null`, and `target.getBattlePokemon().getMaxHealth() > 0`; each outgoing evaluation/damage call is associated with that target wrapper or its underlying `BattlePokemon` by object identity or non-null UUID equality. Targets failing this coverage filter provide no coverage, but incoming eligibility follows the separate native filter below.
- For each eligible opponent target, resolve each candidate move and calculate native damage. If at least one resolved move is super-effective (`RBTypeChart` effectiveness `>= 2.0`) and has positive native damage, the target’s coverage is the maximum such move; otherwise use the maximum positive native damage from neutral resolved moves (`effectiveness == 1.0`). A `RBTypeChart` zero caused by an ability-sensitive path is not a discard decision: use native damage/guard result as the fallback. NVE/immune moves with no positive native damage contribute `0`.
- Convert each target’s best damage to `targetCoverage = min(1000, ceilDiv((long) max(0, damage) * 1000, targetMaxHP))`, where `1000` means `100.0%` of that target’s max HP, `targetMaxHP > 0`, and positive `ceilDiv(n, d)` is `(n + d - 1) / d` in `long` arithmetic. Aggregate offensive coverage with saturating addition after every target: `coverageTotal = min(2000, coverageTotal + targetCoverage)`. Thus Singles is one target, Doubles rewards coverage of both targets, and arbitrary malformed lists cannot overflow the key or the numerator.
- Incoming damage has a separate, native-mapped source and direction. Build `eligibleIncomingOpponents` with exactly the native `isSwitching` loop filter: `target != null`, `!target.isGone()`, and `target.getBattlePokemon() != null`; do not add a `maxHP > 0` filter here because the verified native loop does not. For each target, let `opponentBP = target.getBattlePokemon()` and read `incomingMoves = opponentBP.getMoveSet().getMoves()`, the same source used by native `isSwitching` before its `isOHKO`/`is2HKO` calls. The direction is `attacker = opponentBP`, `defender = candidate`, with the exact `activeBattlePokemon` and `stages` passed to `score`.
- For a non-null, non-empty `incomingMoves`, resolve each non-null move with `DynamicMoveResolver.resolveEffectiveMove(move, opponentBP, activeBattlePokemon, false)` and compute `PokeMathMax.damage(opponentBP, candidate, resolvedMove, activeBattlePokemon, false, false, stages)`; `D_i` is the maximum non-negative returned native damage for that opponent. Status moves remain in the list and naturally contribute native damage `0`; no manual status/PP/power filter is permitted. A null moveset/list or a null move element is malformed context and yields `DamageEvidence.UNKNOWN` for the candidate before any native helper call; a non-null empty list is known `D_i = 0`. Do not use `highestPercentDamageMove`, because its native `ceil(damage / defender.maxHP)` result loses the absolute damage needed for aggregation.
- For each target also compute `singleLethal_i = RunBunAI.isOHKO(incomingMoves, opponentBP, candidate, activeBattlePokemon, stages)` with the exact descriptor `(List<Move>, BattlePokemon, BattlePokemon, ActiveBattlePokemon, RBStatStages)Z`; this preserves native Sturdy/Focus Sash handling for the single-target decision. A true `singleLethal_i` makes the candidate Tier 2. Combined Doubles remains the bounded sum of the absolute `D_i` values, so it can identify two non-lethal native hits whose total reaches the candidate HP; it is not presented as a second native battle simulator.
- If `opponents` contains no eligible incoming target, classify incoming evidence as UNKNOWN and Tier 2 rather than accepting Tier 1 vacuously. If any eligible target has null moveset/list, a null move element, unresolved effective move, or missing raw context, propagate UNKNOWN/Tier 2; do not skip that target. `candidate.getHealth() <= 0` is immediately Tier 2 with no division and no incoming helper call. For `candidate.getHealth() > 0`, maintain two accumulators: `classificationDamage = min(currentHP, classificationDamage + min(currentHP, D_i))` for the lethal boundary, and the unsaturated-but-capped severity accumulator specified below. Tier 1 requires every `singleLethal_i` false, every known `D_i < currentHP`, and `classificationDamage < currentHP`; equality is unsafe because native `isOHKO` uses `damage >= currentHP`. Unknown context is Tier 2, not immune/safe.
- Tier 2 includes single OHKO, combined lethal, zero/unknown HP, and unknown incoming evidence. It is still selected when all candidates are Tier 2; there is no new “no switch” result. The native map max chooses the least-bad key deterministically.

#### Determinate packed score

The native score range and seam are known from `RunBunAI.choose` bytecode: the speed/OHKO branch contributes `[-1,5]` at offsets 1206–1342, then native `ditto +2`, `wynaut/wobbuffet +2`, and weather-setter `+1` occur through offset 1606. Therefore `nativeSwitchScore` is bounded `[-1,10]`, with `nativeRank = nativeSwitchScore + 1` in `[0,11]`. Any value outside this range is a contract failure, not a clamp.

Use these exact integer fields:

```text
TIER1 = 1, TIER2 = 0
TARGET_COVERAGE_MAX = 1000
COVERAGE_TOTAL_MAX = 2000
DEFENSE_RATIO_MAX = 40000       // 4000.0% sentinel, in tenths of a percent
KNOWN_DEFENSE_RATIO_MAX = 39999 // reserve 40000 for UNKNOWN/invalid HP
NATIVE_MIN = -1, NATIVE_MAX = 10, NATIVE_RADIX = 12

// Evaluate this block only for currentHP > 0 and fully known incoming data.
classificationDamage = 0
severityDamageCap = floor((long) KNOWN_DEFENSE_RATIO_MAX * currentHP / 10000)
severityDamage = 0
for each known D_i:
    classificationDamage = min((long) currentHP,
                               classificationDamage
                               + (long) min(currentHP, max(0, D_i)))
    severityDamage = min(severityDamageCap,
                         severityDamage
                         + (long) min(Integer.MAX_VALUE, max(0, D_i)))
incomingRatio = currentHP <= 0 or UNKNOWN
    ? DEFENSE_RATIO_MAX
    : min(KNOWN_DEFENSE_RATIO_MAX,
          ceil(10000L * severityDamage / currentHP))
defenseRank = DEFENSE_RATIO_MAX - incomingRatio
// Tier 1 does not use defensive tie-break; keep it constant.
defenseRank = tier == TIER1 ? DEFENSE_RATIO_MAX : defenseRank

score = (((tierRank * (COVERAGE_TOTAL_MAX + 1) + coverageTotal)
          * (DEFENSE_RATIO_MAX + 1) + defenseRank)
         * NATIVE_RADIX + nativeRank)
```

All intermediate arithmetic uses `long`; both `classificationDamage` and `severityDamage` are `long` accumulators. `classificationDamage` is saturated at `currentHP`, while `severityDamage` is separately saturated at `severityDamageCap` and therefore retains bounded overkill information instead of collapsing every lethal case to one defense rank. For `currentHP > 0`, `floor(KNOWN_DEFENSE_RATIO_MAX * currentHP / 10000)` ensures the known ratio is at most `39999`; `40000` is reserved for UNKNOWN/invalid HP. The packed maximum is
`(((1 * 2001 + 2000) * 40001 + 40000) * 12 + 11) = 1,921,008,023`, below `Integer.MAX_VALUE`; assert this before the final cast. Native `BattlePokemon.getHealth()` and `PokeMathMax.damage(...)` are both `int`, so every input is widened before multiplication/addition and every non-negative damage is bounded before accumulation. Precedence is therefore fixed: Tier 1 > Tier 2; within a tier, aggregate offensive coverage > Tier 2 defensive severity (lower incoming ratio) > untouched native score/bonuses. For Tier 1, defensive rank is constant and native bonuses remain the final tie-break. The existing native `Map<BattlePokemon,Integer>` max selection remains unchanged.

All-Tier-2 acceptance is explicit: select the candidate with highest `coverageTotal`; if tied, highest `defenseRank`; if still tied, highest `nativeSwitchScore`; if the packed key is identical, retain native map iteration/tie behavior. The defensive representation must distinguish equal-coverage Tier 2 candidates even when both are lethal: with `currentHP=100`, `D_sum=100` gives `incomingRatio=10000`, `defenseRank=30000`, while `D_sum=300` gives `incomingRatio=30000`, `defenseRank=10000`; both remain Tier 2, but overkill is ordered as worse. Tests must prove (a) a Tier 1 candidate beats every Tier 2 candidate even with native bonus, (b) same-coverage Tier 2 candidates prefer lower combined incoming damage including the 100-vs-300 example, (c) a higher coverage candidate beats a safer but lower-coverage Tier 2 candidate, (d) known over-cap severity is deterministically capped at `39999` while UNKNOWN is `40000`, and (e) the packed maximum and negative native minimum do not overflow.

`SwitchCandidateScoringMixin` contract:

- Full target descriptor is:

  ```text
  choose(Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Lcom/cobblemon/mod/common/battles/BattleSide;Lcom/cobblemon/mod/common/battles/ShowdownMoveset;Z)Lcom/cobblemon/mod/common/battles/ShowdownActionResponse;
  ```

- The mixin declares `@Shadow(remap = false) private RBStatStages battleStatStages;` against owner `com/gitlab/surilexa/rbrctai/api/ai/RunBunAI`, field name `battleStatStages`, and descriptor `Lcom/gitlab/surilexa/rbrctai/api/ai/utils/RBStatStages;`. This is the private instance field used by native choose; it is not captured as a method-local.
- Use an executable `@WrapOperation` selector at the final score consumer, not at the initialization store:

  ```java
  @WrapOperation(
      method = "choose(Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Lcom/cobblemon/mod/common/battles/BattleSide;Lcom/cobblemon/mod/common/battles/ShowdownMoveset;Z)Lcom/cobblemon/mod/common/battles/ShowdownActionResponse;",
      at = @At(
          value = "INVOKE",
          target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
          ordinal = 0
      ),
      require = 1,
      remap = false
  )
  ```

- Handler exact erased target types and captured locals are:

  ```java
  private Object replaceSwitchScore(
      Map<?, ?> switchingScores,
      Object possibleSwitchKey,
      Object nativeBoxedScore,
      Operation<Object> original,
      @Local(name = "possibleSwitch", index = 38) BattlePokemon possibleSwitch,
      @Local(name = "allOpponentActiveBattlePokemon", index = 26) List<ActiveBattlePokemon> opponents,
      @Local(name = "activeBattlePokemon", index = 1, argsOnly = true) ActiveBattlePokemon activeBattlePokemon,
      @Local(name = "battlePokemon", index = 13) BattlePokemon currentActive,
      @Local(name = "switchScore", index = 32) int nativeLocalScore
  )
  ```

  It must assert `possibleSwitchKey == possibleSwitch`, assert `nativeBoxedScore instanceof Integer`, and assert `((Integer) nativeBoxedScore).intValue() == nativeLocalScore`; a mismatch is a contract failure, not a fallback. It computes `int replacementScore = SwitchCandidateScorer.score(possibleSwitch, opponents, activeBattlePokemon, this.battleStatStages, currentActive, nativeLocalScore)`, then calls `original.call(switchingScores, possibleSwitchKey, Integer.valueOf(replacementScore))` exactly once. The effective non-static `@WrapOperation` target arguments are therefore `Map`, `Object`, `Object`, `Operation<Object>`; the `@Local` parameters above are the only captured method locals.
- Dependency evidence for the selector: `switchScore` is `istore 32` at offset 930 with LVT slot 32 starting at 932; `possibleSwitch` is slot 38, `allOpponentActiveBattlePokemon` slot 26, and `battlePokemon` slot 13. The `Map.put` call-site window starts at offset 1609 (`aload 31` for `switchingScores`) and invokes `java/util/Map.put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` at offset 1618 with `possibleSwitch`/`switchScore`.
- Replace only the candidate score; do not replace native candidate enumeration, random choice, speed checks, selected-opponent selection, or final `switchingScores` max selection.
- Raw Layer 3 must assert the full descriptor, private field descriptor, exact LVT slots/types, `istore 32`, `Map.put` call window, and all selector attributes with `require=1`. If any target LVT or instruction window does not match exact dependency bytes, fail the contract and stop. Do not silently fall back to `@Overwrite`.

## 7. Correction sequencing and same-session review boundary

1. Main records this candidate path and computes a plan hash at baseline `8161e99`; no implementation starts from an unhashed or modified candidate.
2. Plan Reviewer independently rechecks the seven adjudications, exact descriptors, executable selectors, score formula/bounds, guard order, and raw-vs-transformed verification boundary.
3. If review finds a blocking evidence issue, Main may send at most one bounded correction request to this same Planner session. Planner updates only this candidate plan, Main rehashes it, and Plan Reviewer rechecks the new hash. No production file is edited during this reconciliation; a second unresolved blocker routes to Owner Decision and sets `ready=false`.
4. After plan PASS/acceptance, Implementor works only within the exact file/symbol scope above. Implementation Reviewer reviews the resulting diff and fresh verification artifacts in the same managed episode.
5. Any mixin-apply failure, dependency drift, or new cross-module coupling is a stop condition and requires a new evidence/review boundary; it is not permission to broaden scope.

## 8. Verification matrix

### Layer 0 — Markdown and plan structure

- Validate relative links and file paths in this plan; keep the candidate plan within repository path ownership.
- Confirm the plan contains no implementation diff, model mapping, remote-action instruction, or production claim.

### Layer 2 — Java unit and boundary tests

Add focused tests for:

- Gate 2 status-move case: one non-fail damaging move plus three status moves; the correction must not infer offensive pressure from status score.
- Low pressure denominator/association: use `ActiveBattlePokemon` wrappers returned by `MoveEvaluation.getOpponent()`, associate only by wrapper or underlying `BattlePokemon` identity/UUID, and read the denominator from `evaluationOpponent.getBattlePokemon().getMaxHealth()`. `15/100` and `15/300` across two active targets gives global `maxRatio=0.15` and true; `60/300=0.20` is false; a target outside `opponents` is ignored; a zero-max-HP target supplies no measurement and cannot open FIX B; zero-damage evaluations with one valid target produce ratio `0`; null/empty evaluations or no associated valid target are false.
- `isUnderCriticalThreat`: one opponent OHKO vs every eligible opponent OHKO; null/gone and empty eligible lists.
- Exact lethal boundary: single damage equal to current HP and combined damage equal to current HP are Tier 2.
- Incoming `D_i` mapping: null/gone/null-`BattlePokemon` opponents are excluded exactly like native; an opponent with `maxHP <= 0` remains incoming-eligible because native does not filter it; null moveset/list or null move element yields UNKNOWN/Tier 2; a non-null empty move list yields known `D_i=0`; no eligible target yields UNKNOWN/Tier 2; `candidate.getHealth() <= 0` does not divide or call an incoming helper.
- Doubles combined cases: 60% + 60% is Tier 2; 40% + 30% is Tier 1.
- Packed score: maximum `1,921,008,023`, `nativeSwitchScore=-1` and `10`, Tier 1 vs Tier 2 precedence, coverage aggregation saturation, same-coverage Tier 2 defensive ordering including unsaturated 100-vs-300 overkill, UNKNOWN/zero-HP sentinel behavior, and native-bonus-last ordering.
- All-Tier-2 acceptance: coverage wins over defense, defense wins over native bonus, and identical keys preserve native iteration/tie behavior; no new no-switch result.
- Ability table completeness and guards: `dryskin`, `wellbakedbody`, suppression, Mold Breaker/Teravolt/Turboblaze, Ability Shield precedence, transformed ability, missing actor/battle/raw effected Pokémon/raw ability/held-item manager/active-list context, native-helper failure, and unknown-as-unsafe behavior. Missing raw context must assert UNKNOWN/Tier 2 and no helper call; an unexpected native exception must fail the contract. `AbilityImmunityTableTest` may assert only table data; scorer tests must exercise the existing `BattleStates`/`PokeMathMax`/`HeldItemManager` authority or narrow test doubles and must not duplicate native suppression, bypass, immunity, or damage formulas.
- Dynamic type: Weather Ball under Sun/Rain/Sandstorm/Snow and no weather; type-first must use resolved type and full context.

Run at minimum from `companion-mod`:

```text
.\gradlew test --tests "com.cobbleverse.legendaryrule.strategy.switchai.*" --tests "com.cobbleverse.legendaryrule.strategy.dynamic.DynamicMoveResolverTest" --tests "com.cobbleverse.legendaryrule.strategy.guard.RedirectAbilityGuardTest" --tests "com.cobbleverse.legendaryrule.strategy.spread.RunBunAICallSiteContractTest"
.\gradlew test
```

### Layer 3 — Bytecode and Mixin runtime contracts

`scripts/runtime-contract/test_rct_runtime_contract.py` is the **raw contract** check only. Its output must label these checks as raw dependency/source assertions; it must not claim that a Mixin transformed a class.

- `isSwitching` full descriptor, Gate 1 `anyMatch` target/offset, Gate 2 `hasLowScore` `istore 12` offset 82, LVT slot 12/type `Z`, and required parameter names/types.
- `isOHKO`, `is2HKO`, and `highestPercentDamageMove` descriptors including `ActiveBattlePokemon`.
- `choose` full descriptor, private field `battleStatStages` owner/name/descriptor, LVT names/slots/types, `switchScore` `istore 32` offset 930, and the `switchingScores.put` window beginning at offset 1609 with `Map.put` invocation at offset 1618.
- Hook 1 `@WrapOperation` exact `Stream.anyMatch(Predicate)Z` target, no `@Local`, handler receiver/argument/`Operation` types, and explicit `require=1`.
- Hook 2 `@ModifyVariable` source selector contains the full `isSwitching` descriptor, `STORE`/`ISTORE`, exact `hasLowScore` `name`, `index`, `ordinal=0`, and `require=1`; scoring `@WrapOperation` contains the full `choose` descriptor, `INVOKE` target `Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;`, `ordinal=0`, and `require=1`. The mixin config declares both new mixins with top-level `required=true`/`defaultRequire=1`.
- The raw assertion must compare the dependency SHA/artifact coordinates used by Gradle and assert the bytecode operand relationship `switchingScores` receiver → `possibleSwitch` key → final local `switchScore` at the `Map.put` call. A dependency drift invalidates the raw result. Successful raw checks are not transformed-application evidence.
- The transformed-call assertion is separate: the scoring wrapper must receive the erased `Map`/`Object`/`Object`/`Operation<Object>` arguments, verify key and final score against the exact `@Local` bindings, and call the original operation once with only the score argument replaced. Layer 4 may establish that this required wrapper applied during bootstrap; neither Layer 3 nor Layer 4 may claim the AI score is semantically correct.

Run with the repository’s configured Python interpreter; on this host the verified fallback is:

```text
C:\Users\khang\.pyenv\pyenv-win\versions\3.11.6\python.exe scripts/runtime-contract/test_rct_runtime_contract.py
```

### Layer 4 — Headless bootstrap

Run exactly this command from the stated cwd after mixin registration:

```text
cwd=C:\Users\khang\Downloads\Doctors Cobblemon\companion-mod
.\gradlew runServer
```

Use a bounded 180-second process watchdog. Capture complete stdout/stderr and the fresh `C:\Users\khang\Downloads\Doctors Cobblemon\companion-mod\run\logs\latest.log` (the existing file is stale evidence until replaced by this run). A fresh run may be intentionally stopped after readiness because `runServer` is long-lived.

- `PASS/READY_STOP`: startup sentinel `Done (...s)! For help, type "help"` appears within 180 seconds, the process either exits 0 or is intentionally terminated after that sentinel, and no fatal `InvalidInjectionException`, `InjectionError`, `MixinApplyError`, `MixinTransformerError`, `NoSuchMethodError`, `NoClassDefFoundError`, or `ClassNotFoundException` occurs for the new mixins. With `required=true` and every new injection `require=1`, this is the transformed-mixin application evidence.
- `FAIL`: non-zero exit before the sentinel, or any listed fatal transform/classloading error; preserve stdout/stderr and `run/logs/latest.log`.
- `TIMEOUT`: no sentinel after 180 seconds; fail the boundary and preserve the same logs. A post-sentinel intentional stop is not a timeout failure.

Layer 4 proves only Knot/Mixin/Cobblemon bootstrap and required injection application. It does not prove AI semantic correctness; Layer 5 remains the only production gameplay authority.

### Layer 5 — Production canary

Only after Layers 0–4 pass and separate owner authorization, run dedicated live scenarios for:

- status/non-damaging Gate 2 and 15% vs 35% pressure;
- Singles and Doubles all-opponent OHKO/combined lethal;
- Tier 1 preference, all-Tier-2 least-bad defensive tie-break;
- Weather Ball dynamic type under each weather;
- effective ability suppression, Mold Breaker family, and Ability Shield;
- native random/HP gate preservation.

Offline PASS is not production semantic PASS. Report canary results separately and preserve `SKIPPED` as `SKIPPED`.

## 9. Residual limitations and stop conditions

- Combined lethal is a bounded heuristic based on each opponent’s highest one-hit damage; it does not claim full battle simulation for multi-hit, priority, status, terrain, secondary effects, or turn order.
- `RBTypeChart` prefilter behavior can drift with upstream ability semantics; final damage must remain authoritative.
- `BattleStates.getTransformationOrEffected` and `PokeMathMax` behavior belongs to the resolved dependency JAR; a version drift invalidates the bytecode evidence and requires re-discovery.
- The packed offensive coverage cap is `100.0%` per target and the aggregate cap is `200.0%`; damage beyond those coverage caps is intentionally equivalent for offensive ordering. Defensive severity retains overkill up to `3999.9%` of candidate current HP; known values beyond that cap share the lowest known rank, while `4000.0%` is reserved for UNKNOWN/invalid HP. This remains a bounded ranking heuristic, not a claim that capped overkill values are semantically equal in battle.
- The current worktree also contains an owner change reported by the review artifact at `datapacks/hell-mode/data/rctmod/trainers/kanto_sabrina.json`; it is outside this workstream, was not edited, and must not be staged with a later implementation without separate authority.
- `companion-mod/run/logs/latest.log` currently reflects an older runtime classpath (`mixinextras 0.5.0`/Sponge Mixin `0.17.0`); only a fresh `runServer` execution may close transformed-application evidence.
- Current baseline tests and runtime contract pass only the pre-existing implementation. They do not establish acceptance for this candidate plan.
- F-01 through F-06 are now closed as determinate plan contracts from repository/dependency evidence, including the four residual R2 blockers: unsaturated bounded Tier-2 severity, the confirmed `ActiveBattlePokemon` target type, native incoming source/direction and null/HP behavior, and raw native-helper preconditions/failure behavior. The plan is ready for independent re-review, but implementation remains gated by Main hash/freeze and reviewer PASS; no Owner Decision is currently required.

candidate_path=docs/workstreams/ai-switch-decision-upgrade/plan.md
ready=true
semantic_handoff_ready=true
