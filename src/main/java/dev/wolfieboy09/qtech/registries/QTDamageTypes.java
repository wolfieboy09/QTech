package dev.wolfieboy09.qtech.registries;

import dev.wolfieboy09.qtech.api.util.ResourceHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public class QTDamageTypes {
    public static final ResourceKey<DamageType> OXYGEN_DEPRIVATION_DAMAGE = create("oxygen_deprivation");

    public static final ResourceKey<DamageType> NULLZONE_PEARL_THROWN = create("nullzone_pearl_thrown");

    private static ResourceKey<DamageType> create(String id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceHelper.asResource(id));
    }
}
