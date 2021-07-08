package ai.evolution.neat;

import ai.evolution.utils.TrainingUtils;

/**
 * Created by vishnughosh on 01/03/17.
 */
public class NEAT_Config {

    public static final int INPUTS = 25 + 7; // 25 states, 7 - hot/one unit type
    public static final int OUTPUTS = 6 + 2 + 7 + 7 + 2; // 6 - action type, 7 unit to move to/from, 7 unit to harvest
    public static final int HIDDEN_NODES = TrainingUtils.HIDDEN_UNITS;
    public static final int POPULATION = TrainingUtils.POPULATION;

    public static final float COMPATIBILITY_THRESHOLD = 1;
    public static final float EXCESS_COEFFICENT = 2;
    public static final float DISJOINT_COEFFICENT = 2;
    public static final float WEIGHT_COEFFICENT = 0.4f;

    public static final float STALE_SPECIES = 15;

    public static final float STEPS = 0.1f;
    public static final float PERTURB_CHANCE = 0.9f;
    public static final float WEIGHT_CHANCE = 0.3f;

    public static final float WEIGHT_MUTATION_CHANCE = 0.9f;
    public static final float NODE_MUTATION_CHANCE = 0.03f;
    public static final float CONNECTION_MUTATION_CHANCE = 0.05f;
    public static final float BIAS_CONNECTION_MUTATION_CHANCE = 0.05f;

    public static final float DISABLE_MUTATION_CHANCE = 0.1f;
    public static final float ENABLE_MUTATION_CHANCE = 0.1f ;

    public static final float CROSSOVER_CHANCE = 0.75f;

    public static final int STALE_POOL = 20;
}
