package com.lin.bot.model.entity;


import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.annotation.*;
import com.lin.bot.handler.TreeTypeHandler;
import com.lin.bot.model.QuarkNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @Author Lin.
 * @Date 2025/1/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName(value = "update_list", autoResultMap = true)
public class UpdateListEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private int id;

    private String name;

    @TableField(typeHandler = TreeTypeHandler.class)
    private Tree<QuarkNode> tree;

    private String url;
    private String share;

    @TableField("ending")
    private boolean ending;
    @TableField(fill = FieldFill.INSERT)
    private Timestamp updateTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Timestamp createTime;

    public boolean getEnding() {
        return ending;
    }
}

