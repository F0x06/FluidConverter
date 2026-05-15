package me.f0x.fluidconverter.menu;

import me.f0x.fluidconverter.ModMenus;
import me.f0x.fluidconverter.blockentity.FluidConverterBlockEntity;
import me.f0x.fluidconverter.network.SyncLearnedRecipesPayload;
import me.f0x.fluidconverter.recipe.FluidConvertingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class FluidConverterAdminMenu extends AbstractContainerMenu {

    public static final int IMAGE_HEIGHT = 186;
    public static final int PLAYER_INV_Y = 104;
    public static final int HOTBAR_Y = 162;

    public static final int LEARN_INPUT_SLOT_X = 44;
    public static final int LEARN_OUTPUT_SLOT_X = 116;
    public static final int LEARN_SLOTS_Y = 22;

    public static final int RECIPE_LIST_HEADER_Y = 50;
    public static final int RECIPE_LIST_FIRST_ROW_Y = 62;
    public static final int RECIPE_LIST_ROW_HEIGHT = 10;
    public static final int RECIPE_LIST_MAX_ROWS = 3;
    public static final int DELETE_BTN_SIZE = 8;

    public static final int BTN_BACK = 200;

    private final FluidConverterBlockEntity be;
    private final ContainerLevelAccess access;
    private final SimpleContainer learnSlots = new SimpleContainer(2);

    private List<FluidConvertingRecipe> clientRecipes = Collections.emptyList();

    public FluidConverterAdminMenu(int id, Inventory playerInv, FluidConverterBlockEntity be) {
        super(ModMenus.FLUID_CONVERTER_ADMIN.get(), id);
        this.be = be;
        this.access = be == null ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        addSlot(new BucketOnlySlot(learnSlots, 0, LEARN_INPUT_SLOT_X, LEARN_SLOTS_Y));
        addSlot(new BucketOnlySlot(learnSlots, 1, LEARN_OUTPUT_SLOT_X, LEARN_SLOTS_Y));

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInv, col, 8 + col * 18, HOTBAR_Y));
    }

    public static FluidConverterAdminMenu clientCtor(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Level level = playerInv.player.level();
        FluidConverterBlockEntity be = level.getBlockEntity(pos) instanceof FluidConverterBlockEntity b ? b : null;
        return new FluidConverterAdminMenu(id, playerInv, be);
    }

    public FluidConverterBlockEntity blockEntity() { return be; }
    public SimpleContainer learnSlots() { return learnSlots; }

    public List<FluidConvertingRecipe> clientRecipes() { return clientRecipes; }

    public void setClientRecipes(List<FluidConvertingRecipe> recipes) {
        this.clientRecipes = List.copyOf(recipes);
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (be == null) return false;
        if (buttonId == BTN_BACK) {
            final BlockPos pos = be.getBlockPos();
            player.openMenu(be, pos);
            return true;
        }
        return false;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
            slot.setChanged();
            return stack.copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        for (int i = 0; i < learnSlots.getContainerSize(); i++) {
            ItemStack s = learnSlots.removeItemNoUpdate(i);
            if (!s.isEmpty()) player.getInventory().placeItemBackInInventory(s);
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (!player.hasPermissions(2) && !player.isCreative()) return false;
        return be != null && access.evaluate((lvl, pos) ->
                        lvl.getBlockEntity(pos) == be
                                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0,
                false);
    }

    private static final class BucketOnlySlot extends Slot {
        BucketOnlySlot(SimpleContainer c, int i, int x, int y) { super(c, i, x, y); }
        @Override public boolean mayPlace(@NotNull ItemStack stack) {
            return FluidUtil.getFluidContained(stack).isPresent();
        }
        @Override public int getMaxStackSize() { return 1; }
    }
}
