package pepjebs.map_atlases.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.Util;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import pepjebs.map_atlases.MapAtlasesMod;
import pepjebs.map_atlases.MapAtlasesModClient;
import pepjebs.map_atlases.item.MapAtlasItem;
import pepjebs.map_atlases.utils.MapAtlasesAccessUtils;

import java.util.Arrays;

@OnlyIn(Dist.CLIENT)
public class MapAtlasesHUD extends GuiComponent
{
    public static final ResourceLocation MAP_BACKGROUND = new ResourceLocation("map_atlases:textures/gui/hud/map_background.png");
    public static final ResourceLocation MAP_FOREGROUND = new ResourceLocation("map_atlases:textures/gui/hud/map_foreground.png");
    private static Minecraft client;
    private static MapRenderer mapRenderer;
    private static String currentMapId = "";

    public MapAtlasesHUD() {
        client = Minecraft.getInstance();
        mapRenderer = client.gameRenderer.getMapRenderer();
    }

    public void render(PoseStack context) {
        if (shouldDraw(client))
            renderMapHUD(context);
    }

    private boolean shouldDraw(Minecraft client) {
        if (client.player == null) return false;
        // Check config disable
        if (MapAtlasesMod.config.options != null && !MapAtlasesMod.config.options.drawMiniMapHUD()) return false;
        // Check F3 menu displayed
        if (client.options.renderDebug) return false;
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
        // Check the player for an Atlas
        if (atlas.isEmpty()) return false;
        // Check the client has an active map id
        if (MapAtlasesMod.currentMapStateId == null) return false;
        // Check the active map id is in the active atlas
        return atlas.getTag() != null && atlas.getTag().contains(MapAtlasItem.MAP_LIST_NBT) &&
                Arrays.stream(atlas.getTag().getIntArray(MapAtlasItem.MAP_LIST_NBT)).anyMatch(i ->
                                i == MapAtlasesAccessUtils.getMapIntFromString(MapAtlasesMod.currentMapStateId));
    }

    private void renderMapHUD(PoseStack matrices) {
        // Handle early returns
        if (client.level == null || client.player == null)
            return;
        String curMapId = MapAtlasesMod.currentMapStateId;
        MapItemSavedData state = client.level.getMapData(MapAtlasesMod.currentMapStateId);
        if (state == null) return;
        // Update client current map id
        if (currentMapId == null || curMapId.compareTo(currentMapId) != 0) {
            if (currentMapId != null && currentMapId.compareTo("") != 0) {
                client.level.playLocalSound(client.player.getX(), client.player.getY(), client.player.getZ(),
                        MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT, SoundSource.PLAYERS,
                        MapAtlasesMod.config.options.soundScalar(), 1.0F, false);
            }
            currentMapId = curMapId;
        }
        // Set zoom-level for map icons
        MapAtlasesModClient.setWorldMapZoomLevel(MapAtlasesMod.config.options.miniMapDecorationScale());
        // Draw map background
        int mapBgScaledSize = (int)Math.floor(.2 * client.getWindow().getGuiScaledHeight());
        if (MapAtlasesMod.config.options != null)
            mapBgScaledSize = (int) Math.floor(MapAtlasesMod.config.options.forceMiniMapScaling() / 100.0 * client.getWindow().getGuiScaledHeight());

        double drawnMapBufferSize = mapBgScaledSize / 20.0;
        int mapDataScaledSize = (int) ((mapBgScaledSize - (2 * drawnMapBufferSize)));
        float mapDataScale = mapDataScaledSize / 128.0f;
        String anchorLocation = "UpperLeft";
        if (MapAtlasesMod.config.options != null)
            anchorLocation = MapAtlasesMod.config.options.miniMapAnchoring();
        int x = anchorLocation.contains("Left") ? 0 : client.getWindow().getGuiScaledWidth() - mapBgScaledSize;
        int y = anchorLocation.contains("Lower") ? client.getWindow().getGuiScaledHeight() - mapBgScaledSize : 0;
        if (MapAtlasesMod.config.options != null) {
            x += MapAtlasesMod.config.options.miniMapHorizontalOffset();
            y += MapAtlasesMod.config.options.miniMapVerticalOffset();
        }
        if (anchorLocation.contentEquals("UpperRight")) {
            boolean hasBeneficial = client.player.getActiveEffects().stream().anyMatch(p -> p.getEffect().isBeneficial());
            boolean hasNegative = client.player.getActiveEffects().stream().anyMatch(p -> !p.getEffect().isBeneficial());
            int offsetForEffects = 26;

            if (MapAtlasesMod.config.options != null)
                offsetForEffects = MapAtlasesMod.config.options.activePotionVerticalOffset();

            if (hasNegative && y < 2 * offsetForEffects)
                y += (2  * offsetForEffects - y);
            else if (hasBeneficial && y < offsetForEffects)
                y += (offsetForEffects - y);
        }
        RenderSystem.setShaderTexture(0, MAP_BACKGROUND);
        Minecraft.getInstance().getTextureManager().bindForSetup(MAP_BACKGROUND);
        Gui.blit(matrices,x,y,0,0,mapBgScaledSize,mapBgScaledSize,mapBgScaledSize,mapBgScaledSize);

        // Draw map data
        MultiBufferSource.BufferSource vcp;
        vcp = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        matrices.pushPose();
        matrices.translate(x + drawnMapBufferSize, y + drawnMapBufferSize, 0.0);
        matrices.scale(mapDataScale, mapDataScale, -1);
        mapRenderer.render(matrices, vcp, MapAtlasesAccessUtils.getMapIntFromString(curMapId), state, false, Integer.parseInt("F000F0", 16));
        vcp.endBatch();
        matrices.popPose();
        RenderSystem.setShaderTexture(0, MAP_FOREGROUND);
        Minecraft.getInstance().getTextureManager().bindForSetup(MAP_BACKGROUND);
        Gui.blit(matrices,x,y,0,0,mapBgScaledSize,mapBgScaledSize,mapBgScaledSize,mapBgScaledSize);

        // Draw text data
        float textScaling = MapAtlasesMod.config.options.minimapCoordsAndBiomeScale();
        int textHeightOffset = mapBgScaledSize + 4;
        int textWidthOffset = mapBgScaledSize;
        if (anchorLocation.contains("Lower"))
            textHeightOffset = (int) (-24 * textScaling);
        if (MapAtlasesMod.config.options.drawMinimapCoords()) {
            drawMapTextCoords(matrices, x, y, textWidthOffset, textHeightOffset, textScaling, new BlockPos(new Vec3i(towardsZero(client.player.position().x), towardsZero(client.player.position().y), towardsZero(client.player.position().z))));
            textHeightOffset += (12 * textScaling);
        }
        if (MapAtlasesMod.config.options.drawMinimapBiome())
            drawMapTextBiome(matrices, x, y, textWidthOffset, textHeightOffset, textScaling, client.player.blockPosition(), client.level);
    }

    private static int towardsZero(double d) {
        if (d < 0.0)
            return -1 * (int) Math.floor(-1 * d);
        return (int) Math.floor(d);
    }

    public static void drawMapTextCoords(PoseStack context, int x, int y, int originOffsetWidth, int originOffsetHeight, float textScaling, BlockPos blockPos) {
        String coordsToDisplay = blockPos.toShortString();
        drawScaledText(context, x, y, coordsToDisplay, textScaling, originOffsetWidth, originOffsetHeight);
    }

    public static void drawMapTextBiome(PoseStack context, int x, int y, int originOffsetWidth, int originOffsetHeight, float textScaling, BlockPos blockPos, Level world) {
        String biomeToDisplay = getBiomeStringToDisplay(world, blockPos);
        drawScaledText(context, x, y, biomeToDisplay, textScaling, originOffsetWidth, originOffsetHeight);
    }

    public static void drawScaledText(PoseStack matrices, int x, int y, String text, float textScaling, int originOffsetWidth, int originOffsetHeight) {
        float textWidth = client.font.width(text) * textScaling;
        float textX = (float) (x + (originOffsetWidth / 2.0) - (textWidth / 2.0));
        float textY = y + originOffsetHeight;
        if (textX + textWidth >= client.getWindow().getGuiScaledWidth())
            textX = client.getWindow().getGuiScaledWidth() - textWidth;
        matrices.pushPose();
        matrices.translate(textX, textY, 5);
        matrices.scale(textScaling, textScaling, 1);
        Gui.drawString(matrices, client.font, text, 1, 1, 0x595959);
        Gui.drawString(matrices, client.font, text, 0, 0, 0xE0E0E0);
        matrices.popPose();
    }

    private static String getBiomeStringToDisplay(Level world, BlockPos blockPos) {
        if (world == null || world.getBiome(blockPos).unwrapKey().isEmpty())
            return "";
        ResourceKey<Biome> biomeKey = world.getBiome(blockPos).unwrapKey().get();
        return I18n.get(Util.makeDescriptionId("biome", biomeKey.location()));
    }
}