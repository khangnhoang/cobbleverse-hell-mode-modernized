# Canonical Compatibility Audit Summary

**Target Environment:**
- **Modpack:** COBBLEVERSE 1.7.42-CF (1.21.1 Fabric)
- **Cobblemon:** 1.7.3
- **Radical Cobblemon Trainers (RCT):** 0.18.1-beta (API 0.15.2-beta)
- **Mega Showdown:** 1.8.4
- **Legacy Addon Audited:** `!Doctors HELL MODE DOUBLE BATTLE EVERYTHING`

---

## 1. Executive Headline Metrics

| Audit Category | Total Audited | Valid Exact | Invalid with Safe Match | Invalid Ambiguous / No Match | Needs Runtime Test |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Trainer Files** | 1,663 files | 1,660 shared | N/A | 3 obsolete | 54 missing upstream |
| **Held Items** | 214 unique | 181 items | 33 items | 0 items | 0 items |
| **Species** | 784 unique | 784 species | 0 | 0 | 0 |
| **Moves** | 703 unique | 681 moves | 21 moves | 1 move (`shadowblitz`) | 0 |
| **Abilities** | 277 unique | 275 abilities | 2 abilities | 0 | 0 |
| **Aspect Combinations** | 114 pairs | 79 pairs | 20 pairs | 11 pairs (`sevii`, custom) | 4 pairs (cosmetics) |
| **Gimmick Usages** | 173 usages | 171 usages | 2 (`gimmicks.mega`) | 0 | 0 |
| **Multi-Held Items** | 201 Pokémon | N/A | 200 (gimmick intent) | 1 (review needed) | N/A |

---

## 2. Answers to Canonical Audit Questions

### 1. How many trainer files were audited?
- **1,663 trainer JSON files** located under `!Doctors HELL MODE DOUBLE BATTLE EVERYTHING/data/rctmod/trainers/`.

### 2. How many unique held-item identifiers exist?
- **214 unique held-item strings** across 2,101 item assignments.

### 3. How many are valid?
- **181 items** are `VALID_EXACT` against current installed registries (`cobblemon`, `mega_showdown`, and `minecraft`).

### 4. How many are invalid with safe replacements?
- **33 items** are `INVALID_UNIQUE_CANONICAL_MATCH`:
  - **22 Hyphenated Z-Crystals:** Stale `-z` replaced with `_z` (e.g. `mega_showdown:waterium-z` -> `mega_showdown:waterium_z`).
  - **6 Missing Underscores:** `blueorb` -> `blue_orb`, `redorb` -> `red_orb`, `steelmemory` -> `steel_memory`, `dousedrive` -> `douse_drive`, `pixieplate` -> `pixie_plate`, `adrenalineorb` -> `mega_showdown:adrenaline_orb`.
  - **4 Bare/Unnamespaced Items:** `charcoal` -> `minecraft:charcoal`, `booster_energy` -> `mega_showdown:booster_energy`, `adamant_crystal` -> `mega_showdown:adamant_crystal`, `lustrous_globe` -> `mega_showdown:lustrous_globe`.
  - **1 Namespace Typo:** `megas_showdown:wellspring_mask` -> `mega_showdown:wellspring_mask`.

### 5. How many remain ambiguous?
- **0 held items remain ambiguous.** Every single invalid item has exactly one registered canonical target.

### 6. How many multi-held-item Pokémon exist?
- **201 Pokémon** have a multi-item array in `heldItem`:
  - **200 cases** are `[Gimmick Item, Passive Item]` where gameplay intent is clearly to equip the Mega Stone or Z-Crystal.
  - **1 case** (`trainer_brendan_0039.json` Gardevoir with `['mega_showdown:pixieplate', 'fairy_feather']`) requires owner design review.

### 7. Are all species valid?
- **Yes. 100% of the 784 species** match current Cobblemon 1.7.3 species definitions.

### 8. Are all moves valid?
- **681 moves are valid.**
- **21 moves are truncated legacy typos** with obvious unique canonical matches (`belly` -> `bellydrum`, `vicegrip` -> `visegrip`, `stonea` -> `stoneaxe`, `moonbl` -> `moonblast`, etc.).
- **1 move (`shadowblitz`)** is an unsupported Pokémon Colosseum shadow move.

### 9. Are all abilities valid?
- **275 abilities are valid.**
- **2 abilities are truncated typos:**
  - `magic` on Hatterene in `hoenn_tell.json` -> `magicbounce`.
  - `shield` on Wurmple in `youngster_dallas_03f4.json` -> `shielddust`.

### 10. Which aspects/forms are invalid or uncertain?
- **20 syntax discrepancies with safe matches:**
  - `calyrex::ice` -> `ice-rider`
  - `necrozma::dusk-mane` / `dusk_mane` -> `dusk-fusion`
  - `urshifu::rapid-strike` / `rapid_strike` -> `rapid_strike-style`
  - `rotom::mow` -> `mow-appliance`
  - `tauros::paldea-blaze` -> `blaze-breed`
  - `indeedee::f` and `basculegion::f` -> `female`
  - `toxtricity::low_key` -> `low_key-form`
  - `shellos::east_sea` -> `east-sea`
- **11 unsupported forms (`INVALID_NO_MATCH`):**
  - Radical Red Sevii forms (Mantine, Zebstrika, Zoroark, Ursaring, Milotic, Dodrio with aspect `sevii`).
  - Radical Red `wishiwashi::hisuian` (Wishiwashi has no official Hisuian form).
- **4 cosmetic aspects needing runtime check (`NEEDS_RUNTIME_TEST`):**
  - `gholdengo::netherite-coating-full`, `pikachu::surfing`, `pikachu::flying`, `pikachu::libre`.

### 11. Which gimmick usages are invalid?
- Exactly **2 invalid `"mega": true` keys inside the `gimmicks` record**:
  - `team_rocket_admin_apollo.json` (Sharpedo)
  - `team_rocket_giovanni.json` (Tyranitar)
  RCT API `Gimmicks` record only supports `tera`, `dynamax`, and `gmax`. Mega is handled via held stones or aspect tags.

### 12. How many trainer IDs are missing/obsolete relative to current Cobbleverse?
- **54 missing current trainers:** Major Team Galactic commanders (`team_galactic_cyrus`, `mars`, `jupiter`, `saturn`, `charon`) and Hisui characters (`hisui_damon`, `hisui_perula`).
- **3 obsolete legacy trainers:** `galaxy_bobbo.json`, `galaxy_ominorosso.json`, `swimmer_gengar.json`.

### 13. Which findings block Phase C/D?
- **None.** All invalid held items, moves, and abilities have deterministic canonical replacements, giving Phase C/D full automated repair blueprints.

### 14. Which findings require runtime testing rather than static correction?
- Verification that Cobblemon selects slot 0 when array held items are passed.
- Verification of visual cosmetic aspects (`netherite-coating-full`, `surfing`).
