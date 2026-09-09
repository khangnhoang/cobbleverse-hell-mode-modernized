# Doubles Archetypes, Team Systems & Speed Control

This reference provides detailed architectural guidance for selecting competitive Doubles archetypes, win conditions, speed control mechanisms, and interactive doubles combinations for Cobbleverse Hell Mode NPC rosters.

---

## 1. Supported Doubles Archetypes

A competitive Doubles team must function as an interactive engine rather than six disconnected strong Pokémon.

### Tailwind Offense
- **Core Concept:** Exploits priority speed advantage to exert heavy spread and single-target offensive pressure before opponents can act.
- **Key Setters:** Whimsicott (Prankster), Talonflame (Gale Wings), Murkrow (Prankster), Kilowattrel (Wind Power), Corviknight.
- **Beneficiaries:** Hard-hitting sweepers with mid-tier speed (base 80–105) that become unstoppable when doubled (e.g., Chi-Yu, Urshifu, Dragonite, Hydreigon, Landorus).

### Trick Room (Hard Room vs. Semi-Room)
- **Core Concept:** Inverts turn order for 5 turns, enabling bulky, minimum-Speed attackers to strike first.
- **Hard Room:** Dedicated setters, minimum-speed IVs (0 Speed), Brave/Quiet natures across the entire roster, and redirection/Fake Out to guarantee setup.
- **Semi-Room:** Possesses dual modes—a fast primary mode (e.g., Tailwind or high base speed) and a slow secondary mode designed to reverse opposing Tailwind or punish hyper-offense.
- **Key Setters:** Farigiraf (Armor Tail prevents priority), Indeedee-F (Psychic Surge + Follow Me), Dusclops (Eviolite bulk), Dusknoir, Porygon2, Hatterene.
- **Beneficiaries:** Torkoal, Ursaluna (Bloodmoon or standard), Rhyperior, Kingambit, Iron Hands, Armarouge, Glastrier.

### Weather Synergies
- **Sun (Drought):**
  - Setters: Torkoal, Ninetales.
  - Abusers: Chlorophyll speed sweepers (Leafeon, Venusaur, Scovillain), Protosynthesis (Flutter Mane, Roaring Moon), Flower Gift (Cherrim), Solar Blade users, boosted Fire STAB.
- **Rain (Drizzle):**
  - Setters: Pelipper, Politoed.
  - Abusers: Swift Swim (Kingdra, Barraskewda, Basculegion, Ludicolo), 100% accurate Hurricane/Thunder (Tornadus, Kilowattrel, Zapdos), Storm Drain redirection/absorption.
- **Snow / Hail (Snow Warning):**
  - Setters: Abomasnow, Ninetales-Alolan.
  - Abusers: 50% Ice Defense boost, 100% accurate Blizzard spreads, Slush Rush sweepers (Cetitan, Beartic).
- **Sandstorm (Sand Stream):**
  - Setters: Tyranitar, Hippowdon.
  - Abusers: 50% Rock SpD boost, Sand Rush (Excadrill, Houndstone), Sand Force.

### Electric / Terrain Offense
- **Electric Surge (Pincurchin, Miraidon if allowed):**
  - Activates Quark Drive for Paradox threats (Iron Hands, Iron Bundle, Iron Jugulis, Iron Moth).
  - Blocks Sleep status (counters Spore/Sleep Powder).
- **Psychic Surge (Indeedee):**
  - Blocks all priority moves hitting grounded targets (Fake Out, Sucker Punch, Extreme Speed, Prankster Taunt).
  - Powers Expanding Force into a lethal 120 BP spread attack.

### Redirection + Setup / Wallbreaking
- **Redirection Users:** Amoonguss (Rage Powder + Spore), Indeedee-F (Follow Me), Ogerpon-Wellspring (Follow Me), Clefairy (Friend Guard + Follow Me), Maushold (Friend Guard + Follow Me).
- **Protected Beneficiaries:** Belly Drum users, Nasty Plot/Swords Dance sweepers, Trick Room setters, frail high-output attackers.

### Spread Defense / Defensive Control
- **Wide Guard:** Counters opposing spread attacks (Rock Slide, Earthquake, Surf, Heat Wave, Blizzard, Hyper Voice, Expanding Force, Make It Rain).
- **Intimidate & Snarl:** Lowers opponent physical and special output on entry and spread, compounding team durability (Arcanine, Incineroar, Gyarados, Salamence).

---

## 2. Speed Control Framework

Every team must deliberately control turn order or have a dedicated mechanism for operating while slower:

1. **Active Speed Multipliers:**
   - Tailwind (+100% Speed to party for 4 turns).
   - Weather abilities: Chlorophyll (Sun), Swift Swim (Rain), Slush Rush (Snow), Sand Rush (Sand).
   - Booster Energy / Quark Drive / Protosynthesis (Speed tier +50%).

2. **Turn Inversion:**
   - Trick Room (5 turns). Beneficiaries MUST have Brave/Quiet nature and **0 Speed IVs**.

3. **Spread Speed Drops:**
   - Icy Wind (100% accurate spread -1 Spe).
   - Electroweb (95% accurate spread -1 Spe).
   - Bulldoze (spread -1 Spe; requires Levitate/Flying/Telepathy partner).

4. **Priority Pressure:**
   - Fake Out (+3 tempo flinch on Turn 1).
   - Extreme Speed (+2 priority).
   - Sucker Punch, Aqua Jet, Shadow Sneak, Bullet Punch, Ice Shard (+1 priority).
   - Prankster status moves (+1 priority).

5. **Bulky Redirection (No Speed Advantage Needed):**
   - High natural bulk, Friend Guard damage reduction, and redirection designed to absorb incoming hits without requiring a speed advantage.

---

## 3. Interactive Doubles Combinations

- **Fake Out + Setup:** Fake Out immobilizes one threat while the partner sets Tailwind, Trick Room, weather, or screens.
- **Redirection + Setup:** Follow Me / Rage Powder diverts single-target super-effective attacks away from frail sweepers or setup users.
- **Wide Guard Shielding:** Blocks incoming Earthquake or Surf from hitting the team, allowing an adjacent attacker to freely click spread moves or setup.
- **Friendly-Fire Avoidance:**
  - If using `earthquake`: Partner must have Flying type, Levitate, Telepathy, Protect, or Air Balloon.
  - If using `surf`: Partner must have Water Absorb, Storm Drain, Dry Skin, Telepathy, or Protect.
  - Otherwise, replace with single-target equivalents: `highhorsepower`, `drillrun`, `liquidation`, `wavecrash`.
- **Helping Hand:** Elevates partner damage thresholds past critical OHKO benchmarks.
- **Pollen Puff:** Dual utility: damaging opponents (90 BP Bug) or healing adjacent partner for 50% max HP.
