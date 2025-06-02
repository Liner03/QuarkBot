package com.lin.bot.service;


import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.util.Common;
import com.lin.bot.util.OkhttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @Author Lin.
 * @Date 2025/1/11
 */
@Service
@Slf4j
public class FileSaveService {
    private final String baseUrl = "https://drive-m.quark.cn";
    @Value("${quark.config.cookie}")
    private String cookie;
    @Autowired
    private OkhttpUtil okHttpUtil;
    @Autowired
    @Lazy
    private AsyncService async;

    /**
     * 创建保存任务并异步执行
     */
    public String saveSharedFiles(String pwdId, String stoken, List<String> fidList,
                                List<String> shareFidTokenList, String toPdirFid) {
        String saveAsTopFids = null;
        try {
            String taskId = getShareSaveTaskId(pwdId, stoken, fidList, shareFidTokenList, toPdirFid);
            Future<String> future = async.submitTaskAsync(baseUrl, cookie, taskId);
            saveAsTopFids = future.get();
        } catch (Exception e) {
            log.error("保存分享文件失败 {}", e.getMessage());
        }
        return saveAsTopFids;
    }
    public String saveSharedFiles(String pwdId, String stoken, List<String> fidList,
                                  List<String> shareFidTokenList, String toPdirFid, String cookie) {
        String saveAsTopFids = null;
        try {
            String taskId = getShareSaveTaskId(pwdId, stoken, fidList, shareFidTokenList, toPdirFid, cookie);
            Future<String> future = async.submitTaskAsync(baseUrl, cookie, taskId);
            saveAsTopFids = future.get();
        } catch (Exception e) {
            log.error("保存分享文件失败 {}", e.getMessage());
        }
        return saveAsTopFids;
    }

    /**
     * 获取保存任务ID
     */
    private String getShareSaveTaskId(String pwdId, String stoken, List<String> fidList,
                                      List<String> shareFidTokenList, String toPdirFid) throws IOException {
        return this.getShareSaveTaskId(pwdId, stoken, fidList, shareFidTokenList, toPdirFid, this.cookie);
    }
    private String getShareSaveTaskId(String pwdId, String stoken, List<String> fidList,
                                      List<String> shareFidTokenList, String toPdirFid, String cookie) throws IOException {
        String taskUrl = baseUrl + "/1/clouddrive/share/sharepage/save";

        // 构建查询参数
        Map<String, String> params = new HashMap<>();
        params.put("pr", "ucpro");
        params.put("fr", "pc");
        params.put("uc_param_str", "");
        params.put("__dt", String.valueOf(600 + new Random().nextInt(9400)));
        params.put("__t", String.valueOf(System.currentTimeMillis()));

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fid_list", fidList);
        requestBody.put("fid_token_list", shareFidTokenList);
        requestBody.put("to_pdir_fid", toPdirFid);
        requestBody.put("pwd_id", pwdId);
        requestBody.put("stoken", stoken);
        requestBody.put("pdir_fid", "0");
        requestBody.put("scene", "link");

        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", cookie);
        headers.put("Content-Type", "application/json");

        headers.put("User-Agent", Common.randomUA());

        // 发送请求
        String fullUrl = taskUrl + "?" + buildQueryString(params);
        JSONObject response = okHttpUtil.postJsonObject(fullUrl, headers, requestBody, okHttpUtil.getOkHttpClient());
        if (response.getInteger("status") != 200) throw new RuntimeException(response.getString(
                "message"
        ));
        String taskId = response.getJSONObject("data").getString("task_id");
        log.info("获取到任务ID: {}", taskId);
        return taskId;
    }



    /**
     * 构建查询字符串
     */
    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

}
