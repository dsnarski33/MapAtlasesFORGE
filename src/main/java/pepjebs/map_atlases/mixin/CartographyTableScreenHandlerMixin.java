/**
 * This class was forked from:
 * https://github.com/AntiqueAtlasTeam/AntiqueAtlas/blob/37038a399ecac1d58bcc7164ef3d309e8636a2cb/src/main/java
 *      /hunternif/mc/impl/atlas/mixin/MixinCartographyTableScreenHandler.java
 * Under the GPL-3 license.
 */
package pepjebs.map_atlases.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.map_atlases.MapAtlasesMod;
import org.spongepowered.asm.mixin.Shadow;
import pepjebs.map_atlases.item.MapAtlasItem;
import pepjebs.map_atlases.utils.MapAtlasesAccessUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(CartographyTableMenu.class)
public abstract class CartographyTableScreenHandlerMixin extends AbstractContainerMenu {

    @Shadow @Final
    private ResultContainer resultContainer;

    @Shadow @Final
    private ContainerLevelAccess access;

    protected CartographyTableScreenHandlerMixin(@Nullable MenuType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    void mapAtlasUpdateResult(ItemStack atlas, ItemStack bottomItem, ItemStack oldResult, CallbackInfo info) {
        if (atlas.getItem() == MapAtlasesMod.MAP_ATLAS && bottomItem.getItem() == MapAtlasesMod.MAP_ATLAS) {
            final int[] allMapIds = Stream.of(Arrays.stream(MapAtlasesAccessUtils.getMapIdsFromItemStack(atlas)),
                    Arrays.stream(MapAtlasesAccessUtils.getMapIdsFromItemStack(bottomItem)))
                    .flatMapToInt(x -> x)
                    .distinct()
                    .toArray();
            this.access.execute((world, blockPos) -> {
                int[] filteredMapIds = filterIntArrayForUniqueMaps(world, allMapIds);
                ItemStack result = new ItemStack(MapAtlasesMod.MAP_ATLAS);
                CompoundTag mergedNbt = new CompoundTag();
                int halfEmptyCount = (int)Math.ceil((MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas)
                        + MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(bottomItem)) / 2.0);
                mergedNbt.putInt(MapAtlasItem.EMPTY_MAP_NBT, halfEmptyCount);
                mergedNbt.putIntArray(MapAtlasItem.MAP_LIST_NBT, filteredMapIds);
                result.setTag(mergedNbt);
                result.grow(1);
                resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
            });
            broadcastChanges();
            info.cancel();
        } else if (atlas.getItem() == MapAtlasesMod.MAP_ATLAS && (bottomItem.getItem() == Items.MAP
                || (MapAtlasesMod.config.options.acceptPaperForEmptyMaps() && bottomItem.getItem() == Items.PAPER))) {
            ItemStack result = atlas.copy();
            CompoundTag nbt = result.getTag() != null ? result.getTag() : new CompoundTag();
            int amountToAdd = MapAtlasesAccessUtils.getMapCountToAdd(atlas, bottomItem);
            nbt.putInt(MapAtlasItem.EMPTY_MAP_NBT, nbt.getInt(MapAtlasItem.EMPTY_MAP_NBT) + amountToAdd);
            result.setTag(nbt);
            resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
            broadcastChanges();
            info.cancel();
        } else if (atlas.getItem() == MapAtlasesMod.MAP_ATLAS && bottomItem.getItem() == Items.FILLED_MAP) {
            ItemStack result = atlas.copy();
            if (bottomItem.getTag() == null || !bottomItem.hasTag() || !bottomItem.getTag().contains("map"))
                return;
            int mapId = bottomItem.getTag().getInt("map");
            CompoundTag compound = result.getTag();
            if (compound == null) return;
            int[] existentMapIdArr = compound.getIntArray(MapAtlasItem.MAP_LIST_NBT);
            List<Integer> existentMapIds = Arrays.stream(existentMapIdArr).boxed().distinct().collect(Collectors.toList());
            if (!existentMapIds.contains(mapId))
                existentMapIds.add(mapId);
            access.execute((world, blockPos) -> {
                int[] filteredMapIds = filterIntArrayForUniqueMaps(world, existentMapIds.stream().mapToInt(s -> s).toArray());
                compound.putIntArray(MapAtlasItem.MAP_LIST_NBT, filteredMapIds);
                result.setTag(compound);
                resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
            });
            this.broadcastChanges();
            info.cancel();
        } else if (atlas.getItem() == Items.BOOK && bottomItem.getItem() == Items.FILLED_MAP) {
            ItemStack result = new ItemStack(MapAtlasesMod.MAP_ATLAS);
            if (bottomItem.getTag() == null || !bottomItem.hasTag() || !bottomItem.getTag().contains("map"))
                return;
            int mapId = bottomItem.getTag().getInt("map");
            CompoundTag compound = new CompoundTag();
            compound.putIntArray(MapAtlasItem.MAP_LIST_NBT, new int[]{ mapId });
            if (MapAtlasesMod.config.options != null && MapAtlasesMod.config.options.pityActivationMapCount() > 0)
                compound.putInt(MapAtlasItem.EMPTY_MAP_NBT, MapAtlasesMod.config.options.pityActivationMapCount());
            result.setTag(compound);
            this.resultContainer.setItem(CartographyTableMenu.RESULT_SLOT, result);
            this.broadcastChanges();
            info.cancel();
        }
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    void mapAtlasTransferSlot(Player player, int index, CallbackInfoReturnable<ItemStack> info) {
        if (index >= 0 && index <= 2) return;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            if (stack.getItem() != MapAtlasesMod.MAP_ATLAS) return;
            boolean result = this.moveItemStackTo(stack, 0, 2, false);
            if (!result)
                info.setReturnValue(ItemStack.EMPTY);
        }
    }

    // Filters for both duplicate map id (e.g. "map_25") and duplicate X+Z+Dimension
    private int[] filterIntArrayForUniqueMaps(Level world, int[] toFilter) {
        Map<String, Pair<Integer, MapItemSavedData>> uniqueXZMapIds =
                Arrays.stream(toFilter)
                        .mapToObj(mId -> new Pair<>(mId, world.getMapData("map_" + mId)))
                        .filter(m -> m.getSecond() != null)
                        .collect(Collectors.toMap(
                                m -> m.getSecond().x + ":" + m.getSecond().z + ":"  + m.getSecond().dimension,
                                m -> m, (m1, m2) -> m1));
        return uniqueXZMapIds.values().stream().mapToInt(Pair::getFirst).toArray();
    }
}