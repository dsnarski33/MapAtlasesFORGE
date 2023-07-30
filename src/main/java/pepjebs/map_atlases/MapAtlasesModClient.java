package pepjebs.map_atlases;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
import pepjebs.map_atlases.client.ui.MapAtlasesHUD;
import pepjebs.map_atlases.networking.MapAtlasesOpenGUIC2SPacket;
import pepjebs.map_atlases.networking.ModPacketHandler;
import pepjebs.map_atlases.screen.MapAtlasesAtlasOverviewScreen;
import pepjebs.map_atlases.utils.MapAtlasesAccessUtils;

@Mod.EventBusSubscriber(modid = MapAtlasesMod.MOD_ID, value = {Dist.CLIENT}, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MapAtlasesModClient
{
    public static MapAtlasesHUD mapAtlasesAtlasHUD;
    public static KeyMapping displayMapGUIBinding;

    private static final ThreadLocal<Float> worldMapZoomLevel = new ThreadLocal<>();
    public static float getWorldMapZoomLevel() { return worldMapZoomLevel.get() != null ? worldMapZoomLevel.get() : 1.0f; }
    public static void setWorldMapZoomLevel(float i) { worldMapZoomLevel.set(i);}

    @SubscribeEvent
    public static void onHUD(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL)
            mapAtlasesAtlasHUD.render(event.getMatrixStack());
    }

    public static void setup(FMLClientSetupEvent event)
    {
        // Screens
        event.enqueueWork(() -> {
            MenuScreens.register(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER, MapAtlasesAtlasOverviewScreen::new);
            displayMapGUIBinding = new KeyMapping("key.map_atlases.open_minimap", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.map_atlases.minimap");
            ClientRegistry.registerKeyBinding(displayMapGUIBinding);
            ItemProperties.register(MapAtlasesMod.MAP_ATLAS, new ResourceLocation("atlas"), MapAtlasesModClient::getModelForAtlas);
            mapAtlasesAtlasHUD = new MapAtlasesHUD();
        });
    }

    public static void tick(TickEvent.ClientTickEvent event)
    {
        Level world = Minecraft.getInstance().level;
        if (event.phase == TickEvent.Phase.END && world != null && !Minecraft.getInstance().isPaused())
        {
            Minecraft client = Minecraft.getInstance();
            while (displayMapGUIBinding.consumeClick()) {
                if (client.level == null || client.player == null) break;
                ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
                if (atlas.isEmpty()) break;
                ModPacketHandler.INSTANCE.sendToServer(new MapAtlasesOpenGUIC2SPacket(atlas));
            }
        }
    }

    public static float getModelForAtlas(ItemStack _stack, ClientLevel world, LivingEntity entity, int _seed) {
        // Using ClientWorld will render default Atlas in inventories
        Level queryWorld = world;
        if (queryWorld == null && entity != null)
            queryWorld = entity.getLevel();
        if (queryWorld == null)
            return 0.0f;
        if (queryWorld.dimension() == Level.OVERWORLD) return 0.1f;
        if (queryWorld.dimension() == Level.NETHER) return 0.2f;
        if (queryWorld.dimension() == Level.END) return 0.3f;
        return 0.0f;
    }

    public static void handleAtlasMap(String mapId, MapItemSavedData mapState) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null)
            return;
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(client.player);
        mapState.tickCarriedBy(client.player, atlas);
        mapState.getHoldingPlayer(client.player);
        client.player.getLevel().setMapData(mapId, mapState);
    }
}