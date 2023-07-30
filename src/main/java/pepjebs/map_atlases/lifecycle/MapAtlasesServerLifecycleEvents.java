package pepjebs.map_atlases.lifecycle;

import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import pepjebs.map_atlases.MapAtlasesMod;
import pepjebs.map_atlases.item.MapAtlasItem;
import pepjebs.map_atlases.networking.MapAtlasesInitAtlasS2CPacket;
import pepjebs.map_atlases.networking.ModPacketHandler;
import pepjebs.map_atlases.utils.MapAtlasesAccessUtils;
import pepjebs.map_atlases.networking.MapAtlasesActiveStateChangeS2CPacket;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class MapAtlasesServerLifecycleEvents
{
    // Used to prevent Map creation spam consuming all Empty Maps on auto-create
    private static final Semaphore mutex = new Semaphore(1);

    // Holds the current MapItemSavedData ID for each player
    private static final Map<String, String> playerToActiveMapId = new HashMap<>();

    public static void mapAtlasPlayerJoinImpl(ServerPlayer player) {
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        if (atlas.isEmpty()) return;
        Map<String, MapItemSavedData> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(player.getLevel(), atlas);
        for (Map.Entry<String, MapItemSavedData> info : mapInfos.entrySet()) {
            String mapId = info.getKey();
            MapItemSavedData state = info.getValue();
            state.tickCarriedBy(player, atlas);
            state.getHoldingPlayer(player);
            ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new MapAtlasesInitAtlasS2CPacket(mapId, state));
        }
    }

    public static void mapAtlasServerTick(TickEvent.ServerTickEvent event) {
        if(event.phase != TickEvent.Phase.END /*|| event.type != TickEvent.Type.SERVER*/)
            return;
        ArrayList<String> seenPlayers = new ArrayList<>();
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            var playerName = player.getName().getString();
            seenPlayers.add(playerName);
            if (player.isRemoved() || player.isChangingDimension() || player.hasDisconnected()) continue;
            ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
            if (atlas.isEmpty()) continue;
            Map<String, MapItemSavedData> currentMapInfos = MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(player.getLevel(), atlas);
            // changedMapState has non-null value if player has a new active Map ID
            Map.Entry<String, MapItemSavedData> activeInfo = relayActiveMapIdToPlayerClient(currentMapInfos, player);
            if (activeInfo == null) {
                maybeCreateNewMapEntry(player, atlas, 0, Mth.floor(player.getX()), Mth.floor(player.getZ()));
                continue;
            }
            MapItemSavedData activeState = activeInfo.getValue();

            int playX = player.blockPosition().getX();
            int playZ = player.blockPosition().getZ();
            byte scale = activeState.scale;
            int scaleWidth = (1 << scale) * 128;
            ArrayList<Pair<Integer, Integer>> discoveringEdges = getPlayerDiscoveringMapEdges(activeState.x, activeState.z, scaleWidth, playX, playZ);

            // Update Map states & colors
            // updateColors is *easily* the most expensive function in the entire server tick
            // As a result, we will only ever call updateColors twice per tick (same as vanilla's limit)
            Map<String, MapItemSavedData> nearbyExistentMaps = currentMapInfos.entrySet().stream()
                    .filter(e -> discoveringEdges.stream().anyMatch(edge -> edge.getFirst() == e.getValue().x && edge.getFirst() == e.getValue().x))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            for (var mapInfo : currentMapInfos.entrySet())
                updateMapDataForPlayer(mapInfo, player, atlas);
            updateMapColorsForPlayer(activeState, player);
            if (!nearbyExistentMaps.isEmpty())
                updateMapColorsForPlayer((MapItemSavedData)nearbyExistentMaps.values().toArray()[ServerLifecycleHooks.getCurrentServer().getTickCount() % nearbyExistentMaps.size()], player);

            // Create new Map entries
            if (MapAtlasesMod.config.options != null && !MapAtlasesMod.config.options.enableEmptyMapEntryAndFill()) continue;
            boolean isPlayerOutsideAllMapRegions = MapAtlasesAccessUtils.distanceBetweenMapStateAndPlayer(activeState, player) > scaleWidth;
            if (isPlayerOutsideAllMapRegions)
                maybeCreateNewMapEntry(player, atlas, scale, Mth.floor(player.getX()), Mth.floor(player.getZ()));
            discoveringEdges.removeIf(e -> nearbyExistentMaps.values().stream().anyMatch(d -> d.x == e.getFirst() && d.z == e.getSecond()));
            for (var p : discoveringEdges)
                maybeCreateNewMapEntry(player, atlas, scale, p.getFirst(), p.getSecond());
        }
        // Clean up disconnected players in server tick, since when using Disconnect event,
        // the tick will sometimes re-add the Player after they disconnect
        playerToActiveMapId.keySet().removeIf(playerName -> !seenPlayers.contains(playerName));
    }

    private static void updateMapDataForPlayer(Map.Entry<String, MapItemSavedData> mapInfo, ServerPlayer player, ItemStack atlas) {
        mapInfo.getValue().tickCarriedBy(player, atlas);
        relayMapStateSyncToPlayerClient(mapInfo, player);
    }

    private static void updateMapColorsForPlayer(MapItemSavedData state, ServerPlayer player) {
        ((MapItem)Items.FILLED_MAP).update(player.getLevel(), player, state);
    }

    public static void relayMapStateSyncToPlayerClient(Map.Entry<String, MapItemSavedData> mapInfo, ServerPlayer player) {
        int mapId = MapAtlasesAccessUtils.getMapIntFromString(mapInfo.getKey());
        Packet<?> p = null;
        int tries = 0;
        while (p == null && tries < 10) {
            p = mapInfo.getValue().getUpdatePacket(mapId, player);
            tries++;
        }
        if (p != null)
            player.connection.send(p);
    }

    private static Map.Entry<String, MapItemSavedData> relayActiveMapIdToPlayerClient(Map<String, MapItemSavedData> currentMapInfos, ServerPlayer player) {
        Map.Entry<String, MapItemSavedData> activeInfo = MapAtlasesAccessUtils.getActiveAtlasMapStateServer(currentMapInfos, player);
        String playerName = player.getName().getString();
        if (activeInfo != null) {
            boolean addingPlayer = !playerToActiveMapId.containsKey(playerName);
            boolean activatingPlayer = playerToActiveMapId.get(playerName) == null;
            // Players that pick up an atlas will need their MapStates initialized
            if (addingPlayer || activatingPlayer)
                mapAtlasPlayerJoinImpl(player);
            if (addingPlayer || activatingPlayer || activeInfo.getKey().compareTo(playerToActiveMapId.get(playerName)) != 0) {
                playerToActiveMapId.put(playerName, activeInfo.getKey());
                ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new MapAtlasesActiveStateChangeS2CPacket(activeInfo.getKey()));
            }
        } else if (playerToActiveMapId.get(playerName) != null){
            ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new MapAtlasesActiveStateChangeS2CPacket((String)null));
            playerToActiveMapId.put(playerName, null);
        }
        return activeInfo;
    }

    private static void maybeCreateNewMapEntry(ServerPlayer player, ItemStack atlas, int scale, int destX, int destZ) {
        List<Integer> mapIds = new ArrayList<>();
        if (atlas.getTag() != null) {
            mapIds = Arrays.stream(atlas.getTag().getIntArray(MapAtlasItem.MAP_LIST_NBT)).boxed().collect(Collectors.toList());
        } else {
            // If the Atlas is "inactive", give it a pity Empty Map count
            CompoundTag defaultAtlasNbt = new CompoundTag();
            defaultAtlasNbt.putInt(MapAtlasItem.EMPTY_MAP_NBT, MapAtlasesMod.config.options != null ? MapAtlasesMod.config.options.pityActivationMapCount() : 1);
            atlas.setTag(defaultAtlasNbt);
        }
        int emptyCount = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas);
        boolean bypassEmptyMaps = !MapAtlasesMod.config.options.requireEmptyMapsToExpand();
        if (mutex.availablePermits() > 0 && (emptyCount > 0 || player.isCreative() || bypassEmptyMaps)) {
            try {
                mutex.acquire();

                // Make the new map
                if (!player.isCreative() && !bypassEmptyMaps)
                    atlas.getTag().putInt(MapAtlasItem.EMPTY_MAP_NBT, atlas.getTag().getInt(MapAtlasItem.EMPTY_MAP_NBT) - 1);
                ItemStack newMap = MapItem.create(player.getLevel(), destX, destZ, (byte)scale, true, false);
                mapIds.add(MapItem.getMapId(newMap));
                atlas.getTag().putIntArray(MapAtlasItem.MAP_LIST_NBT, mapIds);

                // Play the sound
                player.getLevel().playSound(null, player.blockPosition(), MapAtlasesMod.ATLAS_CREATE_MAP_SOUND_EVENT,
                        SoundSource.PLAYERS, MapAtlasesMod.config.options.soundScalar(), 1.0F);
            } catch (InterruptedException e) {
                MapAtlasesMod.LOGGER.warn(e.toString());
            } finally {
                mutex.release();
            }
        }
    }

    private static ArrayList<Pair<Integer, Integer>> getPlayerDiscoveringMapEdges(int xCenter, int zCenter, int width, int xPlayer, int zPlayer) {
        int halfWidth = width / 2;
        ArrayList<Pair<Integer, Integer>> results = new ArrayList<>();
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (i != 0 || j != 0) {
                    int qI = xCenter;
                    int qJ = zCenter;
                    if (i == -1 && xPlayer - 128 < xCenter - halfWidth)
                        qI -= width;
                    else if (i == 1 && xPlayer + 128 > xCenter + halfWidth)
                        qI += width;
                    if (j == -1 && zPlayer - 128 < zCenter - halfWidth)
                        qJ -= width;
                    else if (j == 1 && zPlayer + 128 > zCenter + halfWidth)
                        qJ += width;
                    // Some lambda...
                    int finalQI = qI;
                    int finalQJ = qJ;
                    if ((qI != xCenter || qJ != zCenter) && results.stream().noneMatch(p -> p.getFirst() == finalQI && p.getSecond() == finalQJ))
                        results.add(new Pair<>(qI, qJ));
                }
            }
        }
        return results;
    }
}