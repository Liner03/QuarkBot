package com.lin.bot.api.base;

import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.util.OkhttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 个人模块
 */
@Service
public class PersonalApi {
    @Autowired
    private OkhttpUtil okhttpUtil;

    /**
     * 获取个人资料
     */
    public JSONObject getProfile(String appId){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        return okhttpUtil.postJSON("/personal/getProfile",param);
    }


    /**
     * 获取自己的二维码
     */
    public JSONObject getQrCode(String appId){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        return okhttpUtil.postJSON("/personal/getQrCode",param);
    }

    /**
     * 获取设备记录
     */
    public JSONObject getSafetyInfo(String appId){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        return okhttpUtil.postJSON("/personal/getSafetyInfo",param);
    }

    /**
     * 隐私设置
     */
    public JSONObject privacySettings(String appId,Integer option,boolean open){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("option",option);
        param.put("open",open);
        return okhttpUtil.postJSON("/personal/privacySettings",param);
    }

    /**
     * 修改个人信息
     */
    public JSONObject updateProfile(String appId,String city,String country,String nickName,String province,String sex,String signature){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("city",city);
        param.put("country",country);
        param.put("nickName",nickName);
        param.put("province",province);
        param.put("sex",sex);
        param.put("signature",signature);
        return okhttpUtil.postJSON("/personal/updateProfile",param);
    }

    /**
     * 修改头像
     */
    public JSONObject updateHeadImg(String appId,String headImgUrl){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("headImgUrl",headImgUrl);
        return okhttpUtil.postJSON("/personal/updateHeadImg",param);
    }

}
