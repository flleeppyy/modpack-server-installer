package com.flleeppyy.serverinstaller.Utils;

public class StringUtils {
    public static String repeatString(String string, int numberOfTimes) {
        return new String(new char[numberOfTimes]).replace("\0", string);
    }
}
