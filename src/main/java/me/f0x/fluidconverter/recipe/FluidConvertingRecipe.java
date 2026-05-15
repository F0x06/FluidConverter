package me.f0x.fluidconverter.recipe;

import me.f0x.fluidconverter.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

public record FluidConvertingRecipe(FluidStack input, FluidStack output, boolean reverse) implements Recipe<FluidRecipeInput> {

    public FluidConvertingRecipe(FluidStack input, FluidStack output) {
        this(input, output, true);
    }

    public static final MapCodec<FluidConvertingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            FluidStack.CODEC.fieldOf("input").forGetter(FluidConvertingRecipe::input),
            FluidStack.CODEC.fieldOf("output").forGetter(FluidConvertingRecipe::output),
            Codec.BOOL.optionalFieldOf("reverse", true).forGetter(FluidConvertingRecipe::reverse)
    ).apply(inst, FluidConvertingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidConvertingRecipe> STREAM_CODEC = StreamCodec.composite(
            FluidStack.STREAM_CODEC, FluidConvertingRecipe::input,
            FluidStack.STREAM_CODEC, FluidConvertingRecipe::output,
            ByteBufCodecs.BOOL, FluidConvertingRecipe::reverse,
            FluidConvertingRecipe::new
    );

    @Override
    public boolean matches(FluidRecipeInput in, Level level) {
        FluidStack provided = in.fluid();
        return FluidStack.isSameFluidSameComponents(input, provided)
                && provided.getAmount() >= input.getAmount();
    }

    public FluidStack resultFluid() {
        return output.copy();
    }

    @Override public ItemStack assemble(FluidRecipeInput in, HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.CONVERTING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.CONVERTING_TYPE.get(); }

    public static final class Serializer implements RecipeSerializer<FluidConvertingRecipe> {
        @Override public MapCodec<FluidConvertingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, FluidConvertingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
