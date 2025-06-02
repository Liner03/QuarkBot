package com.lin.bot.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.api.base.LoginApi;
import com.lin.bot.data.TempData;
import com.lin.bot.service.LoginService;
import com.lin.bot.util.constants.LoginConst;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


/**
 * @Author Lin.
 * @Date 2024/12/28
 */
@Controller
@RequestMapping("/login")
@Slf4j
public class LoginController {
    @Autowired
    private LoginService loginService;
    @Autowired
    private LoginApi loginApi;
    @Autowired
    private TempData data;
    @Value("${bot.config.callback-url}")
    private String callbackUrl;

    /**
     * 登录
     */
    @GetMapping
    public void login(HttpServletResponse response) throws IOException {
        String appId = data.getDataByString("appId");
        log.info("appId:{}", appId);
        if (StrUtil.isNotBlank(appId)) {
            JSONObject jsonObject = loginApi.checkOnline(appId);
            if (jsonObject == null) {
                String htmlContent = """
                        <html>
                        <head><title>登录</title></head>
                        <body><p>请重新刷新页面</p></body>
                        </html>
                        """;
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(htmlContent);
                return;
            }
            if (jsonObject.getInteger("ret") == 200 && jsonObject.getBoolean("data")) {
                loginApi.setCallback(data.getDataByString("X-GEWE-TOKEN"),callbackUrl);
                // 已登陆
                String htmlContent = """
                        <html>
                        <head><title>登录</title></head>
                        <body><p>已经登陆</p></body>
                        </html>
                        """;
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(htmlContent);
                return;
//            }else if(loginApi.reconnection(appId)) {
//                log.info("已重新登录");
//                loginApi.setCallback(data.getDataByString("X-GEWE-TOKEN"),callbackUrl);
//                String htmlContent = """
//                        <html>
//                        <head><title>登录</title></head>
//                        <body><p>已经登陆</p></body>
//                        </html>
//                        """;
//                response.setContentType("text/html;charset=UTF-8");
//                response.getWriter().write(htmlContent);
//                return;
            }else {
                log.info("请重新扫码登录");
            }
        }
        loginService.login();
        log.info("开始渲染页面");
        // 渲染 HTML 页面
        String imgPath = "/img/" + data.getDataByString("qrImg");
        String htmlContent = "<html>" +
                "<head><title>二维码登录</title></head>" +
                "<body style='margin:0 auto;'>" +
                "<h1>请扫码登录</h1>" +
                "<img src='" + imgPath + "' alt='二维码加载失败'>" +
                "<p id='status'>等待扫码...</p>" +
                "<script>" +
                "setInterval(function() {" +
                "    fetch('/login/check-status').then(response => response.json()).then(data => {" +
                "        if (data.status === '1') {" +
                "            document.getElementById('status').innerText = '登录成功！';" +
                "        } else if (data.status === '0') {" +
                "            document.getElementById('status').innerText = '已扫码，请点击登录';" +
                "        }" +
                "    });" +
                "}, 2000);" +
                "</script>" +
                "</body>" +
                "</html>";

        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(htmlContent);
    }

    /**
     * 轮询登陆状态
     */
    @GetMapping("/check-status")
    @ResponseBody
    public Map<String, String> checkStatus() {
        Map<String, String> response = new HashMap<>();
        String status = data.getDataByString("loginStatus");
        if (status == null) {
            status = "waiting"; // 默认状态
        }
        response.put("status", status);
        return response;
    }
    /**
     * 注销登录
     */
    @GetMapping("/logout")
    @ResponseBody
    public void logout() {
        if (String.valueOf(LoginConst.LOGIN_IN).equals(data.getDataByString("loginStatus"))) {
            loginService.logout();
        }
    }
}
