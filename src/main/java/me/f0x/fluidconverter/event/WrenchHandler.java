package me.f0x.fluidconverter.event;

import me.f0x.fluidconverter.block.FluidConverterBlock;
import me.f0x.fluidconverter.blockentity.FluidConverterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class WrenchHandler {

    private static final TagKey<Item> WRENCH_C_SINGULAR = tag("c", "tools/wrench");
    private static final TagKey<Item> WRENCH_C_PLURAL = tag("c", "tools/wrenches");
    private static final TagKey<Item> WRENCH_C_TOP = tag("c", "wrenches");
    private static final TagKey<Item> WRENCH_FORGE_SINGULAR = tag("forge", "tools/wrench");
    private static final TagKey<Item> WRENCH_FORGE_PLURAL = tag("forge", "tools/wrenches");

    private WrenchHandler() {}

    private static TagKey<Item> tag(String namespace, String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    public static boolean isWrench(ItemStack stack) {
        return stack.is(WRENCH_C_SINGULAR)
                || stack.is(WRENCH_C_PLURAL)
                || stack.is(WRENCH_C_TOP)
                || stack.is(WRENCH_FORGE_SINGULAR)
                || stack.is(WRENCH_FORGE_PLURAL);
    }

    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidConverterBlock)) return;
        ItemStack stack = event.getItemStack();
        if (!isWrench(stack)) return;

        event.setCanceled(true);
        if (level.isClientSide) return;

        Player player = event.getEntity();
        if (player.isSecondaryUseActive()) {
            pickBlock(level, pos, state, player);
        } else if (level.getBlockEntity(pos) instanceof FluidConverterBlockEntity be) {
            be.rotateClockwise();
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.4f, 1.2f);
        }
    }

    private static void pickBlock(Level level, BlockPos pos, BlockState state, Player player) {
        ItemStack drop = new ItemStack(state.getBlock());
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            CompoundTag tag = be.saveWithId(level.registryAccess());
            drop.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        }
        level.removeBlockEntity(pos);
        level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4f, 1.6f);
        if (!player.getInventory().add(drop)) {
            player.drop(drop, false);
        }
    }
}
