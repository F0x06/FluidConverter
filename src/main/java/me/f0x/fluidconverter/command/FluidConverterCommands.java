package me.f0x.fluidconverter.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.f0x.fluidconverter.learned.LearnedRecipesStore;
import me.f0x.fluidconverter.network.SyncLearnedRecipesPayload;
import me.f0x.fluidconverter.recipe.FluidConvertingRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class FluidConverterCommands {

    private static final SimpleCommandExceptionType UNKNOWN_FLUID =
            new SimpleCommandExceptionType(Component.literal("Unknown fluid"));
    private static final SimpleCommandExceptionType EMPTY_FLUID =
            new SimpleCommandExceptionType(Component.literal("Fluid cannot be 'minecraft:empty'"));
    private static final SimpleCommandExceptionType SAME_FLUID =
            new SimpleCommandExceptionType(Component.literal("Input and output must differ"));

    private static final SuggestionProvider<CommandSourceStack> FLUID_SUGGESTIONS =
            (ctx, b) -> SharedSuggestionProvider.suggestResource(
                    BuiltInRegistries.FLUID.keySet().stream()
                            .filter(id -> !id.equals(BuiltInRegistries.FLUID.getKey(Fluids.EMPTY))),
                    b);

    private static final SuggestionProvider<CommandSourceStack> LEARNED_INPUT_SUGGESTIONS =
            (ctx, b) -> SharedSuggestionProvider.suggestResource(
                    LearnedRecipesStore.get().all().stream()
                            .map(r -> BuiltInRegistries.FLUID.getKey(r.input().getFluid()))
                            .filter(id -> id != null)
                            .distinct(),
                    b);

    private static final SuggestionProvider<CommandSourceStack> LEARNED_OUTPUT_SUGGESTIONS =
            (ctx, b) -> {
                ResourceLocation inId;
                try {
                    inId = ResourceLocationArgument.getId(ctx, "inFluid");
                } catch (Exception e) {
                    inId = null;
                }
                final ResourceLocation filterIn = inId;
                return SharedSuggestionProvider.suggestResource(
                        LearnedRecipesStore.get().all().stream()
                                .filter(r -> filterIn == null
                                        || filterIn.equals(BuiltInRegistries.FLUID.getKey(r.input().getFluid())))
                                .map(r -> BuiltInRegistries.FLUID.getKey(r.output().getFluid()))
                                .filter(id -> id != null)
                                .distinct(),
                        b);
            };

    private FluidConverterCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("fluidconverter")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("recipe")
                                .then(Commands.literal("list")
                                        .executes(FluidConverterCommands::list))
                                .then(Commands.literal("learn")
                                        .then(Commands.argument("inFluid", ResourceLocationArgument.id())
                                                .suggests(FLUID_SUGGESTIONS)
                                                .then(Commands.argument("inAmount", IntegerArgumentType.integer(1))
                                                        .then(Commands.argument("outFluid", ResourceLocationArgument.id())
                                                                .suggests(FLUID_SUGGESTIONS)
                                                                .then(Commands.argument("outAmount", IntegerArgumentType.integer(1))
                                                                        .executes(FluidConverterCommands::learn)
                                                                        .then(Commands.argument("reverse", BoolArgumentType.bool())
                                                                                .executes(FluidConverterCommands::learn)))))))
                                .then(Commands.literal("forget")
                                        .then(Commands.argument("inFluid", ResourceLocationArgument.id())
                                                .suggests(LEARNED_INPUT_SUGGESTIONS)
                                                .then(Commands.argument("outFluid", ResourceLocationArgument.id())
                                                        .suggests(LEARNED_OUTPUT_SUGGESTIONS)
                                                        .executes(FluidConverterCommands::forget))))
                                .then(Commands.literal("clear")
                                        .executes(FluidConverterCommands::clear))));
    }

    private static int list(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        List<FluidConvertingRecipe> all = LearnedRecipesStore.get().all();
        CommandSourceStack src = ctx.getSource();
        if (all.isEmpty()) {
            src.sendSuccess(() -> Component.literal("No learned recipes.").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Learned recipes (" + all.size() + "):")
                .withStyle(ChatFormatting.YELLOW), false);
        for (FluidConvertingRecipe r : all) {
            ResourceLocation in = BuiltInRegistries.FLUID.getKey(r.input().getFluid());
            ResourceLocation out = BuiltInRegistries.FLUID.getKey(r.output().getFluid());
            src.sendSuccess(() -> Component.literal(
                    " - " + in + " x" + r.input().getAmount()
                            + (r.reverse() ? " <-> " : " -> ")
                            + out + " x" + r.output().getAmount()), false);
        }
        return all.size();
    }

    private static int learn(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Fluid in = resolveFluid(ResourceLocationArgument.getId(ctx, "inFluid"));
        int inAmt = IntegerArgumentType.getInteger(ctx, "inAmount");
        Fluid out = resolveFluid(ResourceLocationArgument.getId(ctx, "outFluid"));
        int outAmt = IntegerArgumentType.getInteger(ctx, "outAmount");
        boolean reverse = true;
        try {
            reverse = BoolArgumentType.getBool(ctx, "reverse");
        } catch (IllegalArgumentException ignored) {
        }
        if (in == out) throw SAME_FLUID.create();

        FluidStack inStack = new FluidStack(in, inAmt);
        FluidStack outStack = new FluidStack(out, outAmt);
        LearnedRecipesStore.get().learn(inStack, outStack, reverse);
        broadcastSync(ctx.getSource());

        ResourceLocation inId = BuiltInRegistries.FLUID.getKey(in);
        ResourceLocation outId = BuiltInRegistries.FLUID.getKey(out);
        boolean finalReverse = reverse;
        ctx.getSource().sendSuccess(() -> Component.literal(
                        "Learned " + inId + " x" + inAmt + " -> " + outId + " x" + outAmt
                                + (finalReverse ? " (reversible)" : " (one-way)"))
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int forget(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Fluid in = resolveFluid(ResourceLocationArgument.getId(ctx, "inFluid"));
        Fluid out = resolveFluid(ResourceLocationArgument.getId(ctx, "outFluid"));

        FluidStack inStack = new FluidStack(in, 1);
        FluidStack outStack = new FluidStack(out, 1);
        boolean removed = LearnedRecipesStore.get().forget(inStack, outStack);
        if (!removed) {
            ctx.getSource().sendFailure(Component.literal("No matching recipe."));
            return 0;
        }
        broadcastSync(ctx.getSource());

        ResourceLocation inId = BuiltInRegistries.FLUID.getKey(in);
        ResourceLocation outId = BuiltInRegistries.FLUID.getKey(out);
        ctx.getSource().sendSuccess(() -> Component.literal(
                        "Forgot " + inId + " -> " + outId)
                .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int clear(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        int n = LearnedRecipesStore.get().clear();
        broadcastSync(ctx.getSource());
        ctx.getSource().sendSuccess(() -> Component.literal("Cleared " + n + " recipe(s).")
                .withStyle(ChatFormatting.YELLOW), true);
        return n;
    }

    private static Fluid resolveFluid(ResourceLocation id) throws CommandSyntaxException {
        Fluid f = BuiltInRegistries.FLUID.getOptional(id).orElseThrow(UNKNOWN_FLUID::create);
        if (f == Fluids.EMPTY) throw EMPTY_FLUID.create();
        return f;
    }

    private static void broadcastSync(CommandSourceStack src) {
        if (src.getServer() == null) return;
        List<FluidConvertingRecipe> snapshot = LearnedRecipesStore.get().all();
        for (var player : src.getServer().getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, new SyncLearnedRecipesPayload(snapshot));
        }
    }
}
