# Historical Case Studies from Phase E Pilot

This reference documents real-world design analyses, failure modes, and lessons learned from the modernized trainer pilot implementations in Cobbleverse Hell Mode.

---

## Case Study 1: Charon & Mega Gengar (Exemplary Compositional Emergence)

- **Team Design:** Lead Mega Gengar (`heldItem: ["mega_showdown:gengarite"]`) alongside Rotom-Wash (`aspects: ["wash-appliance"]`).
- **Why It Works:**
  - Mega Gengar immediately triggers Shadow Tag on Turn 1 upon Mega Evolving.
  - This traps both opposing Pokémon on the field without requiring the AI to make a timing decision.
  - Trapped opponents cannot switch out of Rotom-Wash's Will-O-Wisp, Hydro Pump, or Crobat's Super Fang.
- **Architectural Lesson:**
  The most reliable NPC Doubles strategies rely on persistent field conditions and immediate entry abilities that execute automatically through game mechanics rather than tactical AI prediction.

---

## Case Study 2: Buck & Dusknoir (Flawed Reasoning: Unjustified Certainty)

- **Team Design:** Minimum-Speed Trick Room team with Mental Herb Dusknoir, Drought Torkoal, and sole Dynamax Rhyperior.
- **The Design Flaw in Documentation:**
  The design brief claimed Mental Herb Dusknoir provided *"guaranteed Trick Room"*.
- **Why It Was Flawed:**
  - While Mental Herb prevents single-target Taunt and Encore, Trick Room can still be disrupted by:
    - Multi-target focus-fire resulting in an early Turn 1 KO.
    - Flinches from Fake Out (Dusknoir lacks Covert Cloak).
    - Opposing Imprison or priority Taunt under Prankster.
- **Architectural Lesson:**
  Never claim setup is "guaranteed" or "cannot fail". Always analyze disruption windows and articulate recovery paths if setup fails.

---

## Case Study 3: Adaman & Leafeon (Flawed Gimmick Assignment: Incomplete Type Audit)

- **Team Design:** Chlorophyll Sun team assigning Terastallize Fire to Leafeon.
- **The Design Flaw in Documentation:**
  The design brief claimed Tera Fire *"cannot backfire"* and created an immediate, uncounterable Turn 1 threat.
- **Why It Was Flawed:**
  1. **Zero Offensive Synergies:** Leafeon's moveset consisted of Solar Blade, Leaf Blade, Bite, and Protect. Without a Fire-type attack or Tera Blast, Tera Fire provided zero offensive damage boost.
  2. **Severe Defensive Liability:** While Tera Fire removes Bug, Fire, and Ice weaknesses, it introduces weaknesses to Water, Rock, and Ground—three of the most prevalent offensive types in Doubles (via Surf, Muddy Water, Rock Slide, and High Horsepower).
- **Architectural Lesson:**
  Always audit the entire defensive type chart and verify that the Pokémon's moveset actually gains offensive benefit before approving a Tera assignment for an NPC.
