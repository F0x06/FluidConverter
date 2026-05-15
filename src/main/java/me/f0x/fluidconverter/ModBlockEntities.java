package me.f0x.fluidconverter;

import me.f0x.fluidconverter.blockentity.FluidConverterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FluidConverter.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidConverterBlockEntity>> FLUID_CONVERTER =
            REGISTER.register("fluid_converter",
                    () -> BlockEntityType.Builder.of(FluidConverterBlockEntity::new,
                            ModBlocks.FLUID_CONVERTER.get()).build(null));

    private ModBlockEntities() {}
}
