package pepjebs.map_atlases.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import pepjebs.map_atlases.MapAtlasesMod;
import pepjebs.map_atlases.item.MapAtlasItem;

import java.util.function.Supplier;

public class MapAtlasesOpenGUIC2SPacket
{
    public ItemStack atlas;

    public MapAtlasesOpenGUIC2SPacket(FriendlyByteBuf buf) { atlas = buf.readItem(); }
    public MapAtlasesOpenGUIC2SPacket(ItemStack atlas) { this.atlas = atlas; }

    public void write(FriendlyByteBuf buf) { buf.writeItem(atlas); }
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if(player != null) {
                NetworkHooks.openGui(player, new SimpleMenuProvider((i, playerInventory, playerEntity) -> ((MapAtlasItem) this.atlas.getItem()).createMenu(i, playerInventory, playerEntity), new TextComponent("atlas_gui")), b -> ((MapAtlasItem) this.atlas.getItem()).writeScreenOpeningData(player, b));
                player.getLevel().playSound(null, player.blockPosition(), MapAtlasesMod.ATLAS_OPEN_SOUND_EVENT, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
