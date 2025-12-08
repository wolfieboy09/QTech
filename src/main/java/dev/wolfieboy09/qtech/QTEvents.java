package dev.wolfieboy09.qtech;

import dev.wolfieboy09.qtech.api.registry.QTRegistries;
import dev.wolfieboy09.qtech.dimensions.NullZoneSpecialEffects;
import dev.wolfieboy09.qtech.particles.GasParticleProvider;
import dev.wolfieboy09.qtech.registries.QTDimensions;
import dev.wolfieboy09.qtech.registries.QTItems;
import dev.wolfieboy09.qtech.registries.QTParticleTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import org.jetbrains.annotations.NotNull;

public class QTEvents {
    public static void registerRegistries(@NotNull NewRegistryEvent event) {
        event.register(QTRegistries.GAS);
        event.register(QTRegistries.MULTIBLOCK_TYPE);
        event.register(QTRegistries.GAS_INGREDIENT_TYPES);
    }

    public static void particle(@NotNull RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(QTParticleTypes.GAS_PARTICLE.get(), GasParticleProvider::new);
    }

    public static void registerCustomDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(QTDimensions.NULLZONE_LOCATION, new NullZoneSpecialEffects());
    }

    //TODO Maybe use a datamap instead...
    public static void onDimensionChange(EntityTravelToDimensionEvent event) {
        if (event.getDimension() == QTDimensions.NULLZONE) {
            IItemHandler inventory = event.getEntity().getCapability(Capabilities.ItemHandler.ENTITY);
            if (inventory != null) {
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (stack.getCapability(Capabilities.ItemHandler.ITEM) != null) {
                        searchInternal(stack, new ItemStack(Items.ENDER_PEARL), QTItems.BROKEN_ENDER_PEARL.get());
                    } else if (stack.getItem() == Items.ENDER_PEARL) {
                        ItemStack extracted = inventory.extractItem(i, stack.getCount(), false);
                        if (!extracted.isEmpty()) {
                            ItemStack newStack = new ItemStack(QTItems.BROKEN_ENDER_PEARL.get(), extracted.getCount());
                            ItemStack remainder = inventory.insertItem(i, newStack, false);
                            if (!remainder.isEmpty()) {
                                inventory.insertItem(i, extracted, false);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void searchInternal(ItemStack searchable, ItemStack toReplace, Item replaceWith) {
        if (searchable.getCapability(Capabilities.ItemHandler.ITEM) instanceof IItemHandler inventory) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);

                // Backpacks and other containers also need to be searched. (What can go wrong...)
                // Nothing is safe.
                if (stack.getCapability(Capabilities.ItemHandler.ITEM) != null) {
                    searchInternal(stack, toReplace, replaceWith);
                }

                if (stack.getItem() == toReplace.getItem()) {
                    int count = stack.getCount();
                    // Take out the old item and replace it
                    ItemStack extracted = inventory.extractItem(i, count, false);
                    if (!extracted.isEmpty()) {
                        ItemStack newStack = new ItemStack(replaceWith, extracted.getCount());
                        ItemStack remainder = inventory.insertItem(i, newStack, false);
                        // If insertion failed, try to put the original back
                        if (!remainder.isEmpty()) {
                            inventory.insertItem(i, extracted, false);
                        }
                    }
                }
            }
        }
    }
}