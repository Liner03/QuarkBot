package com.lin.bot.api.base;

import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.util.OkhttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 联系人模块
 */
@Service
public class ContactApi {
    @Autowired
    private OkhttpUtil okhttpUtil;
    /**
     *
     * @param appId
     * @return
     */
    public JSONObject a(String appId){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        return okhttpUtil.postJSON("/login/checkOnline",param);
    }

    /**
     * 获取通讯录列表
     * @param appId
     * @return
     */
    public JSONObject fetchContactsList(String appId){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        return okhttpUtil.postJSON("/contacts/fetchContactsList",param);
    }

    /**
     * 获取群/好友简要信息
     * @param appId
     * @return
     */
    public JSONObject getBriefInfo(String appId, List<String> wxids){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("wxids",wxids);
        return okhttpUtil.postJSON("/contacts/getBriefInfo",param);
    }

    /**
     * 获取群/好友详细信息
     * @param appId
     * @return
     */
    public JSONObject getDetailInfo(String appId, List<String> wxids){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("wxids",wxids);
        return okhttpUtil.postJSON("/contacts/getDetailInfo",param);
    }

    /**
     * 搜索好友
     * @param appId
     * @return
     */
    public JSONObject search(String appId,String contactsInfo){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("contactsInfo",contactsInfo);
        return okhttpUtil.postJSON("/contacts/search",param);
    }

    /**
     * 添加联系人/同意添加好友
     * @param appId
     * @return
     */
    public JSONObject search(String appId,Integer scene,Integer option,String v3, String v4,String content){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("scene",scene);
        param.put("option",option);
        param.put("v3",v3);
        param.put("v4",v4);
        param.put("content",content);
        return okhttpUtil.postJSON("/contacts/addContacts",param);
    }

    /**
     * 删除好友
     * @param appId
     * @return
     */
    public JSONObject deleteFriend(String appId,String wxid){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("wxid",wxid);
        return okhttpUtil.postJSON("/contacts/deleteFriend",param);
    }

    /**
     * 设置好友仅聊天
     * @param appId
     * @return
     */
    public JSONObject setFriendPermissions(String appId,String wxid,Boolean onlyChat){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("wxid",wxid);
        param.put("onlyChat",onlyChat);
        return okhttpUtil.postJSON("/contacts/setFriendPermissions",param);
    }

    /**
     * 设置好友备注
     * @param appId
     * @return
     */
    public JSONObject setFriendRemark(String appId,String wxid,String remark){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("wxid",wxid);
        param.put("onlyChat",remark);
        return okhttpUtil.postJSON("/contacts/setFriendRemark",param);
    }

    /**
     * 获取手机通讯录
     * @param appId
     * @return
     */
    public JSONObject getPhoneAddressList(String appId,List<String> phones){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("wxid",phones);
        return okhttpUtil.postJSON("/contacts/getPhoneAddressList",param);
    }

    /**
     * 上传手机通讯录
     * @param appId
     * @return
     */
    public JSONObject uploadPhoneAddressList(String appId,List<String> phones,Integer opType){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("wxid",phones);
        param.put("opType",opType);
        return okhttpUtil.postJSON("/contacts/uploadPhoneAddressList",param);
    }

}
