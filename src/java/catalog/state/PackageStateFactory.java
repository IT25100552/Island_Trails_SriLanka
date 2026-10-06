package com.islandtrails.catalog.state;

import com.islandtrails.catalog.entity.PackageStatus;

/**
 * Factory class providing appropriate PackageState instances based on the PackageStatus lifecycle enum.
 */
public final class PackageStateFactory {

    private static final PackageState PUBLISHED_STATE = new PublishedPackageState();
    private static final PackageState INACTIVE_STATE = new InactivePackageState();

    private PackageStateFactory() {
        // Prevent instantiation of utility factory class
    }

    /**
     * Resolves the PackageState strategy instance for a given PackageStatus enum value.
     *
     * @param status the lifecycle status of the tour package
     * @return corresponding PackageState implementation
     */
    public static PackageState getState(PackageStatus status) {
        if (status == null) {
            return PUBLISHED_STATE;
        }
        return switch (status) {
            case PUBLISHED -> PUBLISHED_STATE;
            case INACTIVE -> INACTIVE_STATE;
        };
    }
}
