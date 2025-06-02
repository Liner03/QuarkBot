package com.lin.bot.service;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lin.bot.mapper.QuarkMapper;
import com.lin.bot.model.DTO.QuarkDTO;
import com.lin.bot.model.entity.QuarkEntity;
import com.lin.bot.util.BeanConverter;
import com.lin.bot.util.Quark;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @Author Lin.
 * @Date 2025/1/15
 */
@Service
@Slf4j
public class QuarkService {
    @Autowired
    private AsyncService asyncService;
    @Autowired
    @Lazy
    private Quark quark;
    @Autowired
    private QuarkMapper quarkMapper;


    public void syncToMysql(List<Map<String, String>> syncList) {

        if (syncList == null || syncList.isEmpty()) return;
        log.info("正在将结果存储到mysql");
        final int[] count = {0};
        QuarkEntity entity = new QuarkEntity().setId(count[0]);
        syncList.forEach(m -> {
            m.entrySet().iterator().forEachRemaining(e -> {
                count[0] = count[0] +1;
                try {
                    String byUrl = quark.checkByUrl(e.getValue(), null);
                    if (StrUtil.isBlank(byUrl)) return;
                    entity.setName(e.getKey()).setUrl(e.getValue()).setValid(true).setId(count[0]);
                    asyncService.insertOrUpdate(entity);
                } catch (IOException ex) {
                    log.error("存储到mysql时失败,{}",ex.getMessage());
                }
            });
        });
    }

    public boolean addOne(QuarkDTO quarkDTO) throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        if (quarkDTO == null) return false;
        if (StrUtil.isBlankIfStr(quarkDTO.getName()) || StrUtil.isBlankIfStr(quarkDTO.getUrl())) return false;
        QuarkEntity entity = BeanConverter.convert(quarkDTO, QuarkEntity.class);
        QueryWrapper<QuarkEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("url", quarkDTO.getUrl());
        long count = quarkMapper.selectCount(wrapper);
        if (count != 0) return false;
        return quarkMapper.insert(entity) > 0;
    }
}
