package me.f0x.fluidconverter.menu;

import me.f0x.fluidconverter.ModMenus;
import me.f0x.fluidconverter.blockentity.FluidConverterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class FluidConverterSideConfigMenu extends AbstractContainerMenu {

    public static final int IMAGE_HEIGHT = 186;
    public static final int PLAYER_INV_Y = 104;
    public static final int HOTBAR_Y = 162;

    public static final int BTN_BACK = 200;
    public static final int BTN_CYCLE_BASE = 210;

    private final FluidConverterBlockEntity be;
    private final ContainerLevelAccess access;

    public FluidConverterSideConfigMenu(int id, Inventory playerInv, FluidConverterBlockEntity be) {
        super(ModMenus.FLUID_CONVERTER_SIDE_CONFIG.get(), id);
        this.be = be;
        this.access = be == null ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInv, col, 8 + col * 18, HOTBAR_Y));
    }

    public static FluidConverterSideConfigMenu clientCtor(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Level level = playerInv.player.level();
        FluidConverterBlockEntity be = level.getBlockEntity(pos) instanceof FluidConverterBlockEntity b ? b : null;
        return new FluidConverterSideConfigMenu(id, playerInv, be);
    }

    public FluidConverterBlockEntity blockEntity() { return be; }

    public static int buttonIdFor(Direction dir) {
        return BTN_CYCLE_BASE + dir.ordinal();
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (be == null) return false;
        if (buttonId == BTN_BACK) {
            player.openMenu(be, be.getBlockPos());
            return true;
        }
        for (Direction d : Direction.values()) {
            if (buttonId == buttonIdFor(d)) {
                be.cycleSide(d);
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return be != null && access.evaluate((lvl, pos) ->
                        lvl.getBlockEntity(pos) == be
                                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0,
                false);
    }
}
