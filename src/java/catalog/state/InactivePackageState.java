package com.islandtrails.catalog.state;

/**
 * Concrete state representing a tour package in INACTIVE status.
 * An inactive package has been taken off the catalog:
 * it cannot be booked or directly edited, but can be reactivated by publishing it,
 * and cannot be deactivated again.
 */
public class InactivePackageState implements PackageState {

    @Override
    public boolean canBook() {
        return false;
    }

    @Override
    public boolean canEdit() {
        return false;
    }

    @Override
    public boolean canPublish() {
        return true;
    }

    @Override
    public boolean canDeactivate() {
        return false;
    }
}
