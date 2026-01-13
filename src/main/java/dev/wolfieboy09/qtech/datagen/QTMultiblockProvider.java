package dev.wolfieboy09.qtech.datagen;

import dev.wolfieboy09.qtech.QuantiumizedTech;
import dev.wolfieboy09.qtech.api.datagen.MultiblockPatternProvider;
import dev.wolfieboy09.qtech.registries.QTBlocks;
import dev.wolfieboy09.qtech.registries.QTMultiblockTypes;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;

public class QTMultiblockProvider extends MultiblockPatternProvider {
    protected QTMultiblockProvider(PackOutput output) {
        super(output, QuantiumizedTech.MOD_ID);
    }

    @Override
    protected void registerPatterns() {
        add(create("centrifuge", QTMultiblockTypes.CENTRIFUGE)
                .controller(QTBlocks.CENTRIFUGE_CONTROLLER)
                .key('B', Blocks.BRICKS)
                .key('H', Blocks.BRICKS, QTBlocks.ITEM_INPUT_HATCH.get())
                .layer("BBBBB", "BBBBB", "BBBBB")
                .layer("BB+BB", "BHBHB", "BBBBB")
                .layer("BBBBB", "BBBBB", "BBBBB"));
    }
}
