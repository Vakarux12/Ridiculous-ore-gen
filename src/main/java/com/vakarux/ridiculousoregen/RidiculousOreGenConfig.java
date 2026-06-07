package com.vakarux.ridiculousoregen;

import net.neoforged.neoforge.common.ModConfigSpec;

public class RidiculousOreGenConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue ORE_MULTIPLIER = BUILDER
            .comment("How many times more ore veins to generate compared to vanilla.",
                     "A value of 1 = vanilla, 20 = twenty times more veins.",
                     "Changes take effect on newly generated chunks immediately.",
                     "Use /ridiculousores populate <radius> to retrofit existing chunks.")
            .defineInRange("oreMultiplier", 20, 1, 100);

    public static final ModConfigSpec.IntValue RETROFIT_MAX_RADIUS = BUILDER
            .comment("Maximum chunk radius allowed for the /ridiculousores populate command.")
            .defineInRange("retrofitMaxRadius", 32, 1, 128);

    static final ModConfigSpec SPEC = BUILDER.build();
}
