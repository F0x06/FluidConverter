package me.f0x.fluidconverter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ClientChunkRefresh {

    private ClientChunkRefresh() {}

    public static void refreshSection(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.levelRenderer == null) return;
        mc.levelRenderer.setSectionDirty(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }
}
