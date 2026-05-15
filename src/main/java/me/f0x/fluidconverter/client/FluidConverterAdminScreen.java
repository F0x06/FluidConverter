package me.f0x.fluidconverter.client;

import me.f0x.fluidconverter.FluidConverter;
import me.f0x.fluidconverter.menu.FluidConverterAdminMenu;
import me.f0x.fluidconverter.network.ForgetRecipePayload;
import me.f0x.fluidconverter.network.LearnRecipePayload;
import me.f0x.fluidconverter.network.RequestLearnedRecipesPayload;
import me.f0x.fluidconverter.recipe.FluidConvertingRecipe;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class FluidConverterAdminScreen extends AbstractContainerScreen<FluidConverterAdminMenu> {

    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(
            FluidConverter.MODID, "textures/gui/fluid_converter_admin.png");

    private static final int LIST_X = 8;
    private static final int LIST_W = 152;
    private static final int SCROLLBAR_X_OFFSET_FROM_RIGHT = 14;
    private static final int SCROLLBAR_W = 6;
    private static final int TEXT_LEFT_PAD = 12;

    private int scrollOffset = 0;
    private int lastRenderedListSize = -1;
    private boolean learnReverse = true;

    public FluidConverterAdminScreen(FluidConverterAdminMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = FluidConverterAdminMenu.IMAGE_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        GuiHelpers.restoreCursor();
        PacketDistributor.sendToServer(RequestLearnedRecipesPayload.INSTANCE);
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();

        addRenderableWidget(Button.builder(Component.literal("<"),
                        b -> {
                            GuiHelpers.captureCursor();
                            minecraft.gameMode.handleInventoryButtonClick(
                                    menu.containerId, FluidConverterAdminMenu.BTN_BACK);
                        })
                .bounds(leftPos + imageWidth - 19, topPos + 4, 12, 12).build());

        int learnW = 50;
        int learnH = 10;
        int learnX = leftPos + (imageWidth - learnW) / 2;
        int learnY = topPos + 39;
        SmallButton learnBtn = new SmallButton(learnX, learnY, learnW, learnH,
                Component.translatable("gui.fluidconverter.admin.learn"), b -> sendLearnPacket(), 0.85f);
        learnBtn.setTooltip(Tooltip.create(Component.translatable("gui.fluidconverter.admin.learn")));
        addRenderableWidget(learnBtn);

        int inSlotX = FluidConverterAdminMenu.LEARN_INPUT_SLOT_X;
        int outSlotX = FluidConverterAdminMenu.LEARN_OUTPUT_SLOT_X;
        int arrowW = 26;
        int arrowH = 10;
        int arrowX = leftPos + (inSlotX + 16 + outSlotX) / 2 - arrowW / 2;
        int arrowY = topPos + FluidConverterAdminMenu.LEARN_SLOTS_Y + (16 - arrowH) / 2;
        ArrowToggleButton arrowBtn = new ArrowToggleButton(arrowX, arrowY, arrowW, arrowH,
                () -> learnReverse,
                () -> {
                    learnReverse = !learnReverse;
                    rebuildButtons();
                });
        arrowBtn.setTooltip(Tooltip.create(Component.translatable(learnReverse
                ? "gui.fluidconverter.admin.reverse.on"
                : "gui.fluidconverter.admin.reverse.off")));
        addRenderableWidget(arrowBtn);

        List<FluidConvertingRecipe> recipes = menu.clientRecipes();
        int maxRows = FluidConverterAdminMenu.RECIPE_LIST_MAX_ROWS;
        scrollOffset = clampOffset(scrollOffset, recipes.size(), maxRows);
        int visible = Math.min(maxRows, Math.max(0, recipes.size() - scrollOffset));
        int btnSize = FluidConverterAdminMenu.DELETE_BTN_SIZE;
        for (int displayRow = 0; displayRow < visible; displayRow++) {
            FluidConvertingRecipe r = recipes.get(scrollOffset + displayRow);
            int rowY = topPos + FluidConverterAdminMenu.RECIPE_LIST_FIRST_ROW_Y
                    + displayRow * FluidConverterAdminMenu.RECIPE_LIST_ROW_HEIGHT;
            FluidStack inputCopy = r.input().copy();
            FluidStack outputCopy = r.output().copy();
            addRenderableWidget(new SmallButton(
                    leftPos + imageWidth - btnSize - 16, rowY + 1, btnSize, btnSize,
                    Component.literal("x"),
                    b -> PacketDistributor.sendToServer(new ForgetRecipePayload(inputCopy, outputCopy)),
                    0.7f));
        }
        lastRenderedListSize = recipes.size();
    }

    private static int clampOffset(int off, int listSize, int maxRows) {
        int maxOff = Math.max(0, listSize - maxRows);
        return Math.max(0, Math.min(maxOff, off));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInListArea(mouseX, mouseY)) {
            int recipes = menu.clientRecipes().size();
            int maxRows = FluidConverterAdminMenu.RECIPE_LIST_MAX_ROWS;
            int before = scrollOffset;
            scrollOffset = clampOffset(scrollOffset - (int) Math.signum(scrollY), recipes, maxRows);
            if (scrollOffset != before) rebuildButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isInListArea(double mouseX, double mouseY) {
        int x0 = leftPos + LIST_X;
        int x1 = leftPos + imageWidth - SCROLLBAR_X_OFFSET_FROM_RIGHT + SCROLLBAR_W;
        int y0 = topPos + FluidConverterAdminMenu.RECIPE_LIST_FIRST_ROW_Y - 2;
        int y1 = y0 + FluidConverterAdminMenu.RECIPE_LIST_MAX_ROWS * FluidConverterAdminMenu.RECIPE_LIST_ROW_HEIGHT + 2;
        return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
    }

    private void sendLearnPacket() {
        ItemStack inStack = menu.learnSlots().getItem(0);
        ItemStack outStack = menu.learnSlots().getItem(1);
        FluidStack inFluid = FluidUtil.getFluidContained(inStack).orElse(FluidStack.EMPTY);
        FluidStack outFluid = FluidUtil.getFluidContained(outStack).orElse(FluidStack.EMPTY);
        if (inFluid.isEmpty() || outFluid.isEmpty()) return;
        PacketDistributor.sendToServer(new LearnRecipePayload(inFluid, outFluid, learnReverse));
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.blit(BG, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        drawScrollbar(g);
    }

    private void drawScrollbar(GuiGraphics g) {
        int recipes = menu.clientRecipes().size();
        int maxRows = FluidConverterAdminMenu.RECIPE_LIST_MAX_ROWS;
        if (recipes <= maxRows) return;

        int trackTop = topPos + 50;
        int trackHeight = 40;
        int trackX = leftPos + imageWidth - SCROLLBAR_X_OFFSET_FROM_RIGHT;
        int trackX2 = trackX + SCROLLBAR_W;
        int trackBottom = trackTop + trackHeight;

        g.fill(trackX, trackTop, trackX2, trackBottom, 0xFF373737);

        int thumbH = Math.max(8, trackHeight * maxRows / recipes);
        int maxOff = recipes - maxRows;
        int thumbY = trackTop + (maxOff == 0 ? 0 : (trackHeight - thumbH) * scrollOffset / maxOff);
        int thumbY2 = thumbY + thumbH;

        g.fill(trackX, thumbY, trackX2, thumbY + 1, 0xFFFFFFFF);
        g.fill(trackX, thumbY, trackX + 1, thumbY2, 0xFFFFFFFF);
        g.fill(trackX, thumbY2 - 1, trackX2, thumbY2, 0xFF373737);
        g.fill(trackX2 - 1, thumbY, trackX2, thumbY2, 0xFF373737);
        g.fill(trackX + 1, thumbY + 1, trackX2 - 1, thumbY2 - 1, 0xFFC6C6C6);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        List<FluidConvertingRecipe> recipes = menu.clientRecipes();
        int total = recipes.size();
        int maxRows = FluidConverterAdminMenu.RECIPE_LIST_MAX_ROWS;
        String header = I18n.get("gui.fluidconverter.admin.learned", total);
        if (total > maxRows) {
            int from = scrollOffset + 1;
            int to = Math.min(total, scrollOffset + maxRows);
            header += "  " + from + "-" + to;
        }
        g.drawString(this.font, header + ":",
                TEXT_LEFT_PAD, FluidConverterAdminMenu.RECIPE_LIST_HEADER_Y, 0x333333, false);

        int visible = Math.min(maxRows, Math.max(0, total - scrollOffset));
        int textEndX = imageWidth - FluidConverterAdminMenu.DELETE_BTN_SIZE - 16;
        for (int displayRow = 0; displayRow < visible; displayRow++) {
            FluidConvertingRecipe r = recipes.get(scrollOffset + displayRow);
            int rowY = FluidConverterAdminMenu.RECIPE_LIST_FIRST_ROW_Y
                    + displayRow * FluidConverterAdminMenu.RECIPE_LIST_ROW_HEIGHT;
            String arrow = r.reverse() ? " ↔ " : " → ";
            String line = compactName(r.input().getFluid()) + arrow + compactName(r.output().getFluid());
            String fit = fitToWidth(line, textEndX - TEXT_LEFT_PAD);
            g.drawString(this.font, fit, TEXT_LEFT_PAD, rowY, 0x224488, false);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        super.render(g, mx, my, pt);

        List<FluidConvertingRecipe> recipes = menu.clientRecipes();
        int visible = Math.min(FluidConverterAdminMenu.RECIPE_LIST_MAX_ROWS,
                Math.max(0, recipes.size() - scrollOffset));
        int textEndX = imageWidth - FluidConverterAdminMenu.DELETE_BTN_SIZE - 16;
        for (int displayRow = 0; displayRow < visible; displayRow++) {
            int rowLocalY = FluidConverterAdminMenu.RECIPE_LIST_FIRST_ROW_Y
                    + displayRow * FluidConverterAdminMenu.RECIPE_LIST_ROW_HEIGHT;
            if (isHovering(TEXT_LEFT_PAD, rowLocalY, textEndX - TEXT_LEFT_PAD, 9, mx, my)) {
                g.renderComponentTooltip(this.font,
                        buildRecipeTooltip(recipes.get(scrollOffset + displayRow)), mx, my);
                break;
            }
        }
        renderTooltip(g, mx, my);
    }

    private List<Component> buildRecipeTooltip(FluidConvertingRecipe r) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.fluidconverter.tank.input").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        lines.add(r.input().getHoverName().copy().withStyle(ChatFormatting.WHITE));
        ResourceLocation inId = BuiltInRegistries.FLUID.getKey(r.input().getFluid());
        if (inId != null) lines.add(Component.literal(inId + "  ×" + r.input().getAmount() + " mB")
                .withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.literal(""));
        lines.add(Component.translatable("gui.fluidconverter.tank.output").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        lines.add(r.output().getHoverName().copy().withStyle(ChatFormatting.WHITE));
        ResourceLocation outId = BuiltInRegistries.FLUID.getKey(r.output().getFluid());
        if (outId != null) lines.add(Component.literal(outId + "  ×" + r.output().getAmount() + " mB")
                .withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private String fitToWidth(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        while (text.length() > 1 && this.font.width(text + ellipsis) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    private static String compactName(Fluid f) {
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(f);
        if (id == null) return "?";
        if (id.getNamespace().equals("minecraft")) return id.getPath();
        return id.getPath() + "[" + id.getNamespace().substring(0, Math.min(3, id.getNamespace().length())) + "]";
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (menu.clientRecipes().size() != lastRenderedListSize) {
            rebuildButtons();
        }
    }

    private static final class ArrowToggleButton extends Button {
        private final BooleanSupplier reverseState;

        ArrowToggleButton(int x, int y, int w, int h, BooleanSupplier reverseState, Runnable onClick) {
            super(x, y, w, h, Component.empty(), b -> onClick.run(), DEFAULT_NARRATION);
            this.reverseState = reverseState;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            int x2 = x + this.getWidth();
            int y2 = y + this.getHeight();
            boolean hover = this.isHoveredOrFocused();
            boolean reverse = reverseState.getAsBoolean();

            g.fill(x, y, x2, y2, 0xFFC6C6C6);
            if (hover) {
                int frame = 0xFF373737;
                g.fill(x, y, x2, y + 1, frame);
                g.fill(x, y2 - 1, x2, y2, frame);
                g.fill(x, y, x + 1, y2, frame);
                g.fill(x2 - 1, y, x2, y2, frame);
            }

            String glyph = reverse ? "↔" : "→";
            int color = reverse ? 0xFF2266AA : 0xFF555555;
            if (hover) color = reverse ? 0xFF3388CC : 0xFF222222;
            Font font = Minecraft.getInstance().font;
            int textW = font.width(glyph);
            int tx = x + (this.getWidth() - textW) / 2;
            int ty = y + (this.getHeight() - font.lineHeight) / 2 + 1;
            g.drawString(font, glyph, tx, ty, color, false);
        }
    }

    private static final class SmallButton extends Button {
        private final float scale;

        SmallButton(int x, int y, int w, int h, Component text, OnPress action, float scale) {
            super(x, y, w, h, text, action, DEFAULT_NARRATION);
            this.scale = scale;
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

            Component msg = this.getMessage();
            Font font = Minecraft.getInstance().font;
            int textW = font.width(msg);
            int color = hover ? 0xFFFFFFFF : 0xFFE6E6E6;

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
