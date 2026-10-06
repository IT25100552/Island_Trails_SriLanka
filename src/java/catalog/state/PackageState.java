package com.islandtrails.catalog.state;

/**
 * State interface defining permissible tour package lifecycle operations and capabilities
 * following the GoF State Pattern (Member 1).
 * Concrete implementations encapsulate the behavior and business invariants for each lifecycle state.
 */
public interface PackageState {

    /**
     * Determines whether the package is currently open and valid for customer bookings.
     *
     * @return true if customers can book this package, false otherwise
     */
    boolean canBook();

    /**
     * Determines whether staff can modify package details (pricing, itinerary, dates, resources).
     *
     * @return true if editing is permitted, false otherwise
     */
    boolean canEdit();

    /**
     * Determines whether the package can transition to PUBLISHED status.
     *
     * @return true if publication is permitted, false otherwise
     */
    boolean canPublish();

    /**
     * Determines whether the package can transition to INACTIVE status.
     *
     * @return true if deactivation is permitted, false otherwise
     */
    boolean canDeactivate();
}
