package com.islandtrails.catalog.state;

/**
 * Concrete state representing a tour package in PUBLISHED status.
 * A published package is live in the public catalog:
 * it can be booked by customers, edited by staff, and deactivated, but cannot be published again.
 */
public class PublishedPackageState implements PackageState {

    @Override
    public boolean canBook() {
        return true;
    }

    @Override
    public boolean canEdit() {
        return true;
    }

    @Override
    public boolean canPublish() {
        return false;
    }

    @Override
    public boolean canDeactivate() {
        return true;
    }
}
