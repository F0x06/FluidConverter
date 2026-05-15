package me.f0x.fluidconverter.network;

import me.f0x.fluidconverter.FluidConverter;
import me.f0x.fluidconverter.menu.FluidConverterAdminMenu;
import me.f0x.fluidconverter.recipe.FluidConvertingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record SyncLearnedRecipesPayload(List<FluidConvertingRecipe> recipes) implements CustomPacketPayload {

    public static final Type<SyncLearnedRecipesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(FluidConverter.MODID, "sync_learned"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLearnedRecipesPayload> STREAM_CODEC =
            StreamCodec.composite(
                    FluidConvertingRecipe.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SyncLearnedRecipesPayload::recipes,
                    SyncLearnedRecipesPayload::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncLearnedRecipesPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null && player.containerMenu instanceof FluidConverterAdminMenu admin) {
                admin.setClientRecipes(p.recipes());
            }
        });
    }
}
