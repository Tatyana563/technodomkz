package com.example.technodomkz;


import com.example.technodomkz.model.Section;
import com.example.technodomkz.repository.CategoryRepository;
import com.example.technodomkz.repository.ItemRepository;
import com.example.technodomkz.repository.MainGroupRepository;
import com.example.technodomkz.repository.SectionRepository;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class SectionParser {
    private static final Logger LOG = LoggerFactory.getLogger(SectionParser.class);

    private static final Set<String> SECTIONS = Set.of("Ноутбуки, компьютеры", "Комплектующие", "Оргтехника", "Смартфоны, планшеты", " Телевизоры, аудио, видео",
            "Техника для дома", "Техника для кухни", "Фото и видео");

    private static final String URL = "https://www.technodom.kz";

    private static final long ONE_SECOND_MS = 1000L;
    private static final long ONE_MINUTE_MS = 60 * ONE_SECOND_MS;
    private static final long ONE_HOUR_MS = 60 * ONE_MINUTE_MS;
    private static final long ONE_DAY_MS = 24 * ONE_HOUR_MS;
    private static final long ONE_WEEK_MS = 7 * ONE_DAY_MS;


    @Value("${fora.api.chunk-size}")
    private Integer chunkSize;
    @Value("${fora.thread-pool.pool-size}")
    private Integer threadPoolSize;
    @Autowired
    private SectionRepository sectionRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private MainGroupRepository mainGroupRepository;
    @Autowired
    private ItemRepository itemRepository;


    @Scheduled(fixedDelay = ONE_WEEK_MS)
    @Transactional
    public void getSections() throws IOException {
        final WebClient webClient = new WebClient(BrowserVersion.FIREFOX);
        webClient.getOptions().setCssEnabled(true);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        final HtmlPage page = webClient.getPage(URL);
        webClient.waitForBackgroundJavaScript(40_000);
        LOG.info("Получили главную страницу, ищем секции...");
        Elements sectionElements = page.getHtmlElementById()
        for (Element sectionElement : sectionElements) {
            Element sectionElementLink = sectionElement.selectFirst(">a");
            String text = sectionElementLink.text();
            if (SECTIONS.contains(text)) {
                LOG.info("Получаем {}...", text);
                String sectionUrl = sectionElementLink.absUrl("href");
                Section section = sectionRepository.findOneByUrl(sectionUrl)
                        .orElseGet(() -> sectionRepository.save(new Section(text, sectionUrl)));

    }

//    private void processGroupWithCategories(Section section, Element currentGroup, List<Element> categories) {
//        if (currentGroup == null) {
//            return;
//        }
//        Element groupLink = currentGroup.selectFirst(">a");
//        String groupUrl = groupLink.absUrl("href");
//        String groupText = groupLink.text();
//        LOG.info("Группа  {}", groupText);
//        MainGroup group = mainGroupRepository.findOneByUrl(groupUrl)
//                .orElseGet(() -> mainGroupRepository.save(new MainGroup(groupText, groupUrl, section)));
//        if (categories.isEmpty()) {
//            if (!categoryRepository.existsByUrl(groupUrl)) {
//                categoryRepository.save(new Category(groupText, groupUrl, group));
//            }
//        } else {
//            for (Element categoryElement : categories) {
//                Element categoryLink = categoryElement.selectFirst(">a");
//                String categoryUrl = categoryLink.absUrl("href");
//                String categoryText = categoryLink.text();
//                LOG.info("\tКатегория  {}", categoryText);
//                if (!categoryRepository.existsByUrl(categoryUrl)) {
//                    categoryRepository.save(new Category(categoryText, categoryUrl, group));
//                }
//
//            }
//        }
//    }


//    @Scheduled(initialDelay = 1200, fixedDelay = ONE_WEEK_MS)
//    @Transactional
//    public void getAdditionalArticleInfo() throws InterruptedException {
//        LOG.info("Получаем дополнитульную информацию о товарe...");
//        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
//        int page = 0;
//        List<Category> categories;
//
//        // 1. offset + limit
//        // 2. page + pageSize
//        //   offset = page * pageSize;  limit = pageSize;
//        while (!(categories = categoryRepository.getChunk(PageRequest.of(page++, chunkSize))).isEmpty()) {
//            LOG.info("Получили из базы {} категорий", categories.size());
//            CountDownLatch latch = new CountDownLatch(categories.size());
//            for (Category category : categories) {
//                executorService.execute(new ItemsUpdateTask(itemRepository, category, latch));
//            }
//            LOG.info("Задачи запущены, ожидаем завершения выполнения...");
//            latch.await();
//            LOG.info("Задачи выполнены, следующая порция...");
//        }
//        executorService.shutdown();
//    }
}

