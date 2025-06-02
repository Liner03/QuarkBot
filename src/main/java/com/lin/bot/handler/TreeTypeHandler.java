package com.lin.bot.handler;

import cn.hutool.core.lang.tree.Tree;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.lin.bot.model.QuarkNode;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@MappedTypes(Tree.class)
public class TreeTypeHandler extends BaseTypeHandler<Tree<QuarkNode>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Tree<QuarkNode> parameter, JdbcType jdbcType) throws SQLException {
        try {
            // 创建一个符合期望格式的Map
            Map<String, Object> treeMap = new HashMap<>();
            treeMap.put("id", parameter.get("id"));
            treeMap.put("name", parameter.get("mame"));
            treeMap.put("parentId", parameter.get("parentId"));
            treeMap.put("shareFidToken",parameter.get("shareFidToken"));
            treeMap.put("extra", parameter.get("extra"));

            List<Tree<QuarkNode>> children = parameter.getChildren();
            if (children != null && !children.isEmpty()) {
                List<Map<String, Object>> childrenMaps = new ArrayList<>();
                for (Tree<QuarkNode> child : children) {
                    Map<String, Object> childMap = new HashMap<>();
                    childMap.put("id", child.get("id"));
                    childMap.put("name", child.get("mame"));
                    childMap.put("parentId", child.get("parentId"));
                    childMap.put("shareFidToken", child.get("shareFidToken"));
                    childMap.put("extra", child.get("extra"));
                    // 递归处理子节点
                    if (child.getChildren() != null) {
                        childMap.put("children", child.getChildren());
                    }
                    childrenMaps.add(childMap);
                }
                treeMap.put("children", childrenMaps);
            }

            String json = JSON.toJSONString(treeMap,
                    JSONWriter.Feature.WriteMapNullValue,
                    JSONWriter.Feature.WriteNulls);
//            log.debug("Serializing tree to JSON: {}", json);
            ps.setString(i, json);
        } catch (Exception e) {
            log.error("Error serializing tree to JSON", e);
            throw new SQLException("Failed to serialize Tree to JSON", e);
        }
    }

    @Override
    public Tree<QuarkNode> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        if (json == null) {
            return null;
        }
        try {
//            log.debug("Deserializing JSON to tree: {}", json);
            JSONObject jsonObj = JSON.parseObject(json);
            return parseTreeFromJson(jsonObj);
        } catch (Exception e) {
            log.error("Error deserializing JSON to tree: {}", json, e);
            throw new SQLException("Failed to deserialize JSON to Tree", e);
        }
    }

    private Tree<QuarkNode> parseTreeFromJson(JSONObject jsonObj) {
        Tree<QuarkNode> tree = new Tree<>();
        tree.put("id",jsonObj.getString("id"));
        tree.put("name",jsonObj.getString("name"));
        tree.put("parentId",jsonObj.getString("parentId"));
        tree.put("shareFidToken",jsonObj.getString("shareFidToken"));

        // 处理extra数据
        if (jsonObj.containsKey("extra")) {
            tree.put("extra", jsonObj.get("extra"));
        }

        // 处理children
        if (jsonObj.containsKey("children")) {
            List<Tree<QuarkNode>> children = new ArrayList<>();
            for (Object childObj : jsonObj.getJSONArray("children")) {
                if (childObj instanceof JSONObject) {
                    children.add(parseTreeFromJson((JSONObject) childObj));
                }
            }
            tree.setChildren(children);
        }

        return tree;
    }

    @Override
    public Tree<QuarkNode> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return getNullableResult(rs, rs.getMetaData().getColumnName(columnIndex));
    }

    @Override
    public Tree<QuarkNode> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        if (json == null) {
            return null;
        }
        try {
            JSONObject jsonObj = JSON.parseObject(json);
            return parseTreeFromJson(jsonObj);
        } catch (Exception e) {
            log.error("Error deserializing JSON to tree", e);
            throw new SQLException("Failed to deserialize JSON to Tree", e);
        }
    }
}
