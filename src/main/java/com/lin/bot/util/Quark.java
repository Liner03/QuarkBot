package com.lin.bot.util;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lin.bot.data.TempData;
import com.lin.bot.mapper.UserSettingMapper;
import com.lin.bot.model.QuarkNode;
import com.lin.bot.model.entity.UserSettingEntity;
import com.lin.bot.service.FileSaveService;
import com.lin.bot.service.LinkService;
import com.lin.bot.service.MessageService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author Lin.
 * @Date 2024/12/5
 * 夸克网盘的一些方法
 */
@Slf4j
@Component
public class Quark {

    @Value("${quark.config.cookie}")
    private String cookie;
    @Autowired
    private OkhttpUtil okhttpUtil;
    @Autowired
    @Lazy
    private FileSaveService fileSaveService;
    @Autowired
    @Lazy
    private LinkService linkService;
    @Autowired
    private TempData data;
    @Getter
    private final String baseUrl = "https://drive-m.quark.cn";
    @Autowired
    @Lazy
    private MessageService messageService;
    @Value("${quark.config.save-path-fid}")
    private String savePath;
    @Autowired
    private UserSettingMapper userSettingMapper;
    @Getter
    private final String rootDir = "bot测试";
    private Notify notify;
    @Autowired
    private Environment environment;

    @PostConstruct
    private void init() {
        renameFile(savePath,rootDir);
    }

    public HashMap<String,String> header() {
        HashMap<String, String> map = new HashMap<>();
        map.put("Cookie", cookie);
        map.put("Content-Type", "application/json");
        map.put("User-Agent", Common.randomUA());
        return map;
    }

    /**
     *
     * @return boolean
     * @author Lin.
     * 验证cookie的有效性
     */
    public boolean isValidCookie(String cookie) throws IOException {
        String url = "https://pan.quark.cn/account/info?fr=pc&platform=pc";
        HashMap<String, String> header = header();
        header.put("Cookie", cookie);
        JSONObject json = okhttpUtil.getJsonObject(url, header, okhttpUtil.getOkHttpClient());
        return !StrUtil.hasEmpty(json.getString("data"));
    }
    public boolean isValidCookie() throws IOException {
        String url = "https://pan.quark.cn/account/info?fr=pc&platform=pc";
        JSONObject json = okhttpUtil.getJsonObject(url, header(), okhttpUtil.getOkHttpClient());
        return !StrUtil.hasEmpty(json.getString("data"));
    }

    /**
     * @return String stoken
     * @author Lin.
     * 验证资源是否失效
     */
    public String checkByUrl(String panUrl,String passcode, String cookie) throws IOException {
        String pwdId = getPwdId(panUrl);
        if (pwdId == null) {
            log.warn("错误的url传入{}",panUrl);
            return null;
        }
        // 判断 cookie 有效性
        boolean validCookie = false;
        if(StrUtil.isBlankIfStr(cookie)){
            validCookie = isValidCookie();
        } else {
            validCookie = isValidCookie(cookie);
        }
        if (!validCookie) {
            UserSettingEntity entity = userSettingMapper.selectById(1);
            if (entity.getCookieValid()) {
                entity.setCookieValid(false);
                userSettingMapper.updateById(entity);
                String remindMsg = this.data.getDataByString("remindMsg");
                if (StrUtil.isNotBlank(remindMsg) && Boolean.parseBoolean(remindMsg)) messageService.send_to_user(this.data.getDataByString("adminWxid"),"COOKIE已失效!");
                log.error("COOKIE失效!");
                notify.send("COOKIE失效!");
            }
            return null;
        }
        passcode = passcode == null ? "" : passcode;
        String url =  baseUrl + "/1/clouddrive/share/sharepage/token?pr=ucpro&fr=pc";
        HashMap<String, String> body = new HashMap<>();
        body.put("pwd_id", pwdId);
        body.put("passcode", passcode);
        JSONObject jsonObject = okhttpUtil.postJsonObject(url, header(), body, okhttpUtil.getOkHttpClient());
        if (jsonObject != null && !jsonObject.isEmpty()) {
            if (jsonObject.getInteger("status") != 200) {
                log.warn("pwd_id: {}, 错误信息: {}", pwdId, jsonObject.getString("message") );
                return null;
            }
            return jsonObject.getJSONObject("data").getString("stoken");
        }

        return null;
    }
    public String checkByUrl(String panUrl,String passcode) throws IOException {
        return this.checkByUrl(panUrl, passcode, null);
    }
    /**
     * 获取当前分享下大的 fid 下的所有json数据 pdirFid 初始为0
     */
    public List<JSONObject> getFileList(String pwdId, String stoken, String pdirFid) {
        return this.getFileList(pwdId, stoken, header().get("Cookie"), pdirFid);
    }
    public List<JSONObject> getFileList(String pwdId, String stoken, String cookie, String pdirFid) {
        List<JSONObject> listMerge = new ArrayList<>();
        int page = 1;
        int fetchShare = 0;
        try {
            while (true) {
                // 构建请求参数
                String url = baseUrl + "/1/clouddrive/share/sharepage/detail";

                // 构建查询参数
                Map<String, String> queryParams = new HashMap<>();
                queryParams.put("pr", "ucpro");
                queryParams.put("fr", "pc");
                queryParams.put("pwd_id", pwdId);
                queryParams.put("stoken", stoken);
                queryParams.put("pdir_fid", pdirFid);
                queryParams.put("force", "0");
                queryParams.put("_page", String.valueOf(page));
                queryParams.put("_size", "50");
                queryParams.put("_fetch_banner", "0");
                queryParams.put("_fetch_share", String.valueOf(fetchShare));
                queryParams.put("_fetch_total", "1");
                queryParams.put("_sort", "file_type:asc,updated_at:desc");

                // 将查询参数拼接到 URL
                String fullUrl = url + "?" + buildQueryString(queryParams);

                // 发送请求
                HashMap<String, String> header = header();
                header.put("cookie", cookie);
                JSONObject response = okhttpUtil.getJsonObject(fullUrl, header, okhttpUtil.getOkHttpClient());

                if (response == null) {
                    break;
                }

                // 获取当前页的数据列表
                JSONObject data = response.getJSONObject("data");
                if (data != null && data.getInteger("is_owner") == 1) {
                    log.error("❌网盘中已经存在该文件，无需再次转存");
                    return new ArrayList<>();
                }
                JSONArray currentList = response.getJSONObject("data").getJSONArray("list");
                if (currentList == null || currentList.isEmpty()) {
                    break;
                }

                if (data != null && response.getJSONObject("metadata").getInteger("_total") == 1) {
                    List<JSONObject> list = currentList.toList(JSONObject.class);
                    if (list.get(0).getBoolean("dir")) {
                        listMerge.add(list.get(0));
                        return listMerge;
                    }else {
                        listMerge.addAll(list);
                        return listMerge;
                    }
                }

                // 添加到合并列表中
                List<JSONObject> list = currentList.toList(JSONObject.class);
                for (JSONObject json:list) {
                    if (json.getBoolean("dir")) {
                        listMerge.add(json);
                    }else {
                        listMerge.add(json);
                    }
                }
                // 检查是否获取完所有数据
                int total = response.getJSONObject("metadata").getInteger("_total");
                if (listMerge.size() >= total) {
                    break;
                }

                page++;

            }
            return listMerge;

        } catch (Exception e) {
            log.error("获取文件列表异常", e);
            return null;
        }
    }

    /**
     * 获取短剧的基础信息 缩略图 集数
     */
    public HashMap<String ,Object> getUrlInfo(String pwdId, String stoken, String cookie, String pdirFid) {
        int retry = 5;
        int fetchShare = 0;
        boolean wrapDir = false;
        HashMap<String, Object> map = new HashMap<>();
        try {
            while (retry > 0) {
                // 构建请求参数
                String url = baseUrl + "/1/clouddrive/share/sharepage/detail";

                // 构建查询参数
                Map<String, String> queryParams = new HashMap<>();
                queryParams.put("pr", "ucpro");
                queryParams.put("fr", "pc");
                queryParams.put("pwd_id", pwdId);
                queryParams.put("stoken", stoken);
                queryParams.put("pdir_fid", pdirFid);
                queryParams.put("force", "0");
                queryParams.put("_page", "1");
                queryParams.put("_size", "200");
                queryParams.put("_fetch_banner", "0");
                queryParams.put("_fetch_share", String.valueOf(fetchShare));
                queryParams.put("_fetch_total", "1");
                queryParams.put("_sort", "file_type:asc,updated_at:desc");

                // 将查询参数拼接到 URL
                String fullUrl = url + "?" + buildQueryString(queryParams);

                // 发送请求
                HashMap<String, String> header = header();
                header.put("cookie", cookie);
                JSONObject response = okhttpUtil.getJsonObject(fullUrl, header, okhttpUtil.getOkHttpClient());

                if (response == null) {
                    retry--;
                    continue;
                }

                // 获取当前页的数据列表
                JSONObject data = response.getJSONObject("data");
                JSONArray list = data.getJSONArray("list");
                JSONObject[] listArray = list.toArray(JSONObject.class);

                if (!wrapDir) {
                    for(JSONObject json: listArray) {
                        if (json.getBoolean("dir")) {
                            pdirFid = json.getString("fid");
                            wrapDir = true;
                            break;
                        }
                    }
                }

                JSONObject metadata = response.getJSONObject("metadata");

                JSONArray currentList = data.getJSONArray("list");
                if (currentList == null || currentList.isEmpty()) {
                    retry--;
                    continue;
                }

                JSONObject[] array = currentList.toArray(JSONObject.class);
                String total = metadata.getString("_total");
                String thumb = null;
                for (JSONObject json:array) {
                    if ("0.jpg".equals(json.getString("file_name")) || "0.png".equals(json.getString("file_name"))) {
                        thumb = json.getString("preview_url");
                        break;
                    }
                }

                map.put("thumb", thumb);
                map.put("total", total);

                retry--;

            }
            return map;
        } catch (Exception e) {
            log.error("获取文件列表异常", e);
            return map;
        }
    }

    /**
     * @param  url 分享url
     * @param savePathFid 转存文件夹id
     * @param expireDay 过期天数 默认 天
     * @param b 是否自动删除
     * @return String 分享链接
     */
    public String saveAndShareAndDel(String url, String savePathFid,String title,String expireDay,boolean b) throws IOException, InterruptedException {
        log.info("开始转存...");
        String stoken = this.checkByUrl(url, null);
        if (StrUtil.isBlankIfStr(stoken)) {
            log.warn("❌链接已失效！");
            return null;
        }
        String pwdId = getPwdId(url);
        List<JSONObject> list = this.getFileList(pwdId, stoken, "0");
        if (list == null) return null;
        // 如果是自己的 url 则直接返回
        if (list.isEmpty()) return url;
        int dirCount = 0;
        int fileCount = 0;
        ArrayList<String> fidList = new ArrayList<>();
        ArrayList<String> shareFidTokenList = new ArrayList<>();
        for (JSONObject jsonObject : list) {
            if (jsonObject.getBoolean("dir")) {
                dirCount++;
            }else {
                fileCount++;
            }
            shareFidTokenList.add(jsonObject.getString("share_fid_token"));
            fidList.add(jsonObject.getString("fid"));
        }
        log.info("⭕文件总数: {},文件数: {},文件夹数: {}",list.size(),fileCount,dirCount);
        if (StrUtil.isBlankIfStr(savePathFid)) {
            log.error("保存目录ID不合法，请重新获取，如果无法获取，请输入0作为文件夹ID");
            return null;
        }
        String saveAsTopFids = fileSaveService.saveSharedFiles(pwdId, stoken, fidList, shareFidTokenList, savePathFid);
        if (StrUtil.isBlankIfStr(saveAsTopFids)) return null;
        ArrayList<String> fids = new ArrayList<>();
        fids.add(saveAsTopFids);
        if (StrUtil.isEmptyIfStr(expireDay)) expireDay = "2";
        return this.share(fids,title,expireDay,b);
    }
    private final Lock lock = new ReentrantLock();
    public String saveAndShareAndDelCustomize(String url, String savePathFid, String adFid,
                                              String cookie, String title, String expireDay,
                                              boolean delFlag) throws IOException, InterruptedException {
        if (lock.tryLock()) {
            try {
                log.info("开始转存...");
                String stoken = this.checkByUrl(url, null, cookie);
                if (StrUtil.isBlankIfStr(stoken)) {
                    log.warn("❌链接已失效！");
                    return null;
                }
                String pwdId = getPwdId(url);
                List<JSONObject> list = this.getFileList(pwdId, stoken, cookie, "0");
                if (list == null) return null;
                // 如果是自己的 url 则直接返回原始 url
                if (list.isEmpty()) return url;
                int dirCount = 0;
                int fileCount = 0;
                ArrayList<String> fidList = new ArrayList<>();
                ArrayList<String> shareFidTokenList = new ArrayList<>();
                for (JSONObject jsonObject : list) {
                    if (jsonObject.getBoolean("dir")) {
                        dirCount++;
                    } else {
                        fileCount++;
                    }
                    shareFidTokenList.add(jsonObject.getString("share_fid_token"));
                    fidList.add(jsonObject.getString("fid"));
                }
                log.info("⭕文件总数: {}, 文件数: {}, 文件夹数: {}", list.size(), fileCount, dirCount);
                if (StrUtil.isBlankIfStr(savePathFid)) {
                    log.error("保存目录ID不合法，请重新获取，如果无法获取，请输入0作为文件夹ID");
                    return null;
                }
                String saveAsTopFids = fileSaveService.saveSharedFiles(pwdId, stoken, fidList, shareFidTokenList, savePathFid, cookie);
                if (StrUtil.isBlankIfStr(saveAsTopFids)) return null;
                ArrayList<String> fids = new ArrayList<>();
                fids.add(saveAsTopFids);
                if (StrUtil.isEmptyIfStr(expireDay)) expireDay = "2";
                return this.share(fids, adFid, title, cookie, expireDay, delFlag);
            } finally {
                lock.unlock();
            }
        } else {
            log.warn("转存任务已在执行，跳过本次请求");
        }
        return null;
    }
    public String saveAndShare(String url, String savePathFid,String title) throws IOException, InterruptedException {
        return this.saveAndShareAndDel(url,savePathFid,title,null,true);
    }
    /**
     * 分享文件 默认只有1天
     * expired_type 1:永久 2:1天 3:7天 4:30天
     */
    public String share(List<String> pFidList,String title,String expireDay,boolean b) throws IOException, InterruptedException {
        log.info("开始生成分享链接");
        String property = environment.getProperty("quark.config.ad-fid");
        pFidList.add(property);
        String url = baseUrl + "/1/clouddrive/share?pr=ucpro&fr=pc&uc_param_str";
        HashMap<String, Object> data = new HashMap<>();
        data.put("fid_list",pFidList);
        data.put("title",title);
        data.put("url_type","1");
        data.put("expired_type",expireDay);
        data.put("passcode","");
        JSONObject jsonObject = okhttpUtil.postJsonObject(url, header(), data, okhttpUtil.getOkHttpClient());
        if (jsonObject.isEmpty() || jsonObject.getInteger("status") != 200) return null;
        String taskId = jsonObject.getJSONObject("data").getString("task_id");
        url = baseUrl + "/1/clouddrive/task?pr=ucpro&fr=pc&uc_param_str&retry_index=0&task_id=" + taskId;
        int retry = 10;
        while (retry > 0) {
            retry++;
            Thread.sleep(200 + new Random().nextInt(500));
            jsonObject = okhttpUtil.getJsonObject(url, header(), okhttpUtil.getOkHttpClient());
            if (jsonObject.isEmpty() || jsonObject.getInteger("status") != 200) continue;
            if (StrUtil.isBlankIfStr(jsonObject.getJSONObject("data").getString("share_id"))) continue;
            break;
        }
        Thread.sleep(200 + new Random().nextInt(500));
        String shareId = jsonObject.getJSONObject("data").getString("share_id");
        url = baseUrl + "/1/clouddrive/share/password?pr=ucpro&fr=pc&uc_param_str";
        data.clear();
        data.put("share_id",shareId);
        jsonObject = okhttpUtil.postJsonObject(url, header(), data, okhttpUtil.getOkHttpClient());
        if (jsonObject.isEmpty() || jsonObject.getInteger("status") != 200) return null;
        Thread.sleep(200 + new Random().nextInt(500));
        String shareUrl = jsonObject.getJSONObject("data").getString("share_url");
        log.info("分享链接: {}",shareUrl);
        // 加入定时删除队列
        pFidList.remove(property);
        if (b) linkService.createLinks(pFidList);
        return shareUrl;
    }

    public String share(List<String> pFidList, String adFid, String title, String cookie,
                        String expireDay, boolean delFlag) throws IOException, InterruptedException {
        log.info("开始生成分享链接");
        if (adFid != null) pFidList.add(adFid);
        String url = baseUrl + "/1/clouddrive/share?pr=ucpro&fr=pc&uc_param_str";
        HashMap<String, Object> data = new HashMap<>();
        data.put("fid_list", pFidList);
        data.put("title", title);
        data.put("url_type", "1");
        data.put("expired_type", expireDay);
        data.put("passcode", "");
        HashMap<String, String> header = header();
        header.put("Cookie", cookie);
        JSONObject jsonObject = okhttpUtil.postJsonObject(url, header, data, okhttpUtil.getOkHttpClient());
        if (jsonObject.isEmpty() || jsonObject.getInteger("status") != 200) return null;
        String taskId = jsonObject.getJSONObject("data").getString("task_id");
        url = baseUrl + "/1/clouddrive/task?pr=ucpro&fr=pc&uc_param_str&retry_index=0&task_id=" + taskId;
        int retry = 10;
        while (retry > 0) {
            retry--;
            Thread.sleep(200 + new Random().nextInt(500));
            jsonObject = okhttpUtil.getJsonObject(url, header, okhttpUtil.getOkHttpClient());
            if (jsonObject.isEmpty() || jsonObject.getInteger("status") != 200) continue;
            if (StrUtil.isBlankIfStr(jsonObject.getJSONObject("data").getString("share_id"))) continue;
            break;
        }
        Thread.sleep(200 + new Random().nextInt(500));
        String shareId = jsonObject.getJSONObject("data").getString("share_id");
        url = baseUrl + "/1/clouddrive/share/password?pr=ucpro&fr=pc&uc_param_str";
        data.clear();
        data.put("share_id", shareId);
        jsonObject = okhttpUtil.postJsonObject(url, header, data, okhttpUtil.getOkHttpClient());
        if (jsonObject.isEmpty() || jsonObject.getInteger("status") != 200) return null;
        Thread.sleep(200 + new Random().nextInt(500));
        String shareUrl = jsonObject.getJSONObject("data").getString("share_url");
        log.info("分享链接: {}", shareUrl);
        HashMap<String, Object> map = getUrlInfo(getPwdId(shareUrl), checkByUrl(shareUrl,null), cookie, "0");
        this.data.setMapData(shareUrl, map);
        if (adFid != null) pFidList.remove(adFid);
        if (delFlag) linkService.createLinks(pFidList, cookie);
        return shareUrl;
    }

    public String share(List<String> pFidList,String title) throws IOException, InterruptedException {
        return this.share(pFidList,title,"2",true);
    }

    /**
     * 更新资源
     */
    public String updateSave(String pwdId, String stoken, String pFid,String fid,String shareToken) {
        return fileSaveService.saveSharedFiles(pwdId, stoken, Collections.singletonList(fid), Collections.singletonList(shareToken), pFid);
    }
    /**
     * 构建查询字符串
     */
    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    public String getPwdId(String url) {
        String pattern = "(?<=s/)([^#]+)";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);  // 返回匹配的部分
        } else {
            return null;  // 如果没有找到匹配，返回 null
        }
    }

    /**
     * 重命名文件
     */
    public boolean renameFile(String fid, String newName) {
        try {
            Map<String, String> bodyMap = new HashMap<>();
            bodyMap.put("fid", fid);
            bodyMap.put("file_name", newName);
            JSONObject response = okhttpUtil.postJsonObject(
                    baseUrl + "/1/clouddrive/file/rename?pr=ucpro&fr=pc&uc_param_str",
                    header(),
                    bodyMap,
                    okhttpUtil.getOkHttpClient()
            );
            if (response != null && response.getInteger("code") != 0) {
                log.error("重命名文件失败: {}, 原因: {}", fid, response.getString("message"));
                return false;
            }
            return true;

        } catch (Exception e) {
            log.error("重命名文件异常", e);
            return false;
        }
    }

    /**
     * 删除文件夹
     */
    public boolean delete(List<String> fid) throws IOException {
        String url = baseUrl + "/1/clouddrive/file/delete?pr=ucpro&fr=pc&uc_param_str=";
        HashMap<String, Object> data = new HashMap<>();
        data.put("action_type","2");
        data.put("filelist",fid);
        JSONArray jsonArray = new JSONArray();
        data.put("exclude_fids",jsonArray);
        JSONObject jsonObject = okhttpUtil.postJsonObject(url, header(), data, okhttpUtil.getOkHttpClient());
        return !jsonObject.isEmpty() && jsonObject.getInteger("status") == 200;
    }
    public boolean delete(List<String> fid, String cookie) throws IOException {
        String url = baseUrl + "/1/clouddrive/file/delete?pr=ucpro&fr=pc&uc_param_str=";
        HashMap<String, Object> data = new HashMap<>();
        data.put("action_type","2");
        data.put("filelist",fid);
        JSONArray jsonArray = new JSONArray();
        data.put("exclude_fids",jsonArray);
        HashMap<String, String> header = header();
        header.put("Cookie",cookie);
        JSONObject jsonObject = okhttpUtil.postJsonObject(url, header, data, okhttpUtil.getOkHttpClient());
        return !jsonObject.isEmpty() && jsonObject.getInteger("status") == 200;
    }
    /**
     * 创建文件夹
     * @param dirPath 文件夹绝对路径
     * @return String 返回文件夹fid
     * 对于已经存在的文件夹，则不会再创建，而是直接返回此文件夹的fid
     */
    public String createDirectory(String dirPath) throws IOException {
        String url = baseUrl + "/1/clouddrive/file?pr=ucpro&fr=pc&uc_param_str=";
        HashMap<String, Object> body = new HashMap<>();
        body.put("pdir_fid", "0");
        body.put("file_name", "");
        body.put("dir_path", dirPath);
        body.put("dir_init_lock", false);

        JSONObject response = okhttpUtil.postJsonObject(url, header(), body, okhttpUtil.getOkHttpClient());
        if (response != null && response.getInteger("code") == 0) {
            return response.getJSONObject("data").getString("fid");
        }
        return null;
    }
    /**
     * @param fid 文件夹id
     * @return JsonObject
     */
    public JSONObject getDirDetails(String fid) throws IOException {
        String url = baseUrl + "/1/clouddrive/file/sort?pr=ucpro&fr=pc&uc_param_str&pdir_fid=" + fid + "&_page=1&_size=50&_fetch_total=1&_fetch_sub_dirs=0&_sort=file_type:asc,updated_at:desc&_fetch_full_path=0";
        return okhttpUtil.getJsonObject(url, header(), okhttpUtil.getOkHttpClient());
    }

    /**
     * 递归获取文件列表 返回Tree
     */
    public Tree<QuarkNode> getAllFileList(String pwdId, String stoken, String pdirFid) {
        try {
            Tree<QuarkNode> rootTree = new Tree<>();
            rootTree.put("id", "root");
            rootTree.put("name", "根目录");
            rootTree.put("parentId", "0");

            // 递归获取文件树
            getFileListRecursive(pwdId, stoken, pdirFid, rootTree);

            return rootTree;
        } catch (Exception e) {
            log.error("获取文件列表异常", e);
            return null;
        }
    }
    public Tree<QuarkNode> getAllFileList(String pdirFid) {
        try {
            Tree<QuarkNode> rootTree = new Tree<>();
            rootTree.put("id", "root");
            rootTree.put("name", "根目录");
            rootTree.put("parentId", "0");
            rootTree.put("shareFidToken",null);

            // 递归获取文件树
            getFileListRecursive(pdirFid, rootTree);

            return rootTree;
        } catch (Exception e) {
            log.error("获取文件列表异常", e);
            return null;
        }
    }
    private void getFileListRecursive(String pwdId, String stoken, String pdirFid, Tree<QuarkNode> parentTree) throws IOException {
        int page = 1;
        Map<String, String> queryParams = buildInitialQueryParams(pwdId, stoken);

        while (true) {
            // 构建请求URL
            String url = baseUrl + "/1/clouddrive/share/sharepage/detail";
            queryParams.put("pdir_fid", pdirFid);
            queryParams.put("_page", String.valueOf(page));
            String fullUrl = url + "?" + buildQueryString(queryParams);

            // 发送请求
            JSONObject response = okhttpUtil.getJsonObject(fullUrl, header(), okhttpUtil.getOkHttpClient());
            if (response == null) break;

            JSONObject data = response.getJSONObject("data");
            if (data == null) break;

            JSONArray fileList = data.getJSONArray("list");
            if (fileList == null || fileList.isEmpty()) break;

            int total = response.getJSONObject("metadata").getInteger("_total");

            // 处理单个文件/文件夹的情况
            if (total == 1) {
                JSONObject singleItem = fileList.getJSONObject(0);
                // 在处理节点时的修改
                QuarkNode node = createQuarkNode(singleItem);
                Tree<QuarkNode> currentTree = node.toTree();
                parentTree.setChildren(Collections.singletonList(currentTree));

                if (node.isDir()) {
                    getFileListRecursive(pwdId, stoken, node.getFid(), currentTree);
                }
                return;
            }

            // 处理多个文件/文件夹的情况
            for (int i = 0; i < fileList.size(); i++) {
                JSONObject item = fileList.getJSONObject(i);
                QuarkNode node = createQuarkNode(item);

                Tree<QuarkNode> currentTree = node.toTree();  // 直接使用 QuarkNode 的 toTree 方法

                // 将当前树添加到父节点的子节点列表中
                if (parentTree.getChildren() == null) {
                    parentTree.setChildren(new ArrayList<>());
                }
                parentTree.getChildren().add(currentTree);

                if (node.isDir()) {
                    // 递归处理子文件夹
                    getFileListRecursive(pwdId, stoken, node.getFid(), currentTree);
                }
            }


            // 检查是否需要继续翻页
            if (fileList.size() < total) {
                page++;
            } else {
                break;
            }
        }
    }
    private void getFileListRecursive(String pdirFid, Tree<QuarkNode> parentTree) throws IOException {
        int page = 1;

        while (true) {
            JSONObject response = this.getDirDetails(pdirFid);

            // 发送请求
            if (response == null) break;

            JSONObject data = response.getJSONObject("data");
            if (data == null) break;

            JSONArray fileList = data.getJSONArray("list");
            if (fileList == null || fileList.isEmpty()) break;

            int total = response.getJSONObject("metadata").getInteger("_total");

            // 处理单个文件/文件夹的情况
            if (total == 1) {
                JSONObject singleItem = fileList.getJSONObject(0);
                // 在处理节点时的修改
                QuarkNode node = createQuarkNode(singleItem);
                Tree<QuarkNode> currentTree = node.toTree();
                parentTree.setChildren(Collections.singletonList(currentTree));

                if (node.isDir()) {
                    getFileListRecursive(node.getFid(), currentTree);
                }
                return;
            }

            // 处理多个文件/文件夹的情况
            for (int i = 0; i < fileList.size(); i++) {
                JSONObject item = fileList.getJSONObject(i);
                QuarkNode node = createQuarkNode(item);

                Tree<QuarkNode> currentTree = node.toTree();

                // 将当前树添加到父节点的子节点列表中
                if (parentTree.getChildren() == null) {
                    parentTree.setChildren(new ArrayList<>());
                }
                parentTree.getChildren().add(currentTree);

                if (node.isDir()) {
                    // 递归处理子文件夹
                    getFileListRecursive(node.getFid(), currentTree);
                }
            }


            // 检查是否需要继续翻页
            if (fileList.size() < total) {
                page++;
            } else {
                break;
            }
        }
    }
    private QuarkNode createQuarkNode(JSONObject json) {
        return new QuarkNode()
                .setFid(json.getString("fid"))
                .setFileName(json.getString("file_name"))
                .setPdirFid(json.getString("pdir_fid"))
                .setShareFidToken(json.getString("share_fid_token"))
                .setDir(json.getBoolean("dir"));
    }
    private Map<String, String> buildInitialQueryParams(String pwdId, String stoken) {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("pr", "ucpro");
        queryParams.put("fr", "pc");
        queryParams.put("pwd_id", pwdId);
        queryParams.put("stoken", stoken);
        queryParams.put("force", "0");
        queryParams.put("_size", "50");
        queryParams.put("_fetch_banner", "0");
        queryParams.put("_fetch_total", "1");
        queryParams.put("_sort", "file_type:asc,updated_at:desc");
        queryParams.put("_fetch_share", "0");
        return queryParams;
    }

}
