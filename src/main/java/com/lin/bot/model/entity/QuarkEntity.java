package com.lin.bot.model.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author Lin.
 * @Date 2025/1/6
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("lin_category")
public class QuarkEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String description;
    private String url;
    // 口令 约束： 英文口令|中文口令
    private String  password;
    private boolean valid;
    private boolean ending;
}
