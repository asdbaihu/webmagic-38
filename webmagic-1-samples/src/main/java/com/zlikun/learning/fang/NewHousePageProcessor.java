package com.zlikun.learning.fang;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.ConsolePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.QueueScheduler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 杭州新房爬虫 (http://newhouse.hz.fang.com/house/s/b91/)
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2017/11/12 21:01
 */
@Slf4j
public class NewHousePageProcessor implements PageProcessor {

    private String domain = "newhouse.hz.fang.com" ;

    private Site site = Site.me()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
            // 设置超时毫秒数
            .setTimeOut(3000)
            // 设置重试次数
            .setRetryTimes(0)
            // 设置循环重试次数：失败后会重新添加到队尾
            .setCycleRetryTimes(3)
            ;

    /**
     * 创建并启动一个爬虫
     * @param args
     */
    public static void main(String[] args) {
        Spider.create(new NewHousePageProcessor())
                .addUrl("http://newhouse.hz.fang.com/house/s/b91/")
                .setScheduler(new QueueScheduler())
                .addPipeline(new ConsolePipeline())
                .thread(20)
                .run();
    }

    @Override
    public void process(Page page) {

        String url = page.getUrl().toString() ;
        log.info("开始处理URL：{}" ,url);

        // 处理列表页
        if (url.matches("^http://newhouse.hz.fang.com/house/s/b\\d+/?$")) {
            handleList(page);
        }

        // 处理详情页
        if (url.matches("^http://[a-zA-Z0-9]+.fang.com/?$")) {
            handleDetails(page);
        }

    }

    private void handleDetails(Page page) {
        Document doc = page.getHtml().getDocument();
        // 信息区块
        Element $div = doc.select("div.information").first() ;
        // 提取分项信息
        String name = $div.select("h1").text() ;
        String price = $div.select("div.information_li > div.inf_left > span").first().text() ;
        log.info("楼盘：{}，均价： {}" ,name ,price.equals("待定") ? price : price + " 元/平米");

        // 其它信息

        // 将信息存入Elasticsearch中
    }

    /**
     * 处理列表页
     * @param page
     */
    private void handleList(Page page) {
        Document doc = page.getHtml().getDocument();
        doc.setBaseUri("http://" + this.domain);

        // 遍历其它列表页链接
        List<String> links = doc.select("div.page a").stream()
                .filter(element -> element.hasAttr("href"))
                .map(element -> {
                    return element.attr("href") ;
                })
                .filter(link -> link.matches("^/house/s/b\\d+/?$"))
                .map(link -> {
                    return "http://" + this.domain + link;
                })
                .collect(Collectors.toList()) ;
        // 将连接加入目标列表中
        if (CollectionUtils.isNotEmpty(links)) {
            page.addTargetRequests(links);
        }

        // 遍历明细页链接
        List<String> details = doc.select("div#newhouse_loupai_list li").stream()
                .map(element -> {
                    return element.select("div.nlcd_name > a").first().attr("href") ;
                })
                .filter(link -> StringUtils.isNotBlank(link) && StringUtils.startsWithIgnoreCase(link ,"http://"))
                .collect(Collectors.toList()) ;

        if (CollectionUtils.isNotEmpty(details)) {
            page.addTargetRequests(details);
        }

        // 提取当前网页信息
        extract(doc);
    }

    private void extract(Document doc) {
        // log.info("title : {}" ,doc.title());
    }

    @Override
    public Site getSite() {
        return this.site ;
    }
}
