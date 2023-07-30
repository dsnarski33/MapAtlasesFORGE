package pepjebs.map_atlases.item;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import pepjebs.map_atlases.MapAtlasesMod;
import pepjebs.map_atlases.lifecycle.MapAtlasesServerLifecycleEvents;
import pepjebs.map_atlases.screen.MapAtlasesAtlasOverviewScreenHandler;
import pepjebs.map_atlases.utils.MapAtlasesAccessUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapAtlasItem extends Item
{
    public static final String EMPTY_MAP_NBT = "empty";
    public static final String MAP_LIST_NBT = "maps";
    public static final BooleanProperty HAS_ATLAS = BooleanProperty.create("has_atlas");

    public MapAtlasItem(Item.Properties settings) { super(settings); }

    public static int getMaxMapCount() {
        return MapAtlasesMod.config.options != null ? MapAtlasesMod.config.options.maxMapCount() : 128;
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        super.appendHoverText(stack, world, tooltip, context);

        if (world != null && world.isClientSide) {
            int mapSize = MapAtlasesAccessUtils.getMapCountFromItemStack(stack);
            int empties = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(stack);
            if (getMaxMapCount() != -1 && mapSize + empties >= getMaxMapCount())
                tooltip.add(new TranslatableComponent("item.map_atlases.atlas.tooltip_full").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
            tooltip.add(new TranslatableComponent("item.map_atlases.atlas.tooltip_1", mapSize)
                    .withStyle(ChatFormatting.GRAY));
            if (MapAtlasesMod.config == null || (MapAtlasesMod.config.options.requireEmptyMapsToExpand() && MapAtlasesMod.config.options.enableEmptyMapEntryAndFill())) {
                // If there's no maps & no empty maps, the atlas is "inactive", so display how many empty maps
                // they *would* receive if they activated the atlas
                if (mapSize + empties == 0 && MapAtlasesMod.config.options != null)
                    empties = MapAtlasesMod.config.options.pityActivationMapCount();
                tooltip.add(new TranslatableComponent("item.map_atlases.atlas.tooltip_2", empties).withStyle(ChatFormatting.GRAY));
            }
            MapItemSavedData mapState = world.getMapData(MapAtlasesMod.currentMapStateId);
            if (mapState == null) return;
            tooltip.add(new TranslatableComponent("item.map_atlases.atlas.tooltip_3", 1 << mapState.scale).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
        openHandledAtlasScreen(world, player);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    public AbstractContainerMenu createMenu(int syncId, @NotNull Inventory inv, @NotNull Player player) {
        ItemStack atlas = getAtlasFromLookingLectern(player);
        if (atlas.isEmpty())
            atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player);
        Map<Integer, Pair<String,List<Integer>>> idsToCenters = new HashMap<>();
        Map<String, MapItemSavedData> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(player.getLevel(), atlas);
        for (Map.Entry<String, MapItemSavedData> state : mapInfos.entrySet()) {
            var id = MapAtlasesAccessUtils.getMapIntFromString(state.getKey());
            var centers = Arrays.asList(state.getValue().x, state.getValue().z);
            var dimStr = MapAtlasesAccessUtils.getMapStateDimKey(state.getValue());
            idsToCenters.put(id, new Pair<>(dimStr, centers));
        }
        var currentIds = MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(player.getLevel(), atlas);
        String centerMap = MapAtlasesAccessUtils.getActiveAtlasMapStateServer(currentIds, (ServerPlayer)player).getKey();
        int atlasScale = MapAtlasesAccessUtils.getAtlasBlockScale(player.getLevel(), atlas);
        return new MapAtlasesAtlasOverviewScreenHandler(syncId, inv, idsToCenters, atlas, centerMap, atlasScale);
    }

    public void writeScreenOpeningData(ServerPlayer serverPlayerEntity, FriendlyByteBuf packetByteBuf) {
        ItemStack atlas = getAtlasFromLookingLectern(serverPlayerEntity);
        if (atlas.isEmpty())
            atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(serverPlayerEntity);
        else
            sendPlayerLecternAtlasData(serverPlayerEntity, atlas);
        if (atlas.isEmpty()) return;
        var mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(serverPlayerEntity.getLevel(), atlas);
        var currentInfos = MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(serverPlayerEntity.getLevel(), atlas);
        String centerMap = MapAtlasesAccessUtils.getActiveAtlasMapStateServer(currentInfos, serverPlayerEntity).getKey();
        int atlasScale = MapAtlasesAccessUtils.getAtlasBlockScale(serverPlayerEntity.getLevel(), atlas);
        packetByteBuf.writeItem(atlas);
        packetByteBuf.writeUtf(centerMap);
        packetByteBuf.writeInt(atlasScale);
        packetByteBuf.writeInt(mapInfos.size());
        for (Map.Entry<String, MapItemSavedData> state : mapInfos.entrySet()) {
            packetByteBuf.writeInt(MapAtlasesAccessUtils.getMapIntFromString(state.getKey()));
            packetByteBuf.writeUtf(MapAtlasesAccessUtils.getMapStateDimKey(state.getValue()));
            packetByteBuf.writeInt(state.getValue().x);
            packetByteBuf.writeInt(state.getValue().z);
        }
    }

    public void openHandledAtlasScreen(Level world, Player player) {
       if (!world.isClientSide)
           NetworkHooks.openGui((ServerPlayer)player, new SimpleMenuProvider(this::createMenu, new TranslatableComponent(getDescriptionId())), buffer -> this.writeScreenOpeningData((ServerPlayer)player, buffer));
        world.playLocalSound(player.getX(), player.getY(), player.getZ(), MapAtlasesMod.ATLAS_OPEN_SOUND_EVENT, SoundSource.PLAYERS, 1.0f, 1.0f, false);
    }

    public ItemStack getAtlasFromLookingLectern(Player player) {
        HitResult h = player.pick(10, 1, false);
        if (h instanceof BlockHitResult blockHitResult) {
            BlockEntity e = player.getLevel().getBlockEntity(blockHitResult.getBlockPos());
            if (e instanceof LecternBlockEntity lecternBlockEntity) {
                ItemStack book = lecternBlockEntity.getBook();
                if (book.getItem() == MapAtlasesMod.MAP_ATLAS)
                    return book;
            }
        }
        return ItemStack.EMPTY;
    }

    private void sendPlayerLecternAtlasData(ServerPlayer serverPlayerEntity, ItemStack atlas) {
        // Send player all MapStates
        var states = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(serverPlayerEntity.getLevel(), atlas);
        for (var state : states.entrySet()) {
            state.getValue().getHoldingPlayer(serverPlayerEntity);
            MapAtlasesServerLifecycleEvents.relayMapStateSyncToPlayerClient(state, serverPlayerEntity);
        }
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null /*|| context.getLevel() == null || context.getItemInHand() == null || context.getClickedPos() == null*/)
            return super.useOn(context);
        BlockState blockState = context.getLevel().getBlockState(context.getClickedPos());
        if (blockState.is(Blocks.LECTERN)) {
            if (!LecternBlock.tryPlaceBook(context.getPlayer(), context.getLevel(), context.getClickedPos(), blockState, context.getItemInHand()))
                return InteractionResult.PASS;
            blockState = context.getLevel().getBlockState(context.getClickedPos());
            LecternBlock.resetBookState(context.getLevel(), context.getClickedPos(), blockState, true);
            context.getLevel().setBlockAndUpdate(context.getClickedPos(), blockState.setValue(HAS_ATLAS, true));
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        if (blockState.is(BlockTags.BANNERS)) {
            if (!context.getLevel().isClientSide) {
                Map<String, MapItemSavedData> currentDimMapInfos = MapAtlasesAccessUtils.getCurrentDimMapInfoFromAtlas(context.getLevel(), context.getItemInHand());
                MapItemSavedData mapState = MapAtlasesAccessUtils.getActiveAtlasMapStateServer(currentDimMapInfos, (ServerPlayer)context.getPlayer()).getValue();
                if (mapState == null)
                    return InteractionResult.FAIL;
                boolean didAdd = mapState.toggleBanner(context.getLevel(), context.getClickedPos());
                if (!didAdd)
                    return InteractionResult.FAIL;
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return super.useOn(context);
    }
}
