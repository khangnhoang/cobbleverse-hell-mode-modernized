package com.cobbleverse.legendaryrule.lead;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobbleverse.legendaryrule.lead.adapter.CobblemonLeadAdapter;
import com.cobbleverse.legendaryrule.lead.adapter.PlayerLeadResolver;
import net.minecraft.entity.player.PlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Runtime composition service orchestrating trainer configuration lookup,
 * trainer-bound validation, player lead resolution, engine invocation, structured logging,
 * and safe fallback decisions.
 */
public final class LeadSelectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger("rct_legendary_rule");

    private static volatile LeadSelectionEngine engine = null;
    private static volatile boolean available = false;

    private LeadSelectionService() {}

    public static synchronized void initialize(TypeMatchupScorer scorer) {
        if (scorer != null) {
            engine = new LeadSelectionEngine(scorer);
            available = true;
        } else {
            engine = null;
            available = false;
        }
    }

    public static synchronized void setUnavailable() {
        engine = null;
        available = false;
    }

    public static boolean isAvailable() {
        return available && engine != null && LeadSelectionConfig.isEnabled();
    }

    /**
     * Evaluates dynamic lead selection for a battle instance.
     * Returns the selected LeadSelectionResult, or Optional.empty() if unconfigured, disabled, or invalid.
     */
    public static Optional<LeadSelectionResult> selectLead(
            String trainerId,
            Pokemon[] trainerTeam,
            PlayerEntity player
    ) {
        return selectLead(trainerId, trainerTeam, player,
                PlayerLeadResolver::resolvePlayerLeads,
                (slot, p) -> CobblemonLeadAdapter.toIdentity(p),
                CobblemonLeadAdapter::toRosterMemberTyping);
    }

    static <P> Optional<LeadSelectionResult> selectLead(
            String trainerId,
            Pokemon[] trainerTeam,
            P player,
            java.util.function.Function<P, List<PlayerLeadTyping>> playerResolver,
            java.util.function.BiFunction<Integer, Pokemon, PokemonIdentity> identityFn,
            java.util.function.BiFunction<Integer, Pokemon, RosterMemberTyping> typingFn
    ) {
        if (!isAvailable() || trainerId == null) {
            return Optional.empty();
        }

        // Preflight: check trainer configuration BEFORE scanning/resolving player party
        Optional<TrainerLeadConfig> optConfig = LeadSelectionConfig.getTrainerConfig(trainerId);
        if (optConfig.isEmpty()) {
            // Unconfigured trainer: native-path preservation with zero player resolution overhead
            return Optional.empty();
        }
        TrainerLeadConfig config = optConfig.get();
        if (config.attempts().isEmpty()) {
            return Optional.empty();
        }

        if (trainerTeam == null || trainerTeam.length == 0 || player == null) {
            return Optional.empty();
        }

        // Trainer is configured: now resolve player leads
        List<PlayerLeadTyping> playerLeads = playerResolver.apply(player);
        if (playerLeads == null || playerLeads.isEmpty()) {
            return Optional.empty();
        }

        return selectLeadWithConfig(trainerId, config, trainerTeam, playerLeads, identityFn, typingFn);
    }

    static Optional<LeadSelectionResult> selectLead(
            String trainerId,
            Pokemon[] trainerTeam,
            List<PlayerLeadTyping> playerLeads,
            java.util.function.BiFunction<Integer, Pokemon, PokemonIdentity> identityFn,
            java.util.function.BiFunction<Integer, Pokemon, RosterMemberTyping> typingFn
    ) {
        if (!isAvailable() || trainerId == null || trainerTeam == null || trainerTeam.length == 0 || playerLeads == null || playerLeads.isEmpty()) {
            return Optional.empty();
        }

        Optional<TrainerLeadConfig> optConfig = LeadSelectionConfig.getTrainerConfig(trainerId);
        if (optConfig.isEmpty()) {
            return Optional.empty();
        }
        TrainerLeadConfig config = optConfig.get();
        if (config.attempts().isEmpty()) {
            return Optional.empty();
        }

        return selectLeadWithConfig(trainerId, config, trainerTeam, playerLeads, identityFn, typingFn);
    }

    private static Optional<LeadSelectionResult> selectLeadWithConfig(
            String trainerId,
            TrainerLeadConfig config,
            Pokemon[] trainerTeam,
            List<PlayerLeadTyping> playerLeads,
            java.util.function.BiFunction<Integer, Pokemon, PokemonIdentity> identityFn,
            java.util.function.BiFunction<Integer, Pokemon, RosterMemberTyping> typingFn
    ) {
        List<LeadAttempt> authoredAttempts = config.attempts();
        if (authoredAttempts.isEmpty()) {
            return Optional.empty();
        }

        // Trainer-bound validation: validate each authored attempt independently
        List<LeadAttempt> validAttempts = new ArrayList<>(authoredAttempts.size());
        for (LeadAttempt attempt : authoredAttempts) {
            int slotA = attempt.leadSlots()[0];
            int slotB = attempt.leadSlots()[1];

            if (slotA >= trainerTeam.length || slotB >= trainerTeam.length) {
                LOGGER.warn("[HellMode-Lead] Trainer '{}' attempt '{}' has leadSlots [{}, {}] exceeding team length {}. Skipping attempt.",
                        trainerId, attempt.id(), slotA, slotB, trainerTeam.length);
                continue;
            }

            // Semantic drift guard: verify expectedLeadMembers against actual PokemonIdentity
            List<ExpectedLeadMember> expected = attempt.expectedLeadMembers();
            if (expected != null && expected.size() == 2) {
                PokemonIdentity actualA = identityFn.apply(slotA, trainerTeam[slotA]);
                PokemonIdentity actualB = identityFn.apply(slotB, trainerTeam[slotB]);

                if (!expected.get(0).matches(actualA) || !expected.get(1).matches(actualB)) {
                    LOGGER.warn("[HellMode-Lead] Trainer '{}' attempt '{}' failed semantic drift check. Expected: {}, Actual: [{}, {}]. Skipping attempt.",
                            trainerId, attempt.id(), expected,
                            actualA != null ? actualA.species() : null,
                            actualB != null ? actualB.species() : null);
                    continue;
                }
            }

            validAttempts.add(attempt);
        }

        if (validAttempts.isEmpty()) {
            LOGGER.warn("[HellMode-Lead] Zero valid lead attempts remaining for trainer '{}'. Falling back to native ordering.", trainerId);
            return Optional.empty();
        }

        // Convert trainer team to domain typing representation
        List<RosterMemberTyping> npcRoster = new ArrayList<>(trainerTeam.length);
        for (int i = 0; i < trainerTeam.length; i++) {
            npcRoster.add(typingFn.apply(i, trainerTeam[i]));
        }

        // Invoke pure domain engine
        LeadSelectionResult result = engine.select(validAttempts, playerLeads, npcRoster);

        // Structured logging of evidence
        LOGGER.info("[HellMode-Lead] Trainer={}, PlayerLeads={}, Selected={}, Scores={}",
                trainerId,
                playerLeads.stream().map(PlayerLeadTyping::species).toList(),
                result.selectedAttempt().id(),
                result.evaluatedScores().stream().map(s -> s.attemptId() + "=" + s.totalScore()).toList());

        return Optional.of(result);
    }
}
