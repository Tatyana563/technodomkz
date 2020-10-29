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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
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
    private final WebDriver webDriver;

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
            String categorySuffix = URLUtil.getCategorySuffix(category.getUrl(), Constants.URL);
            if (city.getUrlSuffix() == null) {
                categoryUrl = String.format("%s/%s", Constants.URL, categorySuffix);
            } else {
                categoryUrl = String.format("%s/%s/%s", Constants.URL, city.getUrlSuffix(), categorySuffix);
            }
            pageUrlFormat = categoryUrl + PAGE_URL_FORMAT;
            String firstPageUrl = String.format(pageUrlFormat, 1);
            Document itemsPage = loadItemsPage(firstPageUrl);
            if (itemsPage != null) {
                int totalPages = getTotalPages(itemsPage);
                parseItems(itemsPage);
                for (int pageNumber = 2; pageNumber <= totalPages; pageNumber++) {
                    LOG.info("Получаем список товаров ({}) - страница {}", category.getName(), pageNumber);
                    String pageUrl = String.format(pageUrlFormat, pageNumber);
                    Document itemsPages = null;
                    synchronized (webDriver) {
                        webDriver.get(pageUrl);
                        long start = System.currentTimeMillis();
                        int attempts = 0;
                        boolean failed = false;
                        while (!webDriver.findElements(By.cssSelector(".CategoryProductList .ProductCard.ProductCard_isLoading")).isEmpty()) {
                            if (System.currentTimeMillis() - start >= context.getWebDriverProperties().getTimeout()) {
                                LOG.warn("Слишком долго получаем данные");
                                if (attempts <= context.getWebDriverProperties().getRetryCount()) {
                                    start = System.currentTimeMillis();
                                    attempts++;
                                    webDriver.get(pageUrl);
                                } else {
                                    // go to next items page.
                                    failed = true;
                                    break;
                                }
                            } else {
                                Thread.sleep(200);
                            }
                        }
                        if (failed) {
                            continue;
                        }
                        itemsPages = Jsoup.parse(webDriver.getPageSource());
                    }
                    parseItems(itemsPages);
                }
            }
            else {
                LOG.error("Не удалось получить первую страницу категории");
            }
        } catch (Exception exception) {
            LOG.error("Не получилось распарсить категорию", exception);
        } finally {
            LOG.warn("Обработка категории '{}' завершена", category.getName());
        }
    }

    private Document loadItemsPage(String pageUrl) {
        int attempts = 0;
        synchronized (webDriver) {
            while (attempts <= context.getWebDriverProperties().getRetryCount()) {
                webDriver.get(pageUrl);
                Wait<WebDriver> wait = new FluentWait<>(webDriver)
                        .withMessage("Product items not found")
                        .withTimeout(Duration.ofSeconds(10))
                        .pollingEvery(Duration.ofMillis(200));

                try {
                    wait.until(
                            ExpectedConditions.presenceOfElementLocated(
                                    By.cssSelector(".CategoryProductList .ProductCard:not(.ProductCard_isLoading)")));
                } catch (Exception e) {
                    LOG.error("Не удалось загрузить список продуктов", e);
                    attempts++;
                    continue;
                }

                return Jsoup.parse(webDriver.getPageSource());
            }
        }
        return null;
    }

    private void parseItems(Document itemsPage) {
        if (!isValidCity(itemsPage)) {
            String text = itemsPage.selectFirst("p.CitySelector__Title").text();
            LOG.error("Используется другой город {}", text);
            return;
        }

        Elements itemElements = itemsPage.select(".CategoryProductList li.ProductCard");

        for (Element itemElement : itemElements) {
            try {
                parseSingleItem(itemElement);
            } catch (Exception e) {
                LOG.error("Не удалось распарсить продукт", e);
            }

        }
    }

    private boolean isValidCity(Document page) {
        return city.getName().equalsIgnoreCase(page.selectFirst("p.CitySelector__Title").text());

    }

    private void parseSingleItem(Element itemElement) {
        String itemPhoto = itemElement.selectFirst(".ProductCard-Image img").absUrl("src");
        String itemUrl = itemElement.selectFirst(".ProductCard-Content").absUrl("href");
        String itemText = itemElement.selectFirst("h4").text();
        String itemPriceValue = itemElement.selectFirst(".ProductPrice data[value]").attr("value");

        LOG.info("Продукт: {} {}", itemText, itemPriceValue);
        //TODO: use itemUrl instead of externalCode (id)
        String externalCode = URLUtil.extractExternalIdFromUrl(itemUrl);
        if (externalCode == null || externalCode.isEmpty()) {
            LOG.warn("Продукт без кода: {}\n{}", itemText, itemUrl);
            return;
        }

        PlatformTransactionManager transactionManager = context.getTransactionManager();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(transactionStatus -> {

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
                boolean notAvailable = itemElement.selectFirst(".ProductCard-ProductActions").text().toLowerCase().contains("нет в наличии");
                newItemPrice.setAvailable(!notAvailable);

                return newItemPrice;
            });

            itemPrice.setPrice(Double.valueOf(itemPriceValue));
            itemPriceRepository.save(itemPrice);
        });

    }

    private int getTotalPages(Document firstPage) {
        Element pageElement = firstPage.selectFirst(".CategoryPage-ItemsCount");
        if (pageElement != null) {
            Integer amountOfProducts;
            String quantity = pageElement.text();
            //Всего 24 продуктов
            Matcher matcher = QUANTITY_PATTERN.matcher(quantity);
            if (matcher.find()) {
                amountOfProducts = Integer.valueOf(matcher.group(1));
                int main = amountOfProducts / NUMBER_OF_PRODUCTS_PER_PAGE;
                return main + (amountOfProducts % NUMBER_OF_PRODUCTS_PER_PAGE > 0 ? 1 : 0);
            }
        }
        return 0;
    }
}






