package dev.wolfieboy09.qtech;

import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.datamaps.NullZoneReplaceable;
import dev.wolfieboy09.qtech.api.registry.QTRegistries;
import dev.wolfieboy09.qtech.api.util.QTUtil;
import dev.wolfieboy09.qtech.dimensions.NullZoneSpecialEffects;
import dev.wolfieboy09.qtech.particles.GasParticleProvider;
import dev.wolfieboy09.qtech.registries.QTDamageTypes;
import dev.wolfieboy09.qtech.registries.QTDataMaps;
import dev.wolfieboy09.qtech.registries.QTDimensions;
import dev.wolfieboy09.qtech.registries.QTParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.NewRegistryEvent;

@NothingNullByDefault
public class QTEvents {
    public static void registerRegistries(NewRegistryEvent event) {
        event.register(QTRegistries.GAS);
        event.register(QTRegistries.MULTIBLOCK_TYPE);
        event.register(QTRegistries.GAS_INGREDIENT_TYPES);
    }

    public static void particle(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(QTParticleTypes.GAS_PARTICLE.get(), GasParticleProvider::new);
    }

    public static void registerCustomDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(QTDimensions.NULLZONE_LOCATION, new NullZoneSpecialEffects());
    }

    // If the ender pearl is removed from the data map, still prevent it anyway
    // Also good for if you somehow snuggled one into the dimension
    public static void preventEnderPearlInNullZone(EntityTeleportEvent.EnderPearl event) {
        if (event.getPearlEntity().level().dimension() == QTDimensions.NULLZONE) {
            event.setCanceled(true);
            event.getEntity().hurt(new DamageSource(QTUtil.damageTypeHolder(event.getEntity().level(), QTDamageTypes.NULLZONE_PEARL_THROWN), event.getEntity(), null, null), 2f);
        }
    }

    public static void onDimensionChange(EntityTravelToDimensionEvent event) {
        if (event.getDimension() == QTDimensions.NULLZONE) {
            IItemHandler inventory = event.getEntity().getCapability(Capabilities.ItemHandler.ENTITY);
            if (inventory != null) {
                searchItemContents(inventory);
            }
        }
    }

    private static void searchItemContents(IItemHandler inventory) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);

            // Backpacks and other containers also need to be searched. (What can go wrong...)
            // Nothing is safe.
            IItemHandler nestedInventory = stack.getCapability(Capabilities.ItemHandler.ITEM);
            if (nestedInventory != null) {
                searchItemContents(nestedInventory);
            }

            interchangeItem(inventory, i);
        }
    }

    private static void interchangeItem(IItemHandler inventory, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        NullZoneReplaceable replaceable = stack.getItemHolder().getData(QTDataMaps.NULL_ZONE_REPLACEABLE);
        if (replaceable != null) {
            int count = stack.getCount();
            // Take out the old item and replace it
            ItemStack extracted = inventory.extractItem(slot, count, false);
            if (!extracted.isEmpty()) {
                ItemStack newStack = new ItemStack(replaceable.replaceWith(), extracted.getCount());
                ItemStack remainder = inventory.insertItem(slot, newStack, false);
                // If insertion failed, try to put the original back
                if (!remainder.isEmpty()) {
                    inventory.insertItem(slot, extracted, false);
                }
            }
        }
    }
}