package com.example.technodomkz;


import com.example.technodomkz.model.Category;
import com.example.technodomkz.model.City;
import com.example.technodomkz.model.Item;
import com.example.technodomkz.model.ItemPrice;
import com.example.technodomkz.repository.ItemPriceRepository;
import com.example.technodomkz.repository.ItemRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemsUpdateTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ItemsUpdateTask.class);
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(\\d+)");
    private static final Integer NUMBER_OF_PRODUCTS_PER_PAGE = 24;
    private static final String PAGE_URL_FORMAT = "?page=%d";
    private final ItemsUpdateTaskContext context;
    private final Category category;
    private final City city;
    WebDriver webDriver;

    public ItemsUpdateTask(ItemsUpdateTaskContext context, Category category, City city, WebDriver webDriver) {
        this.context = context;
        this.category = category;
        this.city = city;
        this.webDriver = webDriver;
    }

    @Override
    public void run() {
        try {
            LOG.warn("Начиначем обработку категории '{}'", category.getName());
            String categoryUrl;
            String pageUrlFormat;
            synchronized (webDriver) {
                //TODO: take paging logic from fora/mechta etc.
                String categorySuffix = URLUtil.getCategorySuffix(category.getUrl(), Constants.URL);
                //TODO: take into account empty city suffix
                //https://www.technodom.kz/noutbuki-i-komp-jutery/noutbuki-i-aksessuary/noutbuki
                //https://www.technodom.kz/aktobe/noutbuki-i-komp-jutery/noutbuki-i-aksessuary/noutbuki
                if (city.getUrlSuffix() == null) {
                    categoryUrl = String.format("%s/%s", Constants.URL, categorySuffix);
                } else {
                    categoryUrl = String.format("%s/%s/%s", Constants.URL, city.getUrlSuffix(), categorySuffix);
                }
                pageUrlFormat = categoryUrl + PAGE_URL_FORMAT;
                String firstPageUrl = String.format(pageUrlFormat, 1);
                webDriver.get(firstPageUrl);
                Document itemsPage = Jsoup.parse(webDriver.getPageSource());
                if (itemsPage != null) {
                    int totalPages = getTotalPages(itemsPage);
                    parseItems(itemsPage, webDriver);
                    for (int pageNumber = 2; pageNumber <= totalPages; pageNumber++) {
                        LOG.info("Получаем список товаров ({}) - страница {}", category.getName(), pageNumber);
                        String pageUrl = String.format(pageUrlFormat, pageNumber);
                        webDriver.get(pageUrl);
                        long start = System.currentTimeMillis();
                        int attempts = 0;

                        while (!webDriver.findElements(By.cssSelector(".CategoryProductList .ProductCard.ProductCard_isLoading")).isEmpty()) {
                            if (System.currentTimeMillis() - start >= context.getWebDriverProperties().getTimeout()) {
                                LOG.warn("Слишком долго получаем данные");
                                if (attempts <= context.getWebDriverProperties().getRetryCount()) {
                                    start = System.currentTimeMillis();
                                    attempts++;
                                    //TODO: get current page
                                    webDriver.get(categoryUrl);
                                } else {
                                    // go to next items page.
                                    break;
                                }
                            } else {
                                Thread.sleep(200);
                            }
                        }
                        Document itemsPages = Jsoup.parse(webDriver.getPageSource());
                        parseItems(itemsPages, webDriver);
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    //TODO: get current page
    // TODO: use webdriver wait object
    //TODO: get current page


// TODO: use webdriver wait object

//TODO: get current page


    private void parseItems(Document itemsPage, WebDriver driver) {
        if (!isValidCity(itemsPage, driver)) {
            String text = itemsPage.selectFirst("p.CitySelector__Title").text();
            LOG.error("Используется другой город {}", text);
            return;
        }

        Elements itemElements = itemsPage.select(".CategoryProductList li.ProductCard");

        for (Element itemElement : itemElements) {
            try {
                parseSingleItem(itemElement, driver);
            } catch (Exception e) {
                LOG.error("Не удалось распарсить продукт", e);
            }

        }
    }

    private boolean isValidCity(Document page, WebDriver driver) {
        Wait<WebDriver> wait = new FluentWait<>(driver)
                .withMessage("City button not found")
                .withTimeout(Duration.ofSeconds(10))
                .pollingEvery(Duration.ofMillis(200));

        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".CitySelector__Button")));
        } catch (Exception e) {
            LOG.error("Не удалось загрузить список категорий", e);
        }

        return city.getName().equalsIgnoreCase(page.selectFirst("p.CitySelector__Title").text());

    }

    private void parseSingleItem(Element itemElement, WebDriver driver) {
        Wait<WebDriver> wait = new FluentWait<>(driver)
                .withMessage("Item card not found")
                .withTimeout(Duration.ofSeconds(10))
                .pollingEvery(Duration.ofMillis(200));

        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".ProductCard-Content")));
        } catch (Exception e) {
            LOG.error("Не удалось загрузитьдетали товара", e);
            return;
        }

        String itemPhoto = itemElement.selectFirst(".ProductCard-Image img").absUrl("src");
        String itemUrl = itemElement.selectFirst(".ProductCard-Content").attr("href");
        String itemText = itemElement.selectFirst("h4").text();
        String itemPriceValue = itemElement.selectFirst(".ProductPrice data[value]").attr("value");

        LOG.info("Продукт: {} {}", itemText, itemPriceValue);
        String externalCode = URLUtil.extractExternalIdFromUrl(itemUrl);
        if (externalCode != null && externalCode.isEmpty()) {
            LOG.warn("Продукт без кода: {}\n{}", itemText, itemUrl);
            return;
        }
        ItemRepository itemRepository = context.getItemRepository();
        Item item = itemRepository.findOneByExternalId(externalCode).orElseGet(() -> new Item(externalCode));

        item.setModel(itemText);
        item.setImage(itemPhoto);

        String itemUrlWithoutCity = URLUtil.removeCityFromUrl(itemUrl, Constants.URL);
        item.setUrl(itemUrlWithoutCity);
        item.setCategory(category);
        itemRepository.save(item);


        ItemPriceRepository itemPriceRepository = context.getItemPriceRepository();
        ItemPrice itemPrice = itemPriceRepository.findOneByItemAndCity(item, city).orElseGet(() -> {
            ItemPrice newItemPrice = new ItemPrice();
            newItemPrice.setItem(item);
            newItemPrice.setCity(city);
            //TODO: item availability (check)
            boolean notAvailable = itemElement.selectFirst(".ProductCard-ProductActions").text().toLowerCase().contains("нет в наличии");
            newItemPrice.setAvailable(!notAvailable);

            return newItemPrice;
        });

        itemPrice.setPrice(Double.valueOf(itemPriceValue));
        itemPriceRepository.save(itemPrice);
    }

    private int getTotalPages(Document firstPage) {
        Element pageElement = firstPage.selectFirst(".CategoryPage-ItemsCount");
        if (pageElement != null) {
            int numberOfPages = 0;
            Integer amountOfProducts;
            String quantity = pageElement.text();
            //Всего 24 продуктов
            Matcher matcher = QUANTITY_PATTERN.matcher(quantity);
            if (matcher.find()) {
                amountOfProducts = Integer.valueOf(matcher.group(1));
                int main = amountOfProducts / NUMBER_OF_PRODUCTS_PER_PAGE;
                if (main != 0) {
                    if ((amountOfProducts % NUMBER_OF_PRODUCTS_PER_PAGE != 0)) {
                        numberOfPages = main + 1;
                    } else {
                        numberOfPages = main;
                    }
                } else {
                    numberOfPages = 1;
                }
            }
            return numberOfPages;
        } else return 0;
    }
}






