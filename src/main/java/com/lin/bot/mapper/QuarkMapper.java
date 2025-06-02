package com.lin.bot.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lin.bot.model.entity.QuarkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author Lin.
 * @Date 2025/1/6
 */
@Mapper
public interface QuarkMapper extends BaseMapper<QuarkEntity> {
    /**
     * 批量更新
     *
     * @param list 要更新的实体列表
     * @return 更新的记录数
     */
    int updateBatchById(@Param("list") List<QuarkEntity> list);
}

