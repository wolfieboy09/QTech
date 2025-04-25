package dev.wolfieboy09.qtech.block.disk_assembler;

import dev.wolfieboy09.qtech.api.slots.EnergySlot;
import dev.wolfieboy09.qtech.api.slots.ItemResultSlot;
import dev.wolfieboy09.qtech.block.AbstractEnergyContainerMenu;
import dev.wolfieboy09.qtech.registries.QTBlocks;
import dev.wolfieboy09.qtech.registries.QTMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.PlayerInvWrapper;
import org.jetbrains.annotations.NotNull;

public class DiskAssemblerMenu extends AbstractEnergyContainerMenu {
    private DiskAssemblerBlockEntity blockEntity;
    private final Level level;
    private ContainerData data;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    public DiskAssemblerMenu(int id, BlockPos pos, Inventory playerInventory, Player playerIn) {
        this(id, pos, playerInventory, playerIn, new SimpleContainerData(4));
    }

    public DiskAssemblerMenu(int id, BlockPos pos, Inventory playerInventory, @NotNull Player player, ContainerData containerData) {
        super(QTMenuTypes.DISK_ASSEMBLER.get(), id);
        addDataSlots(containerData);
        this.level = player.level();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof DiskAssemblerBlockEntity be)) return;
        this.blockEntity = be;
        this.data = containerData;

        PlayerInvWrapper playerInvWrapper = new PlayerInvWrapper(playerInventory);

        final int SLOT_X_SPACING = 18;
        final int SLOT_Y_SPACING = 18;

        final int HOTBAR_XPOS = 8;
        final int HOTBAR_YPOS = 142;
        final int PLAYER_INVENTORY_XPOS = 8;
        final int PLAYER_INVENTORY_YPOS = 84;

        ItemStackHandler itemStackHandler = be.getInventory();

        addSlot(new SlotItemHandler(itemStackHandler, 0, 17, 27));
        addSlot(new SlotItemHandler(itemStackHandler, 1, 17, 45));
        addSlot(new SlotItemHandler(itemStackHandler, 2, 35, 36));

        addSlot(new SlotItemHandler(itemStackHandler, 3, 116, 27));
        addSlot(new SlotItemHandler(itemStackHandler, 4, 134, 27));
        addSlot(new SlotItemHandler(itemStackHandler, 5, 116, 45));
        addSlot(new SlotItemHandler(itemStackHandler, 6, 134, 45));

        addSlot(new ItemResultSlot(itemStackHandler, 7, 80, 36));
        addSlot(new EnergySlot(itemStackHandler, 8, 134, 6));

        for (int x = 0; x < HOTBAR_SLOT_COUNT; x++) {
            addSlot(new SlotItemHandler(playerInvWrapper, x, HOTBAR_XPOS + SLOT_X_SPACING * x, HOTBAR_YPOS));
        }

        for (int y = 0; y < PLAYER_INVENTORY_ROW_COUNT; y++) {
            for (int x = 0; x < PLAYER_INVENTORY_COLUMN_COUNT; x++) {
                int slotNumber = HOTBAR_SLOT_COUNT + y * PLAYER_INVENTORY_COLUMN_COUNT + x;
                int xpos = PLAYER_INVENTORY_XPOS + x * SLOT_X_SPACING;
                int ypos = PLAYER_INVENTORY_YPOS + y * SLOT_Y_SPACING;
                addSlot(new SlotItemHandler(playerInvWrapper, slotNumber, xpos, ypos));
            }
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            if (stackInSlot.getCapability(Capabilities.EnergyStorage.ITEM) != null) {
                if (!this.moveItemStackTo(stackInSlot, 8, TE_INVENTORY_FIRST_SLOT_INDEX + 9, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (index >= VANILLA_SLOT_COUNT && index < slots.size()) {
                if (!this.moveItemStackTo(stackInSlot, VANILLA_SLOT_COUNT, VANILLA_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, itemstack);
            } else if (index >= VANILLA_FIRST_SLOT_INDEX && index < VANILLA_SLOT_COUNT) {
                if (!this.moveItemStackTo(stackInSlot, VANILLA_SLOT_COUNT, slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= TE_INVENTORY_FIRST_SLOT_INDEX) {
                if (!this.moveItemStackTo(stackInSlot, VANILLA_FIRST_SLOT_INDEX, VANILLA_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, QTBlocks.DISK_ASSEMBLER.get());
    }

    public int getTotalProgress() {
        return this.data.get(2);
    }

    public int getCurrentProgress() {
        return this.data.get(1);
    }

    @Override
    public int getEnergy() {
        return this.data.get(0);
    }

    @Override
    public int getMaxEnergy() {
        return this.blockEntity.getMaxEnergyStored();
    }
}
