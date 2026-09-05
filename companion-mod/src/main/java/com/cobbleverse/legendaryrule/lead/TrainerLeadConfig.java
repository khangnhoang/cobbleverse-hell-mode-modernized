package com.cobbleverse.legendaryrule.lead;

import java.util.Collections;
import java.util.List;

/**
 * Pure domain representation of a trainer's authored lead configuration.
 */
public record TrainerLeadConfig(List<LeadAttempt> attempts) {
    public TrainerLeadConfig {
        attempts = attempts != null ? List.copyOf(attempts) : Collections.emptyList();
    }
}
