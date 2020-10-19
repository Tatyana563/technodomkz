package com.example.technodomkz;

import com.example.technodomkz.model.Category;
import com.example.technodomkz.model.City;
import com.example.technodomkz.model.MainGroup;
import com.example.technodomkz.model.Section;
import com.example.technodomkz.repository.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class SectionParser {
    private static final Logger LOG = LoggerFactory.getLogger(SectionParser.class);

    private static final Set<String> SECTIONS = Set.of("Ноутбуки, компьютеры", "Комплектующие", "Оргтехника", "Смартфоны, планшеты",
            "Телевизоры, аудио, видео", "Техника для дома", "Техника для кухни", "Фото и видео");

    private static final String URL = "https://technodom.kz/";
    private static final String CATEGORIES_URL = URL + "all";

    private static final long ONE_SECOND_MS = 1000L;
    private static final long ONE_MINUTE_MS = 60 * ONE_SECOND_MS;
    private static final long ONE_HOUR_MS = 60 * ONE_MINUTE_MS;
    private static final long ONE_DAY_MS = 24 * ONE_HOUR_MS;
    private static final long ONE_WEEK_MS = 7 * ONE_DAY_MS;

    @Value("${parser.chrome.path}")
    private String path;
    @Value("${technodom.api.chunk-size}")
    private Integer chunkSize;
    @Value("${technodom.thread-pool.pool-size}")
    private Integer threadPoolSize;
    @Autowired
    private SectionRepository sectionRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private MainGroupRepository mainGroupRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ItemPriceRepository itemPriceRepository;
    @Autowired
    private CityRepository cityRepository;

    private WebDriver driver = null;

    @PostConstruct
    public void init() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setBinary(path);
//        options.addArguments("--headless");
        options.addArguments("window-size=1920x1080");
        driver = new ChromeDriver(options);
    }

    @PreDestroy
    public void destroy() {
        if(driver != null) {
            driver.quit();
        }
    }


    @Scheduled(fixedDelay = ONE_WEEK_MS)
    @Transactional
    public void getSections() throws IOException {

        driver.get(CATEGORIES_URL);
        Document document = Jsoup.parse(driver.getPageSource());
        LOG.info("Получили главную страницу, ищем секции...");
        Elements sectionElements = document.select("div.CatalogPage-CategorySection");
        for (Element sectionElement : sectionElements) {
            Element sectionTitle = sectionElement.selectFirst("h2.CatalogPage-CategoryTitle");
            String sectionName = sectionTitle.text();
            LOG.info("Секция: {}", sectionName);
            Section section = sectionRepository.findOneByName(sectionName)
                    .orElseGet(() -> sectionRepository.save(new Section(sectionName, null)));
            Elements groupElements = sectionElement.select("div.CatalogPage-Category");
            for (Element groupElement : groupElements) {
                Element groupLink = groupElement.selectFirst("h3.CatalogPage-SubcategoryTitle > a");
                LOG.info("\tГруппа: {}", groupLink.text());
                Elements categoryLinks = groupElement.select("li.CatalogPage-Subcategory > a");
                for (Element categoryLink : categoryLinks) {
                    LOG.info("\t\tКатегория: {}", categoryLink.text());
                }
            }
        }
        parseCities(driver);
    }

    private void parseCities(WebDriver driver) {
        WebElement citySelectModal = driver.findElement(By.cssSelector(".ReactModal__Content.VerifyCityModal"));
        if (citySelectModal.isDisplayed())
        {
            List<WebElement> buttons = citySelectModal.findElements(By.cssSelector(".ButtonNext__Text.ButtonNext__Text_Size-L"));
            for (WebElement button : buttons) {
                if ("да".equalsIgnoreCase(button.getText())) {
                    button.click();
                    break;
                }
            }
        }

        driver.findElement(By.cssSelector(".CitySelector__Button")).click();
        driver.findElement(By.cssSelector(".CitiesModal__More-Btn")).click();


        Document pageWithCitiesModal = Jsoup.parse(driver.getPageSource());
        Elements cityUrls = pageWithCitiesModal.select("a.CitiesModal__List-Item");
        for (Element cityUrl : cityUrls) {
            LOG.info("Город: {}", cityUrl.text());
        }
//        CitiesModal__List-Item
//            if (!cityRepository.existsByUrlSuffix(citySuffix)) {
//                cityRepository.save(new City(cityName, citySuffix));
//            }

    }



//    @Scheduled(initialDelay = 1200, fixedDelay = ONE_WEEK_MS)
//    @Transactional
    public void getAdditionalArticleInfo() throws InterruptedException, IOException {
        LOG.info("Получаем дополнитульную информацию о товарe...");
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        List<Category> categories;

        // 1. offset + limit
        // 2. page + pageSize
        //   offset = page * pageSize;  limit = pageSize;
        List<City> cities = cityRepository.findAll();
//        categories = categoryRepository.getChunk(PageRequest.of(page++, chunkSize)
        Map<String, String> cookies = new HashMap<>();
        for (int i = 0; i < cities.size(); i++) {
            LOG.info("-------------------------------------");
            LOG.info("Получаем списки товаров для {}", cities.get(i).getUrlSuffix());
            LOG.info("-------------------------------------");
            String urlWithCity = URL + cities.get(i).getUrlSuffix();
            Connection.Response response = Jsoup.connect(urlWithCity)
                    .cookies(cookies)
                    .method(Connection.Method.GET)
                    .execute();
            cookies.putAll(response.cookies());
            int page = 0;
            while (!(categories = categoryRepository.getChunk(PageRequest.of(page++, chunkSize))).isEmpty()) {
                LOG.info("Получили из базы {} категорий", categories.size());
                CountDownLatch latch = new CountDownLatch(categories.size());
                for (Category category : categories) {
                    new ItemsUpdateTask(itemRepository,
                            itemPriceRepository,
                            category,
                            cities.get(i),
                            cookies,
                            latch)
                            
                            .run();

                }

                LOG.info("Задачи запущены, ожидаем завершения выполнения...");
                latch.await();
                LOG.info("Задачи выполнены, следующая порция...");

            }
        }
        executorService.shutdown();
    }
}

