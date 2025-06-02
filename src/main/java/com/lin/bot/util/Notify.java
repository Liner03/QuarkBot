package com.lin.bot.util;


import cn.hutool.core.util.StrUtil;
import com.lin.bot.data.TempData;
import com.lin.bot.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * @Author Lin.
 * @Date 2025/1/23
 * 通知类
 */
@Component
@Slf4j
public class Notify {
    private final TempData data;
    private final MessageService messageService;
    @Value("${bot.config.admin-email}")
    private String toEmail;
    @Value("${spring.mail.username}")
    private String fromEmail;
    private final JavaMailSender mailSender;
    @Autowired
    private Environment env;
    @Autowired
    private TempData tempData;

    public Notify(TempData tempData, MessageService messageService,JavaMailSender mailSender) {
        this.data = tempData;
        this.messageService = messageService;
        this.mailSender = mailSender;
    }

    @Async
    public void send(String message) {
        String remindMsg = this.data.getDataByString("remindMsg");
        // 未开启提醒
        if (StrUtil.isBlank(remindMsg) || !"true".equals(remindMsg)) return;
        if (StrUtil.isBlankIfStr(toEmail) || StrUtil.isBlankIfStr(message)) return;

        String adminWxid = data.getDataByString("adminWxid");
        // 机器人在线
        data.getDataByString("loginStatus");
        this.send(adminWxid,toEmail,message);
    }

    private void send(String wxid,String toEmail,String message) {
        if (StrUtil.isBlankIfStr(wxid)) {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);
            mail.setTo(toEmail);
            mail.setSubject("资源短剧机器人通知");
            mail.setText(message);
            mail.setSentDate(new Date());
            mailSender.send(mail);
        } else {
          messageService.send_to_user(wxid,message);
        }
    }

    @Async
    public void sendAllGroup(String message) {
        // 群发列表白名单
        ArrayList<String> exceptGroups = new ArrayList<>();
        exceptGroups.add("test");
        List<String> list = Arrays.stream(env.getProperty("bot.config.active-group", String[].class)).toList();
        Map<Object, Object> chatrooms = tempData.getDataByMap("chatrooms");
        list.parallelStream().filter(item -> !exceptGroups.contains(item)).forEach(item -> {
            Object o = chatrooms.get(item);
            if (o == null) return;
            messageService.send_to_user((String) o, message);
        });
    }
}
