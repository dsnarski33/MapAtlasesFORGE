package pepjebs.map_atlases.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.map_atlases.MapAtlasesMod;
import pepjebs.map_atlases.item.MapAtlasItem;

@Mixin(LecternBlock.class)
public class LecternBlockMixin extends Block
{
    public LecternBlockMixin(BlockBehaviour.Properties settings) { super(settings); }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    public void lecternAtlasConstructorMixin(Properties settings, CallbackInfo ci) {
        this.registerDefaultState(this.defaultBlockState().setValue(MapAtlasItem.HAS_ATLAS, false));
    }

    @Inject(method = "createBlockStateDefinition", at = @At(value = "RETURN"))
    public void injectAtlasProperty(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(MapAtlasItem.HAS_ATLAS);
    }

    @Inject(method = "openScreen", at = @At(value = "HEAD"), cancellable = true)
    public void injectAtlasScreen(Level world, BlockPos pos, Player player, CallbackInfo ci) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.hasProperty(MapAtlasItem.HAS_ATLAS) && blockState.getValue(MapAtlasItem.HAS_ATLAS)) {
            MapAtlasesMod.MAP_ATLAS.openHandledAtlasScreen(world, player);
            ci.cancel();
        }
    }

    @Inject(method = "use", at = @At(value = "HEAD"), cancellable = true)
    public void injectAtlasRemoval(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> ci) {
        if (state.hasProperty(MapAtlasItem.HAS_ATLAS) && state.getValue(MapAtlasItem.HAS_ATLAS) && player.getPose() == Pose.CROUCHING) {
            LecternBlockEntity lbe = (LecternBlockEntity) world.getBlockEntity(pos);
            if (lbe == null) return;
            ItemStack atlas = lbe.getBook();
            if (!player.getInventory().add(atlas))
                player.drop(atlas, false);
            LecternBlock.resetBookState( world, pos, state.setValue(MapAtlasItem.HAS_ATLAS, false), false);
            ci.setReturnValue(InteractionResult.sidedSuccess(world.isClientSide));
        }
    }
}
