package com.example.technodomkz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.technodomkz.Constants.URL;

public class Test {
    public static void main(String[] args) {
        String str = "https://www.technodom.kz/kazygurt/vsjo-dlja-gejmerov/xbox/aksessuary-xbox";
        List<String> result = new ArrayList<>();
        String[] split = str.split("/");
        List<String> list = Arrays.asList(split);
        System.out.println(list);
        for (int i = 4; i < list.size(); i++) {
            result.add(list.get(i));
        }
        System.out.println(result);
        String join = URL+"/"+String.join("/", result);
        System.out.println(join);
    }
}
