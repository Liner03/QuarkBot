package com.lin.bot.api.base;

import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.util.OkhttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 标签模块
 */
@Service
public class LabelApi {
    @Autowired
    private OkhttpUtil okhttpUtil;

    /**
     * 添加标签
     */
    public JSONObject add(String appId, String labelName) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("labelName", labelName);
        return okhttpUtil.postJSON("/label/add", param);
    }

    /**
     * 删除标签
     */
    public JSONObject delete(String appId, String labelIds) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("labelIds", labelIds);
        return okhttpUtil.postJSON("/label/delete", param);
    }

    /**
     * 添加标签
     */
    public JSONObject list(String appId) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        return okhttpUtil.postJSON("/label/list", param);
    }

    /**
     * 添加标签
     */
    public JSONObject modifyMemberList(String appId, String labelIds, List<String> wxIds) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("labelIds", labelIds);
        param.put("wxIds", wxIds);
        return okhttpUtil.postJSON("/label/modifyMemberList", param);
    }

}
