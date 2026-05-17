package com.masuary.createwalkabletracks;

import net.minecraft.core.BlockPos;

public interface IFakeTrackPad {
    boolean cwt$setOwnerPadRange(BlockPos owner, float padMinY, float padMaxY);

    boolean cwt$removeOwner(BlockPos owner);

    float cwt$getPadMinY();

    float cwt$getPadMaxY();

    boolean cwt$isPadSet();
}
