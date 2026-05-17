package com.masuary.createwalkabletracks;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(CreateWalkableTracks.MOD_ID)
public class CreateWalkableTracks {
    public static final String MOD_ID = "createwalkabletracks";

    public CreateWalkableTracks() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
}
