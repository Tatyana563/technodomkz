package com.example.technodomkz;

public class Test {
    public static void main(String[] args) {
      String rawURL=
             "https://www.technodom.kz/smartfony-i-gadzhety/smartfony-i-telefony/smartfony";
            int index = rawURL.indexOf("technodom.kz/");
         String result= rawURL.substring(index+12,rawURL.length());
        System.out.println(result);
    }
}
