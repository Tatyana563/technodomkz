package com.example.technodomkz;

import com.example.technodomkz.model.Category;
import com.example.technodomkz.model.City;
import com.example.technodomkz.model.MainGroup;
import com.example.technodomkz.model.Section;
import com.example.technodomkz.repository.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
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
import java.net.URL;
import java.util.List;
import java.util.Set;

@Component
public class SectionParser {
    private static final Logger LOG = LoggerFactory.getLogger(SectionParser.class);

    private static final Set<String> SECTIONS = Set.of(
            "Смартфоны и гаджеты", "Ноутбуки и компьютеры", "Всё для геймеров",
            "Фототехника и квадрокоптеры", "Бытовая техника", "Техника для кухни",
            "ТВ, аудио, видео");

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
        if (driver != null) {
            driver.quit();
        }
    }


    @Scheduled(fixedDelay = ONE_WEEK_MS)
    @Transactional
    public void getSections() {

        driver.get(Constants.CATEGORIES_URL);
        long loaded = System.currentTimeMillis();
        Document document = Jsoup.parse(driver.getPageSource());
        LOG.info("Получили главную страницу, ищем секции...");
        Elements sectionElements = document.select("div.CatalogPage-CategorySection");
        for (Element sectionElement : sectionElements) {
            Element sectionTitle = sectionElement.selectFirst("h2.CatalogPage-CategoryTitle");
            String sectionName = sectionTitle.text();
            if (SECTIONS.contains(sectionName)) {
                LOG.info("Секция: {}", sectionName);
                Section section = sectionRepository.findOneByName(sectionName)
                        .orElseGet(() -> sectionRepository.save(new Section(sectionName, null)));
                Elements groupElements = sectionElement.select("div.CatalogPage-Category");
                for (Element groupElement : groupElements) {
                    String groupLink = groupElement.selectFirst("h3.CatalogPage-SubcategoryTitle > a").attr("href");
                    String groupTitle = groupElement.selectFirst("h3.CatalogPage-SubcategoryTitle > a").text();
                    LOG.info("\tГруппа: {}", groupTitle);
                    MainGroup group = mainGroupRepository.findOneByUrl(groupLink)
                            .orElseGet(() -> mainGroupRepository.save(new MainGroup(groupTitle, groupLink, section)));
                    Elements categoryLinks = groupElement.select("li.CatalogPage-Subcategory > a");
                    for (Element categoryLink : categoryLinks) {
                        LOG.info("\t\tКатегория: {}", categoryLink.text());
                        String categoryText = categoryLink.text();
                        String categoryUrl = categoryLink.absUrl("href");
                        if (!categoryRepository.existsByUrl(categoryUrl)) {
                            categoryRepository.save(new Category(categoryText, categoryUrl, group));
                        }
                    }
                }
            }
        }
        parseCities(loaded);
    }

    private void parseCities(long loaded) {
        checkForModalPanels(loaded);
        try {
            WebElement citySelectModal = driver.findElement(By.cssSelector(".ReactModal__Content.VerifyCityModal"));
            if (citySelectModal.isDisplayed()) {
                List<WebElement> buttons = citySelectModal.findElements(By.cssSelector(".ButtonNext__Text.ButtonNext__Text_Size-L"));
                for (WebElement button : buttons) {
                    if ("да".equalsIgnoreCase(button.getText())) {
                        button.click();
                        break;
                    }
                }
            }
        } catch (NoSuchElementException noSuchElementException) {
            // nothing to do.
        }

        openCitiesPopup();

        Document pageWithCitiesModal = Jsoup.parse(driver.getPageSource());
        Elements cityUrls = pageWithCitiesModal.select("a.CitiesModal__List-Item");
        for (Element cityUrl : cityUrls) {
            LOG.info("Город: {}", cityUrl.text());
            String cityLink = URLUtil.extractCityFromUrl(cityUrl.attr("href"), Constants.ALL_SUFFIX);;
            String cityText = cityUrl.text();
            if (!cityRepository.existsByUrlSuffix(cityLink)) {
                cityRepository.save(new City(cityText, cityLink));
            }
        }

        closeCitiesPopup();
    }

    private void checkForModalPanels(long loaded) {
        long now = System.currentTimeMillis();
        long past = now - loaded;
        long left = 20L * 1000L - past;
        try {
            LOG.info("Ожидаем возможные модальные окна {} мс...", left);
            Thread.sleep(left);
            LOG.info("Дождались");
            WebElement element;
            while ((element = driver.findElement(
                    By.cssSelector("div[id$='-popup-modal'] [id$='-popup-close']"))).isDisplayed()) {
                element.click();
                Thread.sleep(1000L);
            }
        } catch (NoSuchElementException noSuchElementException) {
            // nothing to do.
        } catch (Exception e) {
            LOG.error("Проблема определения модальыых окон", e);
        }
    }


    @Scheduled(initialDelay = 1200, fixedDelay = ONE_WEEK_MS)
    @Transactional
    public void getAdditionalArticleInfo(){
        LOG.info("Получаем дополнитульную информацию о товарe...");
        int page = 0;

        List<Category> categories;
        List<City> cities = cityRepository.findAll();
        for (int i = 0; i < cities.size(); i++) {
            City city = cities.get(i);
            switchCity(city);
            LOG.info("-------------------------------------");
            LOG.info("Получаем списки товаров для {}", city.getUrlSuffix());
            LOG.info("-------------------------------------");
            page = 0;
            while (!(categories = categoryRepository.getChunk(PageRequest.of(page++, chunkSize))).isEmpty()) {
                LOG.info("Получили из базы {} категорий", categories.size());
                for (Category category : categories) {
                    new ItemsUpdateTask(itemRepository,
                            itemPriceRepository,
                            category,
                            city,
                            driver)
                            .run();

                }
                LOG.info("Задачи выполнены, следующая порция...");

            }
        }

    }

//    private void modalSafeClick(WebElement element) {
//
//        while (!element.isDisplayed()) {
//            checkForModalPanels();
//        }
//        element.click();
//    }

    private void switchCity(City city) {
        openCitiesPopup();
        List<WebElement> cityLinks = driver.findElements(By.cssSelector("a.CitiesModal__List-Item"));
        for (WebElement cityLink : cityLinks) {
            if(city.getName().equalsIgnoreCase(cityLink.getText())) {
                cityLink.click();
                break;
            }
        }
    }

    private void openCitiesPopup() {
        driver.findElement(By.cssSelector(".CitySelector__Button")).click();
        driver.findElement(By.cssSelector(".CitiesModal__More-Btn")).click();
    }

    private void closeCitiesPopup() {
        driver.findElement(By.cssSelector(".ReactModal__Content.CitiesModal .ModalNext__CloseBtn")).click();
    }
}

