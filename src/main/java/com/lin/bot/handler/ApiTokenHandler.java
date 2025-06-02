package com.lin.bot.handler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.lin.bot.model.VO.CommonVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * @Author Lin.
 * @Date 2025/2/23
 */
@Component
public class ApiTokenHandler implements HandlerInterceptor {

    @Value("${bot.config.auth-password}")
    private String authPassword;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getRequestURL().toString().contains("api")) {
            // 获取请求头中的密钥
            String apiKey = request.getHeader("token");
            if (authPassword.equals(apiKey)) {
                return true;
            }
            CommonVO<String> errorVo = CommonVO.FAIL("token验证失败");
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJsonResponse(response, errorVo);
            return false;
        }
       return true;
    }

    private void writeJsonResponse(HttpServletResponse response, Object object) throws IOException, IOException {
        String json = objectMapper.writeValueAsString(object);
        response.getWriter().write(json);
    }
}
