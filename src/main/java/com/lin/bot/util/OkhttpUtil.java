package com.lin.bot.util;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.data.TempData;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OkhttpUtil {
    @Value("${base.api.url:http://localhost:2531/v2/api}")
    private String baseUrl;
    private String token = null;

    @Getter
    private OkHttpClient okHttpClient;  // 声明为成员变量

    @Autowired
    private TempData tempData;


    @PostConstruct  // 在构造后初始化
    public void init() {
        TrustManager[] trustManagers = buildTrustManagers();
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .sslSocketFactory(createSSLSocketFactory(trustManagers), (X509TrustManager) trustManagers[0])
                .hostnameVerifier((hostName, session) -> true)
                .retryOnConnectionFailure(false)
                .build();
    }

    public OkHttpClient okHttpClient() {
        TrustManager[] trustManagers = buildTrustManagers();
        return new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .sslSocketFactory(createSSLSocketFactory(trustManagers), (X509TrustManager) trustManagers[0])
                .hostnameVerifier((hostName, sessino) -> true)
                .retryOnConnectionFailure(false)//是否开启缓存
                .build();
    }

    private TrustManager[] buildTrustManagers() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };
    }

    private SSLSocketFactory createSSLSocketFactory(TrustManager[] trustAllCerts) {
        SSLSocketFactory ssfFactory = null;
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            ssfFactory = sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ssfFactory;
    }

    public JSONObject postJSON(String route,JSONObject param){
        Map<String,Object> header = new HashMap<>();
        token = tempData.getDataByString("X-GEWE-TOKEN");
        if(!StrUtil.isEmptyIfStr(token)){
            header.put("X-GEWE-TOKEN",token);
        }
        try {
            if(baseUrl == null || "".equals(baseUrl)){
                throw new RuntimeException("baseUrl 未配置");
            }
            String res = json(baseUrl + route, header, param.toJSONString(), this.okHttpClient);
            return JSONObject.parse(res);
        } catch (Exception e) {
            log.info("url={}",baseUrl + route);
            log.error(e.getMessage());
        }
        return null;
    }

    private String json(String url, Map<String, Object> header, String json, OkHttpClient client) throws IOException {
        // 创建一个请求 Builder
        Request.Builder builder = new Request.Builder();
        // 创建一个 request
        Request request = builder.url(url).build();

        // 创建一个 Headers.Builder
        Headers.Builder headerBuilder = request.headers().newBuilder();

        // 装载请求头参数
        Iterator<Map.Entry<String, Object>> headerIterator = header.entrySet().iterator();
        headerIterator.forEachRemaining(e -> {
            headerBuilder.add(e.getKey(), (String) e.getValue());
        });
        headerBuilder.add("Content-Type", "application/json");

        // application/octet-stream
        RequestBody requestBody = FormBody.create(MediaType.parse("application/json"), json);

        // 设置自定义的 builder
        builder.headers(headerBuilder.build()).post(requestBody);

        try (Response execute = client.newCall(builder.build()).execute()) {
            return execute.body().string();
        }
    }

    public JSONObject postJsonObject(String url, Map<String, String> header, Map<String, ?> bodyMap, OkHttpClient client) throws IOException {
        // 创建一个 Headers.Builder
        Headers.Builder headerBuilder = new Headers.Builder();

        // 装载请求头参数
        for (Map.Entry<String, String> entry : header.entrySet()) {
            headerBuilder.add(entry.getKey(), entry.getValue());
        }

        // 将请求体 Map 转换为 JSON 字符串
        String jsonBody = JSONObject.toJSONString(bodyMap); // 使用 fastjson，将 Map 转为 JSON 字符串

        // 创建请求体
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);

        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .headers(headerBuilder.build()) // 设置请求头
                .post(requestBody)             // 设置 POST 请求体
                .build();

        // 执行请求
        try (Response response = client.newCall(request).execute()) {
            // 获取响应体
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return null;
            }

            String responseString = responseBody.string();
            return JSONObject.parse(responseString);
        }
    }
    public JSONObject postJsonObject1(String url, Map<String, String> header, Map<String, ?> bodyMap, OkHttpClient client) throws IOException {
        // 创建一个 Headers.Builder
        Headers.Builder headerBuilder = new Headers.Builder();

        // 装载请求头参数
        for (Map.Entry<String, String> entry : header.entrySet()) {
            headerBuilder.add(entry.getKey(), entry.getValue());
        }

        // 将请求体 Map 转换为 JSON 字符串
        String jsonBody = JSONObject.toJSONString(bodyMap); // 使用 fastjson，将 Map 转为 JSON 字符串

        // 创建请求体
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);


        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .headers(headerBuilder.build()) // 设置请求头
                .post(requestBody)             // 设置 POST 请求体
                .build();

        // 执行请求
        try (Response response = client.newCall(request).execute()) {
            // 获取响应体
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return null;
            }

            String responseString = responseBody.string();
            return JSONObject.parse(responseString);
        }
    }

    public JSONObject getJsonObject(String url, Map<String, String> header, OkHttpClient client) throws IOException {
        // 创建一个请求 Builder
        Request.Builder builder = new Request.Builder();
        builder.url(url);

        // 创建一个 Headers.Builder
        Headers.Builder headerBuilder = new Headers.Builder();

        // 装载请求头参数
        if (header != null) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                headerBuilder.add(entry.getKey(), entry.getValue());
            }
        }
        headerBuilder.add("Content-Type", "application/json");

        // 设置自定义的 builder
        builder.headers(headerBuilder.build());

        // 构建请求
        Request request = builder.build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) return null;
            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }
            return JSONObject.parseObject(body.string());
        }
    }

}
