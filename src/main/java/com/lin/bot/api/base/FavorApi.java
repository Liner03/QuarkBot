package com.lin.bot.api.base;

import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.util.OkhttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 收藏夹模块
 */
@Service
public class FavorApi {
    @Autowired
    private OkhttpUtil okhttpUtil;

    /**
     * 同步收藏夹
     */
    public JSONObject sync(String appId, String syncKey) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("syncKey", syncKey);
        return okhttpUtil.postJSON("/favor/sync", param);
    }

    /**
     * 获取收藏夹内容
     */
    public JSONObject getContent(String appId, Integer favId) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("favId", favId);
        return okhttpUtil.postJSON("/favor/getContent", param);
    }

    /**
     * 删除收藏夹
     */
    public JSONObject delete(String appId, Integer favId) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("favId", favId);
        return okhttpUtil.postJSON("/favor/delete", param);
    }

}
