package me.f0x.fluidconverter.network;

import me.f0x.fluidconverter.FluidConverter;
import me.f0x.fluidconverter.learned.LearnedRecipesStore;
import me.f0x.fluidconverter.menu.FluidConverterAdminMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestLearnedRecipesPayload() implements CustomPacketPayload {

    public static final RequestLearnedRecipesPayload INSTANCE = new RequestLearnedRecipesPayload();

    public static final Type<RequestLearnedRecipesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FluidConverter.MODID, "request_learned"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestLearnedRecipesPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RequestLearnedRecipesPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof FluidConverterAdminMenu)) return;
            PacketDistributor.sendToPlayer(player, new SyncLearnedRecipesPayload(
                    LearnedRecipesStore.get().all()));
        });
    }
}
