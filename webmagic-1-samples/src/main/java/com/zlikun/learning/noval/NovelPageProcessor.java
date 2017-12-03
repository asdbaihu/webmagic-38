package com.zlikun.learning.noval;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.nodes.Document;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.QueueScheduler;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 小说爬虫
 * http://www.mywenxue.com/xiaoshuo/64/64489/Index.htm
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2017/12/2 18:01
 */
@Slf4j
public class NovelPageProcessor implements PageProcessor {

    private String domain = "www.mywenxue.com" ;

    private Site site = Site.me()
            // 设置域名，设置Cookie时，该项是必要的
            .setDomain(domain)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
            // 设置超时毫秒数
            .setTimeOut(3000)
            // 设置重试次数
            .setRetryTimes(0)
            // 设置循环重试次数：失败后会重新添加到队尾
            .setCycleRetryTimes(3)
            ;

    public static void main(String[] args) throws IOException {

        // 创建并启动一个爬虫
        Spider.create(new NovelPageProcessor())
                .addUrl("http://www.mywenxue.com/xiaoshuo/64/64489/Index.htm")
                .setScheduler(new QueueScheduler())
                .addPipeline(new CustomPipeline())
                .thread(5)
                .run();

    }

    @Override
    public void process(Page page) {

        final String prefix = "http://www.mywenxue.com/xiaoshuo/64/64489/";

        // 忽略非目标网页
        if (!page.getUrl().toString().startsWith(prefix)) return;

        Document doc = page.getHtml().getDocument();

        if (page.getUrl().toString().matches(prefix + "\\d+\\.htm$")) {
            // 章节内容页
            content(page, doc, prefix);
        } else {
            // 章节列表页
            // 获取全部章节链接
            List<String> links = doc.select("li > strong > a")
                    .stream()
                    .filter(e -> e.hasAttr("href"))
                    .map(e -> {
                        String link = prefix + e.attr("href");
                        return link;
                    })
                    .collect(Collectors.toList());

            // 将链接加入到队列
            page.addTargetRequests(links);
//            page.addTargetRequests(Arrays.asList("http://www.mywenxue.com/xiaoshuo/64/64489/21932626.htm"));
        }
    }

    /**
     * 获取章节正文信息
     * @param page
     * @param doc
     * @param prefix
     */
    private void content(Page page, Document doc, String prefix) {

        String number = StringUtils.substringBetween(doc.location(), prefix, ".htm");

        String title = doc.select("#htmltimu > h2").text();
        String content = doc.select("#chapterContent > p").text() ;

        // 加工正文
        content = content
                .replaceAll("   ", "\r\n")
                .replaceAll(" ", "")
                .replaceAll("  ", "");

        // 将章节信息写入Pipeline组件中
        page.putField("number", NumberUtils.toLong(number));
        page.putField("title", title);
        page.putField("content", content);

    }

    @Override
    public Site getSite() {
        return this.site;
    }
}
