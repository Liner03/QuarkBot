package com.lin.bot.service;


import cn.hutool.core.util.StrUtil;
import com.lin.bot.api.base.LoginApi;
import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.data.TempData;
import com.lin.bot.util.Common;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;


/**
 * @Author Lin.
 * @Date 2024/12/27
 */
@Service
@Slf4j
public class LoginService {
    @Autowired
    private LoginApi loginApi;
    @Autowired
    private TempData data;
    @Autowired
    private AsyncService asyncService;

    public void login() {
        String lastAppId = data.getDataByString("appId");

        // 生成二维码并保存路径
        String token = loginApi.getToken().getString("data");
//        String token = data.getDataByString("X-GEWE-TOKEN");
//        if (StrUtil.isBlankIfStr(token)) {
//            token = loginApi.getToken().getString("data");
//            this.data.setStringData("X-GEWE-TOKEN", token);
//        }
        log.info("token:{}", token);
        this.data.setStringDataWithoutExpiration("X-GEWE-TOKEN", token);

        JSONObject qr = null;
        String uuid = null;
        if(StrUtil.isNotBlank(lastAppId)) {
            qr = loginApi.getQr(lastAppId, "");
            if (qr == null) {
                log.error("获取二维码错误!");
                return;
            }
            if(qr.getString("msg").contains("设备不存在")) {
                log.warn("设备不存在");
                return;
            }
            data.setStringDataWithoutExpiration("appId", lastAppId);
            if (qr.getInteger("ret") == 500) {
                log.error(qr.getString("msg"));
                if (qr.getString("msg").contains("重复登录")) {
                    loginApi.logOut(lastAppId);
                    log.info("已注销登录");
                }
                return;
            }
            uuid = qr.getJSONObject("data").get("uuid").toString();
        }else {
            qr = loginApi.getQr("", "");
            uuid = qr.getJSONObject("data").get("uuid").toString();
            String appId = qr.getJSONObject("data").get("appId").toString();
            data.setStringDataWithoutExpiration("appId", appId);
        }
        data.setStringDataWithoutExpiration("uuid", uuid);

        String tempLocation = "files" + File.separator + "images";  // "files/images" 目录
        String qrImg = Common.generateImage(
                qr.getJSONObject("data").getString("qrImgBase64"),
                tempLocation
        );
        data.setStringData("qrImg", qrImg,3);

        // 开始异步检测二维码状态
        asyncService.checkQrStatus();
    }
    public void logout() {
        loginApi.logOut(data.getDataByString("appId"));
    }
}

