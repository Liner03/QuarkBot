package com.lin.bot.model.DTO;


import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.lin.bot.handler.TreeTypeHandler;
import com.lin.bot.model.QuarkNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @Author Lin.
 * @Date 2025/1/23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class UpdateListDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int id;

    private String name;

    private Tree<QuarkNode> tree;

    private String url;
    private String share;

    private boolean ending;

}
