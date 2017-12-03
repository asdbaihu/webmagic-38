package com.zlikun.learning.noval;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import java.io.File;
import java.io.IOException;

/**
 * 自定义Pipeline，用于持久化章节内容
 * @author zlikun <zlikun-dev@hotmail.com>
 * @date 2017/12/2 20:52
 */
@Slf4j
public class CustomPipeline implements Pipeline {

    @Override
    public void process(ResultItems items, Task task) {
        log.info("download url : {}", items.getRequest().getUrl());

        Long number = items.get("number");
        if (number == null) return;

        String title = items.get("title");
        String content = items.get("content");

        try {
            FileUtils.write(new File("D:\\Temp\\novel\\" + number + ".txt"), title + "\r\n\r\n" + content, "UTF-8");
        } catch (IOException e) {
            log.error("持久化数据到文件出错!", e);
        }

    }
}
