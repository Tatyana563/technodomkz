package com.example.technodomkz;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class URLUtilTest {

    @Test
    public void testCityUrl() {
        // rawUri = "/aktobe/all"
        // suffix = "/all"
        String rawUri = "/aktobe/all";
        String result = URLUtil.extractCityFromUrl(rawUri, Constants.ALL_SUFFIX);
        Assertions.assertEquals("aktobe", result, "Названия городов должны совпадать");
    }

    @Test
    public void testCategoryUrl() {
        // rawUri = "/aktobe/all"
        // suffix = "/all"
        String rawUri = "https://www.technodom.kz/smartfony-i-gadzhety/smartfony-i-telefony/smartfony";
        String result = URLUtil.getCategorySuffix(rawUri, Constants.URL);
        Assertions.assertEquals("smartfony-i-gadzhety/smartfony-i-telefony/smartfony", result, "Нужно получить последнюю часть ссылки");
    }

    @Test
    public void extractExternalIdFromCityUrl() {
        String rawUrl = "https://www.technodom.kz/tv-audio-foto-video/televizory/led-televizory/p/samsung-43-ue43t5300auxce-led-fhd-smart-black-216516";
        String externalId = URLUtil.extractExternalIdFromUrl(rawUrl);
        Assertions.assertEquals(externalId, "216516");
    }

    @Test
    public void testRemoveCityFromUrl() {
        String rawUrl = "https://www.technodom.kz/kazygurt/vsjo-dlja-gejmerov/xbox/aksessuary-xbox";
        String prefix = "https://www.technodom.kz";
        List<String> result = new ArrayList<>();
        String[] split = rawUrl.split("/");
        List<String> list = Arrays.asList(split);
        for (int i = 4; i < list.size(); i++) {
            result.add(list.get(i));
        }
        String join = prefix + "/" + String.join("/", result);
        Assertions.assertEquals(join, "https://www.technodom.kz/vsjo-dlja-gejmerov/xbox/aksessuary-xbox");
    }
}
