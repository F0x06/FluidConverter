package me.f0x.fluidconverter.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;

public final class GuiHelpers {

    private static double pendingCursorX = -1;
    private static double pendingCursorY = -1;

    private GuiHelpers() {}

    public static void captureCursor() {
        Minecraft mc = Minecraft.getInstance();
        pendingCursorX = mc.mouseHandler.xpos();
        pendingCursorY = mc.mouseHandler.ypos();
    }

    public static void restoreCursor() {
        if (pendingCursorX < 0) return;
        Minecraft mc = Minecraft.getInstance();
        GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), pendingCursorX, pendingCursorY);
        pendingCursorX = -1;
        pendingCursorY = -1;
    }

    public static void drawTank(GuiGraphics g, int x, int y, int w, int h, FluidStack stack, int capacity) {
        int hollow = 0xFF8B8B8B;
        g.fill(x, y, x + w, y + h, hollow);

        if (stack.isEmpty() || capacity <= 0) return;

        int fillH = Math.min(h, Math.max(1, (h * stack.getAmount()) / capacity));
        int fluidY = y + (h - fillH);

        drawFluidStretched(g, stack, x, fluidY, w, fillH);

        if (fillH < h) {
            g.fill(x, fluidY, x + w, fluidY + 1, 0x66FFFFFF);
        }
    }

    public static void drawFluidStretched(GuiGraphics g, FluidStack stack, int x, int y, int w, int h) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(stack.getFluid());
        ResourceLocation tex = ext.getStillTexture(stack);
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(tex);
        int color = ext.getTintColor(stack);

        float r = ((color >> 16) & 0xFF) / 255f;
        float gc = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        if (a == 0) a = 1f;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(r, gc, b, a);
        g.blit(x, y, 0, w, h, sprite);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public static void drawArrow(GuiGraphics g, int x, int y, int width) {
        drawArrow(g, x, y, width, 0f);
    }

    public static void drawArrow(GuiGraphics g, int x, int y, int width, float progress) {
        int gray = 0xFF555555;
        int green = 0xFF55CC44;
        int midY = y + 3;
        int headSize = 4;
        int shaftEnd = x + width - headSize;
        int shaftLen = Math.max(0, shaftEnd - x);
        int totalLen = shaftLen + headSize;
        float p = Math.max(0f, Math.min(1f, progress));
        int filledTotal = (int) (totalLen * p);

        g.fill(x, midY, shaftEnd, midY + 2, gray);
        int filledShaft = Math.min(filledTotal, shaftLen);
        if (filledShaft > 0) g.fill(x, midY, x + filledShaft, midY + 2, green);

        for (int i = 0; i < headSize; i++) {
            int half = headSize - i;
            int colReachAt = shaftLen + i + 1;
            int color = filledTotal >= colReachAt ? green : gray;
            g.fill(shaftEnd + i, midY - half + 1, shaftEnd + i + 1, midY + half + 1, color);
        }
    }

    public static void drawEnergyBar(GuiGraphics g, int x, int y, int w, int h, int stored, int capacity) {
        int border = 0xFF373737;
        int bg = 0xFF1A1A1A;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
        g.fill(x, y, x + w, y + h, bg);

        if (capacity <= 0 || stored <= 0) return;
        int fillH = Math.min(h, Math.max(1, (int) (((long) h * stored) / capacity)));
        int fillY = y + (h - fillH);

        int top = 0xFFFFC04D;
        int bot = 0xFFE65C1A;
        g.fillGradient(x, fillY, x + w, y + h, top, bot);
        g.fill(x, fillY, x + w, fillY + 1, 0x80FFFFFF);
    }

    public static void drawSlotFrame(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }

    public static void drawInsetPanel(GuiGraphics g, int x1, int y1, int x2, int y2, int fillArgb) {
        int dark = 0xFF555555;
        int light = 0xFFFFFFFF;
        g.fill(x1, y1, x2, y1 + 1, dark);
        g.fill(x1, y1, x1 + 1, y2, dark);
        g.fill(x1, y2 - 1, x2, y2, light);
        g.fill(x2 - 1, y1, x2, y2, light);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fillArgb);
    }
}
