package me.f0x.fluidconverter.menu;

import me.f0x.fluidconverter.ModMenus;
import me.f0x.fluidconverter.blockentity.FluidConverterBlockEntity;
import me.f0x.fluidconverter.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidConverterMenu extends AbstractContainerMenu {

    public static final int IMAGE_HEIGHT = 186;
    public static final int PLAYER_INV_Y = 104;
    public static final int HOTBAR_Y = 162;

    public static final int BTN_OPEN_ADMIN = 100;
    public static final int BTN_PREV_OUTPUT = 101;
    public static final int BTN_NEXT_OUTPUT = 102;
    public static final int BTN_TOGGLE_PAUSE = 103;
    public static final int BTN_DRAIN_OUTPUT = 104;
    public static final int BTN_DRAIN_INPUT = 105;
    public static final int BTN_OPEN_SIDE_CONFIG = 106;

    private final FluidConverterBlockEntity be;
    private final ContainerLevelAccess access;
    private final boolean canAdmin;

    public FluidConverterMenu(int id, Inventory playerInv, FluidConverterBlockEntity be) {
        super(ModMenus.FLUID_CONVERTER.get(), id);
        this.be = be;
        this.access = be == null ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.canAdmin = Config.adminMenuEnabled()
                && (playerInv.player.hasPermissions(2) || playerInv.player.isCreative());
        addPlayerInventory(playerInv);
    }

    public static FluidConverterMenu clientCtor(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Level level = playerInv.player.level();
        FluidConverterBlockEntity be = level.getBlockEntity(pos) instanceof FluidConverterBlockEntity b ? b : null;
        return new FluidConverterMenu(id, playerInv, be);
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, HOTBAR_Y));
    }

    public FluidConverterBlockEntity blockEntity() { return be; }
    public boolean canAdmin() { return canAdmin; }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (be == null) return false;

        if (buttonId == BTN_OPEN_ADMIN) {
            if (!Config.adminMenuEnabled()) {
                return false;
            }
            if (!player.hasPermissions(2) && !player.isCreative()) {
                player.displayClientMessage(
                        Component.translatable("message.fluidconverter.admin.denied")
                                .withStyle(net.minecraft.ChatFormatting.RED),
                        false);
                return false;
            }
            final FluidConverterBlockEntity beRef = be;
            final BlockPos pos = be.getBlockPos();
            player.openMenu(new MenuProvider() {
                @Override public @NotNull Component getDisplayName() {
                    return Component.translatable("gui.fluidconverter.admin_title");
                }
                @Nullable @Override
                public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
                    return new FluidConverterAdminMenu(id, inv, beRef);
                }
            }, pos);
            return true;
        }
        if (buttonId == BTN_PREV_OUTPUT) {
            be.cycleSelectedOutput(-1);
            return true;
        }
        if (buttonId == BTN_NEXT_OUTPUT) {
            be.cycleSelectedOutput(+1);
            return true;
        }
        if (buttonId == BTN_TOGGLE_PAUSE) {
            be.togglePause();
            return true;
        }
        if (buttonId == BTN_DRAIN_OUTPUT) {
            be.drainOutput();
            return true;
        }
        if (buttonId == BTN_DRAIN_INPUT) {
            be.drainInput();
            return true;
        }
        if (buttonId == BTN_OPEN_SIDE_CONFIG) {
            final FluidConverterBlockEntity beRef = be;
            final BlockPos pos = be.getBlockPos();
            player.openMenu(new MenuProvider() {
                @Override public @NotNull Component getDisplayName() {
                    return Component.translatable("gui.fluidconverter.sides_title");
                }
                @Nullable @Override
                public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
                    return new FluidConverterSideConfigMenu(id, inv, beRef);
                }
            }, pos);
            return true;
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
