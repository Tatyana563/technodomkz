package com.example.technodomkz;

import com.example.technodomkz.repository.ItemPriceRepository;
import com.example.technodomkz.repository.ItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class ItemsUpdateTaskContext {
    private final ItemRepository itemRepository;
    private final ItemPriceRepository itemPriceRepository;
    private final WebDriverProperties webDriverProperties;
    private final PlatformTransactionManager transactionManager;

    public ItemsUpdateTaskContext(ItemRepository itemRepository, ItemPriceRepository itemPriceRepository, WebDriverProperties webDriverProperties, PlatformTransactionManager transactionManager) {
        this.itemRepository = itemRepository;
        this.itemPriceRepository = itemPriceRepository;
        this.webDriverProperties = webDriverProperties;
        this.transactionManager = transactionManager;
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

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }
}
