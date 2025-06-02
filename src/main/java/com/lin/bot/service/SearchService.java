package com.lin.bot.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lin.bot.mapper.DuanjuMapper;
import com.lin.bot.mapper.QuarkMapper;
import com.lin.bot.model.VO.CommonVO;
import com.lin.bot.model.entity.DuanjuEntity;
import com.lin.bot.model.entity.QuarkEntity;
import com.lin.bot.util.BeanConverter;
import com.lin.bot.util.OkhttpUtil;
import com.lin.bot.util.Quark;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
/**
 * @Author Lin.
 * @Date 2025/1/5
 * 搜索服务
 */
@Service
@Slf4j
public class SearchService {
    @Autowired
    private RedisTemplate<String ,?> redisTemplate;
    @Autowired
    private QuarkMapper quarkMapper;
    @Autowired
    private OkhttpUtil okhttpUtil;
    @Value("${bot.config.search-api:http://127.0.0.1:8000/}")
    private String searchApi;
    @Autowired
    private Quark quark;
    @Qualifier("threadExecutor")
    @Autowired
    private ThreadPoolTaskExecutor threadExecutor;
    @Autowired
    private DuanjuMapper duanjuMapper;

    public CommonVO<List<Map<String, String>>> searchDuanju(String name){
        return this.baseSearch(name,"duanju");
    }

    public CommonVO<List<Map<String, String>>> search(String name) throws IOException {
        return this.baseSearch(name,"");
    }

    private CommonVO<List<Map<String, String>>> baseSearch(String name,String type) {
        List<Map<String, String>> data = Collections.synchronizedList(new ArrayList<>());
        // 从 Redis 中获取缓存的 list 列表
        List<Map<String, String>> o = (List<Map<String, String>>) redisTemplate.opsForHash().get("search-name-list", name);
        if (o != null && !o.isEmpty()) {
            log.info("命中缓存 search-name-list");
            return CommonVO.OK(o);
        }
        // 从数据库中查询
        List<QuarkEntity> list = quarkMapper.selectList(
                new QueryWrapper<QuarkEntity>()
                        .like("name", name)
                        .eq("valid", true)
        );
        List<QuarkEntity> listDuanJu = new ArrayList<>();
        if ("duanju".equals(type)) {
            List<QuarkEntity> duanjuList = duanjuMapper.selectList(
                    new QueryWrapper<DuanjuEntity>().like("name", name)
            ).stream().map(duanju -> {
                try {
                    return BeanConverter.convert(duanju, QuarkEntity.class);
                } catch (Exception e) {
                    log.error("Bean转换失败", e);
                }
                return new QuarkEntity();
            }).toList();
            listDuanJu.addAll(duanjuList);
        }

        if (!listDuanJu.isEmpty()) {
            // 使用CountDownLatch等待异步任务完成
            CountDownLatch latch = new CountDownLatch(1);

            threadExecutor.execute(() -> {
                try {
                    List<Integer> idsToDelete = new ArrayList<>();
                    Iterator<QuarkEntity> iterator = listDuanJu.iterator();

                    while (iterator.hasNext()) {
                        QuarkEntity entity = iterator.next();
                        try {
                            String checked = quark.checkByUrl(entity.getUrl(), entity.getPassword());
                            if (StrUtil.isBlank(checked)) {
                                // 收集需要删除的ID
                                idsToDelete.add(entity.getId());
                                iterator.remove();
                            }
                        } catch (IOException e) {
                            log.error("检查URL失败，实体ID: {}, 错误: {}", entity.getId(), e.getMessage(), e);
                        }
                    }

                    // 批量删除失效记录
                    if (!idsToDelete.isEmpty()) {
                        try {
                            duanjuMapper.delete(
                                    new QueryWrapper<DuanjuEntity>()
                                            .in("id", idsToDelete)
                            );
                            log.info("成功删除 {} 条失效记录", idsToDelete.size());
                        } catch (Exception e) {
                            log.error("批量删除失效记录失败: {}", e.getMessage(), e);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });

            try {
                // 等待异步任务完成，设置超时时间
                if (!latch.await(30, TimeUnit.SECONDS)) {
                    log.warn("URL检查任务超时");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待URL检查任务被中断", e);
            }
        }
        if (!list.isEmpty()) {
            // 使用CountDownLatch等待异步任务完成
            CountDownLatch latch = new CountDownLatch(1);

            threadExecutor.execute(() -> {
                try {
                    List<QuarkEntity> entitiesToUpdate = new ArrayList<>();
                    Iterator<QuarkEntity> iterator = list.iterator();

                    while (iterator.hasNext()) {
                        QuarkEntity entity = iterator.next();
                        try {
                            String checked = quark.checkByUrl(entity.getUrl(), entity.getPassword());
                            if (StrUtil.isBlank(checked)) {
                                // 收集需要更新的实体
                                entitiesToUpdate.add(entity.setValid(false));
                                iterator.remove();
                            }
                        } catch (IOException e) {
                            log.error("检查URL失败，实体ID: {}, URL: {}, 错误: {}",
                                    entity.getId(), entity.getUrl(), e.getMessage(), e);
                        }
                    }

                    // 批量更新失效记录
                    if (!entitiesToUpdate.isEmpty()) {
                        try {
                            // 使用MyBatis-Plus的批量更新方法
                            for (int i = 0; i < entitiesToUpdate.size(); i += 1000) {
                                int endIndex = Math.min(i + 1000, entitiesToUpdate.size());
                                List<QuarkEntity> batch = entitiesToUpdate.subList(i, endIndex);
                                // 使用MyBatis-Plus的Service层批量更新
                                quarkMapper.updateBatchById(batch);
                            }
                            log.info("成功更新 {} 条记录为无效状态", entitiesToUpdate.size());
                        } catch (Exception e) {
                            log.error("批量更新记录状态失败: {}", e.getMessage(), e);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });

            try {
                // 等待异步任务完成，设置合理的超时时间
                if (!latch.await(30, TimeUnit.SECONDS)) {
                    log.warn("URL检查任务超时");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待URL检查任务被中断", e);
            }
        }


        list.addAll(listDuanJu);
        log.info("正在进行全网搜索: 【{}】",name);
        data = this.searchAll(name).getData();
        HashMap<String, String> map = new HashMap<>();
        sortListByKeyCount(data);
        if (!list.isEmpty()) {
            list.forEach(item ->  map.put(item.getName(),item.getUrl()));
            data.add(map);
            moveMapToFirst(data,map);
        }
        // 将查询结果存入 Redis 缓存
        redisTemplate.opsForHash().put("search-name-list", name, data);
        redisTemplate.expire("search-name-list", 30, TimeUnit.MINUTES);

        return CommonVO.OK(data);
    }

    private CommonVO<List<Map<String, String>>> searchAll(String name) {
        List<Map<String, String>> list = Collections.synchronizedList(new ArrayList<>());
        Set<String> usedUrls = ConcurrentHashMap.newKeySet(); // 使用ConcurrentHashMap的KeySet更高效
        
        int retry = 3; // 减少重试次数
        JSONObject search = null;
        while (retry > 0) {
            try {
                search = okhttpUtil.getJsonObject(searchApi + name, new HashMap<>(), okhttpUtil.getOkHttpClient());
                break; // 成功获取数据后立即跳出
            } catch (IOException e) {
                retry--;
                if (retry == 0) {
                    log.error("搜索API调用失败: {}", e.getMessage());
                    return CommonVO.OK(list);
                }
            }
        }
        
        if (search == null || search.isEmpty()) {
            return CommonVO.OK(list);
        }
        
        JSONArray jsonArray = search.getJSONArray("data");
        if (jsonArray == null || jsonArray.isEmpty()) {
            return CommonVO.OK(list);
        }
        
        // 使用计数器来跟踪完成的任务数
        CountDownLatch latch = new CountDownLatch(jsonArray.size());
        Map<String, String> finalResults = new ConcurrentHashMap<>();
        
        for (Object o : jsonArray) {
            try {
                HashMap<String, String> originalMap = (HashMap<String, String>) o;
                if (MapUtil.isNotEmpty(originalMap)) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            for (Map.Entry<String, String> entry : originalMap.entrySet()) {
                                String url = entry.getValue();
                                // 快速检查URL是否已存在
                                if (!usedUrls.add(url)) {
                                    continue;
                                }
                                
                                try {
                                    String result = quark.checkByUrl(url, null);
                                    if (StrUtil.isNotEmpty(result)) {
                                        finalResults.put(entry.getKey(), url);
                                    }
                                } catch (Exception e) {
                                    log.warn("URL检查失败: {} - {}", url, e.getMessage());
                                }
                            }
                        } finally {
                            latch.countDown();
                        }
                    }, threadExecutor);
                } else {
                    latch.countDown();
                }
            } catch (ClassCastException e) {
                log.error("数据转换失败: {}", e.getMessage());
                latch.countDown();
            }
        }
        
        try {
            // 等待所有任务完成或超时
            if (!latch.await(3, TimeUnit.SECONDS)) {
                log.warn("部分URL检查任务超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("URL检查被中断");
        }
        
        // 如果有结果则添加到分割后的列表
        if (!finalResults.isEmpty()) {
            list.addAll(splitMapBySize(finalResults, 10));
        }
        
        // 缓存结果
        if (!list.isEmpty()) {
            try {
                redisTemplate.opsForHash().put("search-name-list", name, list);
                redisTemplate.expire("search-name-list", 30, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.error("缓存结果失败: {}", e.getMessage());
            }
        }

        return CommonVO.OK(list);
    }

    /**
     * 将大Map分割成多个小Map，每个小Map的大小不超过指定值
     * @param originalMap 原始Map
     * @param maxSize 每个Map的最大大小
     * @return 分割后的Map列表
     */
    private List<Map<String, String>> splitMapBySize(Map<String, String> originalMap, int maxSize) {
        List<Map<String, String>> result = new ArrayList<>();
        if (originalMap.size() <= maxSize) {
            result.add(new HashMap<>(originalMap));
            return result;
        }
        
        Map<String, String> currentMap = new HashMap<>();
        int currentSize = 0;
        
        for (Map.Entry<String, String> entry : originalMap.entrySet()) {
            if (currentSize >= maxSize) {
                result.add(new HashMap<>(currentMap));
                currentMap.clear();
                currentSize = 0;
            }
            currentMap.put(entry.getKey(), entry.getValue());
            currentSize++;
        }
        
        // 添加最后一个不完整的Map
        if (!currentMap.isEmpty()) {
            result.add(new HashMap<>(currentMap));
        }
        
        return result;
    }

    //根据 Map 的 key 数量对 List 进行排序
    private void sortListByKeyCount(List<Map<String, String>> list) {
        list.sort(Comparator.comparingInt(Map::size));
    }

    // 将指定的 Map 放到 List 的第一个位置
    private void moveMapToFirst(List<Map<String, String>> list, Map<String, String> targetMap) {
        if (list.remove(targetMap)) {
            list.add(0, targetMap);
        }
    }

}
