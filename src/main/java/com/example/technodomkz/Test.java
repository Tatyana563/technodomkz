package com.example.technodomkz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.technodomkz.Constants.URL;

public class Test {
    public static void main2(String[] args) {
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

    public static void main(String[] args) {
       // outer:
        for (int i=0; i<100; i+=10) {
            for (int j=0; j<10; j++){
                if (j!=0 && j % 2 == 0) {
                   continue;
                }
                System.out.println("i=" + i + " j=" + j);
                System.out.println(i+j);
            }
            //sadfsdf
        }
    }
}
