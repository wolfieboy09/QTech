package dev.wolfieboy09.qtech.block.disk_assembler;

import dev.wolfieboy09.qtech.QuantiumizedTech;
import dev.wolfieboy09.qtech.api.items.ExtendedItemStackHandler;
import dev.wolfieboy09.qtech.api.recipes.data.disk_assembler.DiskAssemblerRecipe;
import dev.wolfieboy09.qtech.block.AbstractEnergyBlockEntity;
import dev.wolfieboy09.qtech.registries.QTBlockEntities;
import dev.wolfieboy09.qtech.registries.QTRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
public class DiskAssemblerBlockEntity extends AbstractEnergyBlockEntity implements MenuProvider {
    private final Component TITLE = Component.translatable("block.qtech.disk_assembler");
    private int progress = 0;
    private int crafting_ticks = 0;
    private int energy_required = 0;
    private DiskAssemblerRecipe recipe = null;
    private boolean isValidRecipe = false;
    private final ContainerData containerData = new SimpleContainerData(3);

    public DiskAssemblerBlockEntity(BlockPos pos, BlockState blockState) {
        super(QTBlockEntities.DISK_ASSEMBLER.get(), pos, blockState, 20000, 1000, 0);
    }
    
    @Override
    public @NotNull Component getDisplayName() {
        return TITLE;
    }

    private void updateContainer() {
        this.containerData.set(0, this.energyStorage.getEnergyStored());
        this.containerData.set(1, this.getProgress());
        this.containerData.set(2, this.recipe == null ? 0 : this.recipe.getProcessingDuration());
    }
    
    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        updateContainer();
        return new DiskAssemblerMenu(id, this.getBlockPos(), playerInv, player, this.containerData);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        updateContainer();
    }

    private void doEnergySlot() {
        ItemStack energySlotStack = this.inventory.getStackInSlot(8);
        if (energySlotStack.isEmpty()) return;

        IEnergyStorage itemEnergyStorage = energySlotStack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (itemEnergyStorage == null || !itemEnergyStorage.canExtract()) return;

        int itemEnergyStored = itemEnergyStorage.getEnergyStored();
        int transferRate = Math.min(itemEnergyStored, this.energyStorage.getMaxReceive());
        int energyToExtract = Math.min(transferRate, this.energyStorage.getMaxEnergyStored() - this.energyStorage.getEnergyStored());

        if (energyToExtract > 0) {
            int extractedEnergy = itemEnergyStorage.extractEnergy(energyToExtract, false);
            this.energyStorage.addEnergy(extractedEnergy);
        }
    }

    public void tick() {
        doEnergySlot();
        //                                                         Validate the recipe
        if (this.level == null || getInputContainer().isEmpty() || !this.isValidRecipe) return;

        // Check if output has space before crafting
        ItemStack outputSlotStack = this.inventory.getStackInSlot(DiskAssemblerSlot.OUTPUT_SLOT);
        ItemStack resultItem = this.recipe.getResultItem(this.level.registryAccess());
        
        boolean outputHasSpace = outputSlotStack.isEmpty() ||
            (outputSlotStack.getItem() == resultItem.getItem() &&
                outputSlotStack.getCount() + resultItem.getCount() <= outputSlotStack.getMaxStackSize());
        
        if (!outputHasSpace) return; // Exit if output slot lacks space
        
        // Check energy and progress crafting if energy is sufficient
        boolean energySufficient = this.energyStorage.getEnergyStored() >= this.recipe.getEnergyCost();
        int timeRequired = this.recipe.getProcessingDuration();
        
        if (energySufficient) {
            if (this.crafting_ticks < timeRequired) {
                this.crafting_ticks++;
                this.progress = getProgress();
                this.energyStorage.removeEnergy(this.recipe.getEnergyCost() / 100);
                setChanged();
            }
            
            // If progress reaches 100%, complete the crafting
            if (this.progress >= 100) {
                // Consume input items
                consumeInputItems(recipe);
                
                // Place result in output slot
                if (outputSlotStack.isEmpty()) {
                    this.inventory.setStackInSlot(DiskAssemblerSlot.OUTPUT_SLOT, resultItem.copy());
                } else {
                    outputSlotStack.grow(resultItem.getCount());
                }
                
                // Reset crafting progress
                resetProgress();
            }
        }
    }
    
    private int getProgress() {
        if (this.recipe == null) return 0;
        return (int) (this.crafting_ticks / (float) this.recipe.getProcessingDuration() * 100);
    }
    
    private void consumeInputItems(DiskAssemblerRecipe recipe) {
        // TODO Have it shrink by recipe amount
        this.inventory.getStackInSlot(DiskAssemblerSlot.MAIN_SLOT_1).shrink(1);
        this.inventory.getStackInSlot(DiskAssemblerSlot.MAIN_SLOT_2).shrink(1);
        this.inventory.getStackInSlot(DiskAssemblerSlot.MAIN_SLOT_3).shrink(1);
        this.inventory.getStackInSlot(DiskAssemblerSlot.EXTRA_SLOT_1).shrink(1);
        this.inventory.getStackInSlot(DiskAssemblerSlot.EXTRA_SLOT_2).shrink(1);
        this.inventory.getStackInSlot(DiskAssemblerSlot.EXTRA_SLOT_3).shrink(1);
        this.inventory.getStackInSlot(DiskAssemblerSlot.EXTRA_SLOT_4).shrink(1);
    }
    
    protected void resetProgress() {
        this.crafting_ticks = 0;
        this.progress = 0;
    }
    
    private boolean checkRecipe(){
    //    Get the input items
        if (this.level == null) return false;
        ItemStackHandler inputHandler = new ItemStackHandler(7);
        for (int i = 0; i < 7; i++) {
            inputHandler.setStackInSlot(i, this.inventory.getStackInSlot(i));
        }
        RecipeWrapper input = new RecipeWrapper(inputHandler);
        Optional<RecipeHolder<DiskAssemblerRecipe>> recipeFound = QTRecipeTypes.DISK_ASSEMBLY.find(input, this.level);
        if (recipeFound.isEmpty()) return false;
        DiskAssemblerRecipe recipe = recipeFound.get().value();
        boolean matches = recipe.matches(input, this.level);
        if (!matches) return false;
        ItemStack result = recipe.assemble(input, this.level.registryAccess());
        if (result.isEmpty()) return false;
        this.recipe = recipe;
        return true;
    }
    
    public static class DiskAssemblerSlot {
        public static final int MAIN_SLOT_1 = 0;
        public static final int MAIN_SLOT_2 = 1;
        public static final int MAIN_SLOT_3 = 2;
        public static final int EXTRA_SLOT_1 = 3;
        public static final int EXTRA_SLOT_2 = 4;
        public static final int EXTRA_SLOT_3 = 5;
        public static final int EXTRA_SLOT_4 = 6;
        public static final int OUTPUT_SLOT = 7;
    }

    private final ItemStackHandler inventory = new ExtendedItemStackHandler(9, (slot) -> {
        if (slot < 7) {
            boolean isValidRecipe = checkRecipe();
            if (!isValidRecipe) resetProgress();
            setIsValidRecipe(isValidRecipe);
        }
        setChanged();
    });
    
    private void setIsValidRecipe(boolean value) {
        this.isValidRecipe = value;
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    public EnergyStorage getEnergyHandler(@Nullable Direction side) {
        if (side == null) return this.getEnergyStorage(); // for special cases
        Direction blockFacing = this.getBlockState().getValue(DiskAssemblerBlock.FACING);
        return side == blockFacing.getOpposite() ? this.getEnergyStorage() : null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    private void onCapInvalidate() {
        if (level == null) return;
        level.invalidateCapabilities(getBlockPos());
    }

    public ItemStackHandler getInventoryHandler() {
        return this.inventory;
    }


    public SimpleContainer getInputContainer() {
        SimpleContainer container = new SimpleContainer(7);
        for (int i = 0; i < 7; i++) {
            container.setItem(i, this.inventory.getStackInSlot(i));
        }
        return container;
    }

    public SimpleContainer getOutputContainer() {
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, this.inventory.getStackInSlot(this.inventory.getSlots() - 1));
        return container;
    }

    public SimpleContainer getEnergySlot() {
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, this.inventory.getStackInSlot(8));
        return container;
    }
    
    private void saveRecipeToNBT(CompoundTag modData, HolderLookup.Provider registries) {
        try {
            if (this.recipe instanceof DiskAssemblerRecipe t) {
                modData.put("recipe", DiskAssemblerRecipe.CODEC.encodeStart(NbtOps.INSTANCE, t).getOrThrow());
            }
        } catch (Exception e) {
            QuantiumizedTech.LOGGER.error("Error saving recipe to NBT: {}", e.getMessage());
        }
    }
    
    private void loadRecipeFromNBT(CompoundTag recipeTag) {
        Recipe<?> recipe = Recipe.CODEC.parse(NbtOps.INSTANCE, recipeTag).getOrThrow();
        if (recipe instanceof DiskAssemblerRecipe diskAssemblerRecipe) {
            this.recipe = diskAssemblerRecipe;
        }
    }
    

    @Override
    public void saveExtra(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveExtra(tag, registries);
        tag.putInt("progress", this.progress);
        tag.putInt("energy_required", this.energy_required);
        tag.putInt("crafting_ticks", this.crafting_ticks);
        tag.put("inventory", this.inventory.serializeNBT(registries));
        tag.putBoolean("isValidRecipe", this.isValidRecipe);
        saveRecipeToNBT(tag, registries);
    }

    @Override
    protected void loadExtra(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadExtra(tag, registries);
        this.progress = tag.getInt("progress");
        this.energy_required = tag.getInt("energy_required");
        this.crafting_ticks = tag.getInt("crafting_ticks");
        this.inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        this.isValidRecipe = tag.getBoolean("isValidRecipe");
        // Load the recipe if it exists
        if (tag.contains("recipe")) {
            loadRecipeFromNBT(tag.getCompound("recipe"));
        }
    }

    @Override
    public CompoundTag updateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        tag.putInt("energy_required", this.energy_required);
        tag.putInt("crafting_ticks", this.crafting_ticks);
        return super.updateTag(tag, lookupProvider);
    }

    @Override
    public void handleUpdate(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdate(tag, lookupProvider);
        this.energy_required = tag.getInt("energy_required");
        this.crafting_ticks = tag.getInt("crafting_ticks");
    }
}
