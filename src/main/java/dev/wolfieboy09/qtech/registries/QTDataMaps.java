package dev.wolfieboy09.qtech.registries;

import dev.wolfieboy09.qtech.api.datamaps.NullZoneReplaceable;
import dev.wolfieboy09.qtech.api.datamaps.SmelteryFuel;
import dev.wolfieboy09.qtech.api.util.ResourceHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.NotNull;

public class QTDataMaps {
    // We need them synced and require the client to have it because it'll show up on the recipe viewer for fuel info

    public static final DataMapType<Item, SmelteryFuel> SMELTERY_FUEL_ITEM = DataMapType.builder(
            ResourceHelper.asResource("smeltery_fuel"),
            Registries.ITEM,
            SmelteryFuel.CODEC
    ).synced(
            SmelteryFuel.CODEC,
            true
    ).build();

    public static final DataMapType<Fluid, SmelteryFuel> SMELTERY_FUEL_FLUID = DataMapType.builder(
            ResourceHelper.asResource("smeltery_fuel"),
            Registries.FLUID,
            SmelteryFuel.CODEC
    ).synced(
            SmelteryFuel.CODEC,
            true
    ).build();

    public static final DataMapType<Item, NullZoneReplaceable> NULL_ZONE_REPLACEABLE = DataMapType.builder(
            ResourceHelper.asResource("nullzone_replaceable"),
            Registries.ITEM,
            NullZoneReplaceable.CODEC
    ).build();

    public static void register(@NotNull RegisterDataMapTypesEvent event) {
        event.register(SMELTERY_FUEL_ITEM);
        event.register(SMELTERY_FUEL_FLUID);
        event.register(NULL_ZONE_REPLACEABLE);
    }
}
