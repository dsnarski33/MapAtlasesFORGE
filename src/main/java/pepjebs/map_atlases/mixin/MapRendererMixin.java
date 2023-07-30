package pepjebs.map_atlases.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.MapRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pepjebs.map_atlases.MapAtlasesModClient;

@Mixin(value = MapRenderer.MapInstance.class)
public class MapRendererMixin {
    @Redirect(method = "draw", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    private void scaleProxy(PoseStack matrices, float x, float y, float z) {
        float multiplier = MapAtlasesModClient.getWorldMapZoomLevel();
        matrices.scale(x*multiplier, y*multiplier, z);
    }
}
