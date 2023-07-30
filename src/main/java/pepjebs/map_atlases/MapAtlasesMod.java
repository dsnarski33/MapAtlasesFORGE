package pepjebs.map_atlases;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import pepjebs.map_atlases.item.DummyFilledMap;
import pepjebs.map_atlases.item.MapAtlasItem;
import pepjebs.map_atlases.lifecycle.MapAtlasesServerLifecycleEvents;
import pepjebs.map_atlases.recipe.MapAtlasCreateRecipe;
import pepjebs.map_atlases.recipe.MapAtlasesAddRecipe;
import pepjebs.map_atlases.recipe.MapAtlasesCutExistingRecipe;
import pepjebs.map_atlases.screen.MapAtlasesAtlasOverviewScreenHandler;

@Mod(MapAtlasesMod.MOD_ID)
public final class MapAtlasesMod
{
    public static final String MOD_ID = "map_atlases";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static MapAtlasesConfig config;
    public static MapAtlasesMod INSTANCE;

    public static MapAtlasItem MAP_ATLAS;

    public static SimpleRecipeSerializer<MapAtlasCreateRecipe> MAP_ATLAS_CREATE_RECIPE;
    public static SimpleRecipeSerializer<MapAtlasesAddRecipe> MAP_ATLAS_ADD_RECIPE;
    public static SimpleRecipeSerializer<MapAtlasesCutExistingRecipe> MAP_ATLAS_CUT_RECIPE;

    public static MenuType<MapAtlasesAtlasOverviewScreenHandler> ATLAS_OVERVIEW_HANDLER;

    private static final ResourceLocation ATLAS_OPEN_SOUND_ID = new ResourceLocation(MOD_ID, "atlas_open");
    public static SoundEvent ATLAS_OPEN_SOUND_EVENT = new SoundEvent(ATLAS_OPEN_SOUND_ID);
    private static final ResourceLocation ATLAS_PAGE_TURN_SOUND_ID = new ResourceLocation(MOD_ID, "atlas_page_turn");
    public static SoundEvent ATLAS_PAGE_TURN_SOUND_EVENT = new SoundEvent(ATLAS_PAGE_TURN_SOUND_ID);
    private static final ResourceLocation ATLAS_CREATE_MAP_SOUND_ID = new ResourceLocation(MOD_ID, "atlas_create_map");
    public static SoundEvent ATLAS_CREATE_MAP_SOUND_EVENT = new SoundEvent(ATLAS_CREATE_MAP_SOUND_ID);
    public static final ResourceLocation STICKY_ITEMS_ID = new ResourceLocation(MapAtlasesMod.MOD_ID, "sticky_crafting_items");

    public static String currentMapStateId = null;
    public static final ResourceLocation LECTERN_OVERWORLD_ID = new ResourceLocation(MapAtlasesMod.MOD_ID, "entity/lectern_atlas");
    public static final ResourceLocation LECTERN_NETHER_ID = new ResourceLocation(MapAtlasesMod.MOD_ID, "entity/lectern_atlas_nether");
    public static final ResourceLocation LECTERN_END_ID = new ResourceLocation(MapAtlasesMod.MOD_ID, "entity/lectern_atlas_end");
    public static final ResourceLocation LECTERN_OTHER_ID = new ResourceLocation(MapAtlasesMod.MOD_ID, "entity/lectern_atlas_unknown");

    public MapAtlasesMod()
    {
        LOGGER.info("Initializing MapAtlasesMod");
        INSTANCE = this;

        final var cfg = new ForgeConfigSpec.Builder().configure(MapAtlasesConfig::new);
        config = cfg.getLeft();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, cfg.getRight(), MOD_ID + ".toml");

        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addGenericListener(RecipeSerializer.class, this::registerRecipeSerializers);
        bus.addGenericListener(MenuType.class, this::registerContainers);
        bus.addGenericListener(SoundEvent.class, this::registerSounds);
        bus.addGenericListener(Item.class, this::registerItems);
        bus.addListener(config::loadOptionsFromConfigFile);
        //bus.addListener(MapAtlasesModClient::setup);
        //bus.addListener(this::onRegisterTexture);
        MinecraftForge.EVENT_BUS.addListener(MapAtlasesServerLifecycleEvents::mapAtlasServerTick);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            bus.addListener(MapAtlasesModClient::setup);
            bus.addListener(this::onRegisterTexture);
            MinecraftForge.EVENT_BUS.addListener(MapAtlasesModClient::tick);
        }
    }

    public void registerItems(RegistryEvent.Register<Item> event) {
        Item.Properties properties = new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(16);
        MAP_ATLAS = new MapAtlasItem(properties);
        event.getRegistry().register(MAP_ATLAS.setRegistryName("atlas"));
        //if (enableMultiDimMaps) {
        //    event.getRegistry().register(new MapAtlasItem(properties).setRegistryName("end_atlas"));
        //    event.getRegistry().register(new MapAtlasItem(properties).setRegistryName("nether_atlas"));
        //}
        event.getRegistry().register(new DummyFilledMap(new Item.Properties()).setRegistryName("dummy_filled_map"));
    }

    public void registerRecipeSerializers(RegistryEvent.Register<RecipeSerializer<?>> event) {
        MAP_ATLAS_CREATE_RECIPE = new SimpleRecipeSerializer<>(MapAtlasCreateRecipe::new);
        MAP_ATLAS_ADD_RECIPE = new SimpleRecipeSerializer<>(MapAtlasesAddRecipe::new);
        MAP_ATLAS_CUT_RECIPE = new SimpleRecipeSerializer<>(MapAtlasesCutExistingRecipe::new);
        event.getRegistry().registerAll(
                MAP_ATLAS_CREATE_RECIPE.setRegistryName("crafting_atlas"),
                MAP_ATLAS_ADD_RECIPE.setRegistryName("adding_atlas"),
                MAP_ATLAS_CUT_RECIPE.setRegistryName("cutting_atlas")
        );
    }

    public void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        ATLAS_OPEN_SOUND_EVENT = new SoundEvent(ATLAS_OPEN_SOUND_ID);
        ATLAS_PAGE_TURN_SOUND_EVENT = new SoundEvent(ATLAS_PAGE_TURN_SOUND_ID);
        ATLAS_CREATE_MAP_SOUND_EVENT = new SoundEvent(ATLAS_CREATE_MAP_SOUND_ID);
        event.getRegistry().registerAll(
                ATLAS_OPEN_SOUND_EVENT.setRegistryName(ATLAS_OPEN_SOUND_ID),
                ATLAS_PAGE_TURN_SOUND_EVENT.setRegistryName(ATLAS_PAGE_TURN_SOUND_ID),
                ATLAS_CREATE_MAP_SOUND_EVENT.setRegistryName(ATLAS_CREATE_MAP_SOUND_ID)
        );
    }

    public void registerContainers(RegistryEvent.Register<MenuType<?>> event) {
        ATLAS_OVERVIEW_HANDLER = IForgeMenuType.create(MapAtlasesAtlasOverviewScreenHandler::new);
        event.getRegistry().register(ATLAS_OVERVIEW_HANDLER.setRegistryName("atlas_overview"/*"gui_container"*/));
    }

    public void onRegisterTexture(TextureStitchEvent.Pre event) {
        event.addSprite(LECTERN_OVERWORLD_ID);
        event.addSprite(LECTERN_NETHER_ID);
        event.addSprite(LECTERN_END_ID);
        event.addSprite(LECTERN_OTHER_ID);
    }
}