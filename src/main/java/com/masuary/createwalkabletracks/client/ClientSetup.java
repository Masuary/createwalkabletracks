package com.masuary.createwalkabletracks.client;

import com.masuary.createwalkabletracks.CreateWalkableTracks;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CreateWalkableTracks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    public static final String TOGGLE_KEY_TRANSLATION = "key.createwalkabletracks.toggle_visualizer";
    public static final String KEY_CATEGORY = "key.categories.createwalkabletracks";

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        KeyMapping toggleVisualizer = new KeyMapping(
                TOGGLE_KEY_TRANSLATION,
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_F6,
                KEY_CATEGORY);
        ClientRegistry.registerKeyBinding(toggleVisualizer);
        TrackVisualizer.setToggleKey(toggleVisualizer);
    }
}
