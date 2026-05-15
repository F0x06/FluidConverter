package me.f0x.fluidconverter.network;

import me.f0x.fluidconverter.FluidConverter;
import me.f0x.fluidconverter.learned.LearnedRecipesStore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LearnRecipePayload(FluidStack input, FluidStack output, boolean reverse) implements CustomPacketPayload {

    public LearnRecipePayload(FluidStack input, FluidStack output) {
        this(input, output, true);
    }

    public static final Type<LearnRecipePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FluidConverter.MODID, "learn_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LearnRecipePayload> STREAM_CODEC = StreamCodec.composite(
            FluidStack.STREAM_CODEC, LearnRecipePayload::input,
            FluidStack.STREAM_CODEC, LearnRecipePayload::output,
            ByteBufCodecs.BOOL, LearnRecipePayload::reverse,
            LearnRecipePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LearnRecipePayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            if (!player.hasPermissions(2) && !player.isCreative()) {
                player.displayClientMessage(
                        Component.translatable("message.fluidconverter.learn.denied")
                                .withStyle(net.minecraft.ChatFormatting.RED),
                        false);
                return;
            }

            if (p.input.isEmpty() || p.output.isEmpty()) return;
            if (FluidStack.isSameFluidSameComponents(p.input, p.output)) return;

            LearnedRecipesStore store = LearnedRecipesStore.get();
            store.learn(p.input, p.output, p.reverse);

            String inName = pathOf(p.input);
            String outName = pathOf(p.output);
            player.displayClientMessage(
                    Component.translatable("message.fluidconverter.learn.success",
                            inName, outName,
                            String.valueOf(p.input.getAmount()),
                            String.valueOf(p.output.getAmount())),
                    false);

            PacketDistributor.sendToPlayer(player, new SyncLearnedRecipesPayload(store.all()));
        });
    }

    private static String pathOf(FluidStack s) {
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(s.getFluid());
        return id == null ? "?" : id.getPath();
    }
}
