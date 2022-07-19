package com.flleeppyy.serverinstaller;

import com.flleeppyy.serverinstaller.Json.BasicModpack;
import com.flleeppyy.serverinstaller.Json.ModpackInfo;
import com.flleeppyy.serverinstaller.Json.ModpackVersionSpec;
import com.google.gson.Gson;
import org.apache.hc.client5.http.fluent.Request;

import javax.swing.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class ModpackApi {
    private static final Gson g = new Gson().newBuilder().create();

    public static URI modpacksUri;
    public static URI baseUri;

    static {
        try {
            modpacksUri = new URI("https://fleepy.tv/api/v2/modpacks");
            baseUri = new URI("https://fleepy.tv");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,e,"Error",JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public static ModpackInfo getModpack(String modpackId) throws IOException {
        String url = modpacksUri + "/" + modpackId;
        System.out.println(url);
        String response = Request.get(url)
                .execute().returnContent().asString();
        return g.fromJson(response, ModpackInfo.class);
    }

    public static BasicModpack[] getModpacksBasic() throws IOException {
        String response = Request.get(modpacksUri + "/")
                .execute().returnContent().asString();

        return g.fromJson(response, BasicModpack[].class);
    }

    public static ModpackInfo[] getModpacks() throws IOException {
        BasicModpack[] modpacksRaw = getModpacksBasic();
        ArrayList<ModpackInfo> modpacks = new ArrayList<>();
        for (BasicModpack pack : modpacksRaw) {
            modpacks.add(getModpack(pack.id.toString()));
        }

        ModpackInfo[] convertedList = new ModpackInfo[modpacks.size()];
        for (int i = 0; i < convertedList.length; i++) {
            convertedList[i] = modpacks.get(i);
        }
        return convertedList;
    }

    public static ModpackVersionSpec getLatestModpackVersionSpec(String modpackName, boolean prerelease) throws IOException {
        String response = Request.get(modpacksUri + String.format("/%s/latest/?prerelease=%s", modpackName, (prerelease ? "any" : "stable")))
                .execute().returnContent().asString();
        return g.fromJson(response, ModpackVersionSpec.class);
    }

    public static ModpackVersionSpec getModpackVersionSpec(String modpackName, String version) throws IOException {
        String response = Request.get(modpacksUri + String.format("/%s/" + version, modpackName))
                .execute().returnContent().asString();
        return g.fromJson(response, ModpackVersionSpec.class);
    }
}
