package com.lin.bot.util;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * @Author Lin.
 * @Date 2025/1/10
 */
@Slf4j
public class Py {
    public static JSONObject execute(String fileName, String arg) {
        try {
            Resource resource = new ClassPathResource("file/" + fileName + ".py");

            File tempFile = Files.createTempFile("search", ".py").toFile();

            File pythonFile = resource.getFile();

            String pythonScriptPath = pythonFile.getAbsolutePath();

            String pythonCommand = "python";
            ProcessBuilder processBuilder = new ProcessBuilder(pythonCommand, pythonScriptPath,arg);

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            process.waitFor();

            JSONObject resultObject = JSON.parseObject(output.toString());

            tempFile.deleteOnExit();
            return resultObject;
        } catch (Exception e) {
            log.error("获取资源错误 {}",e.getMessage());
        }
        return null;
    }
}
