package me.f0x.fluidconverter.client;

import me.f0x.fluidconverter.FluidConverter;
import me.f0x.fluidconverter.blockentity.RedstoneMode;
import me.f0x.fluidconverter.config.Config;
import me.f0x.fluidconverter.menu.FluidConverterMenu;
import me.f0x.fluidconverter.network.SetRedstoneModePayload;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FluidConverterScreen extends AbstractContainerScreen<FluidConverterMenu> {

    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(
            FluidConverter.MODID, "textures/gui/fluid_converter.png");

    private static final int TANK_W = 20;
    private static final int TANK_H = 48;
    private static final int TANK_IN_X = 26;
    private static final int TANK_OUT_X = 130;
    private static final int TANKS_Y = 24;
    private static final int ENERGY_X = 12;
    private static final int ENERGY_W = 4;
    private static final int ARROW_W = 72;
    private static final int RECIPE_TEXT_Y = 80;
    private static final int CHEVRON_SIZE = 8;
    private static final int CHEVRON_Y_OFFSET = 1;
    private static final int CHEVRON_MARGIN = 12;

    private int lastOutputsHash = -1;

    public FluidConverterScreen(FluidConverterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = FluidConverterMenu.IMAGE_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        GuiHelpers.restoreCursor();
        rebuildButtons();
    }

    private RedstoneMode lastRedstoneMode = RedstoneMode.IGNORED;
    private boolean lastPaused = false;
    private int lastAvailableOutputsCount = -1;

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.blockEntity() == null) return;
        RedstoneMode m = menu.blockEntity().getRedstoneMode();
        boolean p = menu.blockEntity().isPaused();
        int n = menu.blockEntity().getAvailableOutputs().size();
        if (m != lastRedstoneMode || p != lastPaused || n != lastAvailableOutputsCount) {
            rebuildButtons();
        }
    }

    private void rebuildButtons() {
        clearWidgets();

        int cornerY = topPos + 4;
        int cornerSize = 12;
        int gap = 2;
        int rightX = leftPos + imageWidth - 7 - cornerSize;

        if (menu.canAdmin()) {
            SmallButton admin = new SmallButton(
                    rightX, cornerY, cornerSize, cornerSize,
                    FluidConverterScreen::drawShieldIcon,
                    b -> {
                        GuiHelpers.captureCursor();
                        minecraft.gameMode.handleInventoryButtonClick(
                                menu.containerId, FluidConverterMenu.BTN_OPEN_ADMIN);
                    });
            admin.setTooltip(Tooltip.create(Component.translatable("gui.fluidconverter.tooltip.admin_recipes")));
            addRenderableWidget(admin);
            rightX -= cornerSize + gap;
        }

        boolean isPaused = menu.blockEntity() != null && menu.blockEntity().isPaused();
        SmallButton pause = new SmallButton(
                rightX, cornerY, cornerSize, cornerSize,
                Component.literal(isPaused ? "▶" : "■"),
                b -> minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId, FluidConverterMenu.BTN_TOGGLE_PAUSE),
                0.85f);
        pause.setTooltip(Tooltip.create(Component.translatable(isPaused
                ? "gui.fluidconverter.tooltip.resume"
                : "gui.fluidconverter.tooltip.pause")));
        addRenderableWidget(pause);
        rightX -= cornerSize + gap;

        SmallButton sideCfg = new SmallButton(
                rightX, cornerY, cornerSize, cornerSize,
                FluidConverterScreen::drawSidesIcon,
                b -> {
                    GuiHelpers.captureCursor();
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId, FluidConverterMenu.BTN_OPEN_SIDE_CONFIG);
                });
        sideCfg.setTooltip(Tooltip.create(Component.translatable("gui.fluidconverter.tooltip.side_config")));
        addRenderableWidget(sideCfg);
        rightX -= cornerSize + gap;

        RedstoneMode currentMode = menu.blockEntity() != null
                ? menu.blockEntity().getRedstoneMode()
                : RedstoneMode.IGNORED;
        SmallButton redstone = new SmallButton(
                rightX, cornerY, cornerSize, cornerSize,
                (g, x, y, w, h, color) -> {
                    RedstoneMode m = menu.blockEntity() != null
                            ? menu.blockEntity().getRedstoneMode()
                            : RedstoneMode.IGNORED;
                    int tint = switch (m) {
                        case IGNORED -> 0xFF777777;
                        case ACTIVE_WITH_SIGNAL -> 0xFFE03030;
                        case ACTIVE_WITHOUT_SIGNAL -> 0xFF802020;
                    };
                    drawRedstoneIcon(g, x, y, w, h, tint);
                },
                b -> {
                    if (menu.blockEntity() == null) return;
                    RedstoneMode m = menu.blockEntity().getRedstoneMode().next();
                    PacketDistributor.sendToServer(new SetRedstoneModePayload(
                            menu.blockEntity().getBlockPos(), (byte) m.ordinal()));
                });
        redstone.setTooltip(Tooltip.create(Component.translatable(
                "gui.fluidconverter.tooltip.redstone",
                Component.translatable(currentMode.translationKey()))));
        addRenderableWidget(redstone);
        lastRedstoneMode = currentMode;
        if (menu.blockEntity() != null) {
            lastPaused = menu.blockEntity().isPaused();
            lastAvailableOutputsCount = menu.blockEntity().getAvailableOutputs().size();
        }

        int drainSize = 8;
        int drainY = topPos + TANKS_Y + TANK_H - drainSize + 2;
        SmallButton drainIn = new SmallButton(
                leftPos + TANK_IN_X + TANK_W + 4, drainY, drainSize, drainSize,
                Component.literal("↓"),
                b -> minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId, FluidConverterMenu.BTN_DRAIN_INPUT),
                0.75f);
        drainIn.setTooltip(Tooltip.create(Component.translatable("gui.fluidconverter.tooltip.drain_input")));
        addRenderableWidget(drainIn);
        SmallButton drainOut = new SmallButton(
                leftPos + TANK_OUT_X + TANK_W + 4, drainY, drainSize, drainSize,
                Component.literal("↓"),
                b -> minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId, FluidConverterMenu.BTN_DRAIN_OUTPUT),
                0.75f);
        drainOut.setTooltip(Tooltip.create(Component.translatable("gui.fluidconverter.tooltip.drain_output")));
        addRenderableWidget(drainOut);

        if (menu.blockEntity() != null && menu.blockEntity().getAvailableOutputs().size() > 1) {
            int y = topPos + RECIPE_TEXT_Y + CHEVRON_Y_OFFSET;
            SmallButton prev = new SmallButton(
                    leftPos + CHEVRON_MARGIN, y, CHEVRON_SIZE, CHEVRON_SIZE,
                    Component.literal("<"),
                    b -> minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId, FluidConverterMenu.BTN_PREV_OUTPUT),
                    0.7f);
            prev.setTooltip(Tooltip.create(Component.translatable("gui.fluidconverter.tooltip.prev_output")));
            addRenderableWidget(prev);
            SmallButton next = new SmallButton(
                    leftPos + imageWidth - CHEVRON_MARGIN - CHEVRON_SIZE, y, CHEVRON_SIZE, CHEVRON_SIZE,
                    Component.literal(">"),
                    b -> minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId, FluidConverterMenu.BTN_NEXT_OUTPUT),
                    0.7f);
            next.setTooltip(Tooltip.create(Component.translatable("gui.fluidconverter.tooltip.next_output")));
            addRenderableWidget(next);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

        if (menu.blockEntity() == null) return;
        FluidStack in = menu.blockEntity().getInputTank().getFluid();
        FluidStack out = menu.blockEntity().getOutputTank().getFluid();
        int inCap = menu.blockEntity().getInputTank().getCapacity();
        int outCap = menu.blockEntity().getOutputTank().getCapacity();

        GuiHelpers.drawTank(g, leftPos + TANK_IN_X, topPos + TANKS_Y, TANK_W, TANK_H, in, inCap);
        GuiHelpers.drawTank(g, leftPos + TANK_OUT_X, topPos + TANKS_Y, TANK_W, TANK_H, out, outCap);

        if (Config.energyEnabled()) {
            GuiHelpers.drawEnergyBar(g, leftPos + ENERGY_X, topPos + TANKS_Y, ENERGY_W, TANK_H,
                    menu.blockEntity().getEnergyStored(),
                    menu.blockEntity().getMaxEnergyStored());
        }

        int arrowX = leftPos + TANK_IN_X + TANK_W + 6;
        int arrowY = topPos + TANKS_Y + TANK_H / 2 - 3;
        float progress = 0f;
        var recipeOpt = menu.blockEntity().findCurrentRecipe();
        if (recipeOpt.isPresent()) {
            int p = menu.blockEntity().getRecipeProgressTicks();
            int max = me.f0x.fluidconverter.blockentity.FluidConverterBlockEntity.effectiveTicksFor(recipeOpt.get());
            progress = max <= 0 ? 0f : (float) p / max;
        }
        GuiHelpers.drawArrow(g, arrowX, arrowY, ARROW_W, progress);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        int cornerSize = 12;
        int gap = 2;
        int rightMargin = 7;
        int buttonCount = 3 + (menu.canAdmin() ? 1 : 0);
        int buttonsW = buttonCount * cornerSize + (buttonCount - 1) * gap + rightMargin;
        int titleMaxW = imageWidth - this.titleLabelX - buttonsW - 2;
        String fitTitle = fitToWidth(this.title.getString(), titleMaxW);
        g.drawString(this.font, fitTitle, this.titleLabelX, this.titleLabelY, 0x404040, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        if (menu.blockEntity() == null) return;

        FluidStack in = menu.blockEntity().getInputTank().getFluid();

        boolean hasChevrons = menu.blockEntity().getAvailableOutputs().size() > 1;
        int textMaxW = hasChevrons ? imageWidth - 16 - 2 * (CHEVRON_SIZE + 2) : imageWidth - 16;

        String text; int color;
        var recipeOpt = menu.blockEntity().findCurrentRecipe();
        if (recipeOpt.isPresent()) {
            var r = recipeOpt.get();
            text = displayName(r.input().getFluid()) + " → " + displayName(r.output().getFluid());
            color = 0x227744;
        } else if (!in.isEmpty()) {
            text = I18n.get("gui.fluidconverter.recipe.none", displayName(in.getFluid()));
            color = 0x884422;
        } else {
            text = I18n.get("gui.fluidconverter.recipe.empty");
            color = 0x666666;
        }
        String fit = fitToWidth(text, textMaxW);
        int w = this.font.width(fit);
        g.drawString(this.font, fit, (imageWidth - w) / 2, RECIPE_TEXT_Y, color, false);
    }

    private String fitToWidth(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        while (text.length() > 1 && this.font.width(text + ellipsis) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    private void drawCentered(GuiGraphics g, String s, int cx, int y, int color) {
        int w = this.font.width(s);
        g.drawString(this.font, s, cx - w / 2, y, color, false);
    }

    private static String compactAmount(int mb) {
        if (mb >= 1_000_000) return (mb / 1000) + "k";
        if (mb >= 10_000)    return (mb / 1000) + "k";
        return Integer.toString(mb);
    }

    private List<Component> buildTankTooltip(FluidStack stack, int capacity, String roleKey) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(roleKey).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (stack.isEmpty()) {
            lines.add(Component.translatable("gui.fluidconverter.tank.empty").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.literal("0 / " + capacity + " mB").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        lines.add(stack.getHoverName().copy().withStyle(ChatFormatting.WHITE));
        lines.add(Component.literal(stack.getAmount() + " / " + capacity + " mB").withStyle(ChatFormatting.GRAY));
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        if (id != null) {
            lines.add(Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private List<Component> buildEnergyTooltip(int stored, int capacity) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.fluidconverter.energy").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        lines.add(Component.literal(formatFE(stored) + " / " + formatFE(capacity) + " FE")
                .withStyle(ChatFormatting.WHITE));
        int costPerMb = Config.energyCostPerMb();
        if (costPerMb > 0) {
            lines.add(Component.literal(costPerMb + " FE / mB").withStyle(ChatFormatting.GOLD));
        } else {
            lines.add(Component.translatable("gui.fluidconverter.energy.not_required").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private static String formatFE(int v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000) return String.format("%.1fk", v / 1_000.0);
        return Integer.toString(v);
    }

    private static String pathOf(Fluid f) {
        if (f == Fluids.EMPTY) return "—";
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(f);
        return id == null ? "?" : id.getPath();
    }

    private static String displayName(Fluid f) {
        if (f == Fluids.EMPTY) return "—";
        return new FluidStack(f, 1).getHoverName().getString();
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (menu.blockEntity() != null) {
            int h = menu.blockEntity().getAvailableOutputs().hashCode()
                    ^ (menu.blockEntity().isPaused() ? 0x55AA : 0);
            if (h != lastOutputsHash) {
                lastOutputsHash = h;
                rebuildButtons();
            }
        }
        renderBackground(g, mx, my, pt);
        super.render(g, mx, my, pt);

        if (menu.blockEntity() != null) {
            if (isHovering(TANK_IN_X, TANKS_Y, TANK_W, TANK_H, mx, my)) {
                g.renderComponentTooltip(this.font, buildTankTooltip(
                        menu.blockEntity().getInputTank().getFluid(),
                        menu.blockEntity().getInputTank().getCapacity(),
                        "gui.fluidconverter.tank.input"), mx, my);
            } else if (isHovering(TANK_OUT_X, TANKS_Y, TANK_W, TANK_H, mx, my)) {
                g.renderComponentTooltip(this.font, buildTankTooltip(
                        menu.blockEntity().getOutputTank().getFluid(),
                        menu.blockEntity().getOutputTank().getCapacity(),
                        "gui.fluidconverter.tank.output"), mx, my);
            } else if (Config.energyEnabled() && isHovering(ENERGY_X, TANKS_Y, ENERGY_W, TANK_H, mx, my)) {
                g.renderComponentTooltip(this.font, buildEnergyTooltip(
                        menu.blockEntity().getEnergyStored(),
                        menu.blockEntity().getMaxEnergyStored()), mx, my);
            }
        }

        renderTooltip(g, mx, my);
    }

    @FunctionalInterface
    private interface IconDrawer {
        void draw(GuiGraphics g, int x, int y, int w, int h, int color);
    }

    private static void drawShieldIcon(GuiGraphics g, int x, int y, int w, int h, int color) {
        int sx = x + (w - 8) / 2;
        int sy = y + (h - 8) / 2;
        g.fill(sx + 1, sy + 0, sx + 7, sy + 1, color);
        g.fill(sx + 0, sy + 1, sx + 8, sy + 4, color);
        g.fill(sx + 1, sy + 4, sx + 7, sy + 5, color);
        g.fill(sx + 2, sy + 5, sx + 6, sy + 6, color);
        g.fill(sx + 3, sy + 6, sx + 5, sy + 7, color);
    }

    private static void drawRedstoneIcon(GuiGraphics g, int x, int y, int w, int h, int color) {
        int sx = x + (w - 8) / 2;
        int sy = y + (h - 8) / 2;
        int stick = 0xFF6B4226;
        g.fill(sx + 3, sy + 4, sx + 5, sy + 8, stick);
        g.fill(sx + 2, sy + 0, sx + 6, sy + 4, color);
        g.fill(sx + 3, sy + 4, sx + 5, sy + 5, color);
    }

    private static void drawSidesIcon(GuiGraphics g, int x, int y, int w, int h, int color) {
        int sx = x + (w - 8) / 2;
        int sy = y + (h - 8) / 2;
        g.fill(sx + 3, sy + 0, sx + 5, sy + 2, color);
        g.fill(sx + 0, sy + 3, sx + 2, sy + 5, color);
        g.fill(sx + 3, sy + 3, sx + 5, sy + 5, color);
        g.fill(sx + 6, sy + 3, sx + 8, sy + 5, color);
        g.fill(sx + 3, sy + 6, sx + 5, sy + 8, color);
    }

    private static final class SmallButton extends Button {
        private final float scale;
        private final IconDrawer iconDrawer;

        SmallButton(int x, int y, int w, int h, Component text, OnPress action, float scale) {
            super(x, y, w, h, text, action, DEFAULT_NARRATION);
            this.scale = scale;
            this.iconDrawer = null;
        }

        SmallButton(int x, int y, int w, int h, IconDrawer icon, OnPress action) {
            super(x, y, w, h, Component.empty(), action, DEFAULT_NARRATION);
            this.scale = 1f;
            this.iconDrawer = icon;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int x2 = x + this.getWidth();
            int y2 = y + this.getHeight();
            boolean hover = this.isHoveredOrFocused();

            int fill = hover ? 0xFFA0A0A0 : 0xFF8B8B8B;
            int light = 0xFFFFFFFF;
            int dark  = 0xFF373737;

            g.fill(x, y, x2, y + 1, light);
            g.fill(x, y, x + 1, y2, light);
            g.fill(x, y2 - 1, x2, y2, dark);
            g.fill(x2 - 1, y, x2, y2, dark);
            g.fill(x + 1, y + 1, x2 - 1, y2 - 1, fill);

            int color = hover ? 0xFFFFFFFF : 0xFFE6E6E6;

            if (iconDrawer != null) {
                iconDrawer.draw(g, x, y, this.getWidth(), this.getHeight(), color);
                return;
            }

            Component msg = this.getMessage();
            Font font = Minecraft.getInstance().font;
            int textW = font.width(msg);

            float cx = x + this.getWidth() / 2f;
            float cy = y + this.getHeight() / 2f;

            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().scale(scale, scale, 1f);
            g.drawString(font, msg, Math.round(-textW / 2f), Math.round(-font.lineHeight / 2f + 1), color, false);
            g.pose().popPose();
        }
    }
}
