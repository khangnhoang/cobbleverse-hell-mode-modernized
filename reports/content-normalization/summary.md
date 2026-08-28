# Phase D — Deterministic Content Normalization Report

**Target Environment:** COBBLEVERSE 1.7.42-CF (Minecraft 1.21.1 Fabric)  
**Dataset:** Modernized `pack/data/rctmod/trainers/` (1714 trainers)  

---

## 1. Executive Summary

| Category | Replaced / Corrected | Details |
| :--- | :--- | :--- |
| **Trainer Files Modified** | **99 files** | Out of 1714 total trainers (99 updated, 1615 unchanged) |
| **Held-Item Replacements** | **53 occurrences** | Corrected 33 unique invalid IDs (Z-Crystals, Orbs, Plates, Memories, bare IDs) |
| **Move Typo Replacements** | **22 occurrences** | Corrected 21 unique truncated move typos (`belly` -> `bellydrum`, `moonbl` -> `moonblast`, etc.) |
| **Ability Typo Replacements** | **2 occurrences** | Corrected 2 truncated abilities (`magic` -> `magicbounce`, `shield` -> `shielddust`) |
| **Aspect / Form Syntax Fixes** | **43 occurrences** | Corrected 29 unique species-aspect syntax discrepancies (`ice-rider`, `dusk-fusion`, `blaze-breed`) |
| **Invalid Gimmick Usages** | **2 occurrences** | Removed `"mega": true` inside `gimmicks` record (Apollo Sharpedo, Giovanni Tyranitar) |
| **Multi-Held Arrays** | **201 preserved** | Kept as arrays with identical element order; inner IDs canonicalized |
| **Intentionally Unresolved Values** | **Preserved** | `shadowblitz` (1), Radical Red Sevii forms (6), `wishiwashi::hisuian` (1), cosmetic aspects (4) |

---

## 2. Replacements Applied

### Held Items (53 replacements across 33 unique IDs)
- **Hyphenated Z-Crystals (22 IDs):** Stale `-z` replaced with `_z` (e.g. `mega_showdown:waterium-z` -> `mega_showdown:waterium_z`).
- **Missing Underscores (6 IDs):** `blueorb` -> `blue_orb`, `redorb` -> `red_orb`, `steelmemory` -> `steel_memory`, `dousedrive` -> `douse_drive`, `pixieplate` -> `pixie_plate`, `adrenalineorb` -> `mega_showdown:adrenaline_orb`.
- **Bare & Missing Namespace (4 IDs):** `charcoal` -> `minecraft:charcoal`, `booster_energy` -> `mega_showdown:booster_energy`, `adamant_crystal` -> `mega_showdown:adamant_crystal`, `lustrous_globe` -> `mega_showdown:lustrous_globe`.
- **Namespace Typo (1 ID):** `megas_showdown:wellspring_mask` -> `mega_showdown:wellspring_mask`.

### Moves (22 replacements across 21 unique IDs)
- Truncated move names corrected to canonical Showdown names:
  `absor` -> `absorb`, `belly` -> `bellydrum`, `calmm` -> `calmmind`, `close` -> `closecombat`, `dazz` -> `dazzlinggleam`, `drain` -> `drainingkiss`, `dream` -> `dreameater`, `icebea` -> `icebeam`, `kara` -> `karatechop`, `moonbl` -> `moonblast`, `psych` -> `psychic`, `quick` -> `quickattack`, `reco` -> `recover`, `rockb` -> `rockblast`, `stonea` -> `stoneaxe`, `supers` -> `supersonic`, `thunderfa` -> `thunderfang`, `thunders` -> `thundershock`, `thunderw` -> `thunderwave`, `vicegrip` -> `visegrip`, `waterspo` -> `watersport`.
- **Preserved Unresolved:** `shadowblitz` (Pokémon Colosseum shadow move) was intentionally NOT modified and remains queued for Phase E redesign.

### Abilities (2 replacements across 2 unique IDs)
- `magic` on Hatterene in `hoenn_tell.json` -> `magicbounce`
- `shield` on Wurmple in `youngster_dallas_03f4.json` -> `shielddust`

### Aspect & Form Syntax (43 replacements across 29 unique pairs)
- `calyrex::ice` -> `ice-rider`
- `necrozma::dusk-mane` / `dusk_mane` -> `dusk-fusion`
- `urshifu::rapid-strike` / `rapid_strike` -> `rapid_strike-style`
- `tauros::paldea-blaze` -> `blaze-breed`
- `rotom::mow` -> `mow-appliance`
- `indeedee::f` and `basculegion::f` -> `female`
- `toxtricity::low_key` -> `low_key-form`
- `shellos::east_sea` and `gastrodon::east_sea` -> `east-sea`
- Therian, Origin, and Silvally memory form syntax standardizations.
- **Preserved Unresolved:** Radical Red Sevii forms (Mantine, Zebstrika, Zoroark, Ursaring, Milotic, Dodrio), `wishiwashi::hisuian`, and cosmetic aspects (`netherite-coating-full`, `surfing`, `flying`, `libre`) were intentionally preserved for runtime/gameplay verification.

### Gimmick Record Cleanup
- Removed invalid `"mega": true` from `gimmicks` record on:
  - `team_rocket_admin_apollo.json` (Sharpedo — equipped with `mega_showdown:sharpedonite`)
  - `team_rocket_giovanni.json` (Tyranitar — equipped with `mega_showdown:tyranitarite`, retaining `dynamax: true` and `gmax: true`)

---

## 3. Multi-Held Item Arrays Preservation
All **201** multi-held item arrays were preserved as lists with identical element ordering. Any invalid item identifiers within the arrays (e.g. hyphenated Z-crystals or unnamespaced items) were canonicalized in-place. Destructive array flattening was intentionally deferred to runtime and design verification.

---

## 4. Idempotency Verification
Rerunning `scripts/normalize-pack/normalize.py` on the normalized dataset produces **0 files modified** and **0 replacements**, proving strict idempotency.
