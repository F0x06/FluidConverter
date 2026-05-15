package me.f0x.fluidconverter.client;

import me.f0x.fluidconverter.FluidConverter;
import me.f0x.fluidconverter.ModMenus;
import me.f0x.fluidconverter.client.model.FluidConverterBakedModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.ArrayList;
import java.util.Map;

@EventBusSubscriber(modid = FluidConverter.MODID, value = Dist.CLIENT)
public final class ClientSetup {

    private static final ResourceLocation OVERLAY_INPUT_RL =
            ResourceLocation.fromNamespaceAndPath(FluidConverter.MODID, "block/overlay_input");
    private static final ResourceLocation OVERLAY_OUTPUT_RL =
            ResourceLocation.fromNamespaceAndPath(FluidConverter.MODID, "block/overlay_output");
    private static final ResourceLocation BLOCK_RL =
            ResourceLocation.fromNamespaceAndPath(FluidConverter.MODID, "fluid_converter");

    private ClientSetup() {}

    public static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.FLUID_CONVERTER.get(), FluidConverterScreen::new);
        event.register(ModMenus.FLUID_CONVERTER_ADMIN.get(), FluidConverterAdminScreen::new);
        event.register(ModMenus.FLUID_CONVERTER_SIDE_CONFIG.get(), FluidConverterSideConfigScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(OVERLAY_INPUT_RL));
        event.register(ModelResourceLocation.standalone(OVERLAY_OUTPUT_RL));
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        BakedModel inOverlay = models.get(ModelResourceLocation.standalone(OVERLAY_INPUT_RL));
        BakedModel outOverlay = models.get(ModelResourceLocation.standalone(OVERLAY_OUTPUT_RL));
        if (inOverlay == null || outOverlay == null) return;

        for (ModelResourceLocation key : new ArrayList<>(models.keySet())) {
            if (!BLOCK_RL.equals(key.id())) continue;
            if (ModelResourceLocation.STANDALONE_VARIANT.equals(key.getVariant())) continue;
            BakedModel original = models.get(key);
            if (original == null) continue;
            models.put(key, new FluidConverterBakedModel(original, inOverlay, outOverlay));
        }
    }
}
