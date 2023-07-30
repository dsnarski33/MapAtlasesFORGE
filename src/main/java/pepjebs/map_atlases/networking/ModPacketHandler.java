package pepjebs.map_atlases.networking;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import pepjebs.map_atlases.MapAtlasesMod;
import pepjebs.map_atlases.lifecycle.MapAtlasesServerLifecycleEvents;

@Mod.EventBusSubscriber(modid = MapAtlasesMod.MOD_ID)
public class ModPacketHandler
{
    public static SimpleChannel INSTANCE;
    private static final String PROTOCOL_VERSION = "1";
    private static int ID = 0;

    private static int nextID() { return ID++; }

    public static void registerMessages() {
        INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(MapAtlasesMod.MOD_ID, "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
        //if (FMLEnvironment.dist == Dist.CLIENT)
        INSTANCE.messageBuilder(MapAtlasesInitAtlasS2CPacket.class, ModPacketHandler.nextID()).encoder(MapAtlasesInitAtlasS2CPacket::write).decoder(MapAtlasesInitAtlasS2CPacket::new).consumer(MapAtlasesInitAtlasS2CPacket::handle).add();
        //if (FMLEnvironment.dist == Dist.CLIENT)
        INSTANCE.messageBuilder(MapAtlasesOpenGUIC2SPacket.class, ModPacketHandler.nextID()).encoder(MapAtlasesOpenGUIC2SPacket::write).decoder(MapAtlasesOpenGUIC2SPacket::new).consumer(MapAtlasesOpenGUIC2SPacket::handle).add();
        INSTANCE.messageBuilder(MapAtlasesActiveStateChangeS2CPacket.class, ModPacketHandler.nextID()).encoder(MapAtlasesActiveStateChangeS2CPacket::toBytes).decoder(MapAtlasesActiveStateChangeS2CPacket::new).consumer(MapAtlasesActiveStateChangeS2CPacket::handle).add();
    }

    @SubscribeEvent
    public static void onServerStarting(final ServerStartingEvent event) { }

    @SubscribeEvent
    public static void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent event)
    {
        MapAtlasesServerLifecycleEvents.mapAtlasPlayerJoinImpl((ServerPlayer)event.getPlayer());
    }
}
