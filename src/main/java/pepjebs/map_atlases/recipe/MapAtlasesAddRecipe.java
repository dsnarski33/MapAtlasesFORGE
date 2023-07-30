package pepjebs.map_atlases.recipe;

import com.google.common.primitives.Ints;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.map_atlases.MapAtlasesMod;
import pepjebs.map_atlases.item.MapAtlasItem;
import pepjebs.map_atlases.utils.MapAtlasesAccessUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MapAtlasesAddRecipe extends CustomRecipe {

    private Level world = null;

    public MapAtlasesAddRecipe(ResourceLocation id) { super(id); }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
        this.world = world;
        List<ItemStack> itemStacks = MapAtlasesAccessUtils
                .getItemStacksFromGrid(inv)
                .stream()
                .map(ItemStack::copy)
                .toList();
        ItemStack atlas = getAtlasFromItemStacks(itemStacks).copy();

        // Ensure there's an Atlas
        if (atlas.isEmpty())
            return false;

        MapItemSavedData sampleMap = MapAtlasesAccessUtils.getFirstMapItemSavedDataFromAtlas(world, atlas);

        // Ensure only correct ingredients are present
        List<Item> additems = new ArrayList<>(Arrays.asList(Items.FILLED_MAP, MapAtlasesMod.MAP_ATLAS));
        if (MapAtlasesMod.config == null || MapAtlasesMod.config.options.enableEmptyMapEntryAndFill())
            additems.add(Items.MAP);
        if (MapAtlasesMod.config.options != null && MapAtlasesMod.config.options.acceptPaperForEmptyMaps())
            additems.add(Items.PAPER);
        if (!(itemStacks.size() > 1 && isListOnlyIngredients(itemStacks, additems)))
            return false;
        List<MapItemSavedData> mapStates = getMapStatesFromItemStacks(world, itemStacks);

        // Ensure we're not trying to add too many Maps
        int mapCount = MapAtlasesAccessUtils.getMapCountFromItemStack(atlas) + MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas);
        if (MapAtlasItem.getMaxMapCount() != -1 && mapCount + itemStacks.size() - 1 > MapAtlasItem.getMaxMapCount())
            return false;

        // Ensure Filled Maps are all same Scale & Dimension
        if(mapStates.size() > 0 && sampleMap != null && !areMapsSameScale(sampleMap, mapStates)) return false;

        // Ensure there's only one Atlas
        long atlasCount = itemStacks.stream().filter(i -> i.is(MapAtlasesMod.MAP_ATLAS)).count();
        return atlasCount == 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv) {
        if (world == null) return ItemStack.EMPTY;
        List<ItemStack> itemStacks = MapAtlasesAccessUtils.getItemStacksFromGrid(inv)
                .stream()
                .map(ItemStack::copy)
                .toList();
        // Grab the Atlas in the Grid
        ItemStack atlas = getAtlasFromItemStacks(itemStacks).copy();
        // Get the Map Ids in the Grid
        Set<Integer> mapIds = getMapIdsFromItemStacks(itemStacks);
        // Set NBT Data
        int emptyMapCount = (int)itemStacks.stream().filter(i -> i != null && (i.is(Items.MAP) || i.is(Items.PAPER))).count();
        if (MapAtlasesMod.config.options != null)
            emptyMapCount *= MapAtlasesMod.config.options.mapEntryValueMultiplier();
        CompoundTag compoundTag = atlas.getOrCreateTag();
        Set<Integer> existingMaps = new HashSet<>(Ints.asList(compoundTag.getIntArray(MapAtlasItem.MAP_LIST_NBT)));
        existingMaps.addAll(mapIds);
        compoundTag.putIntArray(MapAtlasItem.MAP_LIST_NBT, existingMaps.stream().filter(Objects::nonNull).mapToInt(i->i).toArray());
        compoundTag.putInt(MapAtlasItem.EMPTY_MAP_NBT, emptyMapCount + compoundTag.getInt(MapAtlasItem.EMPTY_MAP_NBT));
        atlas.setTag(compoundTag);
        return atlas;
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return MapAtlasesMod.MAP_ATLAS_ADD_RECIPE; }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    private boolean areMapsSameScale(MapItemSavedData testAgainst, List<MapItemSavedData> newMaps) {
        return newMaps.stream().filter(m -> m.scale == testAgainst.scale).count() == newMaps.size();
    }

    private boolean areMapsSameDimension(MapItemSavedData testAgainst, List<MapItemSavedData> newMaps) {
        return newMaps.stream().filter(m -> m.dimension == testAgainst.dimension).count() == newMaps.size();
    }

    private ItemStack getAtlasFromItemStacks(List<ItemStack> itemStacks) {
        Optional<ItemStack> item =  itemStacks.stream()
                .filter(i -> i.is(MapAtlasesMod.MAP_ATLAS)).findFirst();
        return item.orElse(ItemStack.EMPTY).copy();
    }

    private List<MapItemSavedData> getMapStatesFromItemStacks(Level world, List<ItemStack> itemStacks) {
        return itemStacks.stream()
                .filter(i -> i.is(Items.FILLED_MAP))
                .map(m -> MapItem.getSavedData(m, world))
                .collect(Collectors.toList());
    }

    private Set<Integer> getMapIdsFromItemStacks(List<ItemStack> itemStacks) {
        return itemStacks.stream().map(MapItem::getMapId).collect(Collectors.toSet());
    }

    private boolean isListOnlyIngredients(List<ItemStack> itemStacks, List<Item> items) {
        return itemStacks.stream().filter(is -> {
            for (Item i : items)
                if (i == is.getItem())
                    return true;
            return false;
        }).count() == itemStacks.size();
    }
}
