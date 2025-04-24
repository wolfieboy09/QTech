package dev.wolfieboy09.qstorage.block.pipe.network;

/**
 * Represents the type of a pipe network.
 * Different network types can transport different resources.
 */
public enum NetworkType {
    ITEM,
    FLUID,
    ENERGY,
    UNIVERSAL;
    
    /**
     * Checks if this network type is compatible with another network type.
     * @param other The other network type to check compatibility with
     * @return True if the network types are compatible, false otherwise
     */
    public boolean isCompatibleWith(NetworkType other) {
        if (this == UNIVERSAL || other == UNIVERSAL) return true;
        return this == other;
    }
}
