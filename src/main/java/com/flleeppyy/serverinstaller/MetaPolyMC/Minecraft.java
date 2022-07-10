package com.flleeppyy.serverinstaller.MetaPolyMC;

import com.google.gson.annotations.SerializedName;

public class Minecraft {

    static class BasePackageVersion {
        @SerializedName("name") public String Name;
        @SerializedName("releaseTime") public String ReleaseTime;
        @SerializedName("sha256") public String Sha256;
        @SerializedName("version") public String Version;
        @SerializedName("requires") public Common.Requirement[] Requires;
        @SerializedName("recommended") public boolean Recommended;
    }

}
