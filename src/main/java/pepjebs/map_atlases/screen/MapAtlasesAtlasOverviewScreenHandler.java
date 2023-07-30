package pepjebs.map_atlases.screen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import pepjebs.map_atlases.MapAtlasesMod;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapAtlasesAtlasOverviewScreenHandler extends AbstractContainerMenu
{
    public ItemStack atlas;
    public String centerMapId;
    public int atlasScale;
    public Map<Integer, Pair<String,List<Integer>>> idsToCenters = new HashMap<>();

    public MapAtlasesAtlasOverviewScreenHandler(int syncId, Inventory _playerInventory, FriendlyByteBuf buf) {
        super(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER, syncId);
        atlas = buf.readItem();
        centerMapId = buf.readUtf();
        atlasScale = buf.readInt();
        int numToRead = buf.readInt();
        for (int i = 0; i < numToRead; i++) {
            int id = buf.readInt();
            var dim = buf.readUtf();
            var centers = Arrays.asList(buf.readInt(), buf.readInt());
            idsToCenters.put(id, new Pair<>(dim, centers));
        }
    }

    public MapAtlasesAtlasOverviewScreenHandler(int syncId, Inventory _playerInventory, Map<Integer, Pair<String,List<Integer>>> idsToCenters,
                                                ItemStack atlas, String centerMapId, int atlasScale) {
        super(MapAtlasesMod.ATLAS_OVERVIEW_HANDLER, syncId);
        this.idsToCenters = idsToCenters;
        this.atlas = atlas;
        this.centerMapId = centerMapId;
        this.atlasScale = atlasScale;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) { return null; }

    @Override
    public boolean stillValid(Player player) { return true; }
}
