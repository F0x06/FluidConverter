package me.f0x.fluidconverter.client;

import me.f0x.fluidconverter.FluidConverter;
import me.f0x.fluidconverter.blockentity.SideConfig;
import me.f0x.fluidconverter.menu.FluidConverterSideConfigMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FluidConverterSideConfigScreen extends AbstractContainerScreen<FluidConverterSideConfigMenu> {

    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(
            FluidConverter.MODID, "textures/gui/fluid_converter_sides.png");

    private static final ResourceLocation SPR_FACE_NONE         = sprite("face_none");
    private static final ResourceLocation SPR_FACE_NONE_HOVER   = sprite("face_none_hover");
    private static final ResourceLocation SPR_FACE_INPUT        = sprite("face_input");
    private static final ResourceLocation SPR_FACE_INPUT_HOVER  = sprite("face_input_hover");
    private static final ResourceLocation SPR_FACE_OUTPUT       = sprite("face_output");
    private static final ResourceLocation SPR_FACE_OUTPUT_HOVER = sprite("face_output_hover");
    private static final ResourceLocation SPR_ICON              = sprite("icon_button");
    private static final ResourceLocation SPR_ICON_HOVER        = sprite("icon_button_hover");

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(FluidConverter.MODID, "sides/" + name);
    }

    private static final int FACE_SIZE = 20;
    private static final int FACE_GAP = 2;
    private static final int GRID_Y = 28;

    private int lastConfigHash = -1;

    public FluidConverterSideConfigScreen(FluidConverterSideConfigMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = FluidConverterSideConfigMenu.IMAGE_HEIGHT;
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

    private void rebuildButtons() {
        clearWidgets();

        addRenderableWidget(new IconButton(
                leftPos + imageWidth - 7 - 12, topPos + 4, 12, 12,
                Component.literal("←"),
                b -> {
                    GuiHelpers.captureCursor();
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId, FluidConverterSideConfigMenu.BTN_BACK);
                }));

        IconButton resetAll = new IconButton(
                leftPos + 8, topPos + 20, 12, 12,
                Component.literal("X"),
                b -> minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId, FluidConverterSideConfigMenu.BTN_RESET_ALL));
        resetAll.setTooltip(Tooltip.create(
                Component.translatable("gui.fluidconverter.side.tooltip.reset_all")));
        addRenderableWidget(resetAll);

        int cy = topPos + GRID_Y + FACE_SIZE + FACE_GAP;
        int totalW = 4 * FACE_SIZE + 3 * FACE_GAP;
        int rowStartX = leftPos + (imageWidth - totalW) / 2;

        int wX = rowStartX;
        int nX = rowStartX + (FACE_SIZE + FACE_GAP);
        int eX = rowStartX + 2 * (FACE_SIZE + FACE_GAP);
        int sX = rowStartX + 3 * (FACE_SIZE + FACE_GAP);

        addFaceButton(Direction.UP,    nX, cy - (FACE_SIZE + FACE_GAP));
        addFaceButton(Direction.WEST,  wX, cy);
        addFaceButton(Direction.NORTH, nX, cy);
        addFaceButton(Direction.EAST,  eX, cy);
        addFaceButton(Direction.SOUTH, sX, cy);
        addFaceButton(Direction.DOWN,  nX, cy + (FACE_SIZE + FACE_GAP));
    }

    private void addFaceButton(Direction dir, int x, int y) {
        addRenderableWidget(new FaceButton(
                x, y, FACE_SIZE, FACE_SIZE, dir, menu,
                b -> minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId, FluidConverterSideConfigMenu.buttonIdFor(dir))));
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (menu.blockEntity() != null) {
            int h = 0;
            for (Direction d : Direction.values()) {
                h = h * 31 + menu.blockEntity().getSideConfig(d).ordinal();
            }
            if (h != lastConfigHash) {
                lastConfigHash = h;
            }
        }
        renderBackground(g, mx, my, pt);
        super.render(g, mx, my, pt);

        for (var w : children()) {
            if (w instanceof FaceButton fb && fb.isHovered()) {
                List<Component> tip = new ArrayList<>();
                tip.add(Component.translatable(dirKey(fb.dir)).withStyle(ChatFormatting.WHITE));
                SideConfig cfg = menu.blockEntity() == null
                        ? SideConfig.NONE
                        : menu.blockEntity().getSideConfig(fb.dir);
                tip.add(Component.translatable(modeKey(cfg)).withStyle(colorFor(cfg)));
                tip.add(Component.translatable("gui.fluidconverter.side.tooltip.cycle").withStyle(ChatFormatting.DARK_GRAY));
                g.renderComponentTooltip(this.font, tip, mx, my);
                break;
            }
        }

        renderTooltip(g, mx, my);
    }

    private static String dirKey(Direction d) {
        return switch (d) {
            case UP -> "gui.fluidconverter.side.dir.up";
            case DOWN -> "gui.fluidconverter.side.dir.down";
            case NORTH -> "gui.fluidconverter.side.dir.north";
            case SOUTH -> "gui.fluidconverter.side.dir.south";
            case EAST -> "gui.fluidconverter.side.dir.east";
            case WEST -> "gui.fluidconverter.side.dir.west";
        };
    }

    private static String modeKey(SideConfig cfg) {
        return switch (cfg) {
            case NONE -> "gui.fluidconverter.side.mode.none";
            case INPUT -> "gui.fluidconverter.side.mode.input";
            case OUTPUT -> "gui.fluidconverter.side.mode.output";
        };
    }

    private static ChatFormatting colorFor(SideConfig cfg) {
        return switch (cfg) {
            case NONE -> ChatFormatting.DARK_GRAY;
            case INPUT -> ChatFormatting.AQUA;
            case OUTPUT -> ChatFormatting.GOLD;
        };
    }

    private static final class IconButton extends Button {
        IconButton(int x, int y, int w, int h, Component text, OnPress action) {
            super(x, y, w, h, text, action, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean hover = this.isHoveredOrFocused();
            g.blitSprite(hover ? SPR_ICON_HOVER : SPR_ICON,
                    this.getX(), this.getY(), this.getWidth(), this.getHeight());

            Component msg = this.getMessage();
            Font font = Minecraft.getInstance().font;
            int textW = font.width(msg);
            int color = hover ? 0xFFFFFFFF : 0xFFE6E6E6;
            g.drawString(font, msg,
                    this.getX() + (this.getWidth() - textW) / 2,
                    this.getY() + (this.getHeight() - font.lineHeight) / 2 + 1,
                    color, false);
        }
    }

    private static final class FaceButton extends Button {
        final Direction dir;
        final FluidConverterSideConfigMenu menu;

        FaceButton(int x, int y, int w, int h, Direction dir, FluidConverterSideConfigMenu menu, OnPress action) {
            super(x, y, w, h, Component.literal(initialFor(dir)), action, DEFAULT_NARRATION);
            this.dir = dir;
            this.menu = menu;
        }

        private static String initialFor(Direction d) {
            return switch (d) {
                case UP -> "U";
                case DOWN -> "D";
                case NORTH -> "N";
                case SOUTH -> "S";
                case EAST -> "E";
                case WEST -> "W";
            };
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean hover = this.isHoveredOrFocused();
            SideConfig cfg = menu.blockEntity() == null
                    ? SideConfig.NONE
                    : menu.blockEntity().getSideConfig(dir);

            ResourceLocation sprite = switch (cfg) {
                case NONE   -> hover ? SPR_FACE_NONE_HOVER   : SPR_FACE_NONE;
                case INPUT  -> hover ? SPR_FACE_INPUT_HOVER  : SPR_FACE_INPUT;
                case OUTPUT -> hover ? SPR_FACE_OUTPUT_HOVER : SPR_FACE_OUTPUT;
            };
            int x = this.getX();
            int y = this.getY();
            g.blitSprite(sprite, x, y, this.getWidth(), this.getHeight());

            Component msg = this.getMessage();
            Font font = Minecraft.getInstance().font;
            int textW = font.width(msg);
            int color = 0xFFFFFFFF;
            g.drawString(font, msg,
                    x + (this.getWidth() - textW) / 2,
                    y + 3,
                    color, false);

            String mode = switch (cfg) {
                case NONE -> "-";
                case INPUT -> "IN";
                case OUTPUT -> "OUT";
            };
            float scale = 0.65f;
            g.pose().pushPose();
            float cx = x + this.getWidth() / 2f;
            float cy = y + this.getHeight() - 8;
            g.pose().translate(cx, cy, 0);
            g.pose().scale(scale, scale, 1f);
            int mw = font.width(mode);
            g.drawString(font, mode, Math.round(-mw / 2f), 0, color, false);
            g.pose().popPose();
        }
    }
}
