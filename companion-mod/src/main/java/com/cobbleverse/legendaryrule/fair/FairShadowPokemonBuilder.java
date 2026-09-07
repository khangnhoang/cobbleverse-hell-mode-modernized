package com.cobbleverse.legendaryrule.fair;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.api.types.tera.TeraTypes;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.item.ItemStack;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * Builds minimal, sanitized shadow {@link BattlePokemon} instances for active opposing Pokémon.
 *
 * <p>Enforces the anti-cheat information boundary:
 * <ul>
 *   <li><b>PUBLIC / SAFE</b>: Species, Form, Level, Gender, Shiny, Aspects, Current Health,
 *       visible Status condition, and active in-battle Stat Changes are preserved.</li>
 *   <li><b>HIDDEN / MUST NOT COPY</b>: Held items, unrevealed moves, unrevealed ability,
 *       exact custom IVs/EVs/nature-derived stats are stripped / replaced with neutral benchmarks.</li>
 *   <li><b>MECHANICAL PLACEHOLDER</b>: Tera type is assigned a safe fallback placeholder to prevent
 *       crashes without leaking the true hidden Tera type.</li>
 * </ul>
 * </p>
 */
public final class FairShadowPokemonBuilder {

    private FairShadowPokemonBuilder() {
    }

    /**
     * Builds a sanitized shadow {@link BattlePokemon} from an active battle Pokémon.
     *
     * @param abp the real ActiveBattlePokemon of the opponent
     * @return a sanitized shadow BattlePokemon, or null if abp or its BP is null
     */
    public static BattlePokemon build(ActiveBattlePokemon abp) {
        if (abp == null) {
            return null;
        }
        BattlePokemon realBp = abp.getBattlePokemon();
        if (realBp == null) {
            return null;
        }
        return build(realBp);
    }

    /**
     * Builds a sanitized shadow {@link BattlePokemon} from a real {@link BattlePokemon}.
     *
     * @param realBp the real BattlePokemon of the opponent
     * @return a sanitized shadow BattlePokemon
     */
    public static BattlePokemon build(BattlePokemon realBp) {
        if (realBp == null) {
            return null;
        }
        Pokemon realPkmn = realBp.getEffectedPokemon();
        if (realPkmn == null) {
            realPkmn = realBp.getOriginalPokemon();
        }
        if (realPkmn == null) {
            return realBp;
        }

        Pokemon shadowPkmn = createShadowPokemon(realPkmn);

        BattlePokemon shadowBp;
        try {
            shadowBp = new BattlePokemon(
                shadowPkmn,
                shadowPkmn,
                Collections.emptyList(),
                Collections.emptyList()
            );
        } catch (Throwable t) {
            shadowBp = allocateUnsafeBattlePokemon(shadowPkmn);
        }

        // Attach real battle actor to preserve battle, side, and showdown targeting identities
        try {
            shadowBp.actor = realBp.getActor();
        } catch (Throwable ignored) {
        }

        // Copy public in-battle stat changes (stages)
        try {
            if (realBp.getStatChanges() != null && shadowBp.getStatChanges() != null) {
                shadowBp.getStatChanges().putAll(realBp.getStatChanges());
            }
        } catch (Throwable ignored) {
        }

        // Ensure shadow BattlePokemon held item is strictly empty and never triggers Minecraft item registries
        attachEmptyHeldItemManager(shadowBp);

        return shadowBp;
    }

    private static Pokemon createShadowPokemon(Pokemon realPkmn) {
        Pokemon shadow = createFreshPokemon();

        // --- PUBLIC / SAFE FIELDS ---
        try {
            shadow.setSpecies(realPkmn.getSpecies());
        } catch (Throwable t) {
            try {
                Field speciesField = Pokemon.class.getDeclaredField("species");
                speciesField.setAccessible(true);
                speciesField.set(shadow, realPkmn.getSpecies());
            } catch (Throwable ignored) {
            }
        }
        try {
            shadow.setForm(realPkmn.getForm());
        } catch (Throwable t) {
            try {
                Field formField = Pokemon.class.getDeclaredField("form");
                formField.setAccessible(true);
                formField.set(shadow, realPkmn.getForm());
            } catch (Throwable ignored) {
            }
        }
        try {
            shadow.setLevel(realPkmn.getLevel());
        } catch (Throwable t) {
            try {
                Field levelField = Pokemon.class.getDeclaredField("level");
                levelField.setAccessible(true);
                levelField.set(shadow, realPkmn.getLevel());
            } catch (Throwable ignored) {
            }
        }
        try {
            shadow.setGender(realPkmn.getGender());
        } catch (Throwable t) {
            try {
                Field genderField = Pokemon.class.getDeclaredField("gender");
                genderField.setAccessible(true);
                genderField.set(shadow, realPkmn.getGender());
            } catch (Throwable ignored) {
            }
        }
        try {
            shadow.setShiny(realPkmn.getShiny());
        } catch (Throwable t) {
            try {
                Field shinyField = Pokemon.class.getDeclaredField("shiny");
                shinyField.setAccessible(true);
                shinyField.set(shadow, realPkmn.getShiny());
            } catch (Throwable ignored) {
            }
        }
        // Health: Only public observed HP fraction is known.
        // Scale the observed fraction to the shadow's benchmark max HP to avoid leaking exact stats.
        applySanitizedHealth(shadow, realPkmn);

        try {
            shadow.swapHeldItem(ItemStack.EMPTY, false, false);
        } catch (Throwable t) {
            try {
                Field heldItemField = Pokemon.class.getDeclaredField("heldItem");
                heldItemField.setAccessible(true);
                heldItemField.set(shadow, ItemStack.EMPTY);
            } catch (Throwable ignored) {
            }
        }

        try {
            if (realPkmn.getAspects() != null && shadow.getAspects() != null) {
                shadow.getAspects().addAll(realPkmn.getAspects());
            }
        } catch (Throwable ignored) {
        }

        try {
            if (realPkmn.getStatus() != null) {
                shadow.setStatus(realPkmn.getStatus());
            }
        } catch (Throwable ignored) {
        }

        try {
            if (realPkmn.getUuid() != null) {
                shadow.setUuid(realPkmn.getUuid());
            }
        } catch (Throwable ignored) {
        }

        try {
            if (shadow.getMoveSet() != null) {
                shadow.getMoveSet().clear();
            }
        } catch (Throwable ignored) {
        }

        // --- HIDDEN FIELDS / NEUTRAL BENCHMARKS ---
        // Ability: Must NOT copy real hidden ability. Use neutral dummy/runaway ability.
        applyNeutralAbility(shadow);

        // Tera Type: Unknown Tera semantics are deferred: use safe placeholder based on public form/primary type
        // or NORMAL, strictly to prevent crashes if inspected. Never copy real hidden teraType.
        applyTeraPlaceholder(shadow, realPkmn);

        return shadow;
    }

    private static Pokemon createFreshPokemon() {
        try {
            return new Pokemon();
        } catch (Throwable t) {
            Pokemon pkmn = allocateUnsafePokemon();
            try {
                Field aspectsField = Pokemon.class.getDeclaredField("aspects");
                aspectsField.setAccessible(true);
                aspectsField.set(pkmn, new LinkedHashSet<String>());
            } catch (Throwable ignored) {
            }
            try {
                Field moveSetField = Pokemon.class.getDeclaredField("moveSet");
                moveSetField.setAccessible(true);
                moveSetField.set(pkmn, new com.cobblemon.mod.common.api.moves.MoveSet());
            } catch (Throwable ignored) {
            }
            try {
                Field uuidField = Pokemon.class.getDeclaredField("uuid");
                uuidField.setAccessible(true);
                uuidField.set(pkmn, java.util.UUID.randomUUID());
            } catch (Throwable ignored) {
            }
            try {
                Field heldItemField = Pokemon.class.getDeclaredField("heldItem");
                heldItemField.setAccessible(true);
                heldItemField.set(pkmn, ItemStack.EMPTY);
            } catch (Throwable ignored) {
            }
            return pkmn;
        }
    }

    private static void applyNeutralAbility(Pokemon shadow) {
        Ability ability = null;
        try {
            AbilityTemplate dummy = Abilities.INSTANCE.getDUMMY();
            if (dummy != null) {
                ability = dummy.create(false, Priority.NORMAL);
            }
        } catch (Throwable ignored) {
        }

        if (ability == null) {
            try {
                AbilityTemplate runaway = Abilities.get("runaway");
                if (runaway != null) {
                    ability = runaway.create(false, Priority.NORMAL);
                }
            } catch (Throwable ignored) {
            }
        }

        if (ability == null) {
            ability = createFallbackDummyAbility();
        }

        setPokemonAbility(shadow, ability);
    }

    private static Ability createFallbackDummyAbility() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            AbilityTemplate template = (AbilityTemplate) unsafe.allocateInstance(AbilityTemplate.class);
            Field nameField = AbilityTemplate.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(template, "dummy");

            Ability ability = (Ability) unsafe.allocateInstance(Ability.class);
            Field templateField = Ability.class.getDeclaredField("template");
            templateField.setAccessible(true);
            templateField.set(ability, template);
            return ability;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void setPokemonAbility(Pokemon shadow, Ability ability) {
        if (ability == null) return;
        try {
            shadow.updateAbility(ability);
        } catch (Throwable t) {
            try {
                Field abilityField = Pokemon.class.getDeclaredField("ability");
                abilityField.setAccessible(true);
                abilityField.set(shadow, ability);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void applyTeraPlaceholder(Pokemon shadow, Pokemon realPkmn) {
        try {
            ElementalType primary = realPkmn.getPrimaryType();
            if (primary != null) {
                TeraType placeholder = TeraTypes.forElementalType(primary);
                if (placeholder != null) {
                    shadow.setTeraType(placeholder);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            shadow.setTeraType(TeraTypes.getNORMAL());
        } catch (Throwable ignored) {
        }
    }

    private static Pokemon allocateUnsafePokemon() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            return (Pokemon) unsafe.allocateInstance(Pokemon.class);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to allocate shadow Pokemon instance", t);
        }
    }

    private static BattlePokemon allocateUnsafeBattlePokemon(Pokemon shadowPkmn) {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            BattlePokemon bp = (BattlePokemon) unsafe.allocateInstance(BattlePokemon.class);

            Field effected = BattlePokemon.class.getDeclaredField("effectedPokemon");
            effected.setAccessible(true);
            effected.set(bp, shadowPkmn);

            Field orig = BattlePokemon.class.getDeclaredField("originalPokemon");
            orig.setAccessible(true);
            orig.set(bp, shadowPkmn);

            Field stats = BattlePokemon.class.getDeclaredField("statChanges");
            stats.setAccessible(true);
            stats.set(bp, new LinkedHashMap<>());

            try {
                Field ctx = BattlePokemon.class.getDeclaredField("contextManager");
                ctx.setAccessible(true);
                ctx.set(bp, new com.cobblemon.mod.common.battles.interpreter.ContextManager());
            } catch (Throwable ignored) {
            }

            attachEmptyHeldItemManager(bp);

            return bp;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to allocate shadow BattlePokemon instance", t);
        }
    }

    private static void attachEmptyHeldItemManager(BattlePokemon bp) {
        if (bp == null) return;
        try {
            HeldItemManager emptyItemManager = (HeldItemManager) Proxy.newProxyInstance(
                HeldItemManager.class.getClassLoader(),
                new Class<?>[]{HeldItemManager.class},
                (proxy, method, args) -> {
                    if ("showdownId".equals(method.getName())) {
                        return "";
                    }
                    if ("shouldConsumeItem".equals(method.getName())) {
                        return false;
                    }
                    return null;
                }
            );
            kotlin.Lazy<HeldItemManager> lazyItem = kotlin.LazyKt.lazy(() -> emptyItemManager);
            Field itemDel = BattlePokemon.class.getDeclaredField("heldItemManager$delegate");
            itemDel.setAccessible(true);
            itemDel.set(bp, lazyItem);
        } catch (Throwable ignored) {
        }
    }

    private static void applySanitizedHealth(Pokemon shadow, Pokemon realPkmn) {
        try {
            int realCurrent = realPkmn.getCurrentHealth();
            int realMax = realPkmn.getMaxHealth();

            if (realCurrent <= 0) {
                setShadowHealth(shadow, 0);
                return;
            }

            int shadowMax = Math.max(1, shadow.getMaxHealth());

            // Full health: preserve exact 100% full-health flag for Sturdy/Focus Sash checks
            if (realCurrent >= realMax) {
                setShadowHealth(shadow, shadowMax);
                return;
            }

            // Public observed fraction (0.0 < fraction < 1.0)
            double fraction = (double) realCurrent / (double) Math.max(1, realMax);
            int scaled = (int) Math.round(fraction * shadowMax);

            // Bound scaled health strictly between 1 and shadowMax - 1
            if (scaled <= 0) {
                scaled = 1;
            } else if (scaled >= shadowMax) {
                scaled = shadowMax - 1;
            }

            setShadowHealth(shadow, scaled);
        } catch (Throwable t) {
            try {
                setShadowHealth(shadow, Math.max(0, realPkmn.getCurrentHealth()));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void setShadowHealth(Pokemon shadow, int health) {
        try {
            shadow.setCurrentHealth(health);
        } catch (Throwable t) {
            try {
                Field healthField = Pokemon.class.getDeclaredField("currentHealth");
                healthField.setAccessible(true);
                healthField.set(shadow, health);
            } catch (Throwable ignored) {
            }
        }
    }

    private static volatile Move SAFE_SENTINEL_MOVE = null;

    /**
     * Returns a cached safe sentinel {@link Move} whose name is "unknown".
     *
     * <p>This move is NEVER assigned to any Pokémon's moveset, NEVER enters damage calculations,
     * and NEVER fabricates candidate moves. It exists solely to satisfy unguarded native call sites
     * (such as RunBunAI line 1779) that immediately inspect {@code move.getName()} on the first element
     * of an opponent's move list.</p>
     *
     * @return a safe sentinel Move with name "unknown"
     */
    public static Move getSafeSentinelMove() {
        if (SAFE_SENTINEL_MOVE == null) {
            synchronized (FairShadowPokemonBuilder.class) {
                if (SAFE_SENTINEL_MOVE == null) {
                    SAFE_SENTINEL_MOVE = createSafeSentinelMove();
                }
            }
        }
        return SAFE_SENTINEL_MOVE;
    }

    private static Move createSafeSentinelMove() {
        try {
            MoveTemplate template = Moves.getByNameOrDummy("unknown");
            if (template != null) {
                return template.create();
            }
        } catch (Throwable ignored) {
        }
        return createFallbackSentinelMove();
    }

    private static Move createFallbackSentinelMove() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(null);
            MoveTemplate template = (MoveTemplate) unsafe.allocateInstance(MoveTemplate.class);
            Field nameField = MoveTemplate.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(template, "unknown");

            Move move = (Move) unsafe.allocateInstance(Move.class);
            Field templateField = Move.class.getDeclaredField("template");
            templateField.setAccessible(true);
            templateField.set(move, template);
            return move;
        } catch (Throwable t) {
            return null;
        }
    }
}
