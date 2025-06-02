package com.lin.bot.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lin.bot.mapper.UpdateListMapper;
import com.lin.bot.util.Notify;
import com.lin.bot.model.entity.UpdateListEntity;
import com.lin.bot.util.QuarkUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author Lin.
 * @Date 2025/1/24
 * 定时更新任务
 */
@Service
@Slf4j
public class UpdateListService {
    private final UpdateListMapper updateListMapper;
    private final QuarkUtil quarkUtil;
    private final Notify notify;

    public UpdateListService(UpdateListMapper updateListMapper, QuarkUtil quarkUtil, Notify notify) {
        this.updateListMapper = updateListMapper;
        this.quarkUtil = quarkUtil;
        this.notify = notify;
    }

    @Transactional
    @Scheduled(cron = "0 0 6-23/3 * * *")
    public void process() {
        log.info("开始检查更新任务");
        QueryWrapper<UpdateListEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("ending","0");
        List<UpdateListEntity> entityList = updateListMapper.selectList(wrapper);
        List<String> updNames = Collections.synchronizedList(new ArrayList<>());
        entityList.forEach(entity -> {
            try {
                // TODO 添加更新后群发功能
                boolean b = quarkUtil.updateAndSave(entity,null,entity.getTree());
                if (b) updNames.add(entity.getName());
            } catch (IOException e) {
                log.error("更新任务失败: {}",e.getMessage());
            }
        });
        log.info("定时更新任务完毕，{} 已更新",updNames);
        if (!updNames.isEmpty()) notify.send("更新任务列表:\n" + updNames);
    }
}
