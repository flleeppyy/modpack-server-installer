package com.flleeppyy.serverinstaller.Utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class OtherUtils {

    public static String getResource(String rsc) {
        StringBuilder val = new StringBuilder();

        try {
            Class<?> cls = Class.forName("ClassLoaderDemo");

            // returns the ClassLoader object associated with this Class
            ClassLoader cLoader = cls.getClassLoader();

            // input stream
            InputStream i = cLoader.getResourceAsStream(rsc);
            BufferedReader r = new BufferedReader(new InputStreamReader(i));

            // reads each line
            String l;
            while((l = r.readLine()) != null) {
                val.append(l);
            }
            i.close();
        } catch(Exception e) {
            System.out.println(e);
        }
        return val.toString();
    }

    public static int indexOf(byte[] strArray, Object element){
        return Arrays.asList(strArray).indexOf(element);
    }
}
