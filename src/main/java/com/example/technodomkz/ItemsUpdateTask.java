package com.example.technodomkz;


import com.example.technodomkz.model.Category;
import com.example.technodomkz.model.City;
import com.example.technodomkz.model.Item;
import com.example.technodomkz.model.ItemPrice;
import com.example.technodomkz.repository.ItemPriceRepository;
import com.example.technodomkz.repository.ItemRepository;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.DocFlavor;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ItemsUpdateTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ItemsUpdateTask.class);
    private static final Pattern PATTERN = Pattern.compile("Артикул:\\s*(\\S*)");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(^([0-9]+\\s*)*)");
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(\\d+)");
    //TODO: export options to application.properties
    private static final long PAGE_TIMEOUT = 7 * 1000L;
    private static final int RETRIES_COUNT = 3;

    private final ItemRepository itemRepository;
    private final ItemPriceRepository itemPriceRepository;
    private final Category category;
    private final City city;
    private final WebDriver webDriver;

    public ItemsUpdateTask(ItemRepository itemRepository, ItemPriceRepository itemPriceRepository, Category category, City city, WebDriver webDriver) {
        this.itemRepository = itemRepository;
        this.itemPriceRepository = itemPriceRepository;
        this.category = category;
        this.city = city;
        this.webDriver = webDriver;
    }

    @Override
    public void run() {
        try {
            LOG.warn("Начиначем обработку категории '{}'", category.getName());
            synchronized (webDriver) {
                String categorySuffix = URLUtil.getCategorySuffix(category.getUrl(), Constants.URL);
                String categoryUrl = String.format("%s/%s/%s", Constants.URL, city.getUrlSuffix(), categorySuffix);
                webDriver.get(categoryUrl);
            }

            WebElement nextPageLink = null;
            outer:
            do {
                if (nextPageLink != null) {
                    nextPageLink.click();
                }
                long start = System.currentTimeMillis();
                while (!webDriver.findElements(By.cssSelector(".CategoryProductList .ProductCard.ProductCard_isLoading")).isEmpty()) {
                    if (System.currentTimeMillis() - start >= PAGE_TIMEOUT) {
                        LOG.warn("Слишком долго получаем данные");
                        //TODO: retry to get data / reload page
                        break outer;
                    }
                    Thread.sleep(200);
                }
                Document itemsPage = Jsoup.parse(webDriver.getPageSource());
                parseItems(itemsPage);
            } while ((nextPageLink = webDriver.findElement(By.cssSelector(".CategoryPagination-ListItem .CategoryPagination-Arrow_direction_next"))).isDisplayed());
        } catch (NoSuchElementException | InterruptedException noSuchElementException) {
            // nothing to do.
        } finally {
            LOG.warn("Обработка категории '{}' завершена", category.getName());
        }
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
        String itemUrl = itemElement.selectFirst(".ProductCard-Content").attr("href");
        String itemText = itemElement.selectFirst("h4").text();
        String itemPrice = itemElement.selectFirst(".ProductPrice data[value]").attr("value");

        LOG.info("Продукт: {} {}", itemText, itemPrice);
//        String externalCode = URLUtil.extractExternalIdFromUrl(itemUrl);
//        if (externalCode != null && externalCode.isEmpty()) {
//            LOG.warn("Продукт без кода: {}\n{}", itemText, itemUrl);
//            return;
//        }
//
//        Item item = itemRepository.findOneByExternalId(externalCode).orElseGet(() -> new Item(externalCode));
//
//        String itemDescription = itemElement.selectFirst(".list-unstyled").text();
//        Matcher matcher = PATTERN.matcher(itemDescription);
//        if (matcher.find()) {
//            String itemCode = matcher.group(1);
//            item.setCode(itemCode);
//        }
//
//        item.setModel(itemText);
//        item.setImage(itemPhoto);
//        item.setDescription(itemDescription);
//        String itemUrlWithoutCity = URLUtil.removeCityFromUrl(itemUrl);
//        item.setUrl(itemUrlWithoutCity);
//        item.setCategory(category);
//        itemRepository.save(item);
//
//        String itemPriceString = itemElement.selectFirst(".price").text();
//        Matcher priceMatcher = PRICE_PATTERN.matcher(itemPriceString);
//        if (priceMatcher.find()) {
//            String price = priceMatcher.group(0).replaceAll("\\s*", "");
//
//            ItemPrice itemPrice = itemPriceRepository.findOneByItemAndCity(item, city).orElseGet(() -> {
//                ItemPrice newItemPrice = new ItemPrice();
//                newItemPrice.setItem(item);
//                newItemPrice.setCity(city);
        //TODO: item availability

//                return newItemPrice;
//            });
//
//            itemPrice.setPrice(Double.valueOf(price));
//            itemPriceRepository.save(itemPrice);
//        }
    }

}





