package com.lin.bot.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lin.bot.model.entity.DuanjuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author Lin.
 * @Date 2025/2/6
 */
@Mapper
public interface DuanjuMapper extends BaseMapper<DuanjuEntity> {
    // 批量插入（返回生成的主键）
    void batchInsertTemp(@Param("list") List<DuanjuEntity> list);

    // 批量更新名称
    void batchUpdateName(@Param("list") List<DuanjuEntity> list);

    boolean existsByUrl(String url);

    void cleanTempData();
}
