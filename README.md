# Cobbleverse Hell Mode Modernized

A community modernization project bringing the intense, world-wide Doubles battle experience of **Doctor''s Hell Mode** up to date with modern **Cobbleverse** versions.

> **Status: Work in Progress**  
> This repository represents an ongoing modernization and compatibility initiative. It is **not yet a finished drop-in replacement** for active play.

---

## Overview

The original Hell Mode created by Doctor provided a comprehensive overhaul that turned Cobbleverse trainer battles into challenging competitive Doubles encounters featuring Protect, speed control, VGC weather/terrain synergies, and competitive team compositions.

However, because the legacy addon was based on a snapshot from early 2026, subsequent updates to Cobbleverse, Cobblemon, and Radical Cobblemon Trainers (RCT) have led to drift, stale items, and missing storyline trainers.

This project aims to preserve the spirit and high-difficulty competitive Doubles design of Doctor''s original work while modernizing it against current modpack standards.

---

## Core Goals

- **Up-to-Date Compatibility:** Ensure seamless compatibility with the latest Cobbleverse modpack releases.
- **Item & Mechanic Corrections:** Fix legacy item identifier discrepancies (such as hyphenated Z-Crystals, namespace mismatches, and Mega Showdown changes).
- **Form & Aspect Consistency:** Validate all Pokémon forms, regional variants, and aspect tags against current Cobblemon data.
- **Reconcile New Content:** Port and design proper Doubles teams for newly added Cobbleverse storyline trainers (including Team Galactic commanders and Hisuian characters).
- **Clean Override Footprint:** Eliminate trivial or unnecessary file overrides, ensuring only intentionally reworked trainers take precedence over upstream data.
- **Optional Competitive Guardrails:** Provide small, optional server-side companion rules (such as enforcing a maximum of 1 Legendary or Mythical Pokémon in the player''s party).

---

## Target Compatibility Baseline

| Component | Target Version |
| :--- | :--- |
| **Modpack** | COBBLEVERSE 1.7.42-CF |
| **Minecraft** | 1.21.1 (Fabric) |
| **Cobblemon** | 1.7.3 |
| **Radical Cobblemon Trainers (RCT)** | 0.18.1-beta |
| **RCT API** | 0.15.2-beta |
| **Mega Showdown** | 1.8.4 |

---

## Project Structure

- `!Doctors HELL MODE DOUBLE BATTLE EVERYTHING/`: Preserved legacy baseline reference dataset.
- `implementation-plans/`: Technical planning documentation, architecture notes, and progress trackers.

Detailed technical implementation documentation can be found in [`implementation-plans/hell-mode-modernization/plan.md`](implementation-plans/hell-mode-modernization/plan.md).

---

## Credits & Disclaimer

- **Original Concept & Overhaul:** Full credit goes to **Doctor** for creating the original *HELL MODE DOUBLE BATTLE EVERYTHING* addon and designing its vast roster of competitive Doubles teams.
- **Disclaimer:** This project is an independent community modernization effort and is **not affiliated with or endorsed by** the original Doctor addon author.
- **Licensing Notice:** The files in this repository represent derivative modifications of third-party community content. Formal licensing and redistribution terms for the original assets are currently under review. No commercial use or relicensing is claimed.