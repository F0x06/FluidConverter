package me.f0x.fluidconverter.network;

import me.f0x.fluidconverter.FluidConverter;
import me.f0x.fluidconverter.blockentity.FluidConverterBlockEntity;
import me.f0x.fluidconverter.blockentity.RedstoneMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetRedstoneModePayload(BlockPos pos, byte modeOrdinal) implements CustomPacketPayload {

    public static final Type<SetRedstoneModePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FluidConverter.MODID, "set_redstone_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetRedstoneModePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetRedstoneModePayload::pos,
                    ByteBufCodecs.BYTE, SetRedstoneModePayload::modeOrdinal,
                    SetRedstoneModePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SetRedstoneModePayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();
            if (!level.isLoaded(p.pos)) return;
            if (player.distanceToSqr(p.pos.getCenter()) > 64.0) return;
            if (!(level.getBlockEntity(p.pos) instanceof FluidConverterBlockEntity be)) return;
            RedstoneMode[] all = RedstoneMode.values();
            int idx = Math.floorMod(p.modeOrdinal & 0xFF, all.length);
            be.setRedstoneMode(all[idx]);
        });
    }
}
