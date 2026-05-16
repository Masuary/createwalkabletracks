package com.masuary.createwalkabletracks.mixin;

import com.masuary.createwalkabletracks.IFakeTrackPad;
import com.simibubi.create.content.trains.track.FakeTrackBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FakeTrackBlock.class)
public abstract class FakeTrackBlockMixin extends Block {

    @SuppressWarnings("unused")
    public FakeTrackBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IFakeTrackPad pad && pad.cwt$isPadSet()) {
            float minY = pad.cwt$getPadMinY();
            float maxY = pad.cwt$getPadMaxY();
            if (maxY > minY) {
                return Shapes.box(0.0, minY, 0.0, 1.0, maxY, 1.0);
            }
        }
        return Shapes.empty();
    }

    @Inject(method = "getAiPathNodeType", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void cwt$makeAiPathable(BlockState state, BlockGetter level, BlockPos pos, Mob mob, CallbackInfoReturnable<BlockPathTypes> cir) {
        cir.setReturnValue(BlockPathTypes.OPEN);
    }
}
