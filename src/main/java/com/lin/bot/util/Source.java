package com.lin.bot.util;


/**
 * @Author Lin.
 * @Date 2025/1/16
 */

import cn.hutool.core.util.ReUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class Source {

    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    // 重命名函数（去除 HTML 标签）
    private String rename(String name) {
        if (name == null) {
            return null;
        }
        // 定义正则表达式模式，匹配 HTML 标签
        String htmlTagPattern = "<[^>]+>";
        // 使用正则表达式替换 HTML 标签为空字符串
        return name.replaceAll(htmlTagPattern, "");
    }

    // 查找标题
    private String findTitle(String content) {
        // 使用 Hutool 的正则工具类
        String regex = "名称.(.*?)\\n";
        return ReUtil.get(regex, content, 1);
    }

    // 查找 URL
    private String findUrl(String content) {
        // 使用 Hutool 的正则工具类
        String regex = "href=\"(.*?)\"";
        return ReUtil.get(regex, content, 1);
    }

    // Source1 请求
    public CompletableFuture<Map<String, String>> source1(String name) {
        String url = "http://kkpan.xyz/backend.php?keyword=" + name;
        Request request = new Request.Builder().url(url).build();

        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                JSONObject jsonResponse = JSON.parseObject(responseBody);
                Map<String, String> resultDict = new HashMap<>();
                Set<String> seenTitles = new HashSet<>();

                jsonResponse.getJSONArray("results").forEach(item -> {
                    JSONObject result = (JSONObject) item;
                    if ("1".equals(result.getString("vaild")) && !seenTitles.contains(result.getString("title"))) {
                        resultDict.put(result.getString("title"), result.getString("url"));
                        seenTitles.add(result.getString("title"));
                    }
                });

                future.complete(resultDict);
            }
        });

        return future;
    }

    // Source2 请求
    public CompletableFuture<Map<String, String>> source2(String name) {
        String url = "https://v.funletu.com/search";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("style", "get");
        requestBody.put("datasrc", "search");
        requestBody.put("query", Collections.singletonMap("searchtext", name));
        requestBody.put("page", Map.of("pageSize", 10, "pageIndex", 1));
        requestBody.put("order", Map.of("prop", "sort", "order", "desc"));
        requestBody.put("message", "请求资源列表数据");

        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, JSONUtil.toJsonStr(requestBody));
        Request request = new Request.Builder().url(url).post(body).build();

        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                JSONObject jsonResponse = JSON.parseObject(responseBody);
                Map<String, String> resultDict = new HashMap<>();
                List<JSONObject> data = jsonResponse.getJSONArray("data").toJavaList(JSONObject.class);

                // 根据 updatetime 降序排序
                data.sort(Comparator.comparing(item -> item.getString("updatetime"), Comparator.reverseOrder()));

                for (JSONObject item : data) {
                    resultDict.put(rename(item.getString("title")), item.getString("url"));
                }

                future.complete(resultDict);
            }
        });

        return future;
    }

    // Source3 请求
    public CompletableFuture<Map<String, String>> source3(String name) {
        String url = "https://www.pansearch.me/_next/data/B08ZwJLhVfBkusIQ5Ys3D/search.json?keyword=" + name + "&pan=quark";
        Request request = new Request.Builder().url(url).build();

        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                JSONObject jsonResponse = JSON.parseObject(responseBody);
                Map<String, String> resultDict = new HashMap<>();

                // 根据 time 字段降序排序
                List<JSONObject> data = jsonResponse.getJSONObject("pageProps")
                        .getJSONObject("data").getJSONArray("data").toJavaList(JSONObject.class);
                data.sort(Comparator.comparing(item -> item.getString("time"), Comparator.reverseOrder()));

                for (JSONObject item : data) {
                    String title = rename(findTitle(item.getString("content")));
                    if (title != null) {
                        resultDict.put(title, findUrl(item.getString("content")));
                    }
                }

                future.complete(resultDict);
            }
        });

        return future;
    }

    // 去重的函数
    public static Map<String, String> deduplicateMap(Map<String, String> map) {
        Set<String> seenKeys = new HashSet<>();
        Map<String, String> newMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!seenKeys.contains(entry.getKey())) {
                newMap.put(entry.getKey(), entry.getValue());
                seenKeys.add(entry.getKey());
            }
        }
        return newMap;
    }

    // 总搜索函数
    public CompletableFuture<List<Map<String, String>>> search(String name) {
        CompletableFuture<Map<String, String>> source1Result = source1(name);
        CompletableFuture<Map<String, String>> source2Result = source2(name);
        CompletableFuture<Map<String, String>> source3Result = source3(name);

        return CompletableFuture.allOf(source1Result, source2Result, source3Result)
                .thenApply(v -> {
                    List<Map<String, String>> results = new ArrayList<>();
                    try {
                        results.add(source1Result.get());
                        results.add(source2Result.get());
                        results.add(source3Result.get());
                    } catch (InterruptedException | ExecutionException e) {
                        log.error(e.getMessage());
                    }
                    return results;
                });
    }
}

