package dev.wolfieboy09.qtech.registries;

import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.util.ResourceHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

@NothingNullByDefault
public class QTDimensions {
    public static final ResourceKey<Level> NULLZONE = createLevel("nullzone");
    public static final ResourceKey<DimensionType> NULLZONE_TYPE = createDimension("nullzone");
    public static final ResourceLocation NULLZONE_LOCATION = resourceLocation("nullzone");

    private static ResourceKey<Level> createLevel(String id) {
        return ResourceKey.create(Registries.DIMENSION, resourceLocation(id));
    }

    private static ResourceKey<DimensionType> createDimension(String id) {
        return ResourceKey.create(Registries.DIMENSION_TYPE, resourceLocation(id));
    }

    private static ResourceLocation resourceLocation(String id) {
        return ResourceHelper.asResource(id);
    }
}
