package dev.wolfieboy09.qtech.registries;

import dev.wolfieboy09.qtech.QuantiumizedTech;
import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

@NothingNullByDefault
public enum QTGuiTextures {
    DISK_ASSEMBLER_GUI("disk_assembler", 256, 256),

    JEI_DISK_ASSEMBLER("jei/disk_assembler", 169, 73),
    JEI_SLOT("jei/icons", 18, 18),
    JEI_CHANCE_SLOT("jei/icons", 19, 0, 18, 18);

    public final ResourceLocation location;
    private final int width;
    private final int height;
    private final int startX;
    private final int startY;

    QTGuiTextures(String location, int width, int height) {
        this(location, 0, 0, width, height);
    }

    QTGuiTextures(String location, int startX, int startY, int width, int height) {
        this(QuantiumizedTech.MOD_ID, location, startX, startY, width, height);
    }

    QTGuiTextures(String namespace, String location, int startX, int startY, int width, int height) {
        this.location = ResourceLocation.fromNamespaceAndPath(namespace, "textures/gui/" + location + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
    }

    public ResourceLocation getLocation() {
        return this.location;
    }

    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(this.location, x, y, this.startX, this.startY, this.width, this.height);
    }

    public void render(GuiGraphics graphics, int x, int y, int u, int v) {
        graphics.blit(this.location, x, y, this.startX, this.startY, this.width, this.height, u, v);
    }

    public int getStartX() {
        return this.startX;
    }

    public int getStartY() {
        return this.startY;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}
