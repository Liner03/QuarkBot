package com.lin.bot.model.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @Author Lin.
 * @Date 2025/2/6
 */
@Data
@TableName("duanju")
@Accessors(chain = true)
public class DuanjuEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String url;
    private Timestamp createTime;
}
