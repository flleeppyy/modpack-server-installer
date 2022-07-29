package com.flleeppyy.serverinstaller.Utils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses PolyMC's cfg files
 */
public class PolyCfgParser {
    String cfg;
    Map<String, String> cfgMap;

    public PolyCfgParser(String cfg) {
        this.cfg = cfg;
        createMap();
    }

    public PolyCfgParser(InputStream cfg) {
        // Convert to string
        StringBuilder sb = new StringBuilder();
        int ch;
        try {
            while ((ch = cfg.read()) != -1) {
                sb.append((char) ch);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.cfg = sb.toString();

        createMap();
    }

    private void createMap() {
        // Split by lines
        Map<String,String> map = new HashMap<>();

        String[] lines = this.cfg.split("\n");
        for (String line : lines) {
            String[] parts = line.split("=",2);
            map.put(parts[0], parts[1]);
        }

        this.cfgMap = map;
    }

    public String get(String key) {
        return cfgMap.get(key);
    }
}
