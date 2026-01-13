package dev.wolfieboy09.qtech.api.items;

import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.IntConsumer;

public class ExtendedItemStackHandler extends ItemStackHandler {
    private final IntConsumer consumer;
    private final Runnable runnable;

    public ExtendedItemStackHandler(int size) {
        super(size);
        this.consumer = null;
        this.runnable = null;
    }

    public ExtendedItemStackHandler(int size, IntConsumer callback) {
        super(size);
        this.consumer = callback;
        this.runnable = null;
    }

    public ExtendedItemStackHandler(int size, Runnable callback) {
        super(size);
        this.runnable = callback;
        this.consumer = null;
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (this.consumer != null) {
            this.consumer.accept(slot);
        }
        if (this.runnable != null) {
            this.runnable.run();
        }
    }
}
