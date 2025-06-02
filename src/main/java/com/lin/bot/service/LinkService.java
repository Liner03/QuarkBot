package com.lin.bot.service;


import com.lin.bot.util.Quark;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Author Lin.
 * @Date 2025/1/15
 * 定时删除夸克转存链接
 */
@Service
@Slf4j
public class LinkService {
    @Autowired
    private Quark quark;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    public void createLinks(List<String> fids) {
        long delay = 30 * 60; // 30分钟

        fids.forEach(fid -> {
            scheduler.schedule(() -> {
                // 删除链接的逻辑
                try {
                    processExpiredLink(fid);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }, delay, TimeUnit.SECONDS);

            log.info("添加定时删除任务: {}" , fid);
        });
    }
    public void createLinks(List<String> fids, String cookie) {
        long delay = 30 * 60; // 30分钟

        fids.forEach(fid -> {
            scheduler.schedule(() -> {
                // 删除链接的逻辑
                try {
                    processExpiredLink(fid, cookie);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }, delay, TimeUnit.SECONDS);

            log.info("添加定时删除任务: {}" , fid);
        });
    }

    private void processExpiredLink(String fid) throws IOException {
        // 具体删除逻辑，例如调用数据库或其他服务
        boolean delete = quark.delete(Collections.singletonList(fid));
        log.info(delete?"任务id{} 删除成功":"任务id{} 删除失败",fid);
    }
    private void processExpiredLink(String fid, String cookie) throws IOException {
        // 具体删除逻辑，例如调用数据库或其他服务
        boolean delete = quark.delete(Collections.singletonList(fid),cookie);
        log.info(delete?"任务id{} 删除成功":"任务id{} 删除失败",fid);
    }
}
