package com.masuary.createwalkabletracks.mixin;

import com.masuary.createwalkabletracks.IFakeTrackPad;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.FakeTrackBlock;
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
            if (derivative.lengthSqr() < 1.0e-12) {
                continue;
            }
            derivative = derivative.normalize();
            Vec3 faceNormal = faceNormal1.equals(faceNormal2) ? faceNormal1 : VecHelper.slerp(t, faceNormal1, faceNormal2);
            Vec3 lateralAxis = faceNormal.cross(derivative);
            if (lateralAxis.lengthSqr() < 1.0e-9) {
                continue;
            }
            lateralAxis = lateralAxis.normalize();
            Vec3 below = result.add(faceNormal.scale(-0.25));

            for (double offset : CWT$LATERAL_OFFSETS) {
                double lateralX = result.x + lateralAxis.x * offset;
                double lateralY = result.y + lateralAxis.y * offset;
                double lateralZ = result.z + lateralAxis.z * offset;
                double lateralBelowY = below.y + lateralAxis.y * offset;
                int blockX = Mth.floor(lateralX);
                int blockZ = Mth.floor(lateralZ);
                long key = cwt$packKey(blockX, blockZ);

                double[] data = bins.computeIfAbsent(key, k -> new double[]{Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE});
                if (lateralY < data[0]) data[0] = lateralY;
                if (lateralY > data[1]) data[1] = lateralY;
                if (lateralBelowY < data[2]) data[2] = lateralBelowY;
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
                cwt$handleRemoval(level, worldPos, tePosition);
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
            if (!(be instanceof IFakeTrackPad pad)) {
                continue;
            }

            if (pad.cwt$setOwnerPadRange(tePosition, padMinY, padMaxY)) {
                be.setChanged();
                if (be instanceof SyncedBlockEntity syncedBe) {
                    syncedBe.sendData();
                }
            }
        }
    }

    private static void cwt$handleRemoval(Level level, BlockPos worldPos, BlockPos ownerPos) {
        BlockState existing = level.getBlockState(worldPos);
        if (!(existing.getBlock() instanceof FakeTrackBlock)) {
            return;
        }
        BlockEntity be = level.getBlockEntity(worldPos);
        if (!(be instanceof IFakeTrackPad pad)) {
            return;
        }
        boolean noOwnersLeft = pad.cwt$removeOwner(ownerPos);
        if (noOwnersLeft) {
            level.removeBlock(worldPos, false);
            return;
        }
        be.setChanged();
        if (be instanceof SyncedBlockEntity syncedBe) {
            syncedBe.sendData();
        }
    }

    private static Block cwt$cachedFakeTrackBlock;

    private static final ResourceLocation CWT$FAKE_TRACK_ID = ResourceLocation.tryParse("create:fake_track");

    private static Block cwt$resolveFakeTrackBlock() {
        Block cached = cwt$cachedFakeTrackBlock;
        if (cached != null) {
            return cached;
        }
        if (CWT$FAKE_TRACK_ID == null) {
            return null;
        }
        Block resolved = ForgeRegistries.BLOCKS.getValue(CWT$FAKE_TRACK_ID);
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
