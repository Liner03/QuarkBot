package com.lin.bot.api.base;

import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.util.OkhttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 下载模块
 */
@Service
public class DownloadApi {
    @Autowired
    private OkhttpUtil okhttpUtil;
    /**
     * 下载图片
     */
    public JSONObject downloadImage(String appId, String xml, Integer type){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("xml",xml);
        param.put("type",type);
        return okhttpUtil.postJSON("/message/downloadImage",param);
    }

    /**
     * 下载语音
     */
    public JSONObject downloadVoice(String appId, String xml, Long msgId){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("xml",xml);
        param.put("msgId",msgId);
        return okhttpUtil.postJSON("/message/downloadVoice",param);
    }

    /**
     * 下载视频
     */
    public JSONObject downloadVideo(String appId, String xml){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("xml",xml);
        return okhttpUtil.postJSON("/message/downloadVideo",param);
    }

    /**
     * 下载emoji
     */
    public JSONObject downloadEmojiMd5(String appId, String emojiMd5){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("emojiMd5",emojiMd5);
        return okhttpUtil.postJSON("/message/downloadEmojiMd5",param);
    }
    /**
     * cdn下载
     */
    public JSONObject downloadImage(String appId, String aesKey, String fileId, String type, String totalSize, String suffix){
        JSONObject param = new JSONObject();
        param.put("appId",appId);
        param.put("aesKey",aesKey);
        param.put("fileId",fileId);
        param.put("totalSize",totalSize);
        param.put("type",type);
        param.put("suffix",suffix);
        return okhttpUtil.postJSON("/message/downloadCdn",param);
    }


}
