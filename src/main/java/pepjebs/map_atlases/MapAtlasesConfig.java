package pepjebs.map_atlases;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import pepjebs.map_atlases.networking.ModPacketHandler;

public class MapAtlasesConfig
{
	private final ConfigOptions configOptions;
	public Options options = null;
	private static final String LANGKEY_CONFIG  = "config";

	public enum Values { maxMapCount, requireEmptyMapsToExpand, forceMiniMapScaling, drawMiniMapHUD, enableEmptyMapEntryAndFill, activationLocation, forceWorldMapScaling, miniMapAnchoring, miniMapHorizontalOffset, miniMapVerticalOffset, mapEntryValueMultiplier, pityActivationMapCount, activePotionVerticalOffset, drawMinimapCoords, drawMinimapBiome, minimapCoordsAndBiomeScale, drawWorldMapCoords, worldMapCoordsScale, miniMapDecorationScale, worldMapDecorationScale, acceptPaperForEmptyMaps, soundScalar }
	public record Options(int maxMapCount, boolean requireEmptyMapsToExpand, int forceMiniMapScaling, boolean drawMiniMapHUD, boolean enableEmptyMapEntryAndFill, String activationLocation, int forceWorldMapScaling, String miniMapAnchoring, int miniMapHorizontalOffset, int miniMapVerticalOffset, int mapEntryValueMultiplier, int pityActivationMapCount, int activePotionVerticalOffset, boolean drawMinimapCoords, boolean drawMinimapBiome, float minimapCoordsAndBiomeScale, boolean drawWorldMapCoords, float worldMapCoordsScale, float miniMapDecorationScale, float worldMapDecorationScale, boolean acceptPaperForEmptyMaps, float soundScalar) { }
	public record ConfigOptions(IntValue maxMapCount, BooleanValue requireEmptyMapsToExpand, IntValue forceMiniMapScaling, BooleanValue drawMiniMapHUD, BooleanValue enableEmptyMapEntryAndFill, ConfigValue<String> activationLocation, IntValue forceWorldMapScaling, ConfigValue<String> miniMapAnchoring, IntValue miniMapHorizontalOffset, IntValue miniMapVerticalOffset, IntValue mapEntryValueMultiplier, IntValue pityActivationMapCount, IntValue activePotionVerticalOffset, BooleanValue drawMinimapCoords, BooleanValue drawMinimapBiome, DoubleValue minimapCoordsAndBiomeScale, BooleanValue drawWorldMapCoords, DoubleValue worldMapCoordsScale, DoubleValue miniMapDecorationScale, DoubleValue worldMapDecorationScale, BooleanValue acceptPaperForEmptyMaps, DoubleValue soundScalar) {
		public Options setup() {
			return new Options(
					maxMapCount.get(),
					requireEmptyMapsToExpand.get(),
					forceMiniMapScaling.get(),
					drawMiniMapHUD.get(),
					enableEmptyMapEntryAndFill.get(),
					activationLocation.get(),
					forceWorldMapScaling.get(),
					miniMapAnchoring.get(),
					miniMapHorizontalOffset.get(),
					miniMapVerticalOffset.get(),
					mapEntryValueMultiplier.get(),
					pityActivationMapCount.get(),
					activePotionVerticalOffset.get(),
					drawMinimapCoords.get(),
					drawMinimapBiome.get(),
					minimapCoordsAndBiomeScale.get().floatValue(),
					drawWorldMapCoords.get(),
					worldMapCoordsScale.get().floatValue(),
					miniMapDecorationScale.get().floatValue(),
					worldMapDecorationScale.get().floatValue(),
					acceptPaperForEmptyMaps.get(),
					soundScalar.get().floatValue()
			);
		}
	}

	public MapAtlasesConfig(final ForgeConfigSpec.Builder builder) {
		builder.comment("  MapAtlasesFORGE Config").push(MapAtlasesMod.MOD_ID);
		configOptions = new ConfigOptions(
				builder.comment("The maximum number of Maps (Filled & Empty combined) allowed to be inside an Atlas (-1 to disable).", "  maxMapCount")
						.translation(getLangKey(LANGKEY_CONFIG, Values.maxMapCount.toString()))
						.defineInRange(Values.maxMapCount.toString(), 512, -1, Integer.MAX_VALUE),
				builder.comment("If true, the Atlas is required to have spare Empty Maps stored to expand the Filled Map size", "  requireEmptyMapsToExpand")
						.translation(getLangKey(LANGKEY_CONFIG, Values.requireEmptyMapsToExpand.toString()))
						.define(Values.requireEmptyMapsToExpand.toString(), true),
				builder.comment("Scale the mini-map to a given % of the height of your screen.", "  forceMiniMapScaling")
						.translation(getLangKey(LANGKEY_CONFIG, Values.forceMiniMapScaling.toString()))
						.defineInRange(Values.forceMiniMapScaling.toString(), 30, 0, 100),
				builder.comment("If 'true', the Mini-Map of the Active Map will be drawn on the HUD while the Atlas is active.", "  drawMiniMapHUD")
						.translation(getLangKey(LANGKEY_CONFIG, Values.drawMiniMapHUD.toString()))
						.define(Values.drawMiniMapHUD.toString(), true),
				builder.comment("If 'true', Atlases will be able to store Empty Maps and auto-fill them as you explore.", "  enableEmptyMapEntryAndFill")
						.translation(getLangKey(LANGKEY_CONFIG, Values.enableEmptyMapEntryAndFill.toString()))
						.define(Values.enableEmptyMapEntryAndFill.toString(), true),
				builder.comment("Controls location where mini-map displays. Any of: 'HANDS', 'HOTBAR', or 'INVENTORY'.", "  activationLocation")
						.translation(getLangKey(LANGKEY_CONFIG, Values.activationLocation.toString()))
						.define(Values.activationLocation.toString(), "HOTBAR"),
				builder.comment("Scale the world-map to a given % of the height of your screen.", "  forceWorldMapScaling")
						.translation(getLangKey(LANGKEY_CONFIG, Values.forceWorldMapScaling.toString()))
						.defineInRange(Values.forceWorldMapScaling.toString(), 80, 0, 100),
				builder.comment("Set to any of 'Upper'/'Lower' & 'Left'/'Right' to control anchor position of mini-map", "  miniMapAnchoring")
						.translation(getLangKey(LANGKEY_CONFIG, Values.miniMapAnchoring.toString()))
						.define(Values.miniMapAnchoring.toString(), "UpperLeft"),
				builder.comment("Enter an integer which will offset the mini-map horizontally", "  miniMapHorizontalOffset")
						.translation(getLangKey(LANGKEY_CONFIG, Values.miniMapHorizontalOffset.toString()))
						.defineInRange(Values.miniMapHorizontalOffset.toString(), 5, 0, Integer.MAX_VALUE),
				builder.comment("Enter an integer which will offset the mini-map vertically", "  miniMapVerticalOffset")
						.translation(getLangKey(LANGKEY_CONFIG, Values.miniMapVerticalOffset.toString()))
						.defineInRange(Values.miniMapVerticalOffset.toString(), 5, 0, Integer.MAX_VALUE),
				builder.comment("Controls how many usable Maps are added when you add a single Map to the Atlas", "  mapEntryValueMultiplier")
						.translation(getLangKey(LANGKEY_CONFIG, Values.mapEntryValueMultiplier.toString()))
						.defineInRange(Values.mapEntryValueMultiplier.toString(), 1, 0, Integer.MAX_VALUE),
				builder.comment("Controls how many free Empty Maps you get for 'activating' an Inactive Atlas", "  pityActivationMapCount")
						.translation(getLangKey(LANGKEY_CONFIG, Values.pityActivationMapCount.toString()))
						.defineInRange(Values.pityActivationMapCount.toString(), 9, 0, Integer.MAX_VALUE),
				builder.comment("The number of pixels to shift vertically when there's an active effect", "  activePotionVerticalOffset")
						.translation(getLangKey(LANGKEY_CONFIG, Values.activePotionVerticalOffset.toString()))
						.defineInRange(Values.activePotionVerticalOffset.toString(), 26, 0, Integer.MAX_VALUE),
				builder.comment("When enabled, the player's current Coords will be displayed", "  drawMinimapCoords")
						.translation(getLangKey(LANGKEY_CONFIG, Values.drawMinimapCoords.toString()))
						.define(Values.drawMinimapCoords.toString(), true),
				builder.comment("When enabled, the player's current Biome will be displayed", "  drawMinimapBiome")
						.translation(getLangKey(LANGKEY_CONFIG, Values.drawMinimapBiome.toString()))
						.define(Values.drawMinimapBiome.toString(), true),
				builder.comment("Sets the scale of the text rendered for Coords and Biome mini-map data", "  minimapCoordsAndBiomeScale")
						.translation(getLangKey(LANGKEY_CONFIG, Values.minimapCoordsAndBiomeScale.toString()))
						.defineInRange(Values.minimapCoordsAndBiomeScale.toString(), 1.0f, 0, Float.MAX_VALUE),
				builder.comment("When enabled, the Atlas world map coordinates will be displayed", "  drawWorldMapCoords")
						.translation(getLangKey(LANGKEY_CONFIG, Values.drawWorldMapCoords.toString()))
						.define(Values.drawWorldMapCoords.toString(), true),
				builder.comment("Sets the scale of the text rendered for Coords world-map data", "  worldMapCoordsScale")
						.translation(getLangKey(LANGKEY_CONFIG, Values.worldMapCoordsScale.toString()))
						.defineInRange(Values.worldMapCoordsScale.toString(), 1.0f, 0, Float.MAX_VALUE),
				builder.comment("Sets the scale of the map icons rendered in the mini-map", "  miniMapDecorationScale")
						.translation(getLangKey(LANGKEY_CONFIG, Values.miniMapDecorationScale.toString()))
						.defineInRange(Values.miniMapDecorationScale.toString(), 1.0f, 0, Float.MAX_VALUE),
				builder.comment("Sets the scale of the map icons rendered in the world-map", "  worldMapDecorationScale")
						.translation(getLangKey(LANGKEY_CONFIG, Values.worldMapDecorationScale.toString()))
						.defineInRange(Values.worldMapDecorationScale.toString(), 1.0f, 0, Float.MAX_VALUE),
				builder.comment("If enabled, you can increase the Empty Map count by inserting Paper", "  acceptPaperForEmptyMaps")
						.translation(getLangKey(LANGKEY_CONFIG, Values.acceptPaperForEmptyMaps.toString()))
						.define(Values.acceptPaperForEmptyMaps.toString(), false),
				builder.comment("Multiplier for all the Atlases sound float", "  soundScalar")
						.translation(getLangKey(LANGKEY_CONFIG, Values.soundScalar.toString()))
						.defineInRange(Values.soundScalar.toString(), 1.0f, 0, Float.MAX_VALUE)
				);
		builder.pop();
	}

	public void loadOptionsFromConfigFile(@SuppressWarnings("unused") FMLCommonSetupEvent event) {
		options = configOptions.setup();
		//MinecraftForge.EVENT_BUS.register(new CommonEventHandler());
		ModPacketHandler.registerMessages();
	}

	/**
	 * A I18n key helper.
	 * -> Sourced originally from FallThru (srsCode) @ github.com/srsCode/FallThru, MIT License
	 *
	 * @param  keys A list of key elements to be joined.
	 * @return      A full I18n key.
	 */
	private static String getLangKey(final String... keys)
	{
		return (keys.length > 0) ? String.join(".", MapAtlasesMod.MOD_ID, String.join(".", keys)) : MapAtlasesMod.MOD_ID;
	}

	/**
	 * This Event handler syncs a server-side config change with all connected players.
	 * This fires when the config file has been changed on disk and only updates on the client if the
	 * client is <b>not</b> connected to a remote server, or if the integrated server <b>is</b> running.
	 * This will always cause syncing on a dedicated server that will propogate to clients.
	 * -> Sourced originally from FallThru (srsCode) @ github.com/srsCode/FallThru, MIT License
	 *
	 * @param event The {@link ModConfigEvent.Reloading} event
	 */
	public void onConfigUpdate(final ModConfigEvent.Reloading event)
	{
		if (event.getConfig().getModId().equals(MapAtlasesMod.MOD_ID)) {
//			if (FMLEnvironment.dist == Dist.CLIENT && (Minecraft.getInstance().getSingleplayerServer() == null || Minecraft.getInstance().getConnection() != null)) {
//				Mod.LOGGER.warn("The config file has changed but the integrated server is not running. Nothing to do.");
//			} else {
//				Mod.LOGGER.warn("The config file has changed and the server is running. Resyncing config.");
//	//////		MapAtlasesMod.config.loadOptionsFromConfigFile(null);
//				DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> NetworkHandler.INSTANCE::updateAll);
//			}
		}
	}
}
