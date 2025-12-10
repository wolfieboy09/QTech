package dev.wolfieboy09.qtech;

import com.mojang.logging.LogUtils;
import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.multiblock.MultiblockPatternManager;
import dev.wolfieboy09.qtech.api.multiblock.tracking.MultiblockTracker;
import dev.wolfieboy09.qtech.client.ClientReloadListener;
import dev.wolfieboy09.qtech.client.KeyInputHandler;
import dev.wolfieboy09.qtech.component.QTDataComponents;
import dev.wolfieboy09.qtech.config.ClientConfig;
import dev.wolfieboy09.qtech.integration.IntegrationLoader;
import dev.wolfieboy09.qtech.registries.*;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

@NothingNullByDefault
@Mod(QuantiumizedTech.MOD_ID)
public class QuantiumizedTech {
    public static final String MOD_ID = "qtech";
    public static final Logger LOGGER = LogUtils.getLogger();

    public QuantiumizedTech(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerKeys);
        modEventBus.addListener(this::onClientReload);
        NeoForge.EVENT_BUS.addListener(this::onServerReload);

        QTDataComponents.register(modEventBus);
        QTItems.register(modEventBus);
        QTBlockEntities.register(modEventBus);
        QTMultiblockTypes.register(modEventBus);
        QTBlocks.register(modEventBus);
        QTRecipes.register(modEventBus); //TODO: Start migrating to the new recipe system
        QTRecipeTypes.register(modEventBus);
        QTCreativeTab.register(modEventBus);
        QTMenuTypes.register(modEventBus);
        QTParticleTypes.register(modEventBus);
        QTGasses.register(modEventBus);
        QTEffects.register(modEventBus);
        QTEntities.register(modEventBus);
        QTGasIngredientTypes.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(MultiblockTracker::onBlockBreak);

        modEventBus.addListener(QTPackets::register);
        modEventBus.addListener(QTEntities::registerRenderers);

        // Register before neoforge because wiki says so
        modEventBus.addListener(EventPriority.HIGH, QTEvents::registerRegistries);
        modEventBus.addListener(QTEvents::particle);
        modEventBus.addListener(QTEvents::registerCustomDimensionEffects);
        NeoForge.EVENT_BUS.addListener(QTEvents::preventEnderPearlInNullZone);

        NeoForge.EVENT_BUS.addListener(QTEvents::onDimensionChange);
        //modEventBus.register(QTClientEvents.class);

        new IntegrationLoader().init(modEventBus);

        modEventBus.addListener(QTDataMaps::register);

        //modEventBus.addListener(QTEntities::registerAttributes);

        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Lets dive into getting small");
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        KeyInputHandler.register(event);
    }

    private void onClientReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ClientReloadListener());
    }

    private void onServerReload(AddReloadListenerEvent event) {
        event.addListener(new MultiblockPatternManager());
    }
}
