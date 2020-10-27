package com.example.technodomkz;

import com.example.technodomkz.repository.ItemPriceRepository;
import com.example.technodomkz.repository.ItemRepository;
import org.springframework.stereotype.Component;

@Component
public class ItemsUpdateTaskContext {
    private final ItemRepository itemRepository;
    private final ItemPriceRepository itemPriceRepository;
    private final WebDriverProperties webDriverProperties;

    public ItemsUpdateTaskContext(ItemRepository itemRepository, ItemPriceRepository itemPriceRepository, WebDriverProperties webDriverProperties) {
        this.itemRepository = itemRepository;
        this.itemPriceRepository = itemPriceRepository;
        this.webDriverProperties = webDriverProperties;
    }

    public ItemRepository getItemRepository() {
        return itemRepository;
    }

    public ItemPriceRepository getItemPriceRepository() {
        return itemPriceRepository;
    }

    public WebDriverProperties getWebDriverProperties() {
        return webDriverProperties;
    }
}
