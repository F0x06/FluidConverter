package me.f0x.fluidconverter.network;

import me.f0x.fluidconverter.FluidConverter;
import me.f0x.fluidconverter.learned.LearnedRecipesStore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ForgetRecipePayload(FluidStack input, FluidStack output) implements CustomPacketPayload {

    public static final Type<ForgetRecipePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FluidConverter.MODID, "forget_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ForgetRecipePayload> STREAM_CODEC =
            StreamCodec.composite(
                    FluidStack.STREAM_CODEC, ForgetRecipePayload::input,
                    FluidStack.STREAM_CODEC, ForgetRecipePayload::output,
                    ForgetRecipePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ForgetRecipePayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!player.hasPermissions(2) && !player.isCreative()) {
                player.displayClientMessage(
                        Component.translatable("message.fluidconverter.forget.denied")
                                .withStyle(ChatFormatting.RED), false);
                return;
            }
            if (p.input.isEmpty() || p.output.isEmpty()) return;

            LearnedRecipesStore store = LearnedRecipesStore.get();
            boolean removed = store.forget(p.input, p.output);
            if (removed) {
                ResourceLocation inId = BuiltInRegistries.FLUID.getKey(p.input.getFluid());
                ResourceLocation outId = BuiltInRegistries.FLUID.getKey(p.output.getFluid());
                String inName = inId == null ? "?" : inId.toString();
                String outName = outId == null ? "?" : outId.toString();
                player.displayClientMessage(
                        Component.translatable("message.fluidconverter.forget.success", inName, outName)
                                .withStyle(ChatFormatting.YELLOW), false);
            }
            PacketDistributor.sendToPlayer(player, new SyncLearnedRecipesPayload(store.all()));
        });
    }
}
