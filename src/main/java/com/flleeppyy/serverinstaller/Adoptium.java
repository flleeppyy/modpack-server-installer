package com.flleeppyy.serverinstaller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.apache.hc.client5.http.fluent.Request;

public class Adoptium {
    static Gson gson = new Gson().newBuilder().setPrettyPrinting().create();

    public static class AdoptiumRelease {
        @SerializedName("binaries")     public Binary[] Binaries;
        @SerializedName("release_name") public String Release;
        @SerializedName("version_data") public VersionData VersionData;
    }

    public static class AdoptiumBinaryView {
        @SerializedName("binary") public Binary Binary;
        @SerializedName("release_name") public String Release;
        @SerializedName("version") public VersionData VersionData;
    }

    public static class Binary {
        @SerializedName("image_type") public String ImageType;
        @SerializedName("package")    public DownloadInfo Package;
        @SerializedName("installer")  public DownloadInfo installer;
    }



    public static class DownloadInfo {
        @SerializedName("checksum") public String Checksum;
        @SerializedName("link") public String Link;
        @SerializedName("name") public String Name;
        @SerializedName("size") public int Size;
    }

    public static class VersionData {
        @SerializedName("semver") public String semver;
    }

    static class InstallProperties {
        public AdoptiumRelease Release;
        public Binary Binary;
        public Path ArchivePath;
    }

    static class AdoptiumJavaProvider {
        public String short_version;
        public String SemverTarget;
        public InstallProperties InstallProps;
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

    public static AdoptiumBinaryView GetLatestAssets(int javaVersion, String architecture, String os, String imageType) throws IOException {
        argumentChecker(javaVersion, architecture, os, imageType);

        String url = ADOPTIUM_URL + "v3/assets/latest/" + javaVersion + "/hotspot";
        // Query params
        url += "?arch=" + architecture;
        url += "&os=" + os;
        url += "&image_type=" + imageType;

        // End query params

        return gson.fromJson(Request.get(url).execute().returnContent().asString(), AdoptiumBinaryView.class);
    }

    public static AdoptiumRelease[] GetLatestReleases(int javaVersion, String architecture, String os, String imageType) throws IOException {
        argumentChecker(javaVersion, architecture, os, imageType);

        String url = ADOPTIUM_URL + "v3/assets/feature_releases/" + javaVersion + "/ga";
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
        if (Arrays.stream(validJavaVersions).noneMatch(validVersion -> javaVersion == validVersion)) {
            throw new IllegalArgumentException("Invalid Java version: " + javaVersion);
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
