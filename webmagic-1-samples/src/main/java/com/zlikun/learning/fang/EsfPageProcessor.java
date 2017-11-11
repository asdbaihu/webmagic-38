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
 * 杭州二手房信息爬虫(http://esf.hz.fang.com/)
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2017/11/11 10:31
 */
@Slf4j
public class EsfPageProcessor implements PageProcessor {

    private String domain = "esf.hz.fang.com" ;

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

    public static void main(String[] args) {
        // 创建并启动一个爬虫
        Spider.create(new EsfPageProcessor())
                // 添加一个(多个)启动URL
                .addUrl("http://esf.hz.fang.com/")
                // 设置Scheduler，用BlockingQueue来管理URL队列
                .setScheduler(new QueueScheduler())
                // 设置Pipeline，这里使用控制台Pipeline，表示直接输出到控制台
                .addPipeline(new ConsolePipeline())
                // 开启5个线程同时运行
                .thread(5)
                // 启动爬虫
                .run();
    }

    @Override
    public void process(Page page) {
        // 获取当前页数据
        // 数据容器结构：div.houseList > dl > 数据内容

        // 个人更喜欢使用Jsoup来处理Html，这样在使用其它爬虫框架时不用再学一套抽取规则
        Document doc = page.getHtml().getDocument();
        // 网页内部的相对路径链接转换为绝对路径
        doc.setBaseUri("http://" + this.domain);

        // 获取网页标题
        page.putField("title", doc.title());

        // 获取数据列表
        doc.select("div.houseList > dl")
                .parallelStream()
                .filter(element -> element != null && element.hasText())
                .forEach(element -> extractInfo(element));

        // 提取链接，这里使用正则表达式实现
        List<String> links = doc.select("#list_D10_15 > a")
                // 转换成Stream
                .stream()
                // 抽取信息映射成为一个新的Stream
                .map(element -> {
                    String link = element.attr("href") ;
                    return StringUtils.isBlank(link) ? null : link ;
                })
                // 过滤空链接
                .filter(link -> link != null)
                // 返回List<String>
                .collect(Collectors.toList());

        // 将提取的链接加入到目标队列中
        if (CollectionUtils.isNotEmpty(links)) {
            page.addTargetRequests(links);
        }

    }

    /**
     * 提取单个节点信息
     * @param node
     */
    private void extractInfo(Element node) {
        if (node == null) return;
        // 封面图片、详情页链接
        String link = node.select("dt > a").first().attr("href") ;
        // 标题、描述
        String title = node.select("p.title").first().text() ;
        String desc = node.select("p.mt12").first().text() ;
        // 小区 (p.mt10)，TODO 简单提取文字，细节后续完善
        String xiaoQu = node.select("p.mt10").first().text() ;
        // 标签 (div.mt8)
        String tags = node.select("div.mt8").first().text() ;
        // 建筑面积 (div.area)
        String area = node.select("div.area > p").first().text() ;
        // 房价 (div.moreInfo)
        String price = node.select("div.moreInfo").first().text() ;
        log.info("link : {} ,title : {} ,desc : {} ,xiaoQu : {} ,tags : {} ,area : {} ,price : {}"
                , "http://" + this.domain + link, title, desc, xiaoQu, tags, area, price);
    }

    @Override
    public Site getSite() {
        return this.site;
    }
}
