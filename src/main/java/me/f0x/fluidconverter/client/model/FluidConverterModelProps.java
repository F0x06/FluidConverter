package me.f0x.fluidconverter.client.model;

import me.f0x.fluidconverter.blockentity.SideConfig;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public final class FluidConverterModelProps {
    public static final ModelProperty<SideConfig[]> SIDES = new ModelProperty<>();

    private FluidConverterModelProps() {}
}
