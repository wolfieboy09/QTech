package dev.wolfieboy09.qtech.block.smeltery;

import dev.wolfieboy09.qtech.QuantiumizedTech;
import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.fluids.ExtendedFluidTank;
import dev.wolfieboy09.qtech.api.items.ExtendedItemStackHandler;
import dev.wolfieboy09.qtech.api.recipes.inputs.CombinedRecipeInput;
import dev.wolfieboy09.qtech.block.GlobalBlockEntity;
import dev.wolfieboy09.qtech.registries.QTBlockEntities;
import dev.wolfieboy09.qtech.registries.QTRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;

@NothingNullByDefault
public class SmelteryBlockEntity extends GlobalBlockEntity implements MenuProvider {
    public static final int TANK_CAPACITY = 10000;
    public static final int INPUT_TANKS_COUNT = 3;
    private int progress = 0;
    private int crafting_ticks = 0;
    private boolean isValidRecipe = false;
    @UnknownNullability
    private SmelteryRecipe recipe = null;

    private final Component TITLE = Component.translatable("block.qtech.smeltery");
    // Expand container data to include both fluid IDs and amounts
    // For each tank: [fluidId, amount]
    // So for 3 tanks we need 6 integers
    // Plus 1 for the progress amount
    private final ContainerData containerData = new SimpleContainerData((INPUT_TANKS_COUNT * 2) + 1);
    private final ExtendedItemStackHandler inventory = new ExtendedItemStackHandler(6, this::onContentsChanged);
    private final List<ExtendedFluidTank> inputTanks = new ArrayList<>(INPUT_TANKS_COUNT);
    private final ExtendedFluidTank outputFluidTank = new ExtendedFluidTank(TANK_CAPACITY, this::onContentsChanged);
    private final ExtendedFluidTank wasteOutputFluidTank = new ExtendedFluidTank(TANK_CAPACITY, this::onContentsChanged);
    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 5;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> inputTanks.getFirst().getFluid();
                case 1 -> inputTanks.get(1).getFluid();
                case 2 -> inputTanks.get(2).getFluid();
                case 3 -> outputFluidTank.getFluid();
                case 4 -> wasteOutputFluidTank.getFluid();
                default -> throw new IllegalArgumentException("Index: " + tank + " was invalid for getting a fluid tank");
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> inputTanks.getFirst().getCapacity();
                case 1 -> inputTanks.get(1).getCapacity();
                case 2 -> inputTanks.get(2).getCapacity();
                case 3 -> outputFluidTank.getCapacity();
                case 4 -> wasteOutputFluidTank.getCapacity();
                default -> throw new IllegalArgumentException("Index: " + tank + " was invalid for getting a fluid tank capacity");
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return switch (tank) {
                case 0 -> inputTanks.getFirst().isFluidValid(stack);
                case 1 -> inputTanks.get(1).isFluidValid(stack);
                case 2 -> inputTanks.get(2).isFluidValid(stack);
                case 3 -> outputFluidTank.isFluidValid(stack);
                case 4 -> wasteOutputFluidTank.isFluidValid(stack);
                default -> throw new IllegalArgumentException("Index: " + tank + " was invalid for fluid validation");
            };
        }

        @Override
        public int fill(FluidStack fluidStack, FluidAction fluidAction) {
            for (ExtendedFluidTank inputTank : inputTanks) {
                if (inputTank.getFluid().getFluid() == fluidStack.getFluid()) {
                    return inputTank.fill(fluidStack, fluidAction);
                } else if (inputTank.getFluid().isEmpty()) {
                    return inputTank.fill(fluidStack, fluidAction);
                }
            }
        //    Cant fill into the output tanks
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction) {
            onContentsChanged();
            if (outputFluidTank.getFluid().getFluid() == fluidStack.getFluid()) {
                return outputFluidTank.drain(fluidStack, fluidAction);
            }
            if (wasteOutputFluidTank.getFluid().getFluid() == fluidStack.getFluid()) {
                return wasteOutputFluidTank.drain(fluidStack, fluidAction);
            }
        //    Cant extract out from the input tanks
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction fluidAction) {
            onContentsChanged();
            if (outputFluidTank.getFluidAmount() >= maxDrain) {
                return outputFluidTank.drain(maxDrain, fluidAction);
            }
            if (wasteOutputFluidTank.getFluidAmount() >= maxDrain) {
                return wasteOutputFluidTank.drain(maxDrain, fluidAction);
            }
        //    Cant extract out from the input tanks
            return FluidStack.EMPTY;
        }
    };

    public SmelteryBlockEntity(BlockPos pos, BlockState blockState) {
        super(QTBlockEntities.SMELTERY.get(), pos, blockState);
        initInputFluidTanks();
    }

    public void onContentsChanged() {
        setChanged();
        updateContainerData();
    }

    public static class SmelterySlot {
        public static final int MAIN_ITEM_SLOT_1 = 0;
        public static final int MAIN_ITEM_SLOT_2 = 1;
        public static final int MAIN_ITEM_SLOT_3 = 2;
        public static final int RESULT_SLOT = 3;
        public static final int WASTE_RESULT_SLOT = 4;
        public static final int FLUID_SLOT_1 = 0;
        public static final int FLUID_SLOT_2 = 1;
        public static final int FLUID_SLOT_3 = 2;
        public static final int RESULT_FLUID_SLOT = 3;
        public static final int WASTE_RESULT_FLUID_SLOT = 4;
        public static final int FUEL_SLOT = 5;
    }

    private void updateContainerData() {
        // Update container data with fluid IDs and amounts
        for (int i = 0; i < INPUT_TANKS_COUNT; i++) {
            FluidStack fluid = this.inputTanks.get(i).getFluid();
            // Store fluid registry ID as an integer
            int fluidId = BuiltInRegistries.FLUID.getId(fluid.getFluid());
            // Store amount
            int amount = fluid.getAmount();
            
            // Update container data (2 slots per tank)
            this.containerData.set(i * 2, fluidId);
            this.containerData.set(i * 2 + 1, amount);
        }

        // INPUT_TANKS_COUNT is equal to 3, and since the container data does * 2 for fluid ID and amount
        // We can do simple math 3 * 2 = 6, so 7 would be the
        // Needs to be 6 since the index stuff starts at 0
        this.containerData.set(6, this.getProgress());
    }
    
    public void initInputFluidTanks() {
        for (int i = 0; i < INPUT_TANKS_COUNT; i++) {
            this.inputTanks.add(new ExtendedFluidTank(TANK_CAPACITY, this::onContentsChanged));
        }
    }

    public @Nullable IFluidHandler getFluidHandler() {
        return this.fluidHandler;
    }

    public List<ExtendedFluidTank> getInputTanks() {
        return this.inputTanks;
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    @Override
    public Component getDisplayName() {
        return this.TITLE;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new SmelteryMenu(id, this.getBlockPos(), playerInv, player, this.containerData);
    }

    private CompoundTag saveFluidTank(FluidTank fluidTank, HolderLookup.Provider registries) {
        return fluidTank.writeToNBT(registries, new CompoundTag());
    }

    private void setIsValidRecipe(boolean val) {
        this.isValidRecipe = val;
    }

    private void loadFluidTank(FluidTank fluidTank, CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.isEmpty()) return;
        fluidTank.readFromNBT(registries, tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveRecipeToNBT(tag);
        ListTag listTag = new ListTag();
        for (ExtendedFluidTank inputTank : this.inputTanks) {
            listTag.add(saveFluidTank(inputTank, registries));
        }

        tag.put("InputTanks", listTag);
        tag.put("Inventory", this.inventory.serializeNBT(registries));
        tag.put("OutputTank", saveFluidTank(this.outputFluidTank, registries));
        tag.put("WasteTank", saveFluidTank(this.wasteOutputFluidTank, registries));

        tag.putInt("progress", this.progress);
        tag.putInt("crafting_ticks", this.crafting_ticks);
        tag.putBoolean("isValidRecipe", this.isValidRecipe);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ListTag listTag = tag.getList("InputTanks", Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            loadFluidTank(this.inputTanks.get(i), listTag.getCompound(i), registries);
        }

        loadFluidTank(this.outputFluidTank, tag.getCompound("OutputTank"), registries);
        loadFluidTank(this.wasteOutputFluidTank, tag.getCompound("WasteTank"), registries);
        this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));

        this.progress = tag.getInt("progress");
        this.crafting_ticks = tag.getInt("crafting_ticks");
        this.isValidRecipe = tag.getBoolean("isValidRecipe");
        if (tag.contains("recipe")) {
            loadRecipeFromNBT(tag.getCompound("recipe"));
        }
    }

    public SimpleContainer getInputContainer() {
        SimpleContainer container = new SimpleContainer(4);
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, this.inventory.getStackInSlot(i));
        }
        return container;
    }

    private final List<FluidTank> recipeTanks = new ArrayList<>();
    private final IFluidHandler recipeTanksHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 5;
        }

        @Override
        public FluidStack getFluidInTank(int i) {
            return recipeTanks.get(i).getFluid();
        }

        @Override
        public int getTankCapacity(int i) {
            return recipeTanks.get(i).getTankCapacity(i);
        }

        @Override
        public boolean isFluidValid(int i, FluidStack fluidStack) {
            return recipeTanks.get(i).isFluidValid(fluidStack);
        }

        @Override
        public int fill(FluidStack fluidStack, FluidAction fluidAction) {
            int filled = 0;
            for (FluidTank recipeTank : recipeTanks) {
                if (recipeTank.isFluidValid(fluidStack)) {
                    filled = recipeTank.fill(fluidStack, fluidAction);
                    return filled;
                }
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction) {
            for (FluidTank recipeTank : recipeTanks) {
                if (recipeTank.isFluidValid(fluidStack)) {
                    return recipeTank.drain(fluidStack, fluidAction);
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int i, FluidAction fluidAction) {
            for (FluidTank recipeTank : recipeTanks) {
                if (recipeTank.isFluidValid(recipeTank.getFluidInTank(i))) {
                    return recipeTank.drain(i, fluidAction);
                }
            }
            return FluidStack.EMPTY;
        }
    };


    private boolean checkRecipe() {
        if (this.level == null) return false;
        ItemStackHandler itemInputHandler = new ItemStackHandler(3);
        this.recipeTanks.clear();
        for (int i = 0; i < 3; i++) {
            this.recipeTanks.add(new FluidTank(TANK_CAPACITY));
        }
        for (int i = 0; i < 3; i++) {
            itemInputHandler.setStackInSlot(i, this.inventory.getStackInSlot(i));
            if (getFluidHandler() != null) recipeTanks.get(i).setFluid(getFluidHandler().getFluidInTank(i));
        }
        CombinedRecipeInput input = new CombinedRecipeInput(recipeTanksHandler, itemInputHandler, null);
        RecipeManager recipes = this.level.getRecipeManager();
        RecipeHolder<SmelteryRecipe> recipeFound = recipes.getRecipeFor(
                QTRecipes.SMELTERY_RECIPE_TYPE.get(),
                input,
                this.level
        ).orElse(null);
        if (recipeFound == null) return false;
        this.recipe = recipeFound.value();
        setIsValidRecipe(true);
        return true;
    }

    public void tick() {
        if (!this.isValidRecipe || this.level == null || (getInputTanks().isEmpty() || getInputContainer().isEmpty())) return;

        ItemStack outputSlotStack = this.inventory.getStackInSlot(SmelterySlot.RESULT_SLOT);
        ItemStack resultItem = this.recipe.getResultItem(this.level.registryAccess());
        FluidStack resultFluidSlot = this.recipe.getFluidStackFromList(this.recipe.result());
        FluidTank outputFluidSlot = this.outputFluidTank;
        ItemStack wasteItem = this.recipe.waste().getFirst().left().orElse(ItemStack.EMPTY);
        ItemStack wasteSlotStack = this.inventory.getStackInSlot(SmelterySlot.WASTE_RESULT_SLOT);
//        FluidStack extraResultFluid = this.recipe.result().getFirst().right().orElse(FluidStack.EMPTY);



        boolean outputItemHasSpace = outputSlotStack.isEmpty() ||
                (outputSlotStack.getItem() == resultItem.getItem() &&
                        outputSlotStack.getCount() + resultItem.getCount() <= outputSlotStack.getMaxStackSize());
        boolean wasteSlotHasSpace = wasteSlotStack.isEmpty() ||
                (wasteSlotStack.getItem() == wasteItem.getItem() &&
                        wasteSlotStack.getCount() + wasteItem.getCount() <= wasteSlotStack.getMaxStackSize());

        boolean outputFluidHasSpace = outputFluidTank.isEmpty() ||
                (outputFluidSlot.getFluidInTank(SmelterySlot.RESULT_FLUID_SLOT).getFluid() == resultFluidSlot.getFluid() && outputFluidSlot.getFluidInTank(SmelterySlot.RESULT_FLUID_SLOT).getAmount() + resultFluidSlot.getAmount() <= outputFluidSlot.getCapacity());


        // No room? Don't continue
        if (!outputItemHasSpace || !outputFluidHasSpace || !wasteSlotHasSpace) return;

        int timeRequired = this.recipe.processingTime();

        if (this.crafting_ticks < timeRequired) {
            this.crafting_ticks++;
            this.progress = getProgress();
            setChanged();
        }

        if (this.progress >= 100) {
            consumeInputItems(recipe);
            if (outputSlotStack.isEmpty()) {
                this.inventory.setStackInSlot(SmelterySlot.RESULT_SLOT, resultItem.copy());
            } else {
                outputSlotStack.grow(resultItem.getCount());
            }
//            handle waste result item
            if (wasteSlotStack.isEmpty()) {
                this.inventory.setStackInSlot(SmelterySlot.WASTE_RESULT_SLOT, wasteItem.copy());
            } else {
                wasteSlotStack.grow(wasteItem.getCount());
            }
            resetProgress();
            setIsValidRecipe(false);
            this.recipe = null;
            checkRecipe();
        }
    }

    private int getProgress() {
        return this.recipe == null ? 0 : (int) (this.crafting_ticks / (float) this.recipe.processingTime() * 100);
    }

    private void consumeInputItems(SmelteryRecipe recipe) {
        recipe.ingredients().forEach((elem)->{
            elem.map(ingredient -> {
                    boolean satisfied = false;
                        for (int i = 0; i < this.inventory.getSlots(); i++) {
                            ItemStack stack = this.inventory.getStackInSlot(i);
                            if (ingredient.test(stack)) {
                                this.inventory.getStackInSlot(i).shrink(1);
                                satisfied = true;
                            }
                            if (satisfied) return true;
                        }
                        return null;
                    },
                    fluidIngredient -> {
                        return true;
//                        TODO: Fix this
//                        boolean satisfied = false;
//                        for (int i = 0; i < this.fluidHandler.getTanks(); i++) {
//                            FluidStack stack = this.fluidHandler.getFluidInTank(i);
//                            if (fluidIngredient.test(stack)) {
//                                this.fluidHandler.getFluidInTank(i).shrink(fluidIngredient.amount());
//                                satisfied = true;
//                            }
//                            if (satisfied) return true;
//                        }
//                        return null;
                    }
                    );
        });
    }

    protected void resetProgress() {
        this.crafting_ticks = 0;
        this.progress = 0;
        updateContainerData();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        updateContainerData();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag toMerge = new CompoundTag();
        toMerge.putInt("crafting_ticks", this.crafting_ticks);
        return super.saveWithoutMetadata(registries).merge(toMerge);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        this.crafting_ticks = tag.getInt("crafting_ticks");
    }

    private void saveRecipeToNBT(CompoundTag nbt) {
        try {
            if (this.recipe instanceof SmelteryRecipe smelteryRecipe) {
                nbt.put("recipe", SmelteryRecipe.CODEC.encodeStart(NbtOps.INSTANCE, smelteryRecipe).getOrThrow());
            }
        } catch (Exception e) {
            QuantiumizedTech.LOGGER.error("Error saving recipe to NBT: {}", e.getMessage());
        }
    }

    private void loadRecipeFromNBT(CompoundTag recipeTag) {
        if (Recipe.CODEC.parse(NbtOps.INSTANCE, recipeTag).getOrThrow() instanceof SmelteryRecipe smelteryRecipe) {
            this.recipe = smelteryRecipe;
        }
    }
}
