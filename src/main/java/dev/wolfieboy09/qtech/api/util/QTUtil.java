package dev.wolfieboy09.qtech.api.util;

import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * A set of un-categorized helper methods
 */
@NothingNullByDefault
public class QTUtil {
    /**
     * Helper method to get a {@link Holder} of a {@link DamageType}
     * @param level The {@link Level} to get the registry access from
     * @param damageTypeResourceKey The resource key to search for
     * @return {@link Holder} of a {@link DamageType}
     */
    public static Holder<DamageType> damageTypeHolder(Level level, ResourceKey<DamageType> damageTypeResourceKey) throws IllegalStateException {
        return level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(damageTypeResourceKey);
    }
}
