package me.f0x.fluidconverter;

import me.f0x.fluidconverter.menu.FluidConverterAdminMenu;
import me.f0x.fluidconverter.menu.FluidConverterMenu;
import me.f0x.fluidconverter.menu.FluidConverterSideConfigMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER =
            DeferredRegister.create(Registries.MENU, FluidConverter.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<FluidConverterMenu>> FLUID_CONVERTER =
            REGISTER.register("fluid_converter",
                    () -> IMenuTypeExtension.create(FluidConverterMenu::clientCtor));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidConverterAdminMenu>> FLUID_CONVERTER_ADMIN =
            REGISTER.register("fluid_converter_admin",
                    () -> IMenuTypeExtension.create(FluidConverterAdminMenu::clientCtor));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidConverterSideConfigMenu>> FLUID_CONVERTER_SIDE_CONFIG =
            REGISTER.register("fluid_converter_side_config",
                    () -> IMenuTypeExtension.create(FluidConverterSideConfigMenu::clientCtor));

    private ModMenus() {}
}
