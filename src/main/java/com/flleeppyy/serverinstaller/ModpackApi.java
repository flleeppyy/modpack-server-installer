package com.flleeppyy.serverinstaller;

import com.flleeppyy.serverinstaller.Json.ModpackInfo;
import com.flleeppyy.serverinstaller.Json.ModpackVersionSpec;
import com.google.gson.Gson;
import org.apache.hc.client5.http.fluent.Request;

import javax.swing.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

public class ModpackApi {
    private static final Gson g = new Gson().newBuilder().create();

    protected static URI baseUri;

    static {
        try {
            baseUri = new URI("https://fleepy.tv/api/modpacks");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,e,"Error",JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public static ModpackInfo getModpack(String modpack) throws IOException {
        String response = Request.get(baseUri + "/" + modpack)
                .execute().returnContent().asString();
        return g.fromJson(response, ModpackInfo.class);
    }

    public static String[] getModpacksRaw() throws IOException {
        String response = Request.get(baseUri + "/")
                .execute().returnContent().asString();

        return g.fromJson(response, String[].class);
    }

    public static ModpackInfo[] getModpacks() throws IOException {
        String[] modpacksRaw = getModpacksRaw();
        ArrayList<ModpackInfo> modpacks = new ArrayList<>();
        for (String packName : modpacksRaw) {
            modpacks.add(getModpack(packName));
        }

        ModpackInfo[] convertedList = new ModpackInfo[modpacks.size()];
        for (int i = 0; i < convertedList.length; i++) {
            convertedList[i] = modpacks.get(i);
        }
        return convertedList;
    }

    public static ModpackVersionSpec getLatestModpackVersionSpec(String modpackName, boolean prerelease) throws IOException {
        String response = Request.get(baseUri + String.format("/%s/latest/?prerelease=%s", modpackName, (prerelease ? "any" : "stable")))
                .execute().returnContent().asString();
        return g.fromJson(response, ModpackVersionSpec.class);
    }

    public static ModpackVersionSpec getModpackVersionSpec(String modpackName, String version) throws IOException {
        String response = Request.get(baseUri + String.format("/%s/" + version, modpackName))
                .execute().returnContent().asString();
        return g.fromJson(response, ModpackVersionSpec.class);
    }
}
