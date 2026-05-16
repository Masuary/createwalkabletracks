package com.masuary.createwalkabletracks.mixin;

import com.masuary.createwalkabletracks.IFakeTrackPad;
import com.simibubi.create.content.trains.track.FakeTrackBlockEntity;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FakeTrackBlockEntity.class)
public abstract class FakeTrackBlockEntityMixin extends SyncedBlockEntity implements IFakeTrackPad {

    @Unique private float cwt$padMinY = 0.0f;
    @Unique private float cwt$padMaxY = 0.125f;
    @Unique private boolean cwt$padSet = false;

    public FakeTrackBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void cwt$setPadRange(float padMinY, float padMaxY) {
        this.cwt$padMinY = padMinY;
        this.cwt$padMaxY = padMaxY;
        this.cwt$padSet = true;
    }

    @Override
    public float cwt$getPadMinY() {
        return this.cwt$padMinY;
    }

    @Override
    public float cwt$getPadMaxY() {
        return this.cwt$padMaxY;
    }

    @Override
    public boolean cwt$isPadSet() {
        return this.cwt$padSet;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.cwt$padSet) {
            tag.putFloat("CwtPadMinY", this.cwt$padMinY);
            tag.putFloat("CwtPadMaxY", this.cwt$padMaxY);
            tag.putBoolean("CwtPadSet", true);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.getBoolean("CwtPadSet")) {
            this.cwt$padMinY = tag.getFloat("CwtPadMinY");
            this.cwt$padMaxY = tag.getFloat("CwtPadMaxY");
            this.cwt$padSet = true;
        }
    }
}
