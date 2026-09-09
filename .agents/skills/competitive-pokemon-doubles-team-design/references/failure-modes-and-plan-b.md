# Failure-Mode Reasoning & Plan B Architecture

This reference outlines the required resilience analysis for competitive NPC Doubles rosters in Cobbleverse Hell Mode, ensuring teams withstand player counterplay and disruptions.

---

## 1. Compositional Emergence vs. AI-Dependent Sequencing

When designing an NPC team, distinguish between behaviors that occur naturally from game mechanics versus those requiring human-level AI sequencing:

| Emerges Naturally from Composition (Reliable) | Requires Intelligent Sequencing (AI-Dependent) |
| :--- | :--- |
| Chlorophyll doubling Speed under active Sun | Recognizing opponent Trick Room and deliberately reversing it |
| Mega Gengar trapping opponents via Shadow Tag | Holding a gimmick for the optimal late-game cleaner |
| Slow Rhyperior moving first under active Trick Room | Dynamically selecting a defensive Tera to counter a specific attack |
| Spread moves (Heat Wave, Rock Slide) hitting both slots | Executing multi-turn defensive switch-cycling into resistances |
| Redirection (Follow Me) automatically drawing single-target hits | Predicting an opponent's Protect and doubling into the other slot |

**Core Rule:** If an interaction depends on complex AI prediction, classify it as `AI-dependent`. Provide passive bulk, redirection, or alternative lines so the team does not crumble if the AI makes a basic move choice.

---

## 2. Common Doubles Failure Modes

Every trainer roster must be tested against these four primary failure modes:

### 1. Disrupted Speed Control
- **Threat:** Tailwind setter is Taunted, flinched by Fake Out, or OHKO'd before clicking Tailwind. Trick Room setter is double-targeted or Imprisoned.
- **Resilience Mechanisms:**
  - Secondary speed control (e.g., Icy Wind, Electroweb, Bulldoze).
  - Priority attacks (Extreme Speed, Sucker Punch, Fake Out, Aqua Jet).
  - Mental Herb to neutralize Taunt/Encore.
  - Covert Cloak to prevent Fake Out flinches.

### 2. Weather & Terrain Overwrites
- **Threat:** Player switches in opposing weather/terrain (e.g., Pelipper bringing Rain against an Adaman Sun team; Rillaboom replacing Psychic Terrain with Grassy Terrain).
- **Resilience Mechanisms:**
  - Dual-weather capability or manual setter (e.g., Prankster Sunny Day / Rain Dance).
  - Pokémon whose offensive viability does not drop to zero without weather (e.g., high base Attack/SpA, alternative STAB).
  - Complementary defensive typings that resist opposing weather STAB.

### 3. Redirection & Spread Counterplay
- **Threat:** Player uses Wide Guard to block Rock Slide, Heat Wave, Surf, or Earthquake. Player uses Follow Me / Rage Powder to redirect super-effective single-target nukes.
- **Resilience Mechanisms:**
  - Single-target coverage moves alongside spread moves (e.g., Flamethrower alongside Heat Wave).
  - Single-target Ground moves (High Horsepower) alongside Earthquake.
  - Grass types, Safety Goggles, or Overcoat to ignore Rage Powder.

### 4. Early Neutralization of the Gimmick User
- **Threat:** The designated Mega or Dynamax user is double-targeted or critically damaged on Turn 1 or 2.
- **Resilience Mechanisms:**
  - The remaining 5 Pokémon must possess independent offensive and defensive synergy.
  - The team must not rely exclusively on a single "carry" Pokémon to deal damage.

---

## 3. Plan B Design Requirements

Each team specification must articulate a clear Plan B:
1. What is the team's secondary line if Plan A (the primary archetype/setup) is denied?
2. Which Pokémon become the focal attackers under the secondary line?
3. Does Plan B naturally integrate with the existing roster without introducing conflicting weather or field conditions?
