package com.lin.bot.model.DTO;


import lombok.Data;

/**
 * @Author Lin.
 * @Date 2024/12/29
 * 前端信息传输
 */
@Data
public class MsgDTO {
    // 通用变量
    private String thumbUrl; // 视频/链接 缩略图链接
    private String title; // 链接/小程序 标题
    // 文本消息
    private String toWxid;
    private String ats;
    private String content;
    // 文件消息
    private String fileName;
    private String fileUrl;
    // 图片消息
    private String imgUrl;
    // 语音消息
    private String voiceUrl;
    // 视频消息
    private String videoUrl;
    private String videoDuration; // 视频的播放时长，单位秒
    // 链接消息
    private String desc;
    private String linkUrl;
    // 名片消息
    private String nickName; // 好友昵称
    private String nameCardWxid; //好友微信id
    // emoji消息
    private String emojiMd5; // emoji图片的md5
    private String emojiSize; // emoji的文件大小
    // appmsg消息
    private String appmsg; // 回调消息中的appmsg节点内容 例如：音乐分享、视频号、引用消息等等
    // 小程序消息
    private String miniAppId; // 小程序ID
    private String displayName; // 小程序名称
    private String pagePath; // 小程序打开的地址
    private String coverImgUrl; // 小程序封面图链接
    private String userName; // 归属的用户ID
    // ...

}
