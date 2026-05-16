package com.masuary.createwalkabletracks.mixin;

import com.masuary.createwalkabletracks.IFakeTrackPad;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.FakeTrackBlock;
import com.simibubi.create.content.trains.track.FakeTrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(TrackBlockEntity.class)
public abstract class TrackBlockEntityMixin {

    private static final double[] CWT$LATERAL_OFFSETS = {-0.75, -0.5, -0.25, 0.0, 0.25, 0.5, 0.75};
    private static final double CWT$PAD_HALF_THICKNESS = 0.0625;

    @Inject(method = "manageFakeTracksAlong", at = @At("TAIL"), remap = false, require = 1)
    private void cwt$manageExtendedFakeTracks(BezierConnection bc, boolean remove, CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockPos tePosition = bc.tePositions.getFirst();
        Vec3 end1 = bc.starts.getFirst().subtract(Vec3.atLowerCornerOf(tePosition)).add(0.0, 0.1875, 0.0);
        Vec3 end2 = bc.starts.getSecond().subtract(Vec3.atLowerCornerOf(tePosition)).add(0.0, 0.1875, 0.0);
        Vec3 axis1 = bc.axes.getFirst();
        Vec3 axis2 = bc.axes.getSecond();
        Vec3 faceNormal1 = bc.normals.getFirst();
        Vec3 faceNormal2 = bc.normals.getSecond();
        double handleLength = bc.getHandleLength();
        Vec3 finish1 = axis1.scale(handleLength).add(end1);
        Vec3 finish2 = axis2.scale(handleLength).add(end2);

        int sampleCount = Math.max(bc.getSegmentCount() * 8, 32);

        Map<Long, double[]> bins = new HashMap<>();

        for (int i = 0; i <= sampleCount; i++) {
            float t = (float) i / (float) sampleCount;
            Vec3 result = VecHelper.bezier(end1, end2, finish1, finish2, t);
            Vec3 derivative = VecHelper.bezierDerivative(end1, end2, finish1, finish2, t);
            Vec3 faceNormal = faceNormal1.equals(faceNormal2) ? faceNormal1 : VecHelper.slerp(t, faceNormal1, faceNormal2);
            Vec3 below = result.add(faceNormal.scale(-0.25));

            double tangentX = derivative.x;
            double tangentZ = derivative.z;
            double tangentLen = Math.sqrt(tangentX * tangentX + tangentZ * tangentZ);
            if (tangentLen < 1.0e-6) {
                continue;
            }
            double perpX = -tangentZ / tangentLen;
            double perpZ = tangentX / tangentLen;

            for (double offset : CWT$LATERAL_OFFSETS) {
                double lateralX = result.x + perpX * offset;
                double lateralZ = result.z + perpZ * offset;
                int blockX = Mth.floor(lateralX);
                int blockZ = Mth.floor(lateralZ);
                long key = cwt$packKey(blockX, blockZ);

                double[] data = bins.computeIfAbsent(key, k -> new double[]{Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE});
                if (result.y < data[0]) data[0] = result.y;
                if (result.y > data[1]) data[1] = result.y;
                if (below.y < data[2]) data[2] = below.y;
            }
        }

        for (Map.Entry<Long, double[]> entry : bins.entrySet()) {
            long key = entry.getKey();
            int blockX = cwt$unpackX(key);
            int blockZ = cwt$unpackZ(key);
            double[] data = entry.getValue();
            int fakeTrackTeY = Mth.floor(data[2]) + 1;
            BlockPos worldPos = new BlockPos(blockX, fakeTrackTeY, blockZ).offset(tePosition);

            if (remove) {
                BlockState existing = level.getBlockState(worldPos);
                if (existing.getBlock() instanceof FakeTrackBlock) {
                    level.removeBlock(worldPos, false);
                }
                continue;
            }

            BlockState existing = level.getBlockState(worldPos);
            if (!(existing.getBlock() instanceof FakeTrackBlock)) {
                if (!existing.getMaterial().isReplaceable()) {
                    continue;
                }
                FluidState fluidState = existing.getFluidState();
                if (!fluidState.isEmpty() && !fluidState.is(Fluids.WATER)) {
                    continue;
                }
                Block fakeTrackBlock = cwt$resolveFakeTrackBlock();
                if (fakeTrackBlock == null) {
                    continue;
                }
                level.setBlock(worldPos,
                        ProperWaterloggedBlock.withWater(level, fakeTrackBlock.defaultBlockState(), worldPos),
                        3);
            }

            FakeTrackBlock.keepAlive(level, worldPos);

            float padMinY = (float) (data[0] - fakeTrackTeY - CWT$PAD_HALF_THICKNESS);
            float padMaxY = (float) (data[1] - fakeTrackTeY + CWT$PAD_HALF_THICKNESS);

            BlockEntity be = level.getBlockEntity(worldPos);
            if (!(be instanceof FakeTrackBlockEntity) || !(be instanceof IFakeTrackPad pad)) {
                continue;
            }

            if (!pad.cwt$isPadSet() || pad.cwt$getPadMinY() != padMinY || pad.cwt$getPadMaxY() != padMaxY) {
                pad.cwt$setPadRange(padMinY, padMaxY);
                be.setChanged();
                if (be instanceof SyncedBlockEntity syncedBe) {
                    syncedBe.sendData();
                }
            }
        }
    }

    private static Block cwt$cachedFakeTrackBlock;

    private static Block cwt$resolveFakeTrackBlock() {
        Block cached = cwt$cachedFakeTrackBlock;
        if (cached != null) {
            return cached;
        }
        Block resolved = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("create:fake_track"));
        if (resolved instanceof FakeTrackBlock) {
            cwt$cachedFakeTrackBlock = resolved;
            return resolved;
        }
        return null;
    }

    private static long cwt$packKey(int blockX, int blockZ) {
        return ((long) blockZ) << 32 | ((long) blockX & 0xFFFFFFFFL);
    }

    private static int cwt$unpackX(long key) {
        return (int) key;
    }

    private static int cwt$unpackZ(long key) {
        return (int) (key >> 32);
    }
}
