package com.masuary.createwalkabletracks;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue VISUALIZER_RANGE_BLOCKS;
    public static final ForgeConfigSpec.IntValue VISUALIZER_BEZIER_SAMPLE_COUNT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("visualizer");
        VISUALIZER_RANGE_BLOCKS = builder
                .comment("Maximum range in blocks to render the track visualizer overlay.")
                .defineInRange("rangeBlocks", 64, 16, 256);
        VISUALIZER_BEZIER_SAMPLE_COUNT = builder
                .comment("Number of line segments used to render each bezier curve.")
                .defineInRange("bezierSampleCount", 64, 8, 256);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
