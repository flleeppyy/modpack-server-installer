package com.flleeppyy.serverinstaller.MetaPolyMC;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.sun.istack.internal.Nullable;
import org.apache.hc.client5.http.fluent.Request;

import java.io.IOException;

public class Common {
    private static final Gson gson = new Gson();
    public static final String baseUrl = "https://meta.polymc.org/v1/";

    public static class Artifact {
        @SerializedName("sha1") public String sha1;
        @SerializedName("size") public int size;
        @SerializedName("url") public String url;
    };

    public static class Index {
        @SerializedName("formatVersion") public int formatVersion;
        @SerializedName("name") public String name;
        @SerializedName("uid") public String uid;
        @SerializedName("versions") public Package[] versions;
    }

    public static class Package {
        @SerializedName("formatVersion") public int formatVersion;
        @SerializedName("name") public String name;
        @SerializedName("sha256") public String sha256;
        @SerializedName("uid") public String uid;
    }

    public static class Conflict {
        @SerializedName("uid") public String uid;
    }

    public static class Requirement {
        @SerializedName("uid") public String uid;
        @Nullable @SerializedName("suggests") public String suggests;
        @Nullable @SerializedName("equals") public String equals;
    }

    public static class Classifier {
        @SerializedName("natives-linux") public Artifact nativesLinux;
        @SerializedName("natives-osx") public Artifact nativesOsx;
        @SerializedName("natives-windows") public Artifact nativesWindows;
    }

    public static class Library {
        @SerializedName("name") public String name;
        @Nullable @SerializedName("url") public String url;
        @Nullable @SerializedName("MMC-hint") public String mmcHint;
    }

    public static class Downloads {
        @Nullable @SerializedName("artifact") public Artifact artifact;
        @Nullable @SerializedName("classifiers") public Classifier[] classifiers;
    }

    public static class PackageIndexVersion {
        @SerializedName("recommended") public boolean recommended;
        @SerializedName("releaseTime") public String releaseTime;
        @SerializedName("sha256") public String sha256;
        @SerializedName("version") public String version;
        @Nullable @SerializedName("conflicts") public Conflict[] conflicts;
        @Nullable @SerializedName("requires") public Requirement[] requires;
        @Nullable @SerializedName("type") public String type;
        @Nullable @SerializedName("volatile") public boolean volatile_;
    }

    public static class BasePackageIndex {
        @SerializedName("formatVersion") public int formatVersion;
        @SerializedName("name") public String name;
        @SerializedName("uid") public String uid;
        @SerializedName("versions") public PackageIndexVersion[] versions;
    }

    public static class BasePackageVersion {
        @SerializedName("name") public String name;
        @SerializedName("version") public String version;
        @SerializedName("uid") public String uid;
        @SerializedName("type") public String type;
        @Nullable @SerializedName("requires") public Requirement[] requires;
        @SerializedName("releaseTime") public String releaseTime;
        @SerializedName("order") public int order;
        //mainClass(nullable)
        @Nullable @SerializedName("mainClass") public String mainClass;
    }

    public static Index getIndex() throws IOException {
        return getUrl(baseUrl, Index.class);
    }

    public static <T> T getUrl(String url, Class<T> clazz) throws IOException {
        Request request = Request.get(url);
        String response = request.execute().returnContent().asString();
        return gson.fromJson(response, clazz);
    }







}
