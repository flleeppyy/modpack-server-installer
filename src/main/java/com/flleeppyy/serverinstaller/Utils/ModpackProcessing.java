package com.flleeppyy.serverinstaller.Utils;

import com.flleeppyy.serverinstaller.Json.ModpackInfo;

public class ModpackProcessing {

    public static String descriptionGenerator(ModpackInfo modpack) {
        StringBuilder desc = new StringBuilder();
        String newline = "<br>";

//        desc.append("[$PROFILE$]: extended").append(newline);

        desc.append(modpack.name).append(" by ");

        if (modpack.authors.length > 1) {
            for (int i = 0; i < modpack.authors.length; i++) {
                if (i == modpack.authors.length - 1) {
                    desc.append("and ").append(modpack.authors[i]);
                } else {
                    desc.append(modpack.authors[i]).append(" ");
                }
            }
        } else if (modpack.authors.length == 1) {
            desc.append(modpack.authors[0]);
        } else {
            desc.append("Unknown");
        }

        desc
            .append(newline)
            .append("MC Version: ").append(modpack.minecraftVersion).append(newline);
            if (modpack.forgeVersion != null) {
                desc.append("Forge Version: ").append(modpack.forgeVersion).append(newline);
            }
            desc.append("<s>" + StringUtils.repeatString("-", 32) + "</s>")
            .append(newline)
            .append(modpack.description)
            .append(newline);

        if (modpack.notes != null && modpack.notes.length >= 1) {
            desc.append("Notes: ").append(newline);
            for (String note : modpack.notes) {
                desc.append("<p> - ").append(note).append("</p>");
            }
        }

        desc.append(newline);

        return desc.toString();
    }


}
