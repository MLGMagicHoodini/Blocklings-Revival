package com.willr27.blocklings.util;

import javax.annotation.Nonnull;

public class Version {
    @Nonnull
    private String versionString;

    public Version(@Nonnull String versionString) {
        this.versionString = versionString;
    }

    public boolean isOlderThan(@Nonnull Version version) {
        return versionString.compareTo(version.versionString) < 0;
    }

    public boolean isNewerThan(@Nonnull Version version) {
        return versionString.compareTo(version.versionString) > 0;
    }

    @Override
    public String toString() {
        return versionString;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Version other && versionString.equals(other.versionString);
    }

    @Override
    public int hashCode() {
        return versionString.hashCode();
    }
}
