package dev.wolfieboy09.qtech.datagen;

import dev.wolfieboy09.qtech.api.datamaps.NullZoneReplaceable;
import dev.wolfieboy09.qtech.api.datamaps.SmelteryFuel;
import dev.wolfieboy09.qtech.registries.QTDataMaps;
import dev.wolfieboy09.qtech.registries.QTItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.DataMapProvider;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings({"SameParameterValue", "deprecated"})
public class QTDataMapProvider extends DataMapProvider {
    protected QTDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    private static @NotNull TagKey<Item> mcTag(String tag) {
        return ItemTags.create(ResourceLocation.withDefaultNamespace(tag));
    }

    @Contract("_, _ -> new")
    private static @NotNull SmelteryFuel fuel(int burnTime, int temperature) {
        return new SmelteryFuel(burnTime, temperature);
    }

    private static @NotNull Holder.Reference<Item> mcItem(@NotNull Item item) {
        return item.builtInRegistryHolder();
    }

    private static NullZoneReplaceable nullReplace(Item item) {
        return new NullZoneReplaceable(item.builtInRegistryHolder());
    }

    @Override
    protected void gather(@NotNull HolderLookup.Provider provider) {
        builder(QTDataMaps.SMELTERY_FUEL_ITEM)
                .add(mcTag("rods/wooden"), fuel(100, 200), false)
                .add(ItemTags.COALS, fuel(1600, 1200), false)
                .add(Tags.Items.BUCKETS_LAVA, fuel(20000, 1500), false)
                .add(Tags.Items.RODS_BLAZE, fuel(2400, 2000), false)
                .add(ItemTags.PLANKS, fuel(300, 400), false)
                .add(ItemTags.LOGS_THAT_BURN, fuel(300, 400), false)
                .add(Tags.Items.STORAGE_BLOCKS_DRIED_KELP, fuel(4000, 800), false)
                .add(Tags.Items.STORAGE_BLOCKS_COAL, fuel(1600, 1800), false)
                .add(mcItem(Items.BAMBOO), fuel(50, 150), false);

        builder(QTDataMaps.SMELTERY_FUEL_FLUID)
                .add(FluidTags.LAVA, fuel(20000, 1500), false);

        builder(QTDataMaps.NULL_ZONE_REPLACEABLE)
                .add(mcItem(Items.ENDER_PEARL), nullReplace(QTItems.BROKEN_ENDER_PEARL.get()), false);
    }
}
