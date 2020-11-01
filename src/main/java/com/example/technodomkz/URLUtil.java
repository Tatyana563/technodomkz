package com.example.technodomkz;

import com.sun.xml.bind.v2.TODO;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class URLUtil {

    private URLUtil() {
        // Only static methods
    }

    //TODO: implement
    public static String removeCityFromUrl(String rawUrl, String prefix) {
        List<String> result = new ArrayList<>();
        String[] split = rawUrl.split("/");
        List<String> list = Arrays.asList(split);
        for (int i = 4; i < list.size(); i++) {
            result.add(list.get(i));
        }
        return prefix + "/" + String.join("/", result);
    }

    public static String extractExternalIdFromUrl(String rawUrl) {
        //  String line=  "https://www.technodom.kz/tv-audio-foto-video/televizory/led-televizory/p/samsung-43-ue43t5300auxce-led-fhd-smart-black-216516";
        int index1 = rawUrl.lastIndexOf("-");
        return rawUrl.substring(index1 + 1);
    }

    public static String extractCityFromUrl(String rawUri, String suffix) {
        // rawUri = "/aktobe/all"
        // suffix = "/all"
        if (!rawUri.equals(suffix) && rawUri.endsWith(suffix)) {
            int start = rawUri.startsWith("/") ? 1 : 0;
            return rawUri.substring(start, rawUri.length() - suffix.length());
        }
        return "";
    }

    public static String getCategorySuffix(String rawURL, String prefix) {
        //  https://www.technodom.kz/smartfony-i-gadzhety/smartfony-i-telefony/smartfony
        String fixedPrefix = prefix.endsWith("/") ? prefix : (prefix + "/");
        return rawURL.replace(fixedPrefix, "");
    }

}

