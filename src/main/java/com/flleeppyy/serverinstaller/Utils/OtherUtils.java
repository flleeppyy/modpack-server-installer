package com.flleeppyy.serverinstaller.Utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class OtherUtils {

    public static String getResource(String rsc) {
        StringBuilder val = new StringBuilder();

        try {
            InputStream i = OtherUtils.class.getResourceAsStream(rsc);
            BufferedReader r = new BufferedReader(new InputStreamReader(i));

            // reads each line
            String l;
            while((l = r.readLine()) != null) {
                val.append(l + "\n");
            }
            i.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return val.toString();
    }

    public static int indexOf(byte[] strArray, Object element){
        return Arrays.asList(strArray).indexOf(element);
    }
}
