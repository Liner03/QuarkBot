package com.lin.bot.api.base;

import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.util.OkhttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息模块
 */
@Service
public class MessageApi {
    @Autowired
    private OkhttpUtil okhttpUtil;

    /**
     * 发送文字消息
     */
    public JSONObject postText(String appId, String toWxid, String content, String ats) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("content", content);
        param.put("ats", ats);
        return okhttpUtil.postJSON("/message/postText", param);
    }

    /**
     * 发送文件消息
     */
    public JSONObject postFile(String appId, String toWxid, String fileUrl, String fileName) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("fileUrl", fileUrl);
        param.put("fileName", fileName);
        return okhttpUtil.postJSON("/message/postFile", param);
    }

    /**
     * 发送图片消息
     */
    public JSONObject postImage(String appId, String toWxid, String imgUrl) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("imgUrl", imgUrl);
        return okhttpUtil.postJSON("/message/postImage", param);
    }

    /**
     * 发送语音消息
     */
    public JSONObject postVoice(String appId, String toWxid, String voiceUrl, Integer voiceDuration) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("voiceUrl", voiceUrl);
        param.put("voiceDuration", voiceDuration);
        return okhttpUtil.postJSON("/message/postVoice", param);
    }

    /**
     * 发送视频消息
     */
    public JSONObject postVideo(String appId, String toWxid, String videoUrl, String thumbUrl,Integer videoDuration) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("videoUrl", videoUrl);
        param.put("thumbUrl", thumbUrl);
        param.put("videoDuration", videoDuration);
        return okhttpUtil.postJSON("/message/postVideo", param);
    }

    /**
     * 发送链接消息
     */
    public JSONObject postLink(String appId, String toWxid, String title, String desc, String linkUrl, String thumbUrl) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("title", title);
        param.put("desc", desc);
        param.put("linkUrl", linkUrl);
        param.put("thumbUrl", thumbUrl);
        return okhttpUtil.postJSON("/message/postLink", param);
    }

    /**
     * 发送名片消息
     */
    public JSONObject postNameCard(String appId, String toWxid, String nickName, String nameCardWxid) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("nickName", nickName);
        param.put("nameCardWxid", nameCardWxid);
        return okhttpUtil.postJSON("/message/postNameCard", param);
    }

    /**
     * 发送emoji消息
     */
    public JSONObject postEmoji(String appId, String toWxid, String emojiMd5, String emojiSize) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("emojiMd5", emojiMd5);
        param.put("emojiSize", emojiSize);
        return okhttpUtil.postJSON("/message/postEmoji", param);
    }

    /**
     * 发送appmsg消息
     */
    public JSONObject postAppMsg(String appId, String toWxid, String appmsg) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("appmsg", appmsg);
        return okhttpUtil.postJSON("/message/postAppMsg", param);
    }

    /**
     * 发送小程序消息
     */
    public JSONObject postMiniApp(String appId, String toWxid, String miniAppId, String displayName, String pagePath, String coverImgUrl, String title, String userName) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("miniAppId", miniAppId);
        param.put("displayName", displayName);
        param.put("pagePath", pagePath);
        param.put("coverImgUrl", coverImgUrl);
        param.put("title", title);
        param.put("userName", userName);
        return okhttpUtil.postJSON("/message/postMiniApp", param);
    }

    /**
     * 转发文件
     */
    public JSONObject forwardFile(String appId, String toWxid, String xml) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("xml", xml);
        return okhttpUtil.postJSON("/message/forwardFile", param);
    }

    /**
     * 转发图片
     */
    public JSONObject forwardImage(String appId, String toWxid, String xml) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("xml", xml);
        return okhttpUtil.postJSON("/message/forwardImage", param);
    }

    /**
     * 转发视频
     */
    public JSONObject forwardVideo(String appId, String toWxid, String xml) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("xml", xml);
        return okhttpUtil.postJSON("/message/forwardVideo", param);
    }

    /**
     * 转发链接
     */
    public JSONObject forwardUrl(String appId, String toWxid, String xml) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("xml", xml);
        return okhttpUtil.postJSON("/message/forwardUrl", param);
    }

    /**
     * 转发小程序
     */
    public JSONObject forwardMiniApp(String appId, String toWxid, String xml, String coverImgUrl) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("xml", xml);
        param.put("coverImgUrl", coverImgUrl);
        return okhttpUtil.postJSON("/message/forwardMiniApp", param);
    }

    /**
     * 撤回消息
     */
    public JSONObject revokeMsg(String appId, String toWxid, String msgId, String newMsgId,String createTime) {
        JSONObject param = new JSONObject();
        param.put("appId", appId);
        param.put("toWxid", toWxid);
        param.put("msgId", msgId);
        param.put("newMsgId", newMsgId);
        param.put("createTime", createTime);
        return okhttpUtil.postJSON("/message/revokeMsg", param);
    }

}
