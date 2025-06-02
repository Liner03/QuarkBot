package com.lin.bot.util;


import cn.hutool.core.lang.tree.Tree;
import com.lin.bot.model.QuarkNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author Lin.
 * @Date 2025/1/22
 */
@Slf4j
@Component
public class TreeDiffUtil {
    private final Quark quark;

    public TreeDiffUtil(Quark quark) {
        this.quark = quark;
    }

    public Tree<QuarkNode> findDiffTree(Tree<QuarkNode> oldTree, Tree<QuarkNode> newTree) {

        // 首先找到实际的根目录节点（"测试"目录）
        Tree<QuarkNode> oldRoot = oldTree.getChildren().isEmpty() ? null : oldTree.getChildren().get(0);
        Tree<QuarkNode> newRoot = newTree.getChildren().isEmpty() ? null : newTree.getChildren().get(0);

        if (oldRoot == null || newRoot == null) {
            return new Tree<>();
        }

        // 创建差异树的根节点（"测试"目录）
        Tree<QuarkNode> diffTree = new Tree<>();
        diffTree.put("id", newRoot.get("id"));
        diffTree.put("name", newRoot.get("name"));
        diffTree.put("parentId", newRoot.get("parentId"));
        diffTree.put("shareFidToken", oldRoot.get("shareFidToken"));
        diffTree.put("children", new ArrayList<>());
        diffTree.put("extra", newRoot.get("extra"));

        // 存储所有旧节点的映射
        Map<String, Tree<QuarkNode>> oldNodeMap = new HashMap<>();
        Map<String, Tree<QuarkNode>> diffNodeMap = new HashMap<>();

        // 收集所有旧节点
        oldRoot.walk(node -> {
            String nodeId = (String) node.get("id");
            if (nodeId != null) {
                oldNodeMap.put(nodeId, node);
            }
        });

        // 将根节点（"测试"目录）加入映射
        diffNodeMap.put((String) diffTree.get("id"), diffTree);

        // 遍历新树找出差异
        newRoot.walk(newNode -> {
            String nodeId = (String) newNode.get("id");
            if (nodeId == null || newNode.equals(newRoot)) {
                return;
            }

            Tree<QuarkNode> oldNode = oldNodeMap.get(nodeId);

            // 如果节点在旧树中不存在，需要添加到差异树中
            if (oldNode == null) {
                String parentId = (String) newNode.get("parentId");

                // 确保父节点存在
                Tree<QuarkNode> parentNode = diffNodeMap.get(parentId);
                if (parentNode == null) {
                    // 需要创建父节点
                    Tree<QuarkNode> originalParent = findNodeById(newRoot, parentId);
                    if (originalParent != null) {
                        parentNode = new Tree<>();
                        parentNode.put("id", originalParent.get("id"));
                        parentNode.put("name", originalParent.get("name"));
                        parentNode.put("parentId", originalParent.get("parentId"));
                        parentNode.put("shareFidToken", originalParent.get("shareFidToken"));
                        parentNode.put("extra", originalParent.get("extra"));

                        // 将父节点添加到其父节点下
                        String grandParentId = (String) originalParent.get("parentId");
                        Tree<QuarkNode> grandParent = diffNodeMap.get(grandParentId);
                        if (grandParent != null) {
                            grandParent.addChildren(parentNode);
                        }
                        diffNodeMap.put(parentId, parentNode);
                    }
                }

                // 创建并添加新节点
                Tree<QuarkNode> diffNode = new Tree<>();
                diffNode.put("id", nodeId);
                diffNode.put("name", newNode.get("name"));
                diffNode.put("parentId", parentId);
                diffNode.put("shareFidToken", newNode.get("shareFidToken"));
                diffNode.put("extra", newNode.get("extra"));

                if (parentNode != null) {
                    parentNode.addChildren(diffNode);
                }
                diffNodeMap.put(nodeId, diffNode);
            }
        });

        // 创建顶层"根目录"节点并添加实际的根目录（"测试"目录）作为子节点
        Tree<QuarkNode> topRoot = new Tree<>();
        topRoot.put("id", "root");
        topRoot.put("name", "根目录");
        topRoot.put("shareFidToken",null);
        topRoot.put("parentId", null);
        if (!diffTree.getChildren().isEmpty()) {
            topRoot.addChildren(diffTree);
        }

        return topRoot;
    }

    public Tree<QuarkNode> findNodeById(Tree<QuarkNode> tree, String id) {
        if (id == null || tree == null || tree.getParent() == null) return null;
        if (id.equals(tree.get("id"))) return tree;

        for (Tree<QuarkNode> child : tree.getChildren()) {
            Tree<QuarkNode> found = findNodeById(child, id);
            if (found != null) return found;
        }

        return null;
    }

    public String getFullPath(Tree<QuarkNode> tree, Tree<QuarkNode> node) {
        List<String> paths = new ArrayList<>();
        Tree<QuarkNode> currentNode = node;

        // 从当前节点开始，迭代获取路径
        while (currentNode != null) {
            String name = (String) currentNode.get("name");
            if (name != null && !"root".equals(currentNode.get("id"))) {
                paths.add(0, name); // 添加到路径的开头
            }
            currentNode = currentNode.getParent(); // 向上遍历到父节点
        }

        // 完整路径
        return "/" + quark.getRootDir() + "/" + String.join("/", paths);
    }


}

