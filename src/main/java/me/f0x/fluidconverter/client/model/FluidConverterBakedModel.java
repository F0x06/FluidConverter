package me.f0x.fluidconverter.client.model;

import me.f0x.fluidconverter.blockentity.FluidConverterBlockEntity;
import me.f0x.fluidconverter.blockentity.SideConfig;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class FluidConverterBakedModel implements IDynamicBakedModel {

    private final BakedModel base;
    private final BakedModel inputOverlay;
    private final BakedModel outputOverlay;

    private static final ChunkRenderTypeSet BASE_ONLY = ChunkRenderTypeSet.of(RenderType.solid());
    private static final ChunkRenderTypeSet BASE_AND_CUTOUT = ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutout());

    public FluidConverterBakedModel(BakedModel base, BakedModel inputOverlay, BakedModel outputOverlay) {
        this.base = base;
        this.inputOverlay = inputOverlay;
        this.outputOverlay = outputOverlay;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                             @NotNull RandomSource rand, @NotNull ModelData data,
                                             @Nullable RenderType renderType) {
        if (renderType == null || renderType == RenderType.solid()) {
            return base.getQuads(state, side, rand, data, renderType);
        }
        if (renderType == RenderType.cutout()) {
            if (side == null) return List.of();
            SideConfig[] sides = data.get(FluidConverterModelProps.SIDES);
            if (sides == null) return List.of();
            SideConfig cfg = sides[side.ordinal()];
            if (cfg == SideConfig.INPUT) return inputOverlay.getQuads(state, side, rand, data, renderType);
            if (cfg == SideConfig.OUTPUT) return outputOverlay.getQuads(state, side, rand, data, renderType);
        }
        return List.of();
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
                                           @NotNull BlockState state, @NotNull ModelData modelData) {
        if (modelData.has(FluidConverterModelProps.SIDES)) return modelData;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FluidConverterBlockEntity fc) {
            return fc.getModelData();
        }
        return modelData;
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand,
                                                     @NotNull ModelData data) {
        SideConfig[] sides = data.get(FluidConverterModelProps.SIDES);
        if (sides != null) {
            for (SideConfig s : sides) {
                if (s == SideConfig.INPUT || s == SideConfig.OUTPUT) return BASE_AND_CUTOUT;
            }
        }
        return BASE_ONLY;
    }

    @Override public boolean useAmbientOcclusion() { return base.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return base.isGui3d(); }
    @Override public boolean usesBlockLight() { return base.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return base.isCustomRenderer(); }
    @Override public @NotNull TextureAtlasSprite getParticleIcon() { return base.getParticleIcon(); }
    @Override public @NotNull ItemTransforms getTransforms() { return base.getTransforms(); }
    @Override public @NotNull ItemOverrides getOverrides() { return base.getOverrides(); }
}
