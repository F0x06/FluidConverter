package me.f0x.fluidconverter;

import me.f0x.fluidconverter.config.Config;
import me.f0x.fluidconverter.learned.LearnedRecipesStore;
import me.f0x.fluidconverter.network.ForgetRecipePayload;
import me.f0x.fluidconverter.network.LearnRecipePayload;
import me.f0x.fluidconverter.network.RequestLearnedRecipesPayload;
import me.f0x.fluidconverter.network.SetRedstoneModePayload;
import me.f0x.fluidconverter.network.SyncLearnedRecipesPayload;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(FluidConverter.MODID)
public class FluidConverter {
    public static final String MODID = "fluidconverter";

    public FluidConverter(IEventBus modBus, ModContainer container) {
        ModBlocks.REGISTER.register(modBus);
        ModItems.REGISTER.register(modBus);
        ModBlockEntities.REGISTER.register(modBus);
        ModMenus.REGISTER.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerCapabilities);
        modBus.addListener(this::buildCreativeTab);
        modBus.addListener(this::registerPayloads);

        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(me.f0x.fluidconverter.event.WrenchHandler::onRightClick);

        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            me.f0x.fluidconverter.client.ClientSetup.registerConfigScreen(container);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private void onServerStarting(final ServerStartingEvent event) {
        LearnedRecipesStore.get().init(event.getServer());
    }

    private void onRegisterCommands(final RegisterCommandsEvent event) {
        me.f0x.fluidconverter.command.FluidConverterCommands.register(event.getDispatcher());
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.FLUID_CONVERTER.get(),
                (be, side) -> be.getFluidHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.FLUID_CONVERTER.get(),
                (be, side) -> be.getEnergyHandler(side)
        );
    }

    private void buildCreativeTab(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.FLUID_CONVERTER_ITEM.get());
        }
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar(MODID).versioned("1");
        reg.playToServer(LearnRecipePayload.TYPE, LearnRecipePayload.STREAM_CODEC, LearnRecipePayload::handle);
        reg.playToServer(ForgetRecipePayload.TYPE, ForgetRecipePayload.STREAM_CODEC, ForgetRecipePayload::handle);
        reg.playToServer(RequestLearnedRecipesPayload.TYPE, RequestLearnedRecipesPayload.STREAM_CODEC, RequestLearnedRecipesPayload::handle);
        reg.playToServer(SetRedstoneModePayload.TYPE, SetRedstoneModePayload.STREAM_CODEC, SetRedstoneModePayload::handle);
        reg.playToClient(SyncLearnedRecipesPayload.TYPE, SyncLearnedRecipesPayload.STREAM_CODEC, SyncLearnedRecipesPayload::handle);
    }
}
