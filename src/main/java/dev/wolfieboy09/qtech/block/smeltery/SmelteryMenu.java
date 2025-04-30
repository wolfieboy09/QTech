package dev.wolfieboy09.qtech.block.smeltery;

import dev.wolfieboy09.qtech.api.QTConstants;
import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.slots.FuelSlot;
import dev.wolfieboy09.qtech.api.slots.ItemResultSlot;
import dev.wolfieboy09.qtech.registries.QTBlocks;
import dev.wolfieboy09.qtech.registries.QTMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.PlayerInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

@NothingNullByDefault
public class SmelteryMenu extends AbstractContainerMenu {
    @UnknownNullability
    private SmelteryBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public SmelteryMenu(int id, BlockPos pos, Inventory playerInventory, @NotNull Player player, ContainerData containerData) {
        super(QTMenuTypes.SMELTERY_MENU.get(), id);
        addDataSlots(containerData);
        this.level = player.level();
        this.data = containerData;
        BlockEntity blockEntity = this.level.getBlockEntity(pos);
        if (!(blockEntity instanceof SmelteryBlockEntity be)) return;
        this.blockEntity = be;

        addSlot(new SlotItemHandler(be.getInventory(), 0, 93, 6));
        addSlot(new SlotItemHandler(be.getInventory(), 1, 93, 29));
        addSlot(new SlotItemHandler(be.getInventory(), 2, 93, 52));

        addSlot(new ItemResultSlot(be.getInventory(), 3, 171, 6));
        addSlot(new ItemResultSlot(be.getInventory(), 4, 171, 52));
        addSlot(new FuelSlot(be.getInventory(), 5, 171, 100));

        createPlayerInventory(playerInventory, 48, 150);
        createPlayerHotbar(playerInventory, 48, 208);
    }

    private void createPlayerInventory(@NotNull Inventory playerInventory, int PLAYER_INVENTORY_XPOS, int PLAYER_INVENTORY_YPOS) {
        PlayerInvWrapper playerInvWrapper = new PlayerInvWrapper(playerInventory);

        for (int y = 0; y < QTConstants.PLAYER_INVENTORY_ROW_COUNT; y++) {
            for (int x = 0; x < QTConstants.PLAYER_INVENTORY_COLUMN_COUNT; x++) {
                int slotNumber = QTConstants.HOTBAR_SLOT_COUNT + y * QTConstants.PLAYER_INVENTORY_COLUMN_COUNT + x;
                int xpos = PLAYER_INVENTORY_XPOS + x * QTConstants.SLOT_X_SPACING;
                int ypos = PLAYER_INVENTORY_YPOS + y * QTConstants.SLOT_Y_SPACING;
                addSlot(new SlotItemHandler(playerInvWrapper, slotNumber, xpos, ypos));
            }
        }
    }

    private void createPlayerHotbar(@NotNull Inventory playerInv, int HOTBAR_XPOS, int HOTBAR_YPOS) {
        for (int col = 0; col < QTConstants.HOTBAR_SLOT_COUNT; ++col) {
            this.addSlot(new Slot(playerInv, col, HOTBAR_XPOS + col * QTConstants.SLOT_X_SPACING, HOTBAR_YPOS));
        }
    }

    public SmelteryMenu(int id, BlockPos pos, Inventory playerInventory, Player playerIn) {
        this(id, pos, playerInventory, playerIn, new SimpleContainerData((SmelteryBlockEntity.INPUT_TANKS_COUNT * 2) + 1));
    }

    // Get fluid from container data
    public FluidStack getFluidInTank(int tankIndex) {
        if (tankIndex < 0 || tankIndex >= SmelteryBlockEntity.INPUT_TANKS_COUNT) {
            return FluidStack.EMPTY;
        }
        
        // Get fluid ID and amount from container data
        int fluidId = data.get(tankIndex * 2);
        int amount = data.get(tankIndex * 2 + 1);
        
        // Convert ID back to fluid
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        if (fluid == Fluids.EMPTY && amount == 0) {
            return FluidStack.EMPTY;
        }
        
        return new FluidStack(fluid, amount);
    }

    public int getProgress() {
        return this.data.get(6);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        //TODO get quick move stack working
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, QTBlocks.SMELTERY.get());
    }

    public SmelteryBlockEntity getBlockEntity() {
        return this.blockEntity;
    }
}
