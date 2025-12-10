package dev.wolfieboy09.qtech.integration;

import dev.wolfieboy09.qtech.api.Pair;
import dev.wolfieboy09.qtech.integration.cctweaked.CCTweakedPlugin;
import dev.wolfieboy09.qtech.integration.kubejs.events.KubeEventHandlers;
import dev.wolfieboy09.qtech.integration.kubejs.gas.KubeJSGasIngredients;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.LoadingModList;

import java.util.List;
import java.util.function.Consumer;

public class IntegrationLoader {
    private List<Pair<String, Consumer<IEventBus>>> listOfStuff;
    private static boolean constructed = false;

    public IntegrationLoader() {
        listOfStuff = List.of(
                Pair.of("computercraft", bus -> CCTweakedPlugin.register()),
                Pair.of("kubejs", bus -> {
                    KubeJSGasIngredients.register(bus);
                    bus.register(KubeEventHandlers.class);
                })
        );
    }

    public void init(IEventBus bus) {
        if (constructed) {
            throw new IllegalStateException("[QTech] IntegrationLoader.init() called more than once. This should never happen.");
        }
        constructed = true;
        for (var pair : listOfStuff) {
            if (modPresent(pair.left())) {
                pair.right().accept(bus);
            }
        }

        // allow garbage collection
        listOfStuff = null;
    }

    private static boolean modPresent(String namespace) {
        return LoadingModList.get().getModFileById(namespace) != null;
    }
}
