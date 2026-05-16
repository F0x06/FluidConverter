package me.f0x.fluidconverter.blockentity;

import me.f0x.fluidconverter.ModBlockEntities;
import me.f0x.fluidconverter.ModRecipes;
import me.f0x.fluidconverter.client.model.FluidConverterModelProps;
import me.f0x.fluidconverter.config.Config;
import me.f0x.fluidconverter.learned.LearnedRecipesStore;
import me.f0x.fluidconverter.menu.FluidConverterMenu;
import me.f0x.fluidconverter.recipe.FluidConvertingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidConverterBlockEntity extends BlockEntity implements MenuProvider {

    private final FluidTank inputTank;
    private final FluidTank outputTank;
    private final FCEnergyStorage energyStorage;
    private final IEnergyStorage receiveOnlyEnergyView;
    private Fluid selectedOutput = Fluids.EMPTY;
    private List<Fluid> availableOutputs = List.of();
    private boolean paused = false;
    private RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    private final EnumMap<Direction, SideConfig> sideConfig = defaultSideConfig();

    private static EnumMap<Direction, SideConfig> defaultSideConfig() {
        EnumMap<Direction, SideConfig> m = new EnumMap<>(Direction.class);
        m.put(Direction.UP, SideConfig.INPUT);
        m.put(Direction.DOWN, SideConfig.OUTPUT);
        m.put(Direction.NORTH, SideConfig.INPUT);
        m.put(Direction.SOUTH, SideConfig.INPUT);
        m.put(Direction.EAST, SideConfig.INPUT);
        m.put(Direction.WEST, SideConfig.INPUT);
        return m;
    }

    public FluidConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_CONVERTER.get(), pos, state);
        int cap = Config.tankCapacityMb();
        this.inputTank = new FluidTank(cap) {
            @Override
            protected void onContentsChanged() {
                setChanged();
                syncToClient();
            }
        };
        this.outputTank = new FluidTank(cap) {
            @Override
            protected void onContentsChanged() {
                setChanged();
                syncToClient();
            }

            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.getFluid() == selectedOutput;
            }
        };
        this.energyStorage = new FCEnergyStorage(
                Config.energyCapacityFe(),
                Integer.MAX_VALUE,
                this::onEnergyChanged);
        this.receiveOnlyEnergyView = new IEnergyStorage() {
            @Override public int receiveEnergy(int maxReceive, boolean simulate) {
                return energyStorage.receiveEnergy(maxReceive, simulate);
            }
            @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
            @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
            @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
            @Override public boolean canExtract() { return false; }
            @Override public boolean canReceive() { return true; }
        };
    }

    private void onEnergyChanged() {
        setChanged();
        syncToClient();
    }

    public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }

    public IEnergyStorage getEnergyHandler(@Nullable Direction side) {
        return receiveOnlyEnergyView;
    }

    public FluidTank getInputTank() { return inputTank; }
    public FluidTank getOutputTank() { return outputTank; }

    public Fluid getSelectedOutput() { return selectedOutput; }

    public List<Fluid> getAvailableOutputs() { return availableOutputs; }

    public void setSelectedOutput(Fluid fluid) {
        if (fluid == null) fluid = Fluids.EMPTY;
        if (fluid == selectedOutput) return;
        if (fluid != Fluids.EMPTY && !availableOutputs.contains(fluid)) return;
        this.selectedOutput = fluid;
        setChanged();
        syncToClient();
    }

    public boolean isPaused() { return paused; }

    public RedstoneMode getRedstoneMode() { return redstoneMode; }

    public void setRedstoneMode(RedstoneMode mode) {
        if (mode == null || mode == redstoneMode) return;
        this.redstoneMode = mode;
        setChanged();
        syncToClient();
    }

    public void cycleRedstoneMode() {
        setRedstoneMode(redstoneMode.next());
    }

    public void togglePause() {
        paused = !paused;
        if (paused) recipeProgressTicks = 0;
        setChanged();
        syncToClient();
    }

    public void drainOutput() {
        if (outputTank.getFluid().isEmpty()) return;
        outputTank.setFluid(FluidStack.EMPTY);
        setChanged();
        syncToClient();
    }

    public void drainInput() {
        if (inputTank.getFluid().isEmpty()) return;
        inputTank.setFluid(FluidStack.EMPTY);
        recipeProgressTicks = 0;
        setChanged();
        syncToClient();
    }

    public void cycleSelectedOutput(int direction) {
        if (availableOutputs.size() < 2) return;
        int idx = availableOutputs.indexOf(selectedOutput);
        if (idx < 0) idx = 0;
        int n = availableOutputs.size();
        int next = ((idx + direction) % n + n) % n;
        setSelectedOutput(availableOutputs.get(next));
    }

    public Optional<FluidConvertingRecipe> findCurrentRecipe() {
        return Optional.ofNullable(activeRecipe);
    }

    private void setActiveRecipe(@Nullable FluidConvertingRecipe next) {
        if (sameRecipe(activeRecipe, next)) return;
        activeRecipe = next;
        setChanged();
        syncToClient();
    }

    private static boolean sameRecipe(@Nullable FluidConvertingRecipe a, @Nullable FluidConvertingRecipe b) {
        if (a == null || b == null) return a == b;
        return FluidStack.isSameFluidSameComponents(a.input(), b.input())
                && a.input().getAmount() == b.input().getAmount()
                && FluidStack.isSameFluidSameComponents(a.output(), b.output())
                && a.output().getAmount() == b.output().getAmount();
    }

    public int getRecipeProgressTicks() {
        return recipeProgressTicks;
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        if (side == null) return new CombinedTanksView(inputTank, outputTank);
        SideConfig cfg = sideConfig.getOrDefault(side, SideConfig.NONE);
        return switch (cfg) {
            case INPUT -> new InsertOnly(inputTank);
            case OUTPUT -> new ExtractOnly(outputTank);
            case NONE -> null;
        };
    }

    public SideConfig getSideConfig(Direction dir) {
        return sideConfig.getOrDefault(dir, SideConfig.NONE);
    }

    public Map<Direction, SideConfig> getSideConfigMap() {
        return new EnumMap<>(sideConfig);
    }

    public void cycleSide(Direction dir) {
        if (dir == null) return;
        SideConfig cur = sideConfig.getOrDefault(dir, SideConfig.NONE);
        sideConfig.put(dir, cur.next());
        setChanged();
        syncToClient();
        if (level != null && !level.isClientSide) {
            invalidateCapabilities();
            notifyNeighbours();
        }
    }

    public void resetAllSides() {
        for (Direction d : Direction.values()) {
            sideConfig.put(d, SideConfig.NONE);
        }
        setChanged();
        syncToClient();
        if (level != null && !level.isClientSide) {
            invalidateCapabilities();
            notifyNeighbours();
        }
    }

    public void rotateClockwise() {
        SideConfig n = sideConfig.getOrDefault(Direction.NORTH, SideConfig.NONE);
        SideConfig e = sideConfig.getOrDefault(Direction.EAST, SideConfig.NONE);
        SideConfig s = sideConfig.getOrDefault(Direction.SOUTH, SideConfig.NONE);
        SideConfig w = sideConfig.getOrDefault(Direction.WEST, SideConfig.NONE);
        sideConfig.put(Direction.EAST, n);
        sideConfig.put(Direction.SOUTH, e);
        sideConfig.put(Direction.WEST, s);
        sideConfig.put(Direction.NORTH, w);
        setChanged();
        syncToClient();
        if (level != null && !level.isClientSide) {
            invalidateCapabilities();
            notifyNeighbours();
        }
    }

    private void notifyNeighbours() {
        if (level == null) return;
        level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
    }

    @Override
    public @NotNull ModelData getModelData() {
        SideConfig[] arr = new SideConfig[6];
        for (Direction d : Direction.values()) {
            arr[d.ordinal()] = sideConfig.getOrDefault(d, SideConfig.NONE);
        }
        return ModelData.builder().with(FluidConverterModelProps.SIDES, arr).build();
    }

    private int recipeProgressTicks = 0;
    @Nullable private FluidConvertingRecipe activeRecipe = null;

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        FluidStack inStack = inputTank.getFluid();
        if (inStack.isEmpty()) {
            recipeProgressTicks = 0;
            setActiveRecipe(null);
            updateAvailableOutputs(List.of());
            return;
        }

        if (activeRecipe != null) {
            syncToClient();
        }

        List<FluidConvertingRecipe> typeMatches = allMatchingRecipesByType(level, inStack);
        List<Fluid> newAvail = new ArrayList<>();
        for (FluidConvertingRecipe r : typeMatches) {
            Fluid f = r.output().getFluid();
            if (!newAvail.contains(f)) newAvail.add(f);
        }
        updateAvailableOutputs(newAvail);

        if (selectedOutput != Fluids.EMPTY && !newAvail.contains(selectedOutput)) {
            selectedOutput = Fluids.EMPTY;
            outputTank.setFluid(FluidStack.EMPTY);
            setChanged();
            syncToClient();
        }
        if (selectedOutput == Fluids.EMPTY && !newAvail.isEmpty()) {
            selectedOutput = newAvail.get(0);
            setChanged();
            syncToClient();
        }

        Optional<FluidConvertingRecipe> match = Optional.empty();
        for (FluidConvertingRecipe r : typeMatches) {
            if (r.output().getFluid() == selectedOutput) { match = Optional.of(r); break; }
        }

        setActiveRecipe(match.orElse(null));

        if (paused) {
            recipeProgressTicks = 0;
            return;
        }

        if (!redstoneMode.shouldRun(level.hasNeighborSignal(pos))) {
            recipeProgressTicks = 0;
            return;
        }

        if (match.isPresent()) {
            FluidConvertingRecipe r = match.get();
            if (inStack.getAmount() < r.input().getAmount()) {
                recipeProgressTicks = 0;
                return;
            }
            FluidStack proposed = r.resultFluid();
            int simulated = fillOutputBypassValidator(proposed, IFluidHandler.FluidAction.SIMULATE);
            if (simulated < proposed.getAmount()) {
                recipeProgressTicks = 0;
                return;
            }
            int costPerTick = Config.energyEnabled()
                    ? Config.energyCostPerMb() * Config.conversionRateMbPerTick()
                    : 0;
            if (costPerTick > 0 && energyStorage.getEnergyStored() < costPerTick) {
                return;
            }
            if (costPerTick > 0) {
                energyStorage.extractEnergy(costPerTick, false);
            }
            recipeProgressTicks++;
            if (recipeProgressTicks > effectiveTicksFor(r) + 2) {
                inputTank.drain(r.input().getAmount(), IFluidHandler.FluidAction.EXECUTE);
                fillOutputBypassValidator(proposed, IFluidHandler.FluidAction.EXECUTE);
                recipeProgressTicks = 0;
            }
            return;
        }
        recipeProgressTicks = 0;
    }

    public static int effectiveTicksFor(FluidConvertingRecipe r) {
        int rate = Config.conversionRateMbPerTick();
        return Math.max(1, (r.input().getAmount() + rate - 1) / rate);
    }

    private List<FluidConvertingRecipe> allMatchingRecipesByType(ServerLevel level, FluidStack inStack) {
        List<FluidConvertingRecipe> all = new ArrayList<>();
        for (RecipeHolder<FluidConvertingRecipe> h : level.getRecipeManager().getAllRecipesFor(ModRecipes.CONVERTING_TYPE.get())) {
            all.add(h.value());
        }
        all.addAll(LearnedRecipesStore.get().all());

        List<FluidConvertingRecipe> out = new ArrayList<>();
        for (FluidConvertingRecipe r : all) {
            if (FluidStack.isSameFluidSameComponents(r.input(), inStack)) {
                out.add(r);
            }
        }
        for (FluidConvertingRecipe r : all) {
            if (!r.reverse()) continue;
            if (!FluidStack.isSameFluidSameComponents(r.output(), inStack)) continue;
            FluidConvertingRecipe reversed = new FluidConvertingRecipe(
                    r.output().copy(), r.input().copy(), true);
            boolean duplicate = false;
            for (FluidConvertingRecipe existing : out) {
                if (FluidStack.isSameFluidSameComponents(existing.input(), reversed.input())
                        && FluidStack.isSameFluidSameComponents(existing.output(), reversed.output())) {
                    duplicate = true; break;
                }
            }
            if (!duplicate) out.add(reversed);
        }
        return out;
    }

    private void updateAvailableOutputs(List<Fluid> newOuts) {
        if (newOuts.equals(availableOutputs)) return;
        availableOutputs = List.copyOf(newOuts);
        setChanged();
        syncToClient();
    }

    private int fillOutputBypassValidator(FluidStack stack, IFluidHandler.FluidAction action) {
        FluidStack existing = outputTank.getFluid();
        if (!existing.isEmpty() && !FluidStack.isSameFluidSameComponents(existing, stack)) return 0;
        int space = outputTank.getCapacity() - existing.getAmount();
        int amount = Math.min(space, stack.getAmount());
        if (amount <= 0) return 0;
        if (action.execute()) {
            if (existing.isEmpty()) {
                outputTank.setFluid(stack.copyWithAmount(amount));
            } else {
                existing.grow(amount);
                outputTank.setFluid(existing);
            }
        }
        return amount;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(selectedOutput);
        tag.putString("SelectedOutput", id == null ? "" : id.toString());
        ListTag outs = new ListTag();
        for (Fluid f : availableOutputs) {
            ResourceLocation fid = BuiltInRegistries.FLUID.getKey(f);
            if (fid != null) outs.add(StringTag.valueOf(fid.toString()));
        }
        tag.put("AvailableOutputs", outs);
        if (activeRecipe != null) {
            CompoundTag rt = new CompoundTag();
            rt.put("Input", activeRecipe.input().save(registries, new CompoundTag()));
            rt.put("Output", activeRecipe.output().save(registries, new CompoundTag()));
            tag.put("ActiveRecipe", rt);
        }
        tag.putInt("Progress", recipeProgressTicks);
        tag.putBoolean("Paused", paused);
        tag.putString("RedstoneMode", redstoneMode.name());
        tag.putInt("Energy", energyStorage.getEnergyStored());
        CompoundTag sides = new CompoundTag();
        for (Direction d : Direction.values()) {
            sides.putString(d.getName(), sideConfig.getOrDefault(d, SideConfig.NONE).name());
        }
        tag.put("SideConfig", sides);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        EnumMap<Direction, SideConfig> sidesBefore = new EnumMap<>(sideConfig);
        super.loadAdditional(tag, registries);
        inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
        outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
        String sel = tag.getString("SelectedOutput");
        if (sel.isEmpty()) {
            selectedOutput = Fluids.EMPTY;
        } else {
            ResourceLocation rl = ResourceLocation.tryParse(sel);
            selectedOutput = rl == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(rl);
        }
        List<Fluid> outs = new ArrayList<>();
        ListTag outsTag = tag.getList("AvailableOutputs", Tag.TAG_STRING);
        for (int i = 0; i < outsTag.size(); i++) {
            ResourceLocation rl = ResourceLocation.tryParse(outsTag.getString(i));
            if (rl != null) {
                Fluid f = BuiltInRegistries.FLUID.get(rl);
                if (f != Fluids.EMPTY) outs.add(f);
            }
        }
        availableOutputs = List.copyOf(outs);
        if (tag.contains("ActiveRecipe")) {
            CompoundTag rt = tag.getCompound("ActiveRecipe");
            FluidStack in = FluidStack.parseOptional(registries, rt.getCompound("Input"));
            FluidStack out = FluidStack.parseOptional(registries, rt.getCompound("Output"));
            if (!in.isEmpty() && !out.isEmpty()) {
                activeRecipe = new FluidConvertingRecipe(in, out);
            } else {
                activeRecipe = null;
            }
        } else {
            activeRecipe = null;
        }
        recipeProgressTicks = tag.getInt("Progress");
        paused = tag.getBoolean("Paused");
        redstoneMode = tag.contains("RedstoneMode", Tag.TAG_STRING)
                ? RedstoneMode.fromName(tag.getString("RedstoneMode"))
                : RedstoneMode.IGNORED;
        energyStorage.setEnergyDirect(tag.getInt("Energy"));
        if (tag.contains("SideConfig", Tag.TAG_COMPOUND)) {
            CompoundTag sides = tag.getCompound("SideConfig");
            for (Direction d : Direction.values()) {
                if (sides.contains(d.getName(), Tag.TAG_STRING)) {
                    sideConfig.put(d, SideConfig.fromName(sides.getString(d.getName())));
                }
            }
        }
        if (level != null && level.isClientSide && !sidesBefore.equals(sideConfig)) {
            requestModelDataUpdate();
            me.f0x.fluidconverter.client.ClientChunkRefresh.refreshSection(getBlockPos());
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.fluidconverter.fluid_converter");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new FluidConverterMenu(containerId, playerInventory, this);
    }

    private static class FCEnergyStorage extends EnergyStorage {
        private final Runnable onChanged;

        FCEnergyStorage(int capacity, int maxReceive, Runnable onChanged) {
            super(capacity, maxReceive, Integer.MAX_VALUE, 0);
            this.onChanged = onChanged;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int r = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && r > 0) onChanged.run();
            return r;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int r = super.extractEnergy(maxExtract, simulate);
            if (!simulate && r > 0) onChanged.run();
            return r;
        }

        void setEnergyDirect(int value) {
            this.energy = Math.max(0, Math.min(this.capacity, value));
        }
    }

    private record InsertOnly(FluidTank tank) implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull FluidStack getFluidInTank(int t) { return tank.getFluid(); }
        @Override public int getTankCapacity(int t) { return tank.getCapacity(); }
        @Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return tank.isFluidValid(s); }
        @Override public int fill(FluidStack r, FluidAction a) { return tank.fill(r, a); }
        @Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return FluidStack.EMPTY; }
        @Override public @NotNull FluidStack drain(int amt, FluidAction a) { return FluidStack.EMPTY; }
    }

    private record ExtractOnly(FluidTank tank) implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull FluidStack getFluidInTank(int t) { return tank.getFluid(); }
        @Override public int getTankCapacity(int t) { return tank.getCapacity(); }
        @Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return false; }
        @Override public int fill(FluidStack r, FluidAction a) { return 0; }
        @Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return tank.drain(r, a); }
        @Override public @NotNull FluidStack drain(int amt, FluidAction a) { return tank.drain(amt, a); }
    }

    private record CombinedTanksView(FluidTank in, FluidTank out) implements IFluidHandler {
        @Override public int getTanks() { return 2; }
        @Override public @NotNull FluidStack getFluidInTank(int t) { return t == 0 ? in.getFluid() : out.getFluid(); }
        @Override public int getTankCapacity(int t) { return t == 0 ? in.getCapacity() : out.getCapacity(); }
        @Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return t == 0 ? in.isFluidValid(s) : out.isFluidValid(s); }
        @Override public int fill(FluidStack r, FluidAction a) { return in.fill(r, a); }
        @Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return out.drain(r, a); }
        @Override public @NotNull FluidStack drain(int amt, FluidAction a) { return out.drain(amt, a); }
    }
}
