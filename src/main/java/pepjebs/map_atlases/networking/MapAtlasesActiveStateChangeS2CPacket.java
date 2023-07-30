package pepjebs.map_atlases.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import pepjebs.map_atlases.MapAtlasesMod;

import java.util.function.Supplier;

public class MapAtlasesActiveStateChangeS2CPacket {
    public String activeMap;
    private static final String NULL = "null";

    public MapAtlasesActiveStateChangeS2CPacket(String activeMap) { this.activeMap = activeMap; }

    public MapAtlasesActiveStateChangeS2CPacket(FriendlyByteBuf buf) {
        activeMap = buf.readUtf();
        if(activeMap.compareTo(NULL) == 0)
            activeMap = null;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(activeMap != null ? activeMap : NULL);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MapAtlasesMod.currentMapStateId = activeMap;
        });
        ctx.get().setPacketHandled(true);
    }
}
