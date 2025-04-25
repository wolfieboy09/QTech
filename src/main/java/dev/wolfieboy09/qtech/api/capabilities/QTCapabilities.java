package dev.wolfieboy09.qtech.api.capabilities;

import dev.wolfieboy09.qtech.api.capabilities.gas.IGasHandler;
import dev.wolfieboy09.qtech.api.capabilities.gas.IGasHandlerItem;
import dev.wolfieboy09.qtech.api.util.ResourceHelper;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;


public class QTCapabilities {
    private QTCapabilities() {}

    public static final class GasStorage {
        public static final BlockCapability<IGasHandler, @Nullable Direction> BLOCK = BlockCapability.createSided(ResourceHelper.asResource("gas_handler"), IGasHandler.class);
        public static final EntityCapability<IGasHandler, @Nullable Direction> ENTITY = EntityCapability.createSided(ResourceHelper.asResource("gas_handler"), IGasHandler.class);
        public static final ItemCapability<IGasHandlerItem, @Nullable Void> ITEM = ItemCapability.createVoid(ResourceHelper.asResource("gas_handler"), IGasHandlerItem.class);
    }
}
