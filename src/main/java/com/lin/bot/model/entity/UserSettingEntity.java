package com.lin.bot.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * 
 * @TableName user_setting
 */
@Data
@Builder
@TableName(value ="user_setting")
public class UserSettingEntity implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 微信id
     */
    private String wxid;

    /**
     * 微信昵称
     */
    private String nickName;

    /**
     * 通知邮箱
     */
    private String adminEmail;

    /**
     * 管理员微信id
     */
    private String adminWxid;

    /**
     * 管理员认证密码
     */
    private String authPassword;

    /**
     * 是否自动同意添加好友
     */
    private Integer autoAcceptFriends;

    /**
     * 添加好友欢迎语
     */
    private String addWelcomeContent;

    /**
     * 匹配进群关键词,支持正则表达式
     */
    private String matchKeywords;

    /**
     * 自动邀请群聊列表,以英文,隔开
     */
    private String inviteGroupName;

    /**
     * 机器人生效群组,以英文,隔开
     */
    private String activeGroup;

    /**
     * 搜索关键词,支持正则表达式
     */
    private String searchKeywords;

    /**
     * 夸克网盘COOKIE
     */
    private String cookie;

    /**
     * 转存文件夹fid,默认为0(根目录)
     */
    private String savePathFid;

    /**
     * appId
     */
    private String appid;

    /**
     * uuid
     */
    private String uuid;

    /**
     * X-GEWE-TOKEN
     */
    private String token;

    /**
     * 是否开启提醒
     */
    private Integer remindMsg;

    /**
     * 群组列表
     */
    private String chatrooms;

    /**
     * ck有效性
     */
    private boolean cookieValid;
    public boolean getCookieValid() {
        return cookieValid;
    }

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}