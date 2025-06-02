package com.lin.bot.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.api.base.ContactApi;
import com.lin.bot.api.base.GroupApi;
import com.lin.bot.api.base.LoginApi;
import com.lin.bot.config.MagicRegexConfig;
import com.lin.bot.data.TempData;
import com.lin.bot.mapper.QuarkMapper;
import com.lin.bot.model.DTO.QuarkDTO;
import com.lin.bot.model.entity.QuarkEntity;
import com.lin.bot.util.Common;
import com.lin.bot.util.OkhttpUtil;
import com.lin.bot.util.Quark;
import com.lin.bot.util.constants.LoginConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @Author Lin.
 * @Date 2024/12/28
 */

@Service
@Slf4j
public class AsyncService {
    @Autowired
    private LoginApi loginApi;
    @Autowired
    private ContactApi contactApi;
    @Autowired
    private TempData data;
    @Autowired
    private GroupApi groupApi;
    @Autowired
    private Environment env;
    @Autowired
    private ThreadPoolTaskExecutor threadExecutor;
    @Autowired
    private OkhttpUtil okhttpUtil;
    @Autowired
    @Lazy
    private Quark quark;
    @Autowired
    private MagicRegexConfig magicRegex;
    @Autowired
    private QuarkMapper quarkMapper;
    @Autowired
    @Lazy
    private ThreadPoolTaskExecutor taskExecutor;
    @Value("${bot.config.callback-url}")
    private String callbackUrl;

    /**
     * 检查登录状态
     */
    @Async("taskExecutor")
    public void checkQrStatus() {
        boolean checking = true;
        while (checking) {
            try {
                String flag = "0"; // 默认状态：未扫码
                JSONObject jsonObject = loginApi.checkQr(
                        data.getDataByString("appId"),
                        data.getDataByString("uuid"),
                        ""
                );
                flag = jsonObject.getJSONObject("data").getString("status");

                if ("1".equals(flag)) { // 已扫码
                    data.setStringDataWithoutExpiration("loginStatus", String.valueOf(LoginConst.SCAN));
                    log.info("二维码已扫码，请点击登录");
                } else if ("2".equals(flag)) { // 已登录
                    data.setStringDataWithoutExpiration("loginStatus", String.valueOf(LoginConst.LOGIN_IN));
                    String loginInfo = jsonObject.getJSONObject("data").getJSONObject("loginInfo").toString();
                    data.setStringDataWithoutExpiration("loginInfo", loginInfo);
                    log.info("{} 登录成功", JSONObject.parse(loginInfo).get("nickName"));
                    Common.removeFile(this.data.getDataByString("qrImg"));
                    loginApi.setCallback(data.getDataByString("X-GEWE-TOKEN"),callbackUrl);
                    threadExecutor.execute(this::getDetails);
                    checking = false; // 停止检测
                }
                Thread.sleep(1500); // 每1.5s检测一次
            } catch (Exception e) {
                log.error("检测二维码状态时发生异常: {}", e.getMessage());
                checking = false;
            }
        }
    }
    /**
     * 获取bot的群组id
     */
    public Future<Boolean> getDetails() {
        Map<Object, Object> obj = data.getDataByMap("chatrooms");
//        if (obj != null && !MapUtil.isEmpty(obj)) {
//            // 直接读取缓存
//            return CompletableFuture.completedFuture(true);
//        }

        log.info("正在获群组列表，耗时时间根据好友数量递增，群聊只会获取保存到通讯录中的群聊");

        String appId = data.getDataByString("appId");

        JSONObject jsonObject = contactApi.fetchContactsList(appId);
        Integer ret = jsonObject.getInteger("ret");
        if (ret != 200) {
            log.warn("获取群组错误 {}", jsonObject);
            return CompletableFuture.completedFuture(false);
        }

        JSONObject dataJson = jsonObject.getJSONObject("data");
        JSONArray chatrooms = dataJson.getJSONArray("chatrooms");
        // bot 的所有已保存群组
        String[] chatroomsArray = chatrooms.toArray(new String[0]);
        Set<String> groupInviteNames = new HashSet<>(Arrays.asList(
                Objects.requireNonNull(env.getProperty("bot.config.active-group", String[].class))
        ));
        groupInviteNames.addAll(Arrays.asList(
                Objects.requireNonNull(env.getProperty("bot.config.invite-group-name", String[].class))
        ));
        Map<String, String> map = new HashMap<>();
        for (String chatroom : chatroomsArray) {
            JSONObject info = groupApi.getChatroomInfo(appId, chatroom);
            if (info == null || info.getInteger("ret") != 200) {
                continue;
            }

            info = info.getJSONObject("data");
            String nickName = info.getString("nickName");
            if (!groupInviteNames.contains(nickName)) {
                // 群组不在列表中
                continue;
            }

            JSONArray memberList = info.getJSONArray("memberList");
            String chatroomId = info.getString("chatroomId");
            if (memberList.size() >= 500) {
                // 微信群人数上限500人 以 ^ 开头
                map.put(nickName, "^" + chatroomId);
            } else {
                map.put(nickName, chatroomId);
            }
        }

        data.setMapWithoutExpiration("chatrooms", map);
        log.info("群组数据获取完成: \n{}",map);
        return CompletableFuture.completedFuture(true);
    }

    /**
     * 消费者，用来消费redis队列中的url
     */
//    @PostConstruct
//    @Async("Consumer-quark-list")
//    public void startConsumer() {
//        while (true) {
//            QuarkDTO quark = (QuarkDTO) redisTemplate.opsForList().rightPop("push_list", 0);
//            if (quark != null) {
//                // 处理消息
//                processMessage(quark);
//            }
//        }
//    }


    /**
     * 异步提交并轮询任务状态
     */
    @Async("taskExecutor")
    public Future<String> submitTaskAsync(String baseUrl, String cookie, String taskId) {
        int maxRetries = 50;

        for (int i = 0; i < maxRetries; i++) {
            try {
                // 随机等待500-1000毫秒
                Thread.sleep(500 + new Random().nextInt(500));

                log.debug("第{}次提交任务", i + 1);

                String submitUrl = String.format(
                        "%s/1/clouddrive/task?pr=ucpro&fr=pc&task_id=%s&retry_index=%d&__dt=21192&__t=%d",
                        baseUrl, taskId, i, System.currentTimeMillis()
                );

                // 构建请求头
                Map<String, String> headers = new HashMap<>();
                headers.put("Cookie", cookie);
                headers.put("User-Agent", Common.randomUA());

                // 发送请求
                JSONObject jsonData = okhttpUtil.getJsonObject(submitUrl, headers, okhttpUtil.getOkHttpClient());

                if ("ok".equals(jsonData.getString("message"))) {
                    JSONObject data = jsonData.getJSONObject("data");
                    if (data.getInteger("status") == 2) {
                        // 任务完成
                        JSONObject saveAs = data.getJSONObject("save_as");
                        String saveAsTopFids = saveAs.getJSONArray("save_as_top_fids").getString(0);
                        // 获取分享内容
                        List<JSONObject> list = this.lsDir(baseUrl,cookie,saveAsTopFids);
                        this.data.setStringData(saveAsTopFids,list.toString(),3);
                        String reAD = magicRegex.getRules().get("AD").getPattern();
                        String replaceAD = magicRegex.getRules().get("AD").getReplace();

                        // 广告匹配
                        for (JSONObject o : list) {
                            String fileName = o.getString("file_name");
                            String fid = o.getString("fid");
                            if (ReUtil.isMatch(reAD,fileName)) {
                                // 匹配含有广告词的文件
                                if (ReUtil.isMatch("(?i)^.+\\.(png|jpeg|jpg|docx|xlsx|txt)$",fileName)) {
                                    ArrayList<String> fidList = new ArrayList<>();
                                    fidList.add(fid);
                                    quark.delete(fidList);
                                    log.warn("匹配到广告文件:{} , 已删除",fileName);
                                    continue;
                                }
                                String newFileName = ReUtil.replaceAll(fileName, reAD, replaceAD);
                                if (!quark.renameFile(fid,newFileName)) {
                                    log.error("文件重命名失败！filename: {}, newFileName: {}", fileName, newFileName);
                                    return CompletableFuture.completedFuture(saveAsTopFids);
                                }
                                log.warn("匹配到广告词语:{} , 已替换为:{}",fileName,newFileName);
                            }
                        }

                        String folderName = saveAs.containsKey("to_pdir_name")
                                ? saveAs.getString("to_pdir_name")
                                : "根目录";

                        if ("分享-转存".equals(data.getString("task_title"))) {
                            log.info("任务完成，ID: {}", taskId);
                            log.info("文件保存位置: {} 文件夹", folderName);
                            return CompletableFuture.completedFuture(saveAsTopFids);
                        }
                    }
                } else {
                    // 处理错误情况
                    int code = jsonData.getInteger("code");
                    String message = jsonData.getString("message");

                    if (code == 32003 && message.contains("capacity limit")) {
                        log.error("转存失败，网盘容量不足！");
                    } else if (code == 41013) {
                        log.error("网盘文件夹不存在");
                    } else {
                        log.error("保存失败: {}", message);
                    }
                    return CompletableFuture.completedFuture(null);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("任务被中断 {}", e.getMessage());
                return CompletableFuture.completedFuture(null);
            } catch (IOException e) {
                log.error("HTTP请求失败 {}", e.getMessage());
                return CompletableFuture.completedFuture(null);
            }
        }
        log.error("任务未在最大重试次数内完成");
        return CompletableFuture.completedFuture(null);
    }

    public List<JSONObject> lsDir(String baseUrl, String cookie, String fid) throws IOException {
        List<JSONObject> list = new ArrayList<>();
        String url =  baseUrl + "/1/clouddrive/file/sort?pr=ucpro&fr=pc&uc_param_str=&_size=50&_fetch_total=1&_fetch_sub_dirs=0_sort=file_type:asc,updated_at:desc&_fetch_full_path=0"
                + "&pdir_fid=" + fid + "&_page=";
        int page = 1;
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", cookie);
        headers.put("User-Agent", Common.randomUA());
        String temp;
        while (true) {
            temp = url + page;
            JSONObject jsonObject = okhttpUtil.getJsonObject(temp, headers, okhttpUtil.getOkHttpClient());
            if (jsonObject.isEmpty() || jsonObject.getJSONObject("data").isEmpty() || jsonObject.getJSONObject("data").getJSONArray("list").isEmpty()) break;
            List<JSONObject> tempList = jsonObject.getJSONObject("data").getJSONArray("list").toJavaList(JSONObject.class);
            list.addAll(tempList);
            page++;
        }
        return list;
    }

    public void insertOrUpdate(QuarkEntity entity) {
        // 使用线程池执行数据库操作，确保异步执行
        taskExecutor.execute(() -> {
            quarkMapper.insertOrUpdate(entity);
        });
    }
}


