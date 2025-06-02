package com.lin.bot.util;

import com.lin.bot.data.TempData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @Author Lin.
 * @Date 2024/12/27
 */
@Slf4j
public class Common {

    public static String generateImage(String base64, String path) {
        try {

            log.info("path: {}", path);
            File dir = new File(path);

            // 如果目录不存在，则创建
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成唯一文件名
            String name = UUID.randomUUID().toString().concat(".png");
            String fullPath = path.concat(File.separator).concat(name);
            log.info("二维码位置:{}",fullPath);

            // 去掉 base64 前缀 data:image/png;base64,
            base64 = base64.substring(base64.indexOf(",", 1) + 1);
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] imgbytes = decoder.decode(base64);

            // 处理可能出现的负数字节
            for (int i = 0; i < imgbytes.length; ++i) {
                if (imgbytes[i] < 0) {
                    imgbytes[i] += 256;
                }
            }

            // 保存图片到文件
            OutputStream out = Files.newOutputStream(Paths.get(fullPath));
            out.write(imgbytes);
            out.flush();
            out.close();

            return name; // 返回图片名字
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public static void removeFile(String name) {
        // 使用 File.separator 保证跨平台兼容性
        // 拼接相对路径，确保跨平台兼容
        String fullPath = "files" + File.separator + "images" + File.separator + name;

        File file = new File(fullPath);
        if (file.exists()) {
            if (file.delete()) {
                log.info("文件删除成功: {}", fullPath);
            } else {
                log.error("文件删除失败: {}", fullPath);
            }
        } else {
            log.warn("文件不存在: {}", fullPath);
        }
    }

    public static String toStringForMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        int count = 1;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append("【").append(count++).append("】").append("|").append("「").append(entry.getKey()).append( "」\n");
        }
        return sb.toString().trim(); // 去掉最后一个多余的换行符
    }

    public static String randomUA() {
        List<String> list = new ArrayList<>();
        String UA = "'Mozilla/5.0 (compatible; U; ABrowse 0.6; Syllable) AppleWebKit/420+ (KHTML, like Gecko)', 'Mozilla/5.0 (compatible; U; ABrowse 0.6;  Syllable) AppleWebKit/420+ (KHTML, like Gecko)', 'Mozilla/5.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0; Acoo Browser 1.98.744; .NET CLR 3.5.30729)', 'Mozilla/5.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0; Acoo Browser 1.98.744; .NET CLR   3.5.30729)', 'Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0;   Acoo Browser; GTB5; Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;   SV1) ; InfoPath.1; .NET CLR 3.5.30729; .NET CLR 3.0.30618)', 'Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; SV1; Acoo Browser; .NET CLR 2.0.50727; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729; Avant Browser)', 'Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; Acoo Browser; SLCC1;   .NET CLR 2.0.50727; Media Center PC 5.0; .NET CLR 3.0.04506)', 'Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; Acoo Browser; GTB5; Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1) ; Maxthon; InfoPath.1; .NET CLR 3.5.30729; .NET CLR 3.0.30618)', 'Mozilla/4.0 (compatible; Mozilla/5.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0; Acoo Browser 1.98.744; .NET CLR 3.5.30729); Windows NT 5.1; Trident/4.0)', 'Mozilla/4.0 (compatible; Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; GTB6; Acoo Browser; .NET CLR 1.1.4322; .NET CLR 2.0.50727); Windows NT 5.1; Trident/4.0; Maxthon; .NET CLR 2.0.50727; .NET CLR 1.1.4322; InfoPath.2)', 'Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0; Acoo Browser; GTB6; Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1) ; InfoPath.1; .NET CLR 3.5.30729; .NET CLR 3.0.30618)'".replaceAll("'","");
        Collections.addAll(list,UA);
        Random rand = new Random();
        return list.get(rand.nextInt(list.size()));
    }

}

