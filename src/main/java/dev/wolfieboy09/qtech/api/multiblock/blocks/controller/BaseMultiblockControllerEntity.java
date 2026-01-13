package dev.wolfieboy09.qtech.api.multiblock.blocks.controller;

import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.multiblock.MultiblockHatchRule;
import dev.wolfieboy09.qtech.api.multiblock.MultiblockPattern;
import dev.wolfieboy09.qtech.api.multiblock.MultiblockPatternManager;
import dev.wolfieboy09.qtech.api.multiblock.blocks.hatch.BaseMultiblockHatchEntity;
import dev.wolfieboy09.qtech.api.multiblock.tracking.MultiblockTracker;
import dev.wolfieboy09.qtech.api.registry.QTRegistries;
import dev.wolfieboy09.qtech.api.registry.multiblock_type.MultiblockType;
import dev.wolfieboy09.qtech.block.AbstractEnergyBlockEntity;
import dev.wolfieboy09.qtech.packets.HideMultiblockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@NothingNullByDefault
public class BaseMultiblockControllerEntity extends AbstractEnergyBlockEntity {
    private static final Logger log = LoggerFactory.getLogger(BaseMultiblockControllerEntity.class);
    private boolean formed = false;
    private MultiblockType multiblockType;
    protected @Nullable MultiblockPattern currentPattern = null;
    protected Set<BlockPos> trackedPositions = new HashSet<>();

    private final Map<Block, Set<BlockPos>> blockCache = new HashMap<>();
    private final Map<TagKey<Block>, Set<BlockPos>> tagCache = new HashMap<>();

    /**
     * Energy for the multiblock is a default of <code>100,000</code> (100k) FE capacity
     */
    public BaseMultiblockControllerEntity(BlockEntityType<? extends BaseMultiblockControllerEntity> type, MultiblockType multiblockType, BlockPos pos, BlockState state) {
        this(type, multiblockType, pos, state, 100_000);
    }

    public BaseMultiblockControllerEntity(BlockEntityType<? extends BaseMultiblockControllerEntity> type, MultiblockType multiblockType, BlockPos pos, BlockState state, int capacity) {
        this(type, multiblockType, pos, state, capacity, capacity);
    }

    public BaseMultiblockControllerEntity(BlockEntityType<? extends BaseMultiblockControllerEntity> type, MultiblockType multiblockType, BlockPos pos, BlockState state, int capacity, int transferRate) {
        this(type, multiblockType, pos, state, capacity, transferRate, transferRate);
    }

    public BaseMultiblockControllerEntity(BlockEntityType<? extends BaseMultiblockControllerEntity> type, MultiblockType multiblockType, BlockPos pos, BlockState blockState, int capacity, int maxReceive, int maxExtract) {
        super(type, pos, blockState, capacity, maxReceive, maxExtract);
        this.multiblockType = multiblockType;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Formed", this.formed);
        tag.putString("MultiblockType", this.multiblockType.getLocation().toString());
        if (this.currentPattern != null) {
            tag.putString("PatternName", this.currentPattern.name());
        }

        ListTag positionsList = new ListTag();
        for (BlockPos pos : trackedPositions) {
            positionsList.add(LongTag.valueOf(pos.asLong()));
        }

        tag.put("TrackedPositions", positionsList);
        super.saveAdditional(tag, registries);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        this.formed = tag.getBoolean("Formed");
        this.multiblockType = Objects.requireNonNull(QTRegistries.MULTIBLOCK_TYPE.get(ResourceLocation.parse(tag.getString("MultiblockType"))));
        if (tag.contains("PatternName")) {
            String patternName = tag.getString("PatternName");
            List<MultiblockPattern> patterns = MultiblockPatternManager.getAllPatternsForType(this.multiblockType);
            for (MultiblockPattern pattern : patterns) {
                if (pattern.name().equals(patternName)) {
                    this.currentPattern = pattern;
                    break;
                }
            }
        }

        this.trackedPositions.clear();
        invalidateCaches();
        if (tag.contains("TrackedPositions")) {
            ListTag positionsList = tag.getList("TrackedPositions", CompoundTag.TAG_LONG);
            for (Tag posTag : positionsList) {
                if (posTag instanceof LongTag longTag) {
                    this.trackedPositions.add(BlockPos.of(longTag.getAsLong()));
                }
            }
        }

        // Re-register with tracker if formed
        if (this.formed && this.level != null) {
            MultiblockTracker.registerMultiblock(this.level, getBlockPos(), this.trackedPositions);
        }

        super.loadAdditional(tag, registries);
    }

    public boolean isFormed() {
        return this.formed;
    }

    protected void formMultiblock(MultiblockPattern pattern) {
        if (this.level == null || this.level.isClientSide()) return;
        PacketDistributor.sendToAllPlayers(new HideMultiblockPattern());
        this.formed = true;
        this.currentPattern = pattern;

        // Get all positions that are part of this multiblock
        Map<BlockPos, Character> positions = pattern.getAllPositions(this.getBlockPos(), this.level.getBlockState(getBlockPos()).getValue(BaseMultiblockController.FACING));
        this.trackedPositions.clear();
        this.trackedPositions.addAll(positions.keySet());

        invalidateCaches();

        // Register this controller with the multiblock tracker
        MultiblockTracker.registerMultiblock(level, this.getBlockPos(), this.trackedPositions);

        // Mark controller as formed
        if (level != null) {
            BlockState state = getBlockState();
            if (state.hasProperty(BaseMultiblockController.FORMED)) {
                level.setBlock(getBlockPos(), state.setValue(BaseMultiblockController.FORMED, true), 3);
            }
        }

        // Call hook for subclasses
        onFormed(pattern);

        setChanged();
    }

    public void breakMultiblock() {
        if (!this.formed) return;

        this.formed = false;

        // Unregister from tracker
        MultiblockTracker.unregisterMultiblock(level, this.getBlockPos());

        invalidateCaches();
        // Call hook for subclasses
        onBroken();

        setChanged();
    }

    public void validateStructure() {
        if (!formed || currentPattern == null || level == null) return;

        // Check if structure is still valid
        if (!currentPattern.matches(level, getBlockPos(), getBlockState())) {
            breakMultiblock();
            // Update block state
            BlockState state = getBlockState();
            if (state.hasProperty(BaseMultiblockController.FORMED)) {
                level.setBlock(getBlockPos(), state.setValue(BaseMultiblockController.FORMED, false), 3);
            }
        }
    }


    public void attemptFormation() {
        if (this.level == null || this.level.isClientSide()) return;
        List<MultiblockPattern> possiblePatterns = MultiblockPatternManager.getAllPatternsForType(this.multiblockType);
        for (MultiblockPattern pattern : possiblePatterns) {
            if (pattern.matches(this.level, this.getBlockPos(), this.getBlockState())) {
                formMultiblock(pattern);
                return;
            }
        }
        this.formed = false;
    }

    public void tick() {}

    public Set<BlockPos> getBlocksOf(Block block) {
        if (this.level == null || this.level.isClientSide()) return Set.of();
        return this.blockCache.computeIfAbsent(block, b ->
                this.trackedPositions.stream()
                        .filter(pos -> this.level.getBlockState(pos).is(b))
                        .collect(Collectors.toUnmodifiableSet())
        );
    }

    public Set<BlockPos> getBlocksOf(TagKey<Block> tag) {
        if (this.level == null || this.level.isClientSide()) return Set.of();
        return this.tagCache.computeIfAbsent(tag, t ->
                this.trackedPositions.stream()
                        .filter(pos -> this.level.getBlockState(pos).is(t))
                        .collect(Collectors.toUnmodifiableSet())
        );
    }

    @OverridingMethodsMustInvokeSuper
    public void invalidateCaches() {
        this.blockCache.clear();
        this.tagCache.clear();
        this.trackedPositions.clear();
        this.currentPattern = null;
    }

    protected void onFormed(MultiblockPattern pattern) {
    }

    protected void onBroken() {
    }

    public Set<BlockPos> getTrackedPositions() {
        return Collections.unmodifiableSet(this.trackedPositions);
    }

    public Set<BlockPos> getHatches(BiPredicate<BaseMultiblockHatchEntity<?>, MultiblockHatchRule> filter) {
        if (this.level == null || this.level.isClientSide) return Set.of();

        Set<BlockPos> found = new HashSet<>();
        for (BlockPos pos : getTrackedPositions()) {
            BlockEntity blockEntity = this.level.getBlockEntity(pos);
            if (blockEntity instanceof BaseMultiblockHatchEntity<?> hatch
                    && filter.test(hatch, hatch.getHatchRules())) {
                found.add(pos);
            }
        }
        return found;
    }

    /**
     * Gets all capability handlers of a specific {@link BlockCapability} exposed by {@link BaseMultiblockHatchEntity}
     * @param <C> The capability handler type (e.g. {@code IItemHandler}, {@code IFluidHandler})
     * @param capability The {@link BlockCapability} to search for
     * @return A set of all the found handler instances
     */
    @SuppressWarnings("unchecked")
    public <C> Set<C> getHandlersOfType(BlockCapability<C, @Nullable Direction> capability) {
        if (this.level == null || this.level.isClientSide()) return Set.of();

        Set<C> handlers = new HashSet<>();

        for (BlockPos pos : getTrackedPositions()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof BaseMultiblockHatchEntity<?> hatch)) continue;

            if (hatch.getBlockCapability() == capability) {
                for (Object cap : hatch.getCapabilities()) {
                    try {
                        C handler = (C) cap;
                        handlers.add(handler);
                    } catch (ClassCastException ignored) {
                    }
                }
            }
        }

        return handlers;
    }


    /**
     * Collects every capability instance exposed by any {@link BaseMultiblockHatchEntity}
     * @return An immutable {@link Set} containing all instances of any capability
     */
    public Set<Object> getAllHandlers() {
        if (this.level == null || this.level.isClientSide()) return Set.of();

        Set<Object> all = new HashSet<>();

        for (BlockPos pos : getTrackedPositions()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BaseMultiblockHatchEntity<?> hatch) {
                all.addAll(hatch.getCapabilities());
            }
        }

        return all;
    }

    public List<IItemHandler> getItemInputs() {
        if (level == null || level.isClientSide) return List.of();

        List<IItemHandler> handlers = new ArrayList<>();
        for (BlockPos pos : getTrackedPositions()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof BaseMultiblockHatchEntity<?> hatch)) continue;
            if (!hatch.getHatchRules().allowInsert()) continue;

            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }

    /**
     * Gets all item handlers from hatches that allow extraction
     */
    public List<IItemHandler> getItemOutputs() {
        if (level == null || level.isClientSide) return List.of();

        List<IItemHandler> handlers = new ArrayList<>();
        for (BlockPos pos : getTrackedPositions()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof BaseMultiblockHatchEntity<?> hatch)) continue;
            if (!hatch.getHatchRules().allowExtract()) continue;

            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }

    /**
     * Gets all energy handlers from hatches that allow insertion
     */
    public List<IEnergyStorage> getEnergyInputs() {
        if (level == null || level.isClientSide) return List.of();

        List<IEnergyStorage> handlers = new ArrayList<>();
        for (BlockPos pos : getTrackedPositions()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof BaseMultiblockHatchEntity<?> hatch)) continue;
            if (!hatch.getHatchRules().allowInsert()) continue;

            IEnergyStorage handler = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }

    /**
     * Gets all energy handlers from hatches that allow extraction
     */
    public List<IEnergyStorage> getEnergyOutputs() {
        if (level == null || level.isClientSide) return List.of();

        List<IEnergyStorage> handlers = new ArrayList<>();
        for (BlockPos pos : getTrackedPositions()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof BaseMultiblockHatchEntity<?> hatch)) continue;
            if (!hatch.getHatchRules().allowExtract()) continue;

            IEnergyStorage handler = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }

    /**
     * Gets all fluid handlers from hatches that allow insertion
     */
    public List<IFluidHandler> getFluidInputs() {
        if (level == null || level.isClientSide) return List.of();

        List<IFluidHandler> handlers = new ArrayList<>();
        for (BlockPos pos : getTrackedPositions()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof BaseMultiblockHatchEntity<?> hatch)) continue;
            if (!hatch.getHatchRules().allowInsert()) continue;

            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }

    /**
     * Gets all fluid handlers from hatches that allow extraction
     */
    public List<IFluidHandler> getFluidOutputs() {
        if (level == null || level.isClientSide) return List.of();

        List<IFluidHandler> handlers = new ArrayList<>();
        for (BlockPos pos : getTrackedPositions()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof BaseMultiblockHatchEntity<?> hatch)) continue;
            if (!hatch.getHatchRules().allowExtract()) continue;

            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }

    /**
     * Generic method to get any capability type from hatches
     * @param capability The capability to search for
     * @param allowInsert Filter for hatches that allow insertion (true) or extraction (false), or null for both
     * @return List of capability handlers
     */
    public <T> List<T> getHatchCapabilities(BlockCapability<T, @Nullable Direction> capability,
                                            @Nullable Boolean allowInsert) {
        if (level == null || level.isClientSide) return List.of();

        List<T> handlers = new ArrayList<>();
        for (BlockPos pos : getTrackedPositions()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof BaseMultiblockHatchEntity<?> hatch)) continue;

            MultiblockHatchRule rules = hatch.getHatchRules();
            if (allowInsert != null) {
                boolean matches = allowInsert ? rules.allowInsert() : rules.allowExtract();
                if (!matches) continue;
            }

            T handler = level.getCapability(capability, pos, null);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }

    /**
     * Attempts to consume items from input hatches matching a predicate
     * @param predicate Test function for items
     * @param amount Amount to consume
     * @return true if items were consumed
     */
    public boolean consumeItems(Predicate<ItemStack> predicate, int amount) {
        List<IItemHandler> inputs = getItemInputs();
        int remaining = amount;

        for (IItemHandler handler : inputs) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (predicate.test(stack)) {
                    int toExtract = Math.min(remaining, stack.getCount());
                    ItemStack extracted = handler.extractItem(slot, toExtract, false);
                    remaining -= extracted.getCount();

                    if (remaining <= 0) {
                        setChanged();
                        return true;
                    }
                }
            }
        }

        return remaining == 0;
    }

    /**
     * Attempts to output items to output hatches
     * @param stack The item stack to output
     * @return The remaining stack that couldn't be inserted (empty if all inserted)
     */
    public ItemStack outputItems(ItemStack stack) {
        List<IItemHandler> outputs = getItemOutputs();
        ItemStack remaining = stack.copy();

        for (IItemHandler handler : outputs) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                remaining = handler.insertItem(slot, remaining, false);
                if (remaining.isEmpty()) {
                    setChanged();
                    return ItemStack.EMPTY;
                }
            }
        }

        return remaining;
    }

    /**
     * Attempts to consume energy from input hatches
     * @param amount Amount of energy to consume
     * @param simulate If true, only simulates the extraction
     * @return Amount of energy actually consumed
     */
    public int consumeEnergy(int amount, boolean simulate) {
        List<IEnergyStorage> inputs = getEnergyInputs();
        int totalExtracted = 0;

        for (IEnergyStorage handler : inputs) {
            int extracted = handler.extractEnergy(amount - totalExtracted, simulate);
            totalExtracted += extracted;

            if (totalExtracted >= amount) break;
        }

        if (!simulate && totalExtracted > 0) {
            setChanged();
        }

        return totalExtracted;
    }

    /**
     * Attempts to output energy to output hatches
     * @param amount Amount of energy to output
     * @param simulate If true, only simulates the insertion
     * @return Amount of energy actually output
     */
    public int outputEnergy(int amount, boolean simulate) {
        List<IEnergyStorage> outputs = getEnergyOutputs();
        int totalInserted = 0;

        for (IEnergyStorage handler : outputs) {
            int inserted = handler.receiveEnergy(amount - totalInserted, simulate);
            totalInserted += inserted;

            if (totalInserted >= amount) break;
        }

        if (!simulate && totalInserted > 0) {
            setChanged();
        }

        return totalInserted;
    }

    /**
     * Checks if the multiblock has enough of a specific item
     * @param predicate Item test predicate
     * @param required Required amount
     * @return true if enough items are available
     */
    public boolean hasItems(Predicate<ItemStack> predicate, int required) {
        List<IItemHandler> inputs = getItemInputs();
        int found = 0;

        for (IItemHandler handler : inputs) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (predicate.test(stack)) {
                    found += stack.getCount();
                    if (found >= required) return true;
                }
            }
        }

        return found >= required;
    }

    /**
     * Checks if the multiblock has enough energy
     * @param required Required energy amount
     * @return true if enough energy is available
     */
    public boolean hasEnergy(int required) {
        List<IEnergyStorage> inputs = getEnergyInputs();
        int available = 0;

        for (IEnergyStorage handler : inputs) {
            available += handler.getEnergyStored();
            if (available >= required) return true;
        }

        return available >= required;
    }
}
