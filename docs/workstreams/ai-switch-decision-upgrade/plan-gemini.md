# AI Switch Decision Upgrade — Implementation Plan (Gemini Reconciled)

> **Trạng thái:** Reconciled Implementation Plan — Ready for Review / Execution  
> **Workspace:** `khangnhoang/cobbleverse-hell-mode-modernized`  
> **Baseline:** `8161e99` trên branch `feat/ai-switch-decision-upgrade`  
> **Target Path:** `docs/workstreams/ai-switch-decision-upgrade/plan-gemini.md`  
> **Nguyên tắc:** Không ghi đè lên `plan.md` hiện tại; bảo toàn mọi bytecode invariant và runtime contract đã xác minh.

---

## 1. Mục tiêu & Phạm vi (Scope)

Workstream này nâng cấp logic quyết định switch của AI trong Cobbleverse Hell Mode (`rbrctai` 0.15.4-beta) tại đúng hai injection seam đã được chứng minh qua bytecode:

1. **Phase 1 (Gỡ Switch Veto Giả & Bổ sung Incoming Threat):**
   - **Gate 1:** Thay predicate của `Stream.anyMatch()` để các status move (Protect, Will-O-Wisp, Thunder Wave...) có score $\ge 6$ nhưng `damage == 0` không veto quyền switch của AI. Chỉ các đòn tấn công có sát thương thực tế (`damage > 0`) và score $\ge 6$ mới veto.
   - **Gate 2 (Status-move hole & Critical Threat):** Sửa biến `hasLowScore` tại bytecode offset 82 (`istore 12`).
     - *FIX A:* Nếu tất cả các đòn non-fail (`score > -5`) đều là đòn không gây sát thương (`damage == 0`), AI không có offensive pressure $\rightarrow$ gỡ false veto (`hasLowScore = true`).
     - *FIX B:* Nếu AI đang chịu nguy hiểm cận kề (`isUnderCriticalThreat`: cả 2 đối thủ đều có khả năng OHKO AI) đồng thời áp lực tấn công của AI quá yếu (`isLowOffensivePressure < 20%`), cho phép AI switch (`hasLowScore = true`).
   - Giữ nguyên toàn bộ các native gates khác (random 75% gate, HP 50% gate, party survivability traversal).

2. **Phase 2 (Scoring Switch Candidate & Doubles Survivability):**
   - Thay thế điểm `switchScore` tại thời điểm put vào `switchingScores` map trong `RunBunAI.choose()` (bytecode offset 1618) bằng packed score đa tầng lexicographical:
     $$\text{Tier 1 (Safe)} > \text{Tier 2 (Unsafe)} \rightarrow \text{Offensive Coverage} \rightarrow \text{Defensive Severity Tie-break} \rightarrow \text{Native Bonuses}$$
   - **Doubles Combined Threat:** Tier 1 yêu cầu candidate không bị bất kỳ đối thủ đơn lẻ nào OHKO **VÀ** tổng sát thương cao nhất từ tất cả đối thủ hợp lệ $< \text{currentHP}$ của candidate (tránh chết vì double-target 60% + 60%).
   - **Dynamic Move Type Resolution:** Giải quyết move type bằng canonical `DynamicMoveResolver` (Weather Ball, Ivy Cudgel, Raging Bull...) trước khi tính toán type chart / damage.
   - **Defensive Ability Immunity:** Bảng tra cứu dữ liệu `AbilityImmunityTable` (data-only), caller kiểm tra đầy đủ runtime guards (`BattleStates.getTransformationOrEffected`, `PokeMathMax.isSuppressed`, `PokeMathMax.ignoreAbilities` của attacker, và `Ability Shield` của candidate) trước khi short-circuit damage về 0.
   - **Preserve Native Bonuses:** Giữ nguyên các bonus bản địa (+2 Ditto, +2 Wynaut/Wobbuffet, +1 Weather Setter) làm tie-break cuối cùng.

---

## 2. Kết quả Xác minh Bytecode & Phán quyết 7 Findings

| # | Finding | Kết quả Verify Bytecode / Source | Chi tiết kỹ thuật & Giải pháp |
|---|---|---|---|
| **1** | Gate 2 status-move hole | **XÁC NHẬN (CONFIRMED)** | Bytecode `RunBunAI.isSwitching` offset 21–82 tính `failCount` với predicate `score <= -5`, sau đó tính `totalMoves - failCount <= badMovesNeeded`. Nếu có 1 đòn đánh bị miễn nhiễm (fail) và 3 status move (+6), `4 - 1 = 3 > 2` (Doubles) $\rightarrow$ `hasLowScore = false`. Hook 1 chỉ sửa Gate 1 nên không giải quyết được lỗ hổng này. **Giải pháp:** Bổ sung Hook 2 vào `istore 12` (`hasLowScore`), kích hoạt FIX A khi mọi non-fail eval đều có `damage == 0`. |
| **2** | `isUnderCriticalThreat` thiếu context `activeBattlePokemon` | **XÁC NHẬN (CONFIRMED)** | `RunBunAI.isOHKO` có descriptor chính xác: `(List, BattlePokemon, BattlePokemon, ActiveBattlePokemon, RBStatStages)Z`. Trong bytecode, `activeBattlePokemon` được truyền trực tiếp vào `PokeMathMax.damage(...)` để xác định slot vị trí, weather và context. **Giải pháp:** Signature của detector phải nhận đủ 4 tham số: `(BattlePokemon self, List<ActiveBattlePokemon> opponents, ActiveBattlePokemon activeBattlePokemon, RBStatStages stages)`. |
| **3** | Safety Tier Phase 2 thiếu Doubles combined threat | **XÁC NHẬN (CONFIRMED)** | Trong `RunBunAI.choose`, AI chỉ kiểm tra từng đối thủ đơn lẻ (`slotOpponent`). Một candidate nhận 60% từ Opp1 và 60% từ Opp2 không bị single OHKO nhưng tổng 120% sẽ chết ngay turn switch-in. **Giải pháp:** `SwitchCandidateScorer.isSurvivable` duyệt toàn bộ `opponents` hợp lệ, lấy highest one-hit damage từ mỗi đối thủ; Tier 1 yêu cầu cả `singleHit < currentHP` VÀ $\sum \text{highestHit} < \text{currentHP}$. |
| **4** | Tier 2 ranking: offensive coverage vs defensive severity tie-break | **XÁC NHẬN (CONFIRMED)** | Nếu toàn bộ candidate đều rơi vào Tier 2 (unsafe), việc chỉ xếp hạng theo offensive coverage sẽ khiến 2 candidate cùng coverage nhưng 1 con ăn 101% và 1 con ăn 200% bị coi như nhau. **Giải pháp:** Sử dụng packed integer key: `tierRank -> coverageTotal -> defenseRank -> nativeSwitchScore`. Coverage là ưu tiên 1 (chọn best offensive unsafe candidate); nếu coverage bằng nhau, `defenseRank` (ít nhận sát thương hơn) đóng vai trò tie-break. |
| **5** | Ability table dễ fork semantic nếu tự zero damage | **XÁC NHẬN (CONFIRMED)** | Bytecode `PokeMathMax` và `RedirectAbilityGuard.java:101-124` chứng minh `isSuppressed` (Neutralizing Gas), `ignoreAbilities` (Mold Breaker, Teravolt, Turboblaze), và `Ability Shield` tương tác đa tầng. Nếu table tự trả về 0 damage sẽ bỏ qua bypass của Mold Breaker hoặc suppression. **Giải pháp:** `AbilityImmunityTable` là data-only (mapping ability ID $\rightarrow$ immune `ElementalType`). `SwitchCandidateScorer` chịu trách nhiệm gọi `BattleStates.getTransformationOrEffected` và thực thi guard order hiện hữu trước khi short-circuit damage = 0. |
| **6** | Type-first dùng raw `move.getType()` với dynamic move | **BÁC BỎ LÀ LỖI HIỆN TẠI; BẢO TOÀN LÀM INVARIANT** | Mã nguồn hiện tại đã có `DynamicMoveResolver`. Bất biến: Mọi bước type evaluation trong scorer phải gọi `DynamicMoveResolver.resolveEffectiveMove(move, attacker, activeBattlePokemon, false)` đồng bộ với `PokeMathMaxMixin`. |
| **7** | Hook 1 không cần capture `evaluations` qua `@Local` | **BÁC BỎ LÀ LỖI CỦA HOOK 1; BẢO TOÀN SIGNATURE TỐI THIỂU** | Signature đề xuất ban đầu đã không thêm `@Local`. Tại call site `Stream.anyMatch` (offset 11), receiver `Stream` và argument `Predicate` đã nằm sẵn trên stack. Việc thay predicate được thực hiện trực tiếp qua `original.call(stream, newPredicate)` với signature tối thiểu `(Stream<MoveEvaluation>, Predicate<MoveEvaluation>, Operation<Boolean>)`. |

---

## 3. Kiến trúc Chi tiết Phase 1: Gỡ Veto & Threat Detection

### 3.1 [`DeadMatchupDetector.java`](file:///c:/Users/khang/Downloads/Doctors%20Cobblemon/companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/switchai/DeadMatchupDetector.java)
- **Package:** `com.cobbleverse.legendaryrule.strategy.switchai`
- **Phương thức 1: `isLowOffensivePressure`**
  ```java
  public static boolean isLowOffensivePressure(
      List<RunBunAI.MoveEvaluation> evaluations,
      List<ActiveBattlePokemon> opponents
  )
  ```
  - Lọc danh sách `eligibleTargets` từ `opponents`: `opp != null && !opp.isGone() && opp.getBattlePokemon() != null && opp.getBattlePokemon().getMaxHealth() > 0`.
  - **Lưu ý kiểu dữ liệu (F-REV-03):** Trong `rbrctai`, `MoveEvaluation.getOpponent()` trả về kiểu `ActiveBattlePokemon`, **không phải** `BattlePokemon`. Do đó không được gọi `eval.getOpponent().getMaxHealth()`.
  - So sánh đối tượng mục tiêu: kiểm tra `eval.getOpponent() == target`, hoặc nếu kiểm tra theo ID thì lấy qua `BattlePokemon`:
    `eval.getOpponent().getBattlePokemon() != null && eval.getOpponent().getBattlePokemon().getUuid().equals(target.getBattlePokemon().getUuid())`.
  - Tính toán tỷ lệ sát thương:
    $$\text{ratio} = \frac{\max(0, \text{eval.getDamage()})}{\text{target.getBattlePokemon().getMaxHealth()}}$$
  - $\text{maxRatio} = \max(\text{ratio})$. Trả về `true` khi $\text{maxRatio} < 0.20$ (strict).
  - Nếu `evaluations` hoặc `opponents` rỗng/không có target hợp lệ: trả về `false` (không tạo false evidence). Nếu tất cả damage đều bằng 0: $\text{maxRatio} = 0 < 0.20 \rightarrow \text{true}$.

- **Phương thức 2: `isUnderCriticalThreat`**
  ```java
  public static boolean isUnderCriticalThreat(
      BattlePokemon self,
      List<ActiveBattlePokemon> opponents,
      ActiveBattlePokemon activeBattlePokemon,
      RBStatStages stages
  )
  ```
  - Lọc `eligibleOpponents` tương tự. Nếu không có eligible opponent nào: trả về `false`.
  - Duyệt qua từng `opp` trong `eligibleOpponents`:
    Lấy `oppMoves = opp.getBattlePokemon().getMoveSet().getMoves()`.
    Kiểm tra `RunBunAI.isOHKO(oppMoves, opp.getBattlePokemon(), self, activeBattlePokemon, stages)`.
  - Trả về `true` **CHỈ KHI MỌI** eligible opponent đều có thể OHKO `self` (Critical Threat đồng thời từ cả 2 phía trong Doubles).

### 3.2 [`IsSwitchingOverrideMixin.java`](file:///c:/Users/khang/Downloads/Doctors%20Cobblemon/companion-mod/src/main/java/com/cobbleverse/legendaryrule/mixin/IsSwitchingOverrideMixin.java)
- **Target Class:** `com.gitlab.surilexa.rbrctai.api.ai.RunBunAI`
- **Target Method:**
  `isSwitching(Ljava/util/List;Ljava/util/List;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Ljava/util/List;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/gitlab/surilexa/rbrctai/api/ai/utils/RBStatStages;Z)Z`

- **Hook 1 (`@WrapOperation` trên Gate 1 `anyMatch`):**
  ```java
  @WrapOperation(
      method = "isSwitching(Ljava/util/List;Ljava/util/List;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Ljava/util/List;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/gitlab/surilexa/rbrctai/api/ai/utils/RBStatStages;Z)Z",
      at = @At(
          value = "INVOKE",
          target = "Ljava/util/stream/Stream;anyMatch(Ljava/util/function/Predicate;)Z"
      ),
      require = 1,
      remap = false
  )
  private static boolean cobbleverse$relaxScoreVeto(
      Stream<RunBunAI.MoveEvaluation> stream,
      Predicate<RunBunAI.MoveEvaluation> nativePredicate,
      Operation<Boolean> original
  ) {
      return original.call(stream, (Predicate<RunBunAI.MoveEvaluation>) eval -> 
          eval != null && eval.getScore() >= 6 && eval.getDamage() > 0
      );
  }
  ```

- **Hook 2 (`@ModifyVariable` trên Gate 2 `hasLowScore` - Reconciled F-REV-01):**
  Bytecode xác minh: LVT slot 12, instruction `istore 12` tại offset 82.
  > [!IMPORTANT]
  > **Reconciled F-REV-01:** Tuyệt đối **không** dùng `ordinal = 0`! Trong `isSwitching()`, `isDoubles` (slot 6) là boolean ordinal 0, còn `hasLowScore` (slot 12) là boolean ordinal 1. Sử dụng `ordinal = 0` sẽ khiến Mixin chọn nhầm slot 6, không khớp với lệnh `istore 12` và gây crash server ngay tại Layer 4 bootstrap (`InvalidInjectionException`). Ta bỏ `ordinal` và dùng `name = "hasLowScore"` cùng `index = 12`.

  ```java
  @ModifyVariable(
      method = "isSwitching(Ljava/util/List;Ljava/util/List;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Ljava/util/List;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/gitlab/surilexa/rbrctai/api/ai/utils/RBStatStages;Z)Z",
      at = @At(value = "STORE", opcode = Opcodes.ISTORE),
      name = "hasLowScore",
      index = 12,
      require = 1,
      remap = false
  )
  private static boolean cobbleverse$adjustHasLowScore(
      boolean nativeHasLowScore,
      @Local(name = "evaluations", index = 0, argsOnly = true) List<RunBunAI.MoveEvaluation> evaluations,
      @Local(name = "self", index = 2, argsOnly = true) BattlePokemon self,
      @Local(name = "opponents", index = 3, argsOnly = true) List<ActiveBattlePokemon> opponents,
      @Local(name = "activeBattlePokemon", index = 4, argsOnly = true) ActiveBattlePokemon activeBattlePokemon,
      @Local(name = "battleStatStages", index = 5, argsOnly = true) RBStatStages battleStatStages
  ) {
      if (nativeHasLowScore) return true;

      // FIX A: Toàn bộ đòn non-fail đều là non-damaging (status move hole)
      if (evaluations != null && !evaluations.isEmpty()) {
          boolean allNonFailAreNonDamaging = evaluations.stream()
              .filter(e -> e != null && e.getScore() > -5)
              .allMatch(e -> e.getDamage() == 0);
          if (allNonFailAreNonDamaging) return true;
      }

      // FIX B: Critical Threat từ cả hai opponent + Offensive Pressure < 20%
      boolean criticalThreat = DeadMatchupDetector.isUnderCriticalThreat(
          self, opponents, activeBattlePokemon, battleStatStages
      );
      boolean lowPressure = DeadMatchupDetector.isLowOffensivePressure(evaluations, opponents);

      return criticalThreat && lowPressure;
  }
  ```

---

## 4. Kiến trúc Chi tiết Phase 2: Scoring Candidate & Survivability

### 4.1 [`AbilityImmunityTable.java`](file:///c:/Users/khang/Downloads/Doctors%20Cobblemon/companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/switchai/AbilityImmunityTable.java)
- **Thuần dữ liệu (Data-only):** Không chứa logic kiểm tra suppression hay mold breaker.
- **Mapping tối thiểu đã xác minh:**
  | Ability ID | Miễn nhiễm (`ElementalType`) |
  |---|---|
  | `stormdrain`, `waterabsorb`, `dryskin` | `ElementalTypes.WATER` |
  | `voltabsorb`, `lightningrod`, `motordrive` | `ElementalTypes.ELECTRIC` |
  | `flashfire`, `wellbakedbody` | `ElementalTypes.FIRE` |
  | `sapsipper` | `ElementalTypes.GRASS` |
  | `levitate`, `eartheater` | `ElementalTypes.GROUND` |

### 4.2 [`SwitchCandidateScorer.java`](file:///c:/Users/khang/Downloads/Doctors%20Cobblemon/companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/switchai/SwitchCandidateScorer.java)

#### Context Gate & Guard Order:
1. **Context Gate:** `candidate`, `opponentBP`, `activeBattlePokemon`, `stages` non-null; `candidate.getActor().getBattle()` và `opponentBP.getActor().getBattle()` non-null. Nếu thiếu $\rightarrow$ trả về `DamageEvidence.UNKNOWN` (xem như unsafe/lethal, không bao giờ tự gán immune).
2. **Effective Ability Resolution:** Gọi `BattleStates.getTransformationOrEffected(candidate)` và `BattleStates.getTransformationOrEffected(opponentBP)`.
3. **Guard Order:**
   - (1) `PokeMathMax.isSuppressed(candidate)`: Nếu bị suppress (ví dụ Neutralizing Gas) mà không có `Ability Shield` $\rightarrow$ ability vô hiệu.
   - (2) `PokeMathMax.ignoreAbilities(opponentBP)`: Kẻ tấn công có Mold Breaker / Teravolt / Turboblaze $\rightarrow$ bypass immunity, **TRỪ KHI** candidate mang `abilityshield` (`HeldItemManager.showdownId(candidate).equalsIgnoreCase("abilityshield")`).
   - (3) Nếu ability hợp lệ và có trong `AbilityImmunityTable` tương ứng với type của đòn đánh đã resolve: sát thương $= 0$.
   - (4) Nếu không rơi vào table immunity: gọi trực tiếp native damage calculation qua `PokeMathMax.damage(...)`.

#### Doubles Survivability Tier Determination & Defense Severity (Reconciled F-REV-02):
- Candidate có `currentHP <= 0` hoặc gặp `UNKNOWN` evidence $\rightarrow$ **Tier 2**.
- Với mỗi eligible opponent $i$:
  - Duyệt qua các move của opponent, resolve move type bằng `DynamicMoveResolver.resolveEffectiveMove(move, oppBP, activeBattlePokemon, false)`.
  - Tính $D_i = \text{highest one-hit effective damage}$ từ opponent $i$ lên candidate.
  - Nếu bất kỳ $D_i \ge \text{candidate.currentHP} \rightarrow$ **Tier 2** (Single OHKO).
- Tính tổng sát thương kết hợp: $D_{\text{combined}} = \sum_i D_i$.
  - Nếu $D_{\text{combined}} \ge \text{candidate.currentHP} \rightarrow$ **Tier 2** (Combined Lethal).
  - Ngược lại $\rightarrow$ **Tier 1 (Safe)**.

> [!IMPORTANT]
> **Reconciled F-REV-02 (Không bão hòa sớm sát thương phòng thủ):**
> Để `defenseRank` phân định được các candidate Tier 2 (ví dụ candidate ăn 101% sát thương phải thắng candidate ăn 200% sát thương khi có cùng offensive coverage), ta **không bão hòa** $D_{\text{combined}}$ tại `currentHP`.
> $D_{\text{combined}}$ dùng cho tính toán tỷ lệ phòng thủ được tích lũy đầy đủ giá trị thực tế lên tới trần $10 \times \text{currentHP}$ (1000.0%).

#### Determinate Packed Lexicographical Score (Reconciled F-REV-02, F-REV-04):
Điểm số được đóng gói thành số nguyên `int` 32-bit tương thích với `Map<BattlePokemon, Integer> switchingScores`:
```text
TIER1 = 1, TIER2 = 0
TARGET_COVERAGE_MAX = 1000       // Max 100.0% coverage cho 1 target
COVERAGE_TOTAL_MAX = 2000        // Max 200.0% coverage cho 2 target (Doubles)
DEFENSE_RATIO_MAX = 10000        // 1000.0% incoming ratio, đơn vị 0.1% (tenths of a percent)
NATIVE_MIN = -1, NATIVE_MAX = 10, NATIVE_RADIX = 12

// Kiểm tra ranh giới fail-loud cho native score (F-REV-04)
if (nativeSwitchScore < NATIVE_MIN || nativeSwitchScore > NATIVE_MAX) {
    throw new IllegalStateException("Native switch score outside contract bounds [-1, 10]: " + nativeSwitchScore);
}
nativeRank = nativeSwitchScore - NATIVE_MIN; // chính xác trong [0, 11]

// Tính tỷ lệ sát thương phòng thủ với hệ số 1000 (F-REV-02)
// 100% sát thương -> incomingRatio = 1000
// 101% sát thương -> incomingRatio = 1010
// 200% sát thương -> incomingRatio = 2000
incomingRatio = (currentHP <= 0 || isUnknown)
    ? DEFENSE_RATIO_MAX
    : min(DEFENSE_RATIO_MAX, (long) ceil(1000.0 * D_combined / currentHP));

// Defense rank: nhận ít sát thương hơn -> rank cao hơn
// Tier 1 luôn có defenseRank cực đại (10000)
defenseRank = (tier == TIER1) ? DEFENSE_RATIO_MAX : (DEFENSE_RATIO_MAX - incomingRatio);

// Packed formula
long packed = (((tierRank * (COVERAGE_TOTAL_MAX + 1) + coverageTotal)
               * (DEFENSE_RATIO_MAX + 1) + defenseRank)
              * NATIVE_RADIX + nativeRank);
if (packed > Integer.MAX_VALUE) {
    throw new IllegalStateException("Packed switch score exceeded Integer.MAX_VALUE: " + packed);
}
return (int) packed;
```

**Chứng minh Bounds & Precedence Chính xác (F-REV-04):**
- Giá trị cực đại của `packed`:
  $$(((1 \times 2001 + 2000) \times 10001 + 10000) \times 12 + 11) = 480,288,023 < 2,147,483,647 \ (\text{Integer.MAX\_VALUE})$$
- Giá trị nhỏ nhất của Tier 1:
  $$(((1 \times 2001 + 0) \times 10001 + 10000) \times 12 + 0) = (2001 \times 10001 + 10000) \times 12 = 20022001 \times 12 = 240,264,012$$
- Giá trị lớn nhất của Tier 2:
  $$(((0 \times 2001 + 2000) \times 10001 + 10000) \times 12 + 11) = (2000 \times 10001 + 10000) \times 12 + 11 = 20012000 \times 12 + 11 = 240,144,011$$
- Vì $240,264,012 > 240,144,011$, **mọi candidate Tier 1 đều luôn chiến thắng mọi candidate Tier 2** bất kể bonus hay coverage.
- Trong cùng một tier: `coverageTotal` quyết định trước. Nếu `coverageTotal` hòa: `defenseRank` phân định (candidate nhận 101% có rank 8990 thắng candidate nhận 200% có rank 8000). Nếu cả 2 đều hòa: `nativeSwitchScore` phân định cuối cùng.

### 4.3 [`SwitchCandidateScoringMixin.java`](file:///c:/Users/khang/Downloads/Doctors%20Cobblemon/companion-mod/src/main/java/com/cobbleverse/legendaryrule/mixin/SwitchCandidateScoringMixin.java)
- **Target Class:** `com.gitlab.surilexa.rbrctai.api.ai.RunBunAI`
- **Target Method:**
  `choose(Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Lcom/cobblemon/mod/common/battles/BattleSide;Lcom/cobblemon/mod/common/battles/ShowdownMoveset;Z)Lcom/cobblemon/mod/common/battles/ShowdownActionResponse;`
- **Shadow Field:**
  `@Shadow(remap = false) private RBStatStages battleStatStages;`
- **Injection Point:**
  `invokeinterface java/util/Map.put` tại offset 1618 (đúng duy nhất 1 call site `Map.put` trong method `choose`).

> [!IMPORTANT]
> **Reconciled F-REV-05:** Thay thế từ khóa `assert` bằng kiểm tra điều kiện tường minh và ném `IllegalStateException` để đảm bảo cơ chế fail-loud luôn hoạt động ngay cả khi server Minecraft production chạy với cờ `-ea` bị tắt.

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
private Object cobbleverse$wrapSwitchCandidateScore(
    Map<BattlePokemon, Integer> switchingScores,
    Object possibleSwitchKey,
    Object nativeBoxedScore,
    Operation<Object> original,
    @Local(name = "possibleSwitch", index = 38) BattlePokemon possibleSwitch,
    @Local(name = "allOpponentActiveBattlePokemon", index = 26) List<ActiveBattlePokemon> opponents,
    @Local(name = "activeBattlePokemon", index = 1, argsOnly = true) ActiveBattlePokemon activeBattlePokemon,
    @Local(name = "battlePokemon", index = 13) BattlePokemon currentActive,
    @Local(name = "switchScore", index = 32) int nativeLocalScore
) {
    if (possibleSwitchKey != possibleSwitch) {
        throw new IllegalStateException("Mixin contract violation: possibleSwitchKey != possibleSwitch");
    }
    if (!(nativeBoxedScore instanceof Integer) || ((Integer) nativeBoxedScore).intValue() != nativeLocalScore) {
        throw new IllegalStateException("Mixin contract violation: nativeBoxedScore does not match nativeLocalScore");
    }

    int upgradedScore = SwitchCandidateScorer.score(
        possibleSwitch,
        opponents,
        activeBattlePokemon,
        this.battleStatStages,
        currentActive,
        nativeLocalScore
    );
    return original.call(switchingScores, possibleSwitchKey, Integer.valueOf(upgradedScore));
}
```

---

## 5. Kế hoạch Verification Đa Tầng (Hell Mode Orthogonal Layers)

### Layer 0: Markdown & Cấu trúc
- Kiểm tra tính toàn vẹn của đường dẫn và tài liệu: file `plan-gemini.md` nằm đúng thư mục `docs/workstreams/ai-switch-decision-upgrade/`, không làm mất hay ghi đè lên `plan.md`.

### Layer 2: Java Unit & Boundary Tests (Reconciled F-REV-07)
Chạy bộ test từ thư mục `companion-mod`, bao gồm toàn bộ test mới và bộ test hồi quy hiện hữu:
```bash
./gradlew test --tests "com.cobbleverse.legendaryrule.strategy.switchai.*" \
               --tests "com.cobbleverse.legendaryrule.strategy.dynamic.DynamicMoveResolverTest" \
               --tests "com.cobbleverse.legendaryrule.strategy.guard.RedirectAbilityGuardTest" \
               --tests "com.cobbleverse.legendaryrule.strategy.spread.RunBunAICallSiteContractTest"
```
Các ca kiểm thử trọng tâm:
1. `DeadMatchupDetectorTest`:
   - 1 attack bị immune + 3 status move $\rightarrow$ `isLowOffensivePressure = true` (0% damage).
   - Attack deal 15% HP $\rightarrow$ `isLowOffensivePressure = true` ($< 20\%$).
   - Attack deal 25% HP $\rightarrow$ `isLowOffensivePressure = false` ($\ge 20\%$).
   - Critical threat: Cả 2 opponent đều OHKO $\rightarrow$ `true`; Chỉ 1 opponent OHKO $\rightarrow$ `false`.
   - Đối soát `MoveEvaluation.getOpponent()` trả về `ActiveBattlePokemon` và không gọi hàm lỗi (F-REV-03).
2. `AbilityImmunityTableTest`:
   - Đảm bảo mapping đúng cho 11 abilities (Water, Electric, Fire, Grass, Ground).
3. `SwitchCandidateScorerTest`:
   - Doubles Combined Lethality: Opp1 60% + Opp2 60% $\rightarrow$ Tier 2.
   - Opp1 40% + Opp2 30% $\rightarrow$ Tier 1.
   - Neutralizing Gas active: Storm Drain candidate bị suppress $\rightarrow$ nhận damage đầy đủ.
   - Opponent có Mold Breaker vs Levitate candidate: bị bypass $\rightarrow$ Tier 2 nếu lethal.
   - Mold Breaker vs Levitate + `abilityshield` $\rightarrow$ Shield bảo vệ, giữ miễn nhiễm.
   - Packed score ordering:
     - Tier 1 candidate luôn có score $>$ Tier 2 candidate bất kể bonus.
     - 2 candidate Tier 2 cùng coverage: candidate nhận 101% sát thương có `defenseRank = 8990` thắng candidate nhận 200% sát thương có `defenseRank = 8000` (F-REV-02).
     - Ranh giới native score: ném `IllegalStateException` khi native score $< -1$ hoặc $> 10$ (F-REV-04).

### Layer 3: Offline Bytecode Contract Tests
Chạy script kiểm tra hợp đồng bytecode:
```bash
python scripts/runtime-contract/test_rct_runtime_contract.py
```
- Bổ sung kiểm tra descriptor của `RunBunAI.isSwitching`, offset của `anyMatch` (11), LVT slot 12 (`hasLowScore` không có `ordinal = 0`).
- Kiểm tra toàn diện bytecode operands và LVT captures tại điểm can thiệp `Map.put` trong `RunBunAI.choose` (offsets 1609–1618):
  - Chuỗi nạp toán hạng lên stack: `aload 31` (nạp receiver `switchingScores`), `aload 38` (nạp key `possibleSwitch`), `iload 32` (nạp value `switchScore`), `invokestatic Integer.valueOf`, và `invokeinterface Map.put`.
  - Phân định vai trò slot 31: Slot 31 (`switchingScores`) là Map receiver trên bytecode stack được `@WrapOperation` truyền trực tiếp vào tham số đầu tiên `Map<BattlePokemon, Integer> switchingScores`, không phải là `@Local` capture.
  - Kiểm tra LVT scope và slot cho toàn bộ 5 biến `@Local` thực tế được capture: slot 38 (`possibleSwitch`), slot 26 (`allOpponentActiveBattlePokemon`), slot 1 (`activeBattlePokemon`, `argsOnly = true`), slot 13 (`battlePokemon`), và slot 32 (`switchScore`).

### Layer 4: Headless Server Bootstrap Smoke
```bash
cd companion-mod && ./gradlew runServer
```
- Xác nhận Knot bootstrap hoàn tất, `Done` sentinel xuất hiện trong vòng 180s, không có `InvalidInjectionException` hay `MixinApplyError`.

### Layer 5: Production Canary (Khi Owner phê duyệt)
- Thử nghiệm trong trận đấu Doubles thực tế:
  - Rotom-W đối đầu với Water-absorb / Storm-drain.
  - Tình huống cả 2 đối thủ cùng đe dọa lethal.
  - Tình huống force switch khi toàn bộ party đều gặp bất lợi.

---

## 6. Danh mục File Tác động

### Files Tạo mới:
1. `companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/switchai/DeadMatchupDetector.java`
2. `companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/switchai/AbilityImmunityTable.java`
3. `companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/switchai/SwitchCandidateScorer.java`
4. `companion-mod/src/main/java/com/cobbleverse/legendaryrule/mixin/IsSwitchingOverrideMixin.java`
5. `companion-mod/src/main/java/com/cobbleverse/legendaryrule/mixin/SwitchCandidateScoringMixin.java`
6. `companion-mod/src/test/java/com/cobbleverse/legendaryrule/strategy/switchai/DeadMatchupDetectorTest.java`
7. `companion-mod/src/test/java/com/cobbleverse/legendaryrule/strategy/switchai/AbilityImmunityTableTest.java`
8. `companion-mod/src/test/java/com/cobbleverse/legendaryrule/strategy/switchai/SwitchCandidateScorerTest.java`

### Files Cập nhật:
1. `companion-mod/src/main/resources/rct_legendary_rule.mixins.json` (đăng ký 2 mixin mới)
2. `scripts/runtime-contract/test_rct_runtime_contract.py` (bổ sung assertions cho bytecode offsets & mixin targets)
3. `docs/workstreams/ai-switch-decision-upgrade/plan-gemini.md` (kế hoạch độc lập này, không ghi đè `plan.md`)
