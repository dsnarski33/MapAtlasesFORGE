package pepjebs.map_atlases.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pepjebs.map_atlases.MapAtlasesMod;
import pepjebs.map_atlases.item.MapAtlasItem;

@Mixin(LecternRenderer.class)
public class MapAtlasesLecternBlockEntityRenderer
{
    private static final Material OVERWORLD_TEXTURE = new Material(InventoryMenu.BLOCK_ATLAS, MapAtlasesMod.LECTERN_OVERWORLD_ID);
    private static final Material NETHER_TEXTURE = new Material(InventoryMenu.BLOCK_ATLAS, MapAtlasesMod.LECTERN_NETHER_ID);
    private static final Material END_TEXTURE = new Material(InventoryMenu.BLOCK_ATLAS, MapAtlasesMod.LECTERN_END_ID);
    private static final Material OTHER_TEXTURE = new Material(InventoryMenu.BLOCK_ATLAS, MapAtlasesMod.LECTERN_OTHER_ID);

    @Redirect(method = "render(Lnet/minecraft/world/level/block/entity/LecternBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/BookModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void renderMapAtlasInLectern(BookModel model, PoseStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha,
                                         LecternBlockEntity lecternBlockEntity, float f, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, int j) {
        BlockState blockState = lecternBlockEntity.getBlockState();
        VertexConsumer vertexConsumer;
        if (blockState.getValue(LecternBlock.HAS_BOOK) && blockState.hasProperty(MapAtlasItem.HAS_ATLAS) && blockState.getValue(MapAtlasItem.HAS_ATLAS)) {
            if(lecternBlockEntity.getLevel() == null)
                vertexConsumer = OTHER_TEXTURE.buffer(vertexConsumerProvider, RenderType::entitySolid);
            else if(lecternBlockEntity.getLevel().dimension() == Level.OVERWORLD)
                vertexConsumer = OVERWORLD_TEXTURE.buffer(vertexConsumerProvider, RenderType::entitySolid);
            else if(lecternBlockEntity.getLevel().dimension() == Level.NETHER)
                vertexConsumer = NETHER_TEXTURE.buffer(vertexConsumerProvider, RenderType::entitySolid);
            else if(lecternBlockEntity.getLevel().dimension() == Level.END)
                vertexConsumer = END_TEXTURE.buffer(vertexConsumerProvider, RenderType::entitySolid);
            else
                vertexConsumer = OTHER_TEXTURE.buffer(vertexConsumerProvider, RenderType::entitySolid);
        } else {
            vertexConsumer = vertices;
        }
        model.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
    }
}