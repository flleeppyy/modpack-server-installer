package com.flleeppyy.serverinstaller;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.apache.hc.client5.http.fluent.Request;

public class Adoptium {
    static Gson gson = new Gson().newBuilder().setPrettyPrinting().create();

    static class AdoptiumRelease {
        @SerializedName("binaries")   Binary[] Binaries;
        @SerializedName("release_name") String Release;
        @SerializedName("version_data") VersionData VersionData;
    }

    static class Binary {
        @SerializedName("image_type") String ImageType;
        @SerializedName("package") Package Package;
    }

    static class Package {
        @SerializedName("checksum") String Checksum;
        @SerializedName("link") String Link;
        @SerializedName("name") String Name;
        @SerializedName("size") int Size;
    }

    static class VersionData {
        @SerializedName("semver") String semver;
    }

    static class InstallProperties {
        AdoptiumRelease Release;
        Binary Binary;
        Path ArchivePath;
    }

    static class AdoptiumJavaProvider {
        String short_version;
        String SemverTarget;
        InstallProperties InstallProps;
    }

    static String ADOPTIUM_URL = "https://api.adoptium.net";

    static ArrayList<String> validArchs = new ArrayList<>(Arrays.asList(
        "x64",
        "x86",
        "x32",
        "ppc64",
        "ppc64le",
        "s390x",
        "aarch64",
        "arm",
        "sparcv9",
        "riscv64"
    ));

    static ArrayList<String> validOSs = new ArrayList<>(Arrays.asList(
        "linux",
        "windows",
        "macos",
        "solaris",
        "aix",
        "alpine-linux"
    ));

    static ArrayList<String> validImageTypes = new ArrayList<>(Arrays.asList(
        "jdk",
        "jre",
        "testimage",
        "debugimage",
        "staticlibs",
        "sources",
        "sbom"
    ));

    static int[] validJavaVersions = new int[] { 8,11,16,17,18 };

    public static AdoptiumRelease[] GetLatestReleaseAssets(int javaVersion, String architecture, String os, String imageType) throws IOException {
        argumentChecker(javaVersion, architecture, os, imageType);

        String url = ADOPTIUM_URL + "v3/assets/latest/" + javaVersion + "/hotspot";
        // Query params
        url += "?arch=" + architecture;
        url += "&os=" + os;
        url += "&image_type=" + imageType;

        // End query params

        return gson.fromJson(Request.get(url).execute().returnContent().asString(), AdoptiumRelease[].class);
    }

    public static AdoptiumRelease GetReleaseAssetByVer(String version, String architecture, String os, String imageType) throws IOException {
        argumentChecker(architecture, os, imageType);

        String url = ADOPTIUM_URL + "v3/assets/version/" + version ;
        // Query params
        url += "&arch=" + architecture;
        url += "&os=" + os;
        url += "&image_type=" + imageType;
        url += "&sort_order=desc";
        // End query params

        // fetch

        AdoptiumRelease[] releases = gson.fromJson(Request.get(url).execute().returnContent().asString(), AdoptiumRelease[].class);
        if (releases.length == 0) {
            throw new IOException("No releases found for version " + version);
        }

        return releases[0];
    }

    private static void argumentChecker(int javaVersion, String architecture, String os, String imageType) {
        boolean tripped = false;
        for (int validJavaVersion : validJavaVersions) {
            if (validJavaVersion != javaVersion) {
                tripped = true;
                break;
            }
        }
        if (tripped) {
            throw new IllegalArgumentException("Invalid Java version");
        }

        argumentChecker(architecture, os, imageType);
    }

    private static void argumentChecker(String architecture, String os, String imageType) {
        if (!validArchs.contains(architecture)) {
            throw new IllegalArgumentException("Invalid architecture: " + architecture);
        }

        if (!validOSs.contains(os)) {
            throw new IllegalArgumentException("Invalid OS: " + os);
        }

        if (!validImageTypes.contains(imageType)) {
            throw new IllegalArgumentException("Invalid image type: " + imageType);
        }
    }


}
