package com.masuary.createwalkabletracks.client;

import com.masuary.createwalkabletracks.ClientConfig;
import com.masuary.createwalkabletracks.CreateWalkableTracks;
import com.masuary.createwalkabletracks.IFakeTrackPad;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = CreateWalkableTracks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrackVisualizer {

    private static boolean enabled = false;
    private static KeyMapping toggleKey;

    private TrackVisualizer() {
    }

    public static void setToggleKey(KeyMapping key) {
        toggleKey = key;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || toggleKey == null) {
            return;
        }
        while (toggleKey.consumeClick()) {
            enabled = !enabled;
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(
                        new TextComponent("Track visualizer " + (enabled ? "enabled" : "disabled")),
                        true);
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void onRenderLevelLast(RenderLevelLastEvent event) {
        if (!enabled) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        int renderRangeBlocks = ClientConfig.VISUALIZER_RANGE_BLOCKS.get();
        int bezierSampleCount = ClientConfig.VISUALIZER_BEZIER_SAMPLE_COUNT.get();

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        BlockPos playerPos = player.blockPosition();
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        int chunkRange = (renderRangeBlocks >> 4) + 1;

        for (int dx = -chunkRange; dx <= chunkRange; dx++) {
            for (int dz = -chunkRange; dz <= chunkRange; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                renderChunkBlockEntities(poseStack, lineConsumer, chunk, playerPos, renderRangeBlocks, bezierSampleCount);
            }
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }

    private static void renderChunkBlockEntities(PoseStack poseStack, VertexConsumer lineConsumer, LevelChunk chunk, BlockPos playerPos, int renderRangeBlocks, int bezierSampleCount) {
        double rangeSquared = (double) renderRangeBlocks * (double) renderRangeBlocks;
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockEntity blockEntity = entry.getValue();
            BlockPos pos = entry.getKey();
            if (pos.distSqr(playerPos) > rangeSquared) {
                continue;
            }

            if (blockEntity instanceof IFakeTrackPad pad) {
                renderPadOutline(poseStack, lineConsumer, pos, pad);
            } else if (blockEntity instanceof TrackBlockEntity trackBlockEntity) {
                renderBezierCurves(poseStack, lineConsumer, trackBlockEntity, bezierSampleCount);
            }
        }
    }

    private static void renderPadOutline(PoseStack poseStack, VertexConsumer lineConsumer, BlockPos pos, IFakeTrackPad pad) {
        boolean padSet = pad.cwt$isPadSet();
        float minY = padSet ? pad.cwt$getPadMinY() : 0.0f;
        float maxY = padSet ? pad.cwt$getPadMaxY() : 0.125f;
        float red = padSet ? 0.2f : 1.0f;
        float green = padSet ? 1.0f : 0.6f;
        float blue = padSet ? 0.3f : 0.1f;

        AABB box = new AABB(
                pos.getX(), pos.getY() + minY, pos.getZ(),
                pos.getX() + 1.0, pos.getY() + maxY, pos.getZ() + 1.0);
        LevelRenderer.renderLineBox(poseStack, lineConsumer, box, red, green, blue, 1.0f);
    }

    private static void renderBezierCurves(PoseStack poseStack, VertexConsumer lineConsumer, TrackBlockEntity trackBlockEntity, int bezierSampleCount) {
        Map<BlockPos, BezierConnection> connections = trackBlockEntity.getConnections();
        if (connections.isEmpty()) {
            return;
        }
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        for (BezierConnection bezier : connections.values()) {
            if (!bezier.isPrimary()) {
                continue;
            }
            renderSingleBezier(lineConsumer, pose, normal, bezier, bezierSampleCount);
        }
    }

    private static void renderSingleBezier(VertexConsumer lineConsumer, Matrix4f pose, Matrix3f normal, BezierConnection bezier, int bezierSampleCount) {
        Vec3 end1 = bezier.starts.getFirst().add(0.0, 0.1875, 0.0);
        Vec3 end2 = bezier.starts.getSecond().add(0.0, 0.1875, 0.0);
        double handleLength = bezier.getHandleLength();
        Vec3 finish1 = bezier.axes.getFirst().scale(handleLength).add(end1);
        Vec3 finish2 = bezier.axes.getSecond().scale(handleLength).add(end2);

        Vec3 previous = VecHelper.bezier(end1, end2, finish1, finish2, 0.0f);
        for (int i = 1; i <= bezierSampleCount; i++) {
            float t = (float) i / (float) bezierSampleCount;
            Vec3 current = VecHelper.bezier(end1, end2, finish1, finish2, t);
            lineConsumer.vertex(pose, (float) previous.x, (float) previous.y, (float) previous.z)
                    .color(0.3f, 0.6f, 1.0f, 1.0f)
                    .normal(normal, 0.0f, 1.0f, 0.0f)
                    .endVertex();
            lineConsumer.vertex(pose, (float) current.x, (float) current.y, (float) current.z)
                    .color(0.3f, 0.6f, 1.0f, 1.0f)
                    .normal(normal, 0.0f, 1.0f, 0.0f)
                    .endVertex();
            previous = current;
        }
    }
}
