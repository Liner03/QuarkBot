package com.lin.bot.api.base;

import com.alibaba.fastjson2.JSONObject;

import com.lin.bot.util.OkhttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 登录模块
 */
@Service
@Slf4j
public class LoginApi {
    @Autowired
    private OkhttpUtil okhttpUtil;
    /**
     * 获取tokenId 将tokenId 配置到OkhttpUtil 类中的token 属性
     *
     * @return
     */
    public JSONObject getToken() {
        return okhttpUtil.postJSON("/tools/getTokenId", new JSONObject());
    }

    /**
     * 设置微信消息的回调地址
     *
     * @return
     */
    public JSONObject setCallback(String token,String callbackUrl) {
        JSONObject param = new JSONObject();
        param.put("token",token);
        param.put("callbackUrl",callbackUrl);
        return okhttpUtil.postJSON("/tools/setCallback", param);
    }

    /**
     * 获取登录二维码
     *
     * @param appId 设备id 首次登录传空，后续登录传返回的appid
     * @param proxy
     * @return
     */
    public JSONObject getQr(String appId, String proxy) {
        log.info("正在获取二维码");
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        return okhttpUtil.postJSON("/login/getLoginQrCode", param);
    }

    /**
     * 确认登陆
     *
     * @param appId
     * @param uuid       取码返回的uuid
     * @param captchCode 登录验证码（跨省登录会出现此提示，使用同省代理ip能避免此问题，也能使账号更加稳定）
     * @return
     */
    public JSONObject checkQr(String appId, String uuid, String captchCode) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("uuid", uuid);
        param.put("captchCode", captchCode);
        return okhttpUtil.postJSON("/login/checkLogin", param);
    }

    /**
     * 退出微信
     *
     * @param appId
     * @return
     */
    public JSONObject logOut(String appId) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        return okhttpUtil.postJSON("/login/logout", param);
    }

    /**
     * 弹框登录
     *
     * @param appId
     * @return
     */
    public JSONObject dialogLogin(String appId) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        return okhttpUtil.postJSON("/login/dialogLogin", param);
    }

    /**
     * 检查是否在线
     *
     * @param appId
     * @return
     */
    public JSONObject checkOnline(String appId) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        return okhttpUtil.postJSON("/login/checkOnline", param);
    }

    /**
     * 退出
     *
     * @param appId
     * @return
     */
    public JSONObject logout(String appId) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        return okhttpUtil.postJSON("/login/logout", param);
    }

    /**
     * 断线重连
     * Lin.
     * @param appId
     * @return
     */
    public boolean reconnection(String appId) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        JSONObject jsonObject = okhttpUtil.postJSON("/login/reconnection", param);
        log.info(jsonObject.toString());
        if (jsonObject != null && jsonObject.getString("msg").contains("断线重连异常")) return false;
        return true;
    }

}
