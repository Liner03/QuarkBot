package com.lin.bot.model;


import cn.hutool.core.lang.tree.Tree;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author Lin.
 * @Date 2025/1/21
 * 夸克链接下File节点
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class QuarkNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // 基本属性
    private String fid;
    private String fileName;
    private String pdirFid;
    private String shareFidToken;
    private boolean dir;

    // 文件/文件夹属性
    private long size;
    private String fileType;
    private String formatType;
    private int status;

    // 文件夹特有属性
    private Integer includeItems;  // 文件夹包含的文件数

    // 树形结构属性
    @JSONField(serialize = false)
    private QuarkNode parent;
    @JSONField(serialize = false)
    private List<QuarkNode> children;

    // 辅助方法
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public void addChild(QuarkNode child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
        child.setParent(this);
    }

    /**
     * 从Tree转换为QuarkNode
     */
//    public static QuarkNode fromTree(Tree<QuarkNode> tree) {
//        if(tree == null) return null;
//
//        Map<String, Object> extra = (Map<String, Object>) tree.get("extra");
//        if(extra != null && extra.containsKey("node")) {
//            return (QuarkNode) extra.get("node");
//        }
//        return null;
//    }

    /**
     * 将QuarkNode转换为Tree
     */
    public Tree<QuarkNode> toTree() {
        Tree<QuarkNode> tree = new Tree<>();
        tree.put("id", this.fid);
        tree.put("name", this.fileName);
        tree.put("parentId", this.pdirFid);
        tree.put("shareFidToken", this.shareFidToken);
        tree.put("extra", Map.of("node", this));
        if(this.children != null) {
            List<Tree<QuarkNode>> childTrees = new ArrayList<>();
            for(QuarkNode child : this.children) {
                childTrees.add(child.toTree());
            }
            tree.setChildren(childTrees);
        }
        return tree;
    }

    /**
     * 从Tree转换为QuarkNode
     */
    public static QuarkNode fromTree(Tree<QuarkNode> tree) {
        if (tree == null) return null;

        Object extraObj = tree.get("extra");
        if (extraObj instanceof Map) {
            Map<?, ?> extraMap = (Map<?, ?>) extraObj;
            Object nodeObj = extraMap.get("node");
            if (nodeObj != null) {
                // 使用 FastJSON 进行转换
                QuarkNode node = JSONObject.parseObject(JSONObject.toJSONString(nodeObj), QuarkNode.class);

                // 处理子节点
                List<?> childrenObj = tree.getChildren();
                if (childrenObj != null) {
                    List<QuarkNode> children = new ArrayList<>();
                    for (Object childObj : childrenObj) {
                        if (childObj instanceof Tree) {
                            QuarkNode childNode = fromTree((Tree<QuarkNode>) childObj);
                            if (childNode != null) {
                                children.add(childNode);
                                childNode.setParent(node);
                            }
                        }
                    }
                    node.setChildren(children);
                }

                return node;
            }
        }
        return null;
    }
}
