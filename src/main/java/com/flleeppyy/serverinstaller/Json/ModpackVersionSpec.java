package com.flleeppyy.serverinstaller.Json;

import com.flleeppyy.serverinstaller.ModpackApi;

import java.net.URI;

public class ModpackVersionSpec {
    public String filename;
    public String name;
    public String version;
    public String filetype;
    private String url;

    public String getUrl() {
        try {
            StringBuffer piss = new StringBuffer();
            piss.append(ModpackApi.baseUri.toString());
            piss.append(this.url);
            return piss.toString();
        } catch (Exception e) {
            // idc lmao
            return null;
        }
    }
}
