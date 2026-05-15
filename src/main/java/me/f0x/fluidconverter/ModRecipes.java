package me.f0x.fluidconverter;

import me.f0x.fluidconverter.recipe.FluidConvertingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, FluidConverter.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, FluidConverter.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<FluidConvertingRecipe>> CONVERTING_TYPE =
            TYPES.register("converting", () -> new RecipeType<>() {
                private final String name = FluidConverter.MODID + ":converting";
                @Override public String toString() { return name; }
            });

    public static final DeferredHolder<RecipeSerializer<?>, FluidConvertingRecipe.Serializer> CONVERTING_SERIALIZER =
            SERIALIZERS.register("converting", FluidConvertingRecipe.Serializer::new);

    private ModRecipes() {}
}
