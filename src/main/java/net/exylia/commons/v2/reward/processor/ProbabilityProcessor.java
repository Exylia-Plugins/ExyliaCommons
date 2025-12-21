package net.exylia.commons.v2.reward.processor;

import net.exylia.commons.v2.reward.model.Reward;

import java.util.concurrent.ThreadLocalRandom;

public class ProbabilityProcessor {

    public static boolean shouldGive(Reward reward) {
        return shouldGive(reward.getChance());
    }

    public static boolean shouldGive(double chance) {
        if (chance <= 0.0) {
            return false;
        }
        if (chance >= 100.0) {
            return true;
        }

        double roll = ThreadLocalRandom.current().nextDouble(0.0, 100.0);
        return roll < chance;
    }

    public static double roll() {
        return ThreadLocalRandom.current().nextDouble(0.0, 100.0);
    }
}
