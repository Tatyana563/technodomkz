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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

public class ItemsUpdateTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ItemsUpdateTask.class);

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
            synchronized (webDriver) {
                //TODO: take paging logic from fora/mechta etc.
                String categorySuffix = URLUtil.getCategorySuffix(category.getUrl(), Constants.URL);
                //TODO: take into account empty city suffix
                //https://www.technodom.kz/noutbuki-i-komp-jutery/noutbuki-i-aksessuary/noutbuki
                //https://www.technodom.kz/aktobe/noutbuki-i-komp-jutery/noutbuki-i-aksessuary/noutbuki
                categoryUrl = String.format("%s/%s/%s", Constants.URL, city.getUrlSuffix(), categorySuffix);
                webDriver.get(categoryUrl);
            }

            WebElement nextPageLink = null;
            pager_loop:
            do {
                if (nextPageLink != null) {
                    nextPageLink.click();
                }
                long start = System.currentTimeMillis();
                //O
                int attempts = 0;
                // TODO: use webdriver wait object
                //wait.until(ExpectedConditions.not(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".CategoryProductList .ProductCard.ProductCard_isLoading"))));
                while (!webDriver.findElements(By.cssSelector(".CategoryProductList .ProductCard.ProductCard_isLoading")).isEmpty()) {
                    if (System.currentTimeMillis() - start >= context.getWebDriverProperties().getTimeout()) {
                        LOG.warn("Слишком долго получаем данные");
                        if (attempts <= context.getWebDriverProperties().getRetryCount()) {
                            start = System.currentTimeMillis();
                            attempts++;
                            //TODO: get current page
                            webDriver.get(categoryUrl);
                        }
                        else {
                            // go to next items page.
                            break pager_loop;
                        }
                    }
                    else {
                        Thread.sleep(200);
                    }
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
}







