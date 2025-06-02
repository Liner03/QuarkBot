package com.lin.bot;


import cn.hutool.core.lang.tree.Tree;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.model.QuarkNode;
import com.lin.bot.util.Common;
import com.lin.bot.util.OkhttpUtil;
import com.lin.bot.util.Quark;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author Lin.
 * @Date 2025/1/21
 */
@Component
@Slf4j
public class QuarkTest {
    @Getter
    private final String baseUrl = "https://drive-m.quark.cn";
    @Autowired
    private OkhttpUtil okhttpUtil;
    @Value("${quark.config.cookie}")
    private String cookie;
    @Autowired
    private Quark quark;

    /**
     * 构建查询字符串
     */
    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
    public HashMap<String,String> header() {
        HashMap<String, String> map = new HashMap<>();
        map.put("Cookie", cookie);
        map.put("Content-Type", "application/json");
        map.put("User-Agent", Common.randomUA());
        return map;
    }


    public Tree<QuarkNode> getAllFileList(String pwdId, String stoken, String pdirFid) {
        try {
            Tree<QuarkNode> rootTree = new Tree<>();
            rootTree.put("id", "root");
            rootTree.put("name", "根目录");
            rootTree.put("parentId", "0");

            // 递归获取文件树
            getFileListRecursive(pwdId, stoken, pdirFid, rootTree);

            return rootTree;
        } catch (Exception e) {
            log.error("获取文件列表异常", e);
            return null;
        }
    }
    public Tree<QuarkNode> getAllFileList(String pdirFid) {
        try {
            Tree<QuarkNode> rootTree = new Tree<>();
            rootTree.put("id", "root");
            rootTree.put("name", "根目录");
            rootTree.put("parentId", "0");
            rootTree.put("shareFidToken",null);

            // 递归获取文件树
            getFileListRecursive(pdirFid, rootTree);

            return rootTree;
        } catch (Exception e) {
            log.error("获取文件列表异常", e);
            return null;
        }
    }
    private void getFileListRecursive(String pwdId, String stoken, String pdirFid, Tree<QuarkNode> parentTree) throws IOException {
        int page = 1;
        Map<String, String> queryParams = buildInitialQueryParams(pwdId, stoken);

        while (true) {
            // 构建请求URL
            String url = baseUrl + "/1/clouddrive/share/sharepage/detail";
            queryParams.put("pdir_fid", pdirFid);
            queryParams.put("_page", String.valueOf(page));
            String fullUrl = url + "?" + buildQueryString(queryParams);

            // 发送请求
            JSONObject response = okhttpUtil.getJsonObject(fullUrl, header(), okhttpUtil.getOkHttpClient());
            if (response == null) break;

            JSONObject data = response.getJSONObject("data");
            if (data == null) break;

            // 检查是否是所有者
//            if (data.getInteger("is_owner") == 1) {
//                log.warn("❌网盘中已经存在该文件，无需再次转存");
//                return;
//            }

            JSONArray fileList = data.getJSONArray("list");
            if (fileList == null || fileList.isEmpty()) break;

            int total = response.getJSONObject("metadata").getInteger("_total");

            // 处理单个文件/文件夹的情况
            if (total == 1) {
                JSONObject singleItem = fileList.getJSONObject(0);
                // 在处理节点时的修改
                QuarkNode node = createQuarkNode(singleItem);
                Tree<QuarkNode> currentTree = node.toTree();
                parentTree.setChildren(Collections.singletonList(currentTree));

                if (node.isDir()) {
                    getFileListRecursive(pwdId, stoken, node.getFid(), currentTree);
                }
                return;
            }

            // 处理多个文件/文件夹的情况
            for (int i = 0; i < fileList.size(); i++) {
                JSONObject item = fileList.getJSONObject(i);
                QuarkNode node = createQuarkNode(item);

                Tree<QuarkNode> currentTree = node.toTree();  // 直接使用 QuarkNode 的 toTree 方法

                // 将当前树添加到父节点的子节点列表中
                if (parentTree.getChildren() == null) {
                    parentTree.setChildren(new ArrayList<>());
                }
                parentTree.getChildren().add(currentTree);

                if (node.isDir()) {
                    // 递归处理子文件夹
                    getFileListRecursive(pwdId, stoken, node.getFid(), currentTree);
                }
            }


            // 检查是否需要继续翻页
            if (fileList.size() < total) {
                page++;
            } else {
                break;
            }
        }
    }
    private void getFileListRecursive(String pdirFid, Tree<QuarkNode> parentTree) throws IOException {
        int page = 1;

        while (true) {
            JSONObject response = quark.getDirDetails(pdirFid);

            // 发送请求
            if (response == null) break;

            JSONObject data = response.getJSONObject("data");
            if (data == null) break;

            // 检查是否是所有者
//            if (data.getInteger("is_owner") == 1) {
//                log.warn("❌网盘中已经存在该文件，无需再次转存");
//                return;
//            }

            JSONArray fileList = data.getJSONArray("list");
            if (fileList == null || fileList.isEmpty()) break;

            int total = response.getJSONObject("metadata").getInteger("_total");

            // 处理单个文件/文件夹的情况
            if (total == 1) {
                JSONObject singleItem = fileList.getJSONObject(0);
                // 在处理节点时的修改
                QuarkNode node = createQuarkNode(singleItem);
                Tree<QuarkNode> currentTree = node.toTree();
                parentTree.setChildren(Collections.singletonList(currentTree));

                if (node.isDir()) {
                    getFileListRecursive(node.getFid(), currentTree);
                }
                return;
            }

            // 处理多个文件/文件夹的情况
            for (int i = 0; i < fileList.size(); i++) {
                JSONObject item = fileList.getJSONObject(i);
                QuarkNode node = createQuarkNode(item);

                Tree<QuarkNode> currentTree = node.toTree();  // 直接使用 QuarkNode 的 toTree 方法

                // 将当前树添加到父节点的子节点列表中
                if (parentTree.getChildren() == null) {
                    parentTree.setChildren(new ArrayList<>());
                }
                parentTree.getChildren().add(currentTree);

                if (node.isDir()) {
                    // 递归处理子文件夹
                    getFileListRecursive(node.getFid(), currentTree);
                }
            }


            // 检查是否需要继续翻页
            if (fileList.size() < total) {
                page++;
            } else {
                break;
            }
        }
    }
    private QuarkNode createQuarkNode(JSONObject json) {
        return new QuarkNode()
                .setFid(json.getString("fid"))
                .setFileName(json.getString("file_name"))
                .setPdirFid(json.getString("pdir_fid"))
                .setShareFidToken(json.getString("share_fid_token"))
                .setDir(json.getBoolean("dir"));
    }
    private Map<String, String> buildInitialQueryParams(String pwdId, String stoken) {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("pr", "ucpro");
        queryParams.put("fr", "pc");
        queryParams.put("pwd_id", pwdId);
        queryParams.put("stoken", stoken);
        queryParams.put("force", "0");
        queryParams.put("_size", "50");
        queryParams.put("_fetch_banner", "0");
        queryParams.put("_fetch_total", "1");
        queryParams.put("_sort", "file_type:asc,updated_at:desc");
        queryParams.put("_fetch_share", "0");
        return queryParams;
    }

}
