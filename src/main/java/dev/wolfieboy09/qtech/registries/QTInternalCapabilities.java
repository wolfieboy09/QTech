package dev.wolfieboy09.qtech.registries;

import dev.wolfieboy09.qtech.QuantiumizedTech;
import dev.wolfieboy09.qtech.api.capabilities.QTCapabilities;
import dev.wolfieboy09.qtech.api.capabilities.gas.IGasHandlerItem;
import dev.wolfieboy09.qtech.api.components.GasCanisterComponent;
import dev.wolfieboy09.qtech.api.registry.gas.GasStack;
import dev.wolfieboy09.qtech.block.disk_assembler.DiskAssemblerBlockEntity;
import dev.wolfieboy09.qtech.component.QTDataComponents;
import dev.wolfieboy09.qtech.item.ItemStorageDisk;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * <b>This class is just the normal stuff for capability registration.
 * Use {@link QTCapabilities} instead as this gives you zero purpose</b>
 */
@EventBusSubscriber(modid = QuantiumizedTech.MOD_ID)
public class QTInternalCapabilities {
    @SubscribeEvent
    public static void register(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, QTBlockEntities.DISK_ASSEMBLER.get(), DiskAssemblerBlockEntity::getEnergyHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, QTBlockEntities.DISK_ASSEMBLER.get(), (block, dir) -> block.getInventoryHandler());

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, QTBlockEntities.CREATIVE_ENERGY_BLOCK.get(), (block, dir) -> block.getEnergyHandler());

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, QTBlockEntities.SMELTERY.get(), (block, dir) -> block.getInventory());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, QTBlockEntities.SMELTERY.get(), (block, dir) -> block.getFluidHandler());

        event.registerBlockEntity(QTCapabilities.GasStorage.BLOCK, QTBlockEntities.GAS_CANISTER.get(), (block, dir) -> block.getGasTank());

       event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, QTBlockEntities.ITEM_INPUT_HATCH.get(), (block, dir) -> block.getCapabilities().getFirst());

       event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, QTBlockEntities.ENERGY_INPUT_HATCH.get(), (block, dir) -> block.getCapabilities().getFirst());

        event.registerItem(
                QTCapabilities.GasStorage.ITEM,
                (itemStack, context) -> {
                    GasCanisterComponent handler = itemStack.get(QTDataComponents.GAS_CANISTER_COMPONENT.get());
                    return handler == null ? null : new GasItemHandlerWrapper(itemStack, handler);
                },
                QTBlocks.GAS_CANISTER
        );

        for (DeferredHolder<Item, ? extends Item> item : QTItems.ITEMS.getEntries()) {
            if (item.get() instanceof ItemStorageDisk) {
                event.registerItem(
                        Capabilities.ItemHandler.ITEM,
                        (stack, ctx) -> ((ItemStorageDisk) stack.getItem()).getInventoryHandler(stack),
                        item.get()
                );
            }
        }

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                QTBlockEntities.ITEM_INPUT_HATCH.get(),
                (hatch, dir) -> {
                    List<IItemHandler> caps = hatch.getCapabilities();
                    return caps.isEmpty() ? null : caps.getFirst();
                }
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                QTBlockEntities.ENERGY_INPUT_HATCH.get(),
                (hatch, dir) -> {
                    List<IEnergyStorage> caps = hatch.getCapabilities();
                    return caps.isEmpty() ? null : caps.getFirst();
                }
        );
    }

    public record GasItemHandlerWrapper(ItemStack itemStack, GasCanisterComponent handler) implements IGasHandlerItem {
        @Override
        public ItemStack getContainer() {
            return this.itemStack;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public GasStack getGasInTank(int index) {
            return this.handler.getGasTank().getGas();
        }

        @Override
        public int getTankCapacity(int index) {
            return this.handler.getGasTank().getCapacity();
        }

        @Override
        public boolean isGasValid(int index, GasStack gasStack) {
            return this.handler.getGasTank().isGasValid(index, gasStack);
        }


        // In order to prevent quantum entanglement with the canisters, we must copy the data component

        @Override
        public int fill(GasStack gasStack, boolean simulate) {
            GasCanisterComponent component = new GasCanisterComponent(this.handler.gasTankHandler());
            int fill = component.getGasTank().fill(gasStack, simulate);
            this.itemStack.set(QTDataComponents.GAS_CANISTER_COMPONENT.get(), component);
            return fill;
        }

        @Override
        public GasStack drain(GasStack gasStack, boolean simulate) {
            GasCanisterComponent component = new GasCanisterComponent(this.handler.gasTankHandler());
            GasStack stack = component.getGasTank().drain(gasStack, simulate);
            this.itemStack.set(QTDataComponents.GAS_CANISTER_COMPONENT.get(), component);
            return stack;
        }

        @Override
        public GasStack drain(int maxDrain, boolean simulate) {
            GasCanisterComponent component = new GasCanisterComponent(this.handler.gasTankHandler());
            GasStack stack = component.getGasTank().drain(maxDrain, simulate);
            this.itemStack.set(QTDataComponents.GAS_CANISTER_COMPONENT.get(), component);
            return stack;
        }
    }
}
