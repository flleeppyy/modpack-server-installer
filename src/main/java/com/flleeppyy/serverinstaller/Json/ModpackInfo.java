package com.flleeppyy.serverinstaller.Json;

import java.util.Objects;
import java.util.UUID;

public class ModpackInfo {
    public UUID id;
    public String name;
    public String description;
    public String[] authors;
    public String minecraftVersion;
    public String forgeVersion;
    public String baseMinecraftFolder;
    // This could be either a string or an array of strings so if it's an array of strings YOU'RE FUCKED
    public String[] notes;
    public String preferredLauncher;
    public String type;
    public String icon;
    public ServerOptions server;
    public String[] versions;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModpackInfo that = (ModpackInfo) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public byte[] iconBytes;
    public String toString() {
        return name;
    }

    public void setIconBytes(byte[] balls) {
        iconBytes = balls;
    }
}

