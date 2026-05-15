package me.f0x.fluidconverter;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(FluidConverter.MODID);

    public static final DeferredItem<BlockItem> FLUID_CONVERTER_ITEM =
            REGISTER.registerSimpleBlockItem(ModBlocks.FLUID_CONVERTER, new Item.Properties());

    private ModItems() {}
}
