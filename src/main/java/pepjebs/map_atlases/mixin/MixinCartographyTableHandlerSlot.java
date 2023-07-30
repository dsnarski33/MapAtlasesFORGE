/**
 * This class was forked from:
 * https://github.com/AntiqueAtlasTeam/AntiqueAtlas/blob/37038a399ecac1d58bcc7164ef3d309e8636a2cb/src/main/java
 *      /hunternif/mc/impl/atlas/mixin/prod/MixinCartographyTableHandlerSlot.java
 * Under the GPL-3 license.
 */
package pepjebs.map_atlases.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.map_atlases.MapAtlasesMod;
import pepjebs.map_atlases.utils.MapAtlasesAccessUtils;

@Mixin(targets = "net.minecraft.world.inventory.CartographyTableMenu$3")
class MixinCartographyTableScreenHandlerFirstSlot
{
    @Inject(method = "mayPlace", at = @At("RETURN"), cancellable = true)
    void mapAtlasCanInsert(ItemStack stack, CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(stack.getItem() == MapAtlasesMod.MAP_ATLAS || stack.getItem() ==  Items.BOOK ||
                info.getReturnValueZ());
    }
}

@Mixin(targets = "net.minecraft.world.inventory.CartographyTableMenu$4")
class MixinCartographyTableScreenHandlerSecondSlot
{
    @Inject(method = "mayPlace", at = @At("RETURN"), cancellable = true)
    void mapAtlasCanInsert(ItemStack stack, CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(stack.getItem() == MapAtlasesMod.MAP_ATLAS || stack.getItem() ==  Items.FILLED_MAP ||
                info.getReturnValueZ());
    }
}

@Mixin(targets = "net.minecraft.world.inventory.CartographyTableMenu$5")
class MixinCartographyTableScreenHandlerSecondSlotMaps
{
    CartographyTableMenu cartographyHandler;

    @Inject(method = "<init>", at = @At("TAIL"))
    void mapAtlasInit(CartographyTableMenu handler, Container inventory, int index, int x, int y, ContainerLevelAccess context, CallbackInfo info) {
        cartographyHandler = handler;
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    void mapAtlasOnTakeItem(Player player, ItemStack stack, CallbackInfo info) {
        ItemStack atlas = cartographyHandler.slots.get(0).getItem();
        Slot slotOne = cartographyHandler.slots.get(1);
        if (cartographyHandler.slots.get(0).getItem().getItem() == MapAtlasesMod.MAP_ATLAS && (slotOne.getItem().getItem() == Items.MAP
                || (MapAtlasesMod.config.options.acceptPaperForEmptyMaps() && slotOne.getItem().getItem() == Items.PAPER))) {
            int amountToTake = MapAtlasesAccessUtils.getMapCountToAdd(atlas, slotOne.getItem());
            // onTakeItem already calls takeStack(1) so we subtract that out
            slotOne.remove(amountToTake - 1);
        } else if (cartographyHandler.slots.get(0).getItem().getItem() == MapAtlasesMod.MAP_ATLAS && slotOne.getItem().getItem() == Items.FILLED_MAP) {
            slotOne.remove(1);
        }
    }
}