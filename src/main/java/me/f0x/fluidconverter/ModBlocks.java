package me.f0x.fluidconverter;

import me.f0x.fluidconverter.block.FluidConverterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(FluidConverter.MODID);

    public static final DeferredBlock<FluidConverterBlock> FLUID_CONVERTER =
            REGISTER.register("fluid_converter",
                    () -> new FluidConverterBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5f)
                            .requiresCorrectToolForDrops()));

    private ModBlocks() {}
}
