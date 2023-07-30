package pepjebs.map_atlases.recipe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.registries.ForgeRegistries;
import pepjebs.map_atlases.MapAtlasesMod;
import pepjebs.map_atlases.item.MapAtlasItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class MapAtlasCreateRecipe extends CustomRecipe
{
    private Level world = null;

    public MapAtlasCreateRecipe(ResourceLocation id) { super(id); }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
        this.world = world;
        ArrayList<ItemStack> itemStacks = new ArrayList<>();
        ItemStack filledMap = ItemStack.EMPTY;
        for(int i = 0; i < inv.getContainerSize(); i++) {
            if (!inv.getItem(i).isEmpty()) {
                itemStacks.add(inv.getItem(i));
                if (inv.getItem(i).getItem() == Items.FILLED_MAP) {
                    filledMap = inv.getItem(i);
                }
            }
        }
        if (itemStacks.size() == 3) {
            List<Item> items = itemStacks.stream().map(ItemStack::getItem).toList();
            boolean hasAllCrafting = new HashSet<>(items).containsAll(Arrays.asList(Items.FILLED_MAP, Items.BOOK)) && itemStacks.stream()
                            .anyMatch(i -> i.is(TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), MapAtlasesMod.STICKY_ITEMS_ID)));
            if (hasAllCrafting && !filledMap.isEmpty()) {
                MapItemSavedData state = MapItem.getSavedData(filledMap, world);
                return state != null;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv) {
        ItemStack mapItemStack = null;
        for(int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(Items.FILLED_MAP)) {
                mapItemStack = inv.getItem(i);
            }
        }
        if (mapItemStack == null || world == null || mapItemStack.getTag() == null) {
            return ItemStack.EMPTY;
        }
        MapItemSavedData mapState = MapItem.getSavedData(mapItemStack.getTag().getInt("map"), world);
        if (mapState == null) return ItemStack.EMPTY;
        CompoundTag compoundTag = new CompoundTag();
        Integer mapId = MapItem.getMapId(mapItemStack);
        if (mapId == null) {
            MapAtlasesMod.LOGGER.warn("MapAtlasCreateRecipe found null Map ID from Filled Map");
            compoundTag.putIntArray(MapAtlasItem.MAP_LIST_NBT, new int[]{});
        }
        else
            compoundTag.putIntArray(MapAtlasItem.MAP_LIST_NBT, new int[]{mapId});
        ItemStack atlasItemStack = new ItemStack(MapAtlasesMod.MAP_ATLAS);
        atlasItemStack.setTag(compoundTag);
        return atlasItemStack;
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return MapAtlasesMod.MAP_ATLAS_CREATE_RECIPE; }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 3; }
}
