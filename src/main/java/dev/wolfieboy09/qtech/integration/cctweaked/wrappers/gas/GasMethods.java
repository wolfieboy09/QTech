package dev.wolfieboy09.qtech.integration.cctweaked.wrappers.gas;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.generic.GenericPeripheral;
import dan200.computercraft.shared.util.CapabilityUtil;
import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.capabilities.QTCapabilities;
import dev.wolfieboy09.qtech.api.capabilities.gas.IGasHandler;
import dev.wolfieboy09.qtech.api.registry.QTRegistries;
import dev.wolfieboy09.qtech.api.registry.gas.Gas;
import dev.wolfieboy09.qtech.api.registry.gas.GasStack;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


// Thanks to CC: Tweaked code for this code and the abstract one that I modified for gasses
@NothingNullByDefault
public final class GasMethods extends AbstractGasMethods<IGasHandler> {
    @Override
    @LuaFunction(mainThread = true)
    public Map<Integer, Map<String, ?>> tanks(IGasHandler gasses) {
        Map<Integer, Map<String, ?>> result = new HashMap<>();
        int size = gasses.getTanks();
        for (int i = 0; i < size; i++) {
            GasStack stack = gasses.getGasInTank(i);
            if (!stack.isEmpty()) result.put(i + 1, detailForGas(stack));
        }
        return result;
    }

    @Override
    @LuaFunction(mainThread = true)
    public int pushGas(IGasHandler from, IComputerAccess computer, String toName, Optional<Integer> limit, Optional<String> gasName) throws LuaException {
        Gas gas = gasName.map(s -> QTRegistries.GAS.get(ResourceLocation.parse(s))).orElse(null);
        IPeripheral location = computer.getAvailablePeripheral(toName);
        if (location == null) throw new LuaException("Target '" + toName + "' does not exist");
        IGasHandler to = extractHandler(location);
        if (to == null) throw new LuaException("Target '" + toName + "' is not an tank");
        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        if (actualLimit <= 0) throw new LuaException("Limit must be > 0");
        return gas == null ? moveGas(from, actualLimit, to) : moveGas(from, new GasStack(gas, actualLimit), to);
    }

    @Override
    @LuaFunction(mainThread = true)
    public int pullGas(IGasHandler to, IComputerAccess computer, String fromName, Optional<Integer> limit, Optional<String> gasName) throws LuaException {
        Gas gas = gasName.map(s -> QTRegistries.GAS.get(ResourceLocation.parse(s))).orElse(null);
        IPeripheral location = computer.getAvailablePeripheral(fromName);
        if (location == null) throw new LuaException("Target '" + fromName + "' does not exist");
        IGasHandler from = extractHandler(location);
        if (from == null) throw new LuaException("Target '" + fromName + "' is not an tank");
        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        if (actualLimit <= 0) throw new LuaException("Limit must be > 0");
        return gas == null
                ? moveGas(from, actualLimit, to)
                : moveGas(from, new GasStack(gas, actualLimit), to);
    }


    private static int moveGas(IGasHandler from, int limit, IGasHandler to) {
        return moveGas(from, from.drain(limit, true), limit, to);
    }

    private static int moveGas(IGasHandler from, GasStack fluid, IGasHandler to) {
        return moveGas(from, from.drain(fluid, true), fluid.getAmount(), to);
    }

    private static int moveGas(IGasHandler from, GasStack extracted, int limit, IGasHandler to) {
        if (extracted.getAmount() <= 0) return 0;
        extracted = extracted.copy();
        extracted.setAmount(Math.min(extracted.getAmount(), limit));
        int inserted = to.fill(extracted.copy(), false);
        if (inserted <= 0) return 0;
        extracted.setAmount(inserted);
        from.drain(extracted, false);
        return inserted;
    }


    @Nullable
    private static IGasHandler extractHandler(IPeripheral peripheral) {
        Object object = peripheral.getTarget();
        Direction direction = peripheral instanceof GenericPeripheral sided ? sided.side() : null;
        if (object instanceof BlockEntity blockEntity) {
            if (blockEntity.isRemoved()) return null;
            Level level = blockEntity.getLevel();
            if (!(level instanceof ServerLevel serverLevel)) return null;
            IGasHandler result = CapabilityUtil.getCapability(serverLevel, QTCapabilities.GasStorage.BLOCK, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, direction);
            if (result != null) return result;
        }
        return object instanceof IGasHandler handler ? handler : null;
    }

    @Contract("_ -> new")
    private static @Unmodifiable Map<String, ?> detailForGas(GasStack stack) {
        return Map.of(
                "name", stack.getGas().getResourceLocation().toString(),
                "amount", stack.getAmount()
        );
    }
}
