# AI Switch Decision Upgrade — Implementation Plan (Gemini Reconciled)

> **Trạng thái:** Reconciled Implementation Plan — Ready for Review / Execution  
> **Workspace:** `khangnhoang/cobbleverse-hell-mode-modernized`  
> **Baseline:** `8161e99` trên branch `feat/ai-switch-decision-upgrade`  
> **Target Path:** `docs/workstreams/ai-switch-decision-upgrade/plan-gemini.md`  
> **Nguyên tắc:** Không ghi đè lên `plan.md` hiện tại; bảo toàn mọi bytecode invariant và runtime contract đã xác minh.

---

## 1. Mục tiêu & Phạm vi (Scope)

Workstream này nâng cấp logic quyết định switch của AI trong Cobbleverse Hell Mode (`rbrctai` 0.15.4-beta) tại đúng hai injection seam đã được chứng minh qua bytecode:

1. **Phase 1 (Gỡ Switch Veto Giả & Bổ sung Incoming Threat — Reconciled Live Canary):**
   - **Gate 1 (Offensive Justification to Stay):** Thay predicate của `Stream.anyMatch()` tại offset 11. Thay thế assumption `score >= 6 && damage > 0` (vốn bị live canary bác bỏ do Shadow Ball 36 dmg có score 6 đã false-veto switch). Chỉ các đòn tấn công có sát thương thực chất (`isMeaningfulOffensiveMove`: `score >= 6 && damage > 0` VÀ gây $\ge 20\%$ max HP của mục tiêu hoặc kết liễu KO mục tiêu còn sống) mới veto quyền switch để ở lại tấn công. Các đòn status (Protect, Will-O-Wisp... damage 0) và đòn tấn công yếu (Shadow Ball 36 dmg, ratio $< 20\%$) không được veto switch.
   - **Gate 2 (Dead Matchup & Threat Eligibility):** Sửa biến `hasLowScore` tại bytecode offset 82 (`istore 12`).
     - Giữ nguyên nếu native `hasLowScore == true`.
     - Gỡ false veto (`hasLowScore = true`) khi AI rơi vào thế trận bế tắc tấn công (`isLowOffensivePressure < 20%` trên mọi đối thủ hợp lệ, bao gồm status moves và weak chip moves).
     - Cho phép switch (`hasLowScore = true`) khi AI chịu nguy hiểm cận kề (`isUnderCriticalThreat`: cả 2 đối thủ đều có khả năng OHKO AI).
     - Reconciled Contract: `hasLowScore = nativeHasLowScore || lowPressure || criticalThreat;` (khắc phục điểm nghẽn điều kiện `criticalThreat && lowPressure` quá hẹp khiến matchup bế tắc không bị OHKO bị kẹt).
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
| **8** | Live Canary RED: Weak damaging move false veto & Gate 2 contract narrowness | **XÁC NHẬN (CONFIRMED QUA LIVE CANARY)** | Live canary Rotom-W vs Swampert + Gastrodon cho thấy: Shadow Ball chỉ gây 36 dmg ($< 20\%$ HP) nhưng có native score 6 $\rightarrow$ trigger Gate 1 `score >= 6 && damage > 0`, hard-veto switch ngay tại offset 20 khiến Gate 2 không bao giờ chạy. Đồng thời, Hook 2 `criticalThreat && lowPressure` quá hẹp vì cả 2 đối thủ không OHKO Rotom-W nên nếu lọt qua Gate 1 thì Gate 2 vẫn veto. **Giải pháp:** Tách bạch 2 ngưỡng: Gate 1 chỉ veto khi đòn đánh có sát thương thực chất $\ge 33\%$ max HP hoặc KO (`STAY_JUSTIFICATION_THRESHOLD = 0.33`). Gate 2 mở switch eligibility khi `nativeHasLowScore || lowPressure (< 20%) || criticalThreat`. |

---

## 3. Kiến trúc Chi tiết Phase 1: Gỡ Veto & Threat Detection

### 3.1 [`DeadMatchupDetector.java`](file:///c:/Users/khang/Downloads/Doctors%20Cobblemon/companion-mod/src/main/java/com/cobbleverse/legendaryrule/strategy/switchai/DeadMatchupDetector.java)
- **Package:** `com.cobbleverse.legendaryrule.strategy.switchai`
- **Ngưỡng tách biệt (Decoupled Thresholds):**
  - `LOW_PRESSURE_THRESHOLD = 0.20`: Ngưỡng áp lực thấp (< 20%), dùng tại Gate 2 để mở switch consideration.
  - `STAY_JUSTIFICATION_THRESHOLD = 0.33`: Ngưỡng biện minh ở lại (>= 33%, tương đương 3HKO), dùng tại Gate 1 để hard-veto tactical switch.
  - Vùng trung gian [20%, 33%): Không hard-veto tại Gate 1; không tự động kích hoạt lowPressure tại Gate 2; defer cho native Gate 2 + random + HP + survivability logic.
- **Phương thức 1: `isMeaningfulOffensiveMove` (Gate 1 Stay Justification)**
  ```java
  public static boolean isMeaningfulOffensiveMove(RunBunAI.MoveEvaluation eval)
  ```
  - Kiểm tra tính hợp lệ cơ bản: `eval != null && eval.getScore() >= 6 && eval.getDamage() > 0`.
  - Lấy thông tin mục tiêu: `opp = eval.getOpponent()`, `oppBP = opp != null ? opp.getBattlePokemon() : null`.
  - Nếu mục tiêu không hợp lệ hoặc đã ngất (`opp == null || opp.isGone() || oppBP == null || oppBP.getMaxHealth() <= 0 || oppBP.getHealth() <= 0`): trả về `false` (tránh false veto khi target đã ngất nhưng chưa unmount - REV-P1-01).
  - Kiểm tra sát thương có ý nghĩa (đảm bảo `oppBP.getHealth() > 0`):
    - **Lethal KO:** `eval.getDamage() >= oppBP.getHealth()` $\rightarrow$ `true` (đòn kết liễu mục tiêu còn sống luôn biện minh cho việc ở lại).
    - **Sát thương áp lực cao:** $\text{ratio} = \frac{\text{(double) eval.getDamage()}}{\text{(double) oppBP.getMaxHealth()}} \ge 0.33$ (strict $\ge 33\%$, ép kiểu `double` tránh integer truncation - REV-P1-02) $\rightarrow$ `true`.
  - Trường hợp status move (Protect: damage 0) hoặc weak chip move (Shadow Ball: 36 dmg, ratio $< 33\%$): trả về `false` $\rightarrow$ KHÔNG hard-veto switch.

- **Phương thức 2: `isLowOffensivePressure`**
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

- **Phương thức 3: `isUnderCriticalThreat`**
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

- **Hook 1 (`@WrapOperation` trên Gate 1 `anyMatch` - Reconciled):**
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
      return original.call(stream, (Predicate<RunBunAI.MoveEvaluation>) DeadMatchupDetector::isMeaningfulOffensiveMove);
  }
  ```

- **Hook 2 (`@ModifyVariable` trên Gate 2 `hasLowScore` - Reconciled F-REV-01 & Live Canary):**
  Bytecode xác minh: LVT slot 12, instruction `istore 12` tại offset 82.
  > [!IMPORTANT]
  > **Reconciled F-REV-01:** Tuyệt đối **không** dùng `ordinal = 0`! Trong `isSwitching()`, `isDoubles` (slot 6) là boolean ordinal 0, còn `hasLowScore` (slot 12) là boolean ordinal 1. Ta bỏ `ordinal` và dùng `name = "hasLowScore"` cùng `index = 12`.
  > **Reconciled Contract:** Không dùng `criticalThreat && lowPressure` vì sẽ chặn đứng các dead matchup mà đối thủ không thể OHKO AI (như Rotom-W vs Swampert + Gastrodon). Thay vào đó, cho phép switch khi: `nativeHasLowScore || lowPressure || criticalThreat`.

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
      if (nativeHasLowScore) {
          return true;
      }

      // Reconciled: Áp lực tấn công yếu (< 20% max damage trên mọi đối thủ, bao gồm status moves và chip moves)
      boolean lowPressure = DeadMatchupDetector.isLowOffensivePressure(evaluations, opponents);
      if (lowPressure) {
          return true;
      }

      // Nguy hiểm cận kề: toàn bộ đối thủ đều có khả năng OHKO self
      boolean criticalThreat = DeadMatchupDetector.isUnderCriticalThreat(
          self, opponents, activeBattlePokemon, battleStatStages
      );
      return criticalThreat;
  }
  ```

### 3.3 Khung Xác minh 6 Required Regression Cases
1. **Case A (Status False Veto):**
   - Moveset: Protect (score 6, damage 0), các move còn lại fail/0.
   - Gate 1: `isMeaningfulOffensiveMove` trả về `false` (damage = 0).
   - Gate 2: `isLowOffensivePressure` trả về `true` (maxRatio = 0.0 < 0.20) $\rightarrow$ `hasLowScore = true`.
   - Kết quả: Không bị hard-veto; mở switch eligibility.
2. **Case B (Weak Damaging Move False Veto — Live Rotom-W Reproduction):**
   - Moveset: Shadow Ball (score 6, damage 36/185 = 19.4% trên Swampert, 38/200 = 19% trên Gastrodon), Volt Switch (fail), Hydro Pump (fail), Protect (0 dmg).
   - Gate 1: Shadow Ball có ratio $< 33\%$ và không KO $\rightarrow$ `isMeaningfulOffensiveMove` trả về `false` $\rightarrow$ không hard-veto.
   - Gate 2: `isLowOffensivePressure` trả về `true` ($< 20\%$) $\rightarrow$ `hasLowScore = true`.
   - Kết quả: Gỡ bỏ thành công false veto; AI được phép xét switch.
3. **Case C (Strong Damaging Move Regression):**
   - Moveset: Volt Switch -> Talonflame (score 9, damage 118/150 = 78.6%).
   - Gate 1: `score >= 6 && ratio = 0.786 >= 0.33` $\rightarrow$ `isMeaningfulOffensiveMove` trả về `true`.
   - Kết quả: Gate 1 hard-veto switch $\rightarrow$ AI ở lại và tấn công. Không bị switch addiction.
4. **Case D (Decoupled Thresholds Boundaries):**
   - 19.9% damage: no stay justification (Gate 1 không veto) + low pressure (Gate 2 `lowPressure = true`) $\rightarrow$ switch eligible.
   - 20.0% damage: no stay justification (Gate 1 không veto) + NOT low pressure (Gate 2 không tự bật `lowPressure`) $\rightarrow$ defer cho native logic.
   - 25.0% damage: no stay justification + NOT low pressure $\rightarrow$ defer cho native logic.
   - 32.9% damage: no stay justification + NOT low pressure $\rightarrow$ defer cho native logic.
   - 33.0% damage: stay justification (Gate 1 veto, ở lại tấn công).
   - >33% damage (ví dụ 35% hay 78.6%): stay justification (Gate 1 veto, ở lại tấn công).
   - Lethal KO nhưng damage $< 20\%$ max HP: stay justification (Gate 1 veto, ở lại dứt điểm mục tiêu còn sống).
5. **Case E (Threat Semantics):**
   - Lưu ý tính chi phối của Gate 1: Gate 2 (`criticalThreat`) chỉ được kích hoạt khi Gate 1 không veto (AI không có đòn score $\ge 6$ và ratio $\ge 33\%$ hoặc KO - REV-P1-03).
   - Cả 2 opponent OHKO: Nếu AI không có strong move, `criticalThreat = true` $\rightarrow$ `hasLowScore = true` (mở switch eligibility để tránh chết oan).
   - Chỉ 1 opponent OHKO: `criticalThreat = false`. Nếu `lowPressure = true` $\rightarrow$ switch eligible; nếu có strong move $\rightarrow$ Gate 1 veto (ở lại chiến đấu).
   - Không opponent nào OHKO: `criticalThreat = false`. Nếu `lowPressure = true` (Rotom-W live case) $\rightarrow$ switch eligible.
6. **Case F (Preserve Native Gates):**
   - Native Random 75% gate (offset 106) giữ nguyên.
   - Native HP > 50% gate (offset 122) giữ nguyên.
   - Native Party Survivability traversal (offset 123-353) giữ nguyên: chỉ switch khi có bench ally sống sót.

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
