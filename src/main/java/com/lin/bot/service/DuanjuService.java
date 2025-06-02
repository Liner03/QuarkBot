package com.lin.bot.service;


import cn.hutool.core.stream.StreamUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lin.bot.mapper.DuanjuMapper;
import com.lin.bot.model.entity.DuanjuEntity;
import com.lin.bot.util.Notify;
import com.lin.bot.util.OkhttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Lin.
 * @Date 2025/2/6
 */
@Slf4j
@Service
@Transactional(timeout=120)
public class DuanjuService {
    @Autowired
    private DuanjuMapper duanjuMapper;
    @Autowired
    private OkhttpUtil okhttp;
    @Autowired
    private Notify notify;
    private final static String dailyUrl = "https://dj.3v.hk/api/?list=today";
    private final Map<String, String> urlNameMapping = new ConcurrentHashMap<>();


    public void insert(String originalName, String url) {
        // 插入临时记录（使用UUID生成唯一占位符）
        DuanjuEntity entity = new DuanjuEntity();
        entity.setName("TEMP_" + UUID.randomUUID());
        entity.setUrl(url);
        duanjuMapper.insert(entity);

        // 拼接新名称并更新
        String newName = entity.getId() + "_" + originalName;
        entity.setName(newName);
        duanjuMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void processBatch(List<DuanjuEntity> dataList) {
        // 1. 批量插入临时数据
        duanjuMapper.batchInsertTemp(dataList);

        // 2. 验证主键回填
        dataList.forEach(item -> {
            if (item.getId() == null) {
                throw new DataIntegrityViolationException("主键回填失败");
            }
        });

        // 3. 设置最终名称
        dataList.forEach(item -> {
            String originalName = getOriginalNameByUrl(item.getUrl()); // 新增方法获取原始名称
            item.setName(item.getId() + "_" + originalName);
        });

        // 4. 批量更新
        duanjuMapper.batchUpdateName(dataList);

        duanjuMapper.cleanTempData();
    }

    private String getOriginalNameByUrl(String url) {
        return urlNameMapping.getOrDefault(url, "default_name");
    }

    private boolean processData(JSONArray jsonArray) {
        List<DuanjuEntity> tempEntities = new ArrayList<>();
        boolean hasNewData = false;

        StreamUtil.of(jsonArray.toArray(JSONObject.class)).forEach(item -> {
            String url = item.getString("url");

            // 增强去重检查（双重验证）
            if (duanjuMapper.existsByUrl(url)) return;

            urlNameMapping.put(url, item.getString("name"));

            // 创建临时实体
            DuanjuEntity temp = new DuanjuEntity();
            temp.setName("TEMP_" + UUID.randomUUID().toString().replace("-", ""));
            temp.setUrl(url);
            tempEntities.add(temp);
        });

        // 批量处理新数据
        if (!tempEntities.isEmpty()) {
            try {
                processBatch(tempEntities);
                hasNewData = true;
                log.info("成功新增 {} 条记录", tempEntities.size());

                // 二次验证清理（防止并发场景）
                duanjuMapper.cleanTempData();
            } catch (Exception e) {
                log.error("批量处理失败", e);
            }
        }

        return hasNewData;
    }


    /**
     * 更新今日新增短剧
     */
    @Scheduled(cron = "0 0/30 6-22 * * *")
    @SuppressWarnings("all")
    public void updDuanjuDaily() throws IOException, InterruptedException {
        log.info("开始更新每日短剧");
        int retry = 10;
        while (retry > 0) {
            JSONObject jsonObject = okhttp.getJsonObject(dailyUrl, null, okhttp.getOkHttpClient());
            if (jsonObject == null) {
                retry--;
                // 休眠 1min
                log.debug("当前尝试次数:{},休眠1min后尝试继续访问",10 - retry);
                Thread.sleep(60*1000);
                continue;
            }
            Boolean b = jsonObject.getBoolean("msg");
            // 今日暂未更新
            if (!b) return;
            QueryWrapper<DuanjuEntity> wrapper = new QueryWrapper<>();
            wrapper.apply(true,"TO_DAYS(NOW()) - TO_DAYS(create_time) = 0");
            List<DuanjuEntity> list = duanjuMapper.selectList(wrapper);
            // 判断是否还未更新
            if (list.size() == jsonObject.getIntValue("count", 0)) {
            log.debug("还未更新，list.size : {} ,jsonSize: {}", list.size(), jsonObject.getIntValue("count", 0));
                return;
            }

            JSONArray jsonArray = jsonObject.getJSONArray("data");

            if (jsonArray == null || jsonArray.isEmpty()) return;

            JSONObject[] array = jsonArray.toArray(JSONObject.class);

            // 核心处理逻辑
            duanjuMapper.cleanTempData();
            boolean hasNewData = processData(jsonArray);

            // 成功处理数据后立即终止循环
            if (hasNewData) {
                log.info("daily数据更新完成");
                return;
            }
        }
        if (retry <= 0) log.warn("获取，每日更新数据失败!");
    }

    @Scheduled(cron = "0 0 22 * * ? ")
    public void onScheduleSend() {
        log.info("开始推送每日更新内容");
        QueryWrapper<DuanjuEntity> wrapper = new QueryWrapper<>();
        LocalDate today = LocalDate.now();
        wrapper.apply("DATE(create_time) = {0}", today);
        List<DuanjuEntity> list = duanjuMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        sb.append("🎦今日更新短剧如下 搜+剧名 即可看剧\r\n")
                .append("-------------------\n");
        list.forEach(item -> {
            String name = item.getName().split("_")[1];
            sb.append(name).append("\n");
        });
        notify.sendAllGroup(sb.toString());
    }
}
