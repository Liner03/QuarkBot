package com.lin.bot.controller;


import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.model.DTO.MsgDTO;
import com.lin.bot.model.VO.CommonVO;
import com.lin.bot.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @Author Lin.
 * @Date 2024/12/29
 * 消息监听
 */
@RestController
@RequestMapping("/msg")
@Slf4j
public class MessageController {
    @Autowired
    private MessageService msgService;
    @Autowired
    private MessageService messageService;

    /**
     * 消息回调
     */
    @PostMapping
    public void receiveMsg(@RequestBody Map<String, Object> msgDTO) throws IOException, InterruptedException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, ExecutionException {
        JSONObject jsonObject = new JSONObject(msgDTO);
        msgService.receive(jsonObject);
    }

    /**
     * 发送消息
     */
    @PostMapping("send")
    public CommonVO<?> sendMsg(
                               @RequestBody MsgDTO msg) {
        JSONObject jsonObject = msgService.send_to_user(msg.getToWxid(), msg.getContent(), msg.getAts());
        Integer status = jsonObject.getInteger("ret");

        return status == 200 ?
                CommonVO.OK(jsonObject.getString("msg")) :
                CommonVO.FAIL(jsonObject.getString("msg"));
    }
}
