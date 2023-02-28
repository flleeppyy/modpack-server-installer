package com.flleeppyy.serverinstaller.MetaPrismLauncher;

import com.flleeppyy.serverinstaller.MetaPrismLauncher.Common.*;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.sun.istack.internal.Nullable;

import java.io.IOException;

import static com.flleeppyy.serverinstaller.MetaPrismLauncher.Common.getUrl;

public class Forge {
    public static final Gson gson = new Gson();
    public static String baseUrl = "https://meta.polymc.org/v1/net.minecraftforge";

    public static class BaseForgeIndex extends BasePackageIndex {
        @SerializedName("versions") public BaseForgeVersion[] versions;
    }

    public static class BaseForgeVersion {
        @SerializedName("name") public String Name;
        @SerializedName("releaseTime") public String ReleaseTime;
        @SerializedName("sha256") public String Sha256;
        @SerializedName("version") public String Version;
        @SerializedName("requires") public Common.Requirement[] Requires;
        @SerializedName("recommended") public boolean Recommended;
    }

    public static class ForgeVersion extends BasePackageVersion {
        @Nullable @SerializedName("+traits") public String[] traits;
        @Nullable @SerializedName("jarMods") public JarMod[] JarMods;
        @Nullable @SerializedName("mavenFiles") public Library[] MavenFiles;
        @Nullable @SerializedName("libraries") public Library[] libraries;
        @Nullable @SerializedName("minecraftArguments") public String MinecraftArguments;
        public String mainClass;
    }

    public static class JarMod extends Library {
        @SerializedName("downloads") public Common.Downloads Downloads;
    }

    public static ForgeVersion getForgeVersion(String version) throws IOException {
        return getUrl(baseUrl + "/" + version + ".json", ForgeVersion.class);
    }

    public static BaseForgeVersion getBaseForgeVersion(String version) throws IOException {
        BaseForgeIndex versionsIndex = getUrl(baseUrl + "/index.json", BaseForgeIndex.class);
        for (BaseForgeVersion versionEntry : versionsIndex.versions) {
            if (versionEntry.Version.equals(version)) {
                return versionEntry;
            }
        }
        throw new IOException("Version " + version + " not found");
    }

    public static ForgeVersion getLatestRecommendedVersion() throws IOException {
        BaseForgeIndex versionsIndex = getUrl(baseUrl + "/index.json", BaseForgeIndex.class);
        // this is assuming the sorting stays the same from the api.
        for (BaseForgeVersion versionEntry : versionsIndex.versions) {
            if (versionEntry.Recommended) {
                return getForgeVersion(versionEntry.Version);
            }
        }
        throw new IOException("No recommended version found");
    }

    public static ForgeVersion getRecommendedVersion(String McVersion) {
        try {
            BaseForgeIndex versionsIndex = getUrl(baseUrl + "/index.json", BaseForgeIndex.class);
            for (BaseForgeVersion versionEntry : versionsIndex.versions) {
                Requirement requirement = null;
                for (Requirement req : versionEntry.Requires) {
                    if (req.uid.equals("net.minecraft")) {
                        requirement = req;
                        break;
                    }
                }
                if (requirement == null) {
                    continue;
                }
                if (versionEntry.Recommended && requirement.equals.equals(McVersion)) {
                    return getForgeVersion(versionEntry.Version);
                }
            }
            throw new IOException("No recommended version found");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static class Utils {
        public static String getInstallerUrl(ForgeVersion forgeVersion) {
            String version = forgeVersion.version;
            if (version == null) {
                return null;
            }

            version = forgeVersion.requires[0].equals + "-" + version;
            return "https://maven.minecraftforge.net/net/minecraftforge/forge/" + version + "/forge-" + version + "-installer.jar";
        }
    }

    public static BaseForgeIndex getIndex() throws IOException {
        return getUrl(baseUrl + "/index.json", BaseForgeIndex.class);
    }
}
