package pepjebs.map_atlases.networking;

//import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.network.NetworkEvent;
import pepjebs.map_atlases.MapAtlasesMod;
import pepjebs.map_atlases.MapAtlasesModClient;
import pepjebs.map_atlases.utils.MapAtlasesAccessUtils;

import java.util.function.Supplier;

public class MapAtlasesInitAtlasS2CPacket
{
    private final String mapId;
    private final MapItemSavedData mapState;

    public MapAtlasesInitAtlasS2CPacket(FriendlyByteBuf buf) {
        mapId = buf.readUtf();
        CompoundTag nbt = buf.readNbt();
        if (nbt == null) {
            MapAtlasesMod.LOGGER.warn("Null MapItemSavedData NBT received by client");
            mapState = null;
        } else {
            mapState = MapItemSavedData.load(nbt);
        }
    }

    public MapAtlasesInitAtlasS2CPacket(String mapId, MapItemSavedData mapState) {
        this.mapId = mapId;
        this.mapState = mapState;
    }

    public void write(FriendlyByteBuf buf) {
        CompoundTag mapAsTag = new CompoundTag();
        mapState.save(mapAsTag);
        buf.writeUtf(mapId);
        buf.writeNbt(mapAsTag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MapAtlasesModClient.handleAtlasMap(mapId, mapState);
        });
        ctx.get().setPacketHandled(true);
    }

    public String getMapId() { return this.mapId; }
    public MapItemSavedData getMapState() { return this.mapState; }
}
