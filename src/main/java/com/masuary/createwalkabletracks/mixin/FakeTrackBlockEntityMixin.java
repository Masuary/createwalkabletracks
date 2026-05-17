package com.masuary.createwalkabletracks.mixin;

import com.masuary.createwalkabletracks.IFakeTrackPad;
import com.simibubi.create.content.trains.track.FakeTrackBlockEntity;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;

@Mixin(FakeTrackBlockEntity.class)
public abstract class FakeTrackBlockEntityMixin extends SyncedBlockEntity implements IFakeTrackPad {

    @Unique private static final float CWT$DEFAULT_PAD_MIN_Y = 0.0f;
    @Unique private static final float CWT$DEFAULT_PAD_MAX_Y = 0.125f;

    @Unique private final Map<Long, float[]> cwt$ownerContributions = new HashMap<>();
    @Unique private float cwt$aggregatePadMinY = CWT$DEFAULT_PAD_MIN_Y;
    @Unique private float cwt$aggregatePadMaxY = CWT$DEFAULT_PAD_MAX_Y;
    @Unique private boolean cwt$padSet = false;

    public FakeTrackBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean cwt$setOwnerPadRange(BlockPos owner, float padMinY, float padMaxY) {
        long key = owner.asLong();
        float[] existing = cwt$ownerContributions.get(key);
        if (existing != null && existing[0] == padMinY && existing[1] == padMaxY) {
            return false;
        }
        cwt$ownerContributions.put(key, new float[]{padMinY, padMaxY});
        cwt$recomputeAggregate();
        return true;
    }

    @Override
    public boolean cwt$removeOwner(BlockPos owner) {
        long key = owner.asLong();
        if (cwt$ownerContributions.remove(key) != null) {
            cwt$recomputeAggregate();
        }
        return cwt$ownerContributions.isEmpty();
    }

    @Override
    public float cwt$getPadMinY() {
        return this.cwt$aggregatePadMinY;
    }

    @Override
    public float cwt$getPadMaxY() {
        return this.cwt$aggregatePadMaxY;
    }

    @Override
    public boolean cwt$isPadSet() {
        return this.cwt$padSet;
    }

    @Unique
    private void cwt$recomputeAggregate() {
        if (cwt$ownerContributions.isEmpty()) {
            this.cwt$aggregatePadMinY = CWT$DEFAULT_PAD_MIN_Y;
            this.cwt$aggregatePadMaxY = CWT$DEFAULT_PAD_MAX_Y;
            this.cwt$padSet = false;
            return;
        }
        float newMin = Float.POSITIVE_INFINITY;
        float newMax = Float.NEGATIVE_INFINITY;
        for (float[] range : cwt$ownerContributions.values()) {
            if (range[0] < newMin) newMin = range[0];
            if (range[1] > newMax) newMax = range[1];
        }
        this.cwt$aggregatePadMinY = newMin;
        this.cwt$aggregatePadMaxY = newMax;
        this.cwt$padSet = true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (cwt$ownerContributions.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        for (Map.Entry<Long, float[]> entry : cwt$ownerContributions.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("Pos", entry.getKey());
            entryTag.putFloat("Min", entry.getValue()[0]);
            entryTag.putFloat("Max", entry.getValue()[1]);
            list.add(entryTag);
        }
        tag.put("CwtOwners", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cwt$ownerContributions.clear();
        if (tag.contains("CwtOwners", Tag.TAG_LIST)) {
            ListTag list = tag.getList("CwtOwners", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entryTag = list.getCompound(i);
                long key = entryTag.getLong("Pos");
                float minY = entryTag.getFloat("Min");
                float maxY = entryTag.getFloat("Max");
                cwt$ownerContributions.put(key, new float[]{minY, maxY});
            }
        }
        cwt$recomputeAggregate();
    }
}
