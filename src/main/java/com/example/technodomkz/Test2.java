package com.example.technodomkz;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.concurrent.TimeUnit;

public class Test2 {
    public static void main(String[] args) {
        WebDriver driver = null;
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setBinary("/usr/bin/google-chrome");
//       options.addArguments("--headless");
        options.addArguments("window-size=1920x1080");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);


        driver.get("https://www.technodom.kz/smartfony-i-gadzhety/smartfony-i-telefony/smartfony/f/brands/samsung?page=3");

        Document itemsPage = Jsoup.parse(driver.getPageSource());

        Element itemElement= itemsPage.selectFirst(".ProductCard-ProductActions");

        boolean notAvailable = itemElement.text().toLowerCase().contains("нет в наличии");
        System.out.println(notAvailable);
    }
}
