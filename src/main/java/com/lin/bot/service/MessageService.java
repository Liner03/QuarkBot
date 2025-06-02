package com.lin.bot.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lin.bot.api.base.ContactApi;
import com.lin.bot.api.base.GroupApi;
import com.lin.bot.api.base.LoginApi;
import com.lin.bot.api.base.MessageApi;
import com.lin.bot.data.TempData;
import com.lin.bot.mapper.UpdateListMapper;
import com.lin.bot.model.DTO.QuarkDTO;
import com.lin.bot.model.DTO.UpdateListDTO;
import com.lin.bot.util.Notify;
import com.lin.bot.model.entity.UpdateListEntity;
import com.lin.bot.util.Common;
import com.lin.bot.util.Quark;
import com.lin.bot.util.QuarkUtil;
import com.lin.bot.util.constants.LoginConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @Author Lin.
 * @Date 2024/12/29
 */
@Service
@Slf4j
public class MessageService {
    @Autowired
    private TempData data;
    @Autowired
    private MessageApi msgApi;
    @Autowired
    private ContactApi contactApi;
    @Autowired
    private GroupApi groupApi;
    @Autowired
    private Environment env;
    @Autowired
    private SearchService searchService;
    @Autowired
    private RedisTemplate<String ,Object> redisTemplate;
    @Autowired
    @Lazy
    private Quark quark;
    @Value("${bot.config.add-welcome-content: }")
    private String addWelcomeContent;
    @Value("${bot.config.match-keywords:进群}")
    private String matchKeywords;
    @Value("${quark.config.save-path-fid:0}")
    private String savePathFid;
    @Value("${bot.config.auth-password}")
    private String authPassword;
    @Autowired
    private QuarkService quarkService;
    @Autowired
    @Lazy
    private QuarkUtil quarkUtil;
    @Autowired
    private UpdateListMapper updateListMapper;
    @Autowired
    @Lazy
    private Notify notify;
    @Autowired
    private AsyncService asyncService;

    /**
     * @param toUser 发送的用户或者群组
     * @param message 发送的消息
     * @param ast @的好友 wxid
     * @author Lin.
     * @date 2024/12/29
     */
    public JSONObject send_to_user(String toUser, String message, String ast) {
        String appId = data.getDataByString("appId");
        return msgApi.postText(appId, toUser, message, ast);
    }
    public JSONObject send_to_user(String toUser, String message) {
        log.debug("正在给用户:{}发送消息:{}", toUser,message);
        return this.send_to_user(toUser, message, null);
    }
    public JSONObject send_to_userByAppMsg(String toUser, String message) {
        String appId = data.getDataByString("appId");
        return msgApi.postAppMsg(appId, toUser, message);
    }

    /**
     * 接收回调消息
     */
    public void receive(JSONObject json) throws IOException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, InterruptedException, ExecutionException {
        // 通过msgId 避免消息重复回调
        if (json == null) return;
        if ("回调地址链接成功！".equals(json.getString("testMsg"))) {
            log.info("回调地址设置成功!");
            return;
        }
        String msgId = json.getJSONObject("Data").getString("NewMsgId") == null ? "" : json.getJSONObject("Data").getString("NewMsgId");
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("NewMsgId", msgId))) {
            return;
        }
        HashSet<String> set = new HashSet<>();
        set.add(msgId);
        // 设置过期时间为3min
        this.data.setSetData("NewMsgId",set,3);
        String typeName = json.getString("TypeName");
        if (!"AddMsg".equals(typeName)) {
            // 非接收到消息，不做处理
            return;
        }
        if("Offline".equals(typeName)) {
            // 掉线通知
            this.data.setStringData("loginStatus", String.valueOf(LoginConst.UNLOGIN));
            log.error("{} 掉线了",JSONObject.parse(this.data.getDataByString("loginInfo")).getString("nickName"));
            notify.send(JSONObject.parseObject(this.data.getDataByString("loginInfo")).getString("nickName") + " 掉线了");
            return;
        }
        JSONObject data = json.getJSONObject("Data");
        if (data !=null && data.isEmpty()) return;
        String fromUserName = data.getJSONObject("FromUserName").getString("string");
        if (JSONObject.parse(this.data.getDataByString("loginInfo")).getString("wxid").equals(fromUserName)) {
            // 是自己发送的消息 直接忽略
            return;
        }
        String content = data.getJSONObject("Content").getString("string");
        Integer MsgType = data.getInteger("MsgType");
        if (MsgType == 1) {
            // 文本消息
            // 用户名称
            String pushContent = data.getString("PushContent");
            if (pushContent == null || !pushContent.contains(":")) {
                log.debug("pushContent {}",pushContent);
                return;
            }
            String uname = pushContent.split(":")[0];
            log.info("uname: {},FromUserName: {}, content: {}", uname,fromUserName, content);
            // 进群匹配
            boolean isMatchGroup = ReUtil.isMatch(matchKeywords, content);
            String[] groups = env.getProperty("bot.config.invite-group-name", String[].class);
            List<String> groupList = Arrays.stream(env.getProperty("bot.config.active-group",String[].class)).toList();
            if (isMatchGroup) {
                // 匹配到进群关键词
                Map<String ,String > map = (HashMap) this.data.getDataByMap("chatrooms");
                // 找到第一个在 map 中存在的 key 并返回其值
                String chatroomId = null;
                String groupName = null;
                for (String group : groups) {
                    if (map.containsKey(group)) {
                        chatroomId = map.get(group);
                        groupName = group;
                        break;
                    }
                }
                if (chatroomId == null) {
                    log.warn("所有群组全部满员！");
                    notify.send("所有群组全部满员！请及时更新群信息哦");
                    return;
                }
                JSONObject res = groupApi.inviteMember(this.data.getDataByString("appId"),
                        fromUserName,
                        chatroomId,
                        "");
                if (res.getInteger("ret") == 200) {
                    this.send_to_user(fromUserName, "邀请成功啦，群聊昵称 " + groupName + "\n请勿在群内发布违法违纪的东西哦");
                    return;
                }
            }

            Map<Object, Object> chatrooms = this.data.getDataByMap("chatrooms");

            // 遍历 map，检查 fromUserName 是否是 value，并且相应的 key 是否在 groupList 中
            boolean result = false;
            for (Map.Entry<Object, Object> entry : chatrooms.entrySet()) {
                if (entry.getValue().equals(fromUserName)) {  // 如果 value 等于 fromUserName
                    if (groupList.contains(entry.getKey())) {  // 检查 key 是否在 groupList 中
                        result = true;
                        break;  // 找到匹配项后可以退出循环
                    }
                }
            }
            if (result){
                String fromWxid = content.split(":\n")[0];
                content = content.split(":\n")[1];
                // 搜剧匹配
                String reg = env.getProperty("bot.config.search-keywords", String.class);
                String replyContent = """
                        @{{username}}
                        为您查询到以下相关资源⬇
                        -------------------
                        ⭕ 观看口令如：看1
                        -------------------
                       {{data}}
                        ⭕ 第 {{now}} / {{page}} 页
                        -------------------
                        😃 翻页：上一页 | 下一页
                        -------------------
                        ⚠ 资源来源于网络，侵权请联系我
                        ⚠ 资源将在30分钟后删除，请尽快转存下载
                       """;
                String appmsg = "<appmsg appid=\"\" sdkver=\"0\"><title>{{title}}</title><des>打开点底部【去APP查看】或点微信右上角【复制链接】再去打开夸克APP获取。</des>\\\\n\\\\t\\\\t<action /><type>5</type><showtype>0</showtype><soundtype>0</soundtype><mediatagname /><messageext /><messageaction /><content /><contentattr>0</contentattr><url>{{url}}</url><lowurl /><dataurl /><lowdataurl /><appattach><totallen>0</totallen><attachid /><emoticonmd5 /><fileext /><cdnthumburl>3052020100044b304902010002042b2031ac02033d12000204d6cf1baf02046780a05d042436333631363837342d373538612d343934352d396632622d3365393966666664356635630204012408030201000400</cdnthumburl><cdnthumbmd5>27a196af505c2f139bc5441daaa25763</cdnthumbmd5><cdnthumblength>4397</cdnthumblength><cdnthumbwidth>100</cdnthumbwidth><cdnthumbheight>100</cdnthumbheight><cdnthumbaeskey>b0c64d219ce87833bb8dd430dc32d6df</cdnthumbaeskey><aeskey>b0c64d219ce87833bb8dd430dc32d6df</aeskey><encryver>1</encryver></appattach><extinfo /><sourceusername>gh_363b924965e9</sourceusername><sourcedisplayname>夸克</sourcedisplayname><thumburl>https://img.alicdn.com/imgextra/i4/O1CN01r67PYz1wWmV49RuGu_!!6000000006316-2-tps-108-44.png</thumburl><md5 /><statextstr /><mmreadershare><itemshowtype>0</itemshowtype></mmreadershare></appmsg>";
                if (ReUtil.isMatch(reg, content)) {
                    String keyword = ReUtil.findAll(reg, content ,2, new ArrayList<>()).get(0).trim();
                    if (StrUtil.isNotBlank(keyword)) {
                        /**
                         * name : uname
                         * keyword : keyword
                         * data : list
                         * now : now
                         * page : list.size()
                         * expire 1min
                         */
                        this.send_to_user(fromUserName,"正在为您搜索中，请稍后...");
                        List<Map<String, String>> list;
                        // 废弃方案
//                        if (content.contains("短剧")) {
//                            list = (List<Map<String, String>>)searchService.searchDuanju(keyword).getData();
//                        }else {
//                            list = (List<Map<String, String>>)searchService.search(keyword).getData();
//                        }
                        list = searchService.searchDuanju(keyword).getData();

                        if (list == null || list.isEmpty()) {
                            this.send_to_user(fromUserName,"⚠暂未找到该资源 建议更换详细的关键字 😘");
                            return;
                        }
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("keyword", keyword);
                        map.put("data",list);
                        map.put("now",1);
                        map.put("page",list.size());
                        this.data.setMapData(uname,map,5);
                        String s = replyContent
                                .replace("{{username}}", uname)
                                .replace("{{data}}", Common.toStringForMap(list.get(0)))
                                .replace("{{now}}", "1")
                                .replace("{{page}}", list.size()+"");
                        this.send_to_user(fromUserName,s,fromWxid);
                        return;
                    }
                }
                // 翻页匹配
                Map<Object, Object> tempMap = this.data.getDataByMap(uname);
                if (MapUtil.isNotEmpty(tempMap)) {
                    String s = null;
                    int now = Integer.parseInt(tempMap.get("now").toString());
                    int page = Integer.parseInt(tempMap.get("page").toString());
                    List<Map<String, String>> list = (List)tempMap.get("data");
                    if ("下一页".equals(content) && now < page) {
                        now += 1;
                        tempMap.put("now", String.valueOf(now));
                        s = replyContent
                                .replace("{{data}}", Common.toStringForMap(list.get(now - 1))) // 使用 now - 1 对应 list 索引
                                .replace("{{username}}", uname)
                                .replace("{{now}}", String.valueOf(now))
                                .replace("{{page}}", String.valueOf(page));
                    } else if ("上一页".equals(content) && now > 1) { // 修改为 now > 1
                        now -= 1;
                        tempMap.put("now", String.valueOf(now));
                        s = replyContent
                                .replace("{{data}}", Common.toStringForMap(list.get(now - 1))) // 使用 now - 1 对应 list 索引
                                .replace("{{username}}", uname)
                                .replace("{{now}}", String.valueOf(now))
                                .replace("{{page}}", String.valueOf(page));
                    } else if (ReUtil.isMatch("^(看)\\s*(.+)$", content)) {
                        int num = 1;
                        try {
                            num = Integer.parseInt(ReUtil.get("^(看)\\s*(.+)$", content, 2));
                        } catch (NumberFormatException e) {
                            return;
                        }
                        Map<String, String> nowMap = list.get(now - 1); // now 从 1 开始，所以减 1
                        // 将 Map 转换为 List（按顺序）
                        List<Map.Entry<String, String>> entryList = new ArrayList<>(nowMap.entrySet());
                        String title,url;
                        // 获取第 num 个元素（num 是从 1 开始，索引从 0 开始，所以需要减 1）
                        if (num > 0 && num <= entryList.size()) {
                            Map.Entry<String, String> entry = entryList.get(num - 1);
                            // 获取对应的 key 和 value
                            title = entry.getKey();
                            url = entry.getValue();
                        } else {
                            this.send_to_user(fromUserName,"未找到匹配的资源,请更换其他资源重试");
                            return;
                        }
                        // 发送 URL 到用户
                        String shareUrl = null;
                        try {
                            shareUrl = quark.saveAndShare(url, savePathFid, "");
                        } catch (IOException | InterruptedException e) {
                            log.error("转存时错误: {}", e.getMessage());
                        }
                        if (StrUtil.isNotBlank(shareUrl)) {
                            appmsg = appmsg.replace("{{url}}", shareUrl).replace("{{title}}",title);
                            this.send_to_userByAppMsg(fromUserName, appmsg);

                        }else {
                            this.send_to_user(fromUserName, "获取分享链接失败！请尝试其它资源");
                        }
                        return;
                    }
                    if (s != null) {
                        this.data.setMapData(uname,tempMap,5);
                        this.send_to_user(fromUserName,s,fromWxid);
                        return;
                    }
                }

            }

            // 机器人命令
            String newMsg = """
                    {{name}} {{description}}
                    {{link}}
                    """;
            String menu = """
                    欢迎使用影视资源机器人!
                    """;
            String adminMenu = """
                    管理员命令
                    ----------------------
                    #ping
                    #开启/关闭提醒
                    #更新群组列表
                    #更新ck 你的COOKIE
                    #更新文件夹id 你的文件夹id
                    下面请使用空格隔开
                    ----------------------
                    如果完结为 false 的话，则自动开启更新
                    #提交 名字 描述 链接 (是否完结 true|false)
                    #转存 名字 描述 链接 (是否完结 true|false)
                    #提交并群发 名字 描述 链接 (是否完结 true|false)
                    #转存并群发 名字 描述 链接 (是否完结 true|false)
                    #更新 你自己的分享链接 新的分享链接(没有则写 无 ) (是否完结 true|false)
                    -----------------------
                    """;
            if (content.startsWith("#")) {
                // 匹配wxid
                content = content.substring(1);
                String adminWxid = this.data.getDataByString("adminWxid");
                if (StrUtil.isBlank(adminWxid) && content.startsWith("auth")) {
                    if (authPassword.equals(content.split("auth")[1].strip())) {
                        this.data.setStringDataWithoutExpiration("adminWxid", fromUserName);
                        this.send_to_user(fromUserName,"认证成功");
                    }else {
                        this.send_to_user(fromUserName,"认证失败!");
                    }
                    return;
                }
                if (content.startsWith("help")) {
                    if (fromUserName.equals(adminWxid)) {
                        send_to_user(fromUserName, adminMenu + "\n" + menu);
                        return;
                    } else {
                        send_to_user(fromUserName, menu);
                        return;
                    }
                }
                // 非管理员
                if (!adminWxid.equals(fromUserName)) return;
                // 管理员指令
                if ("更新群组列表".equals(content)) {
                    Future<Boolean> future = asyncService.getDetails();
                    if (future == null) {
                        send_to_user(adminWxid,"获取失败");
                        return;
                    }
                    if (future.get()) {
                        send_to_user(adminWxid,"获取成功");
                        return;
                    }
                    send_to_user(adminWxid,"获取失败");
                    return;
                } else if ("开启提醒".equals(content)) {
                    this.data.setStringDataWithoutExpiration("remindMsg","true");
                    this.send_to_user(adminWxid,"开启成功");
                    return;
                }else if ("关闭提醒".equals(content)) {
                    this.data.setStringDataWithoutExpiration("remindMsg","false");
                    this.send_to_user(adminWxid,"关闭成功");
                    return;
                }else if ("ping".equals(content)) {
                    this.send_to_user(adminWxid,"pong");
                    return;
                }
                // 提交转存
                if (content.startsWith("提交")) {
                    String[] split = content.split("提交并群发|提交")[1].trim().split(" ");
                    String title = split[0];
                    String desc = split[1];
                    String link = split[2];
                    String update = "false";
                    if (split.length == 4) update = split[3];
                    if (content.contains("群发")) {
                        if (StrUtil.isBlank(title) || StrUtil.isBlank(link)) {
                            send_to_user(fromUserName,"格式错误,名称和链接不能为空");
                            return;
                        }
                        QuarkDTO dto = new QuarkDTO();
                        dto.setName(title).setUrl(link).setDescription(desc).setEnding("true".equals(update)).setValid(true);
                        if (quarkService.addOne(dto)) {
                            notify.sendAllGroup(newMsg
                                    .replace("{{name}}",title)
                                    .replace("{{description}}",desc)
                                    .replace("{{link}}",link)
                                    .trim());
                            send_to_user(fromUserName,"添加成功!");
                            log.info("{} 添加资源 {}, {}",fromUserName,title,link);
                        }else {
                            send_to_user(fromUserName,"添加失败!");
                            log.info("添加 {} 失败",title);
                            return;
                        }
                    }else {
                        if (StrUtil.isBlank(title) || StrUtil.isBlank(link)) {
                            send_to_user(fromUserName,"格式错误,名称和链接不能为空");
                            return;
                        }
                        QuarkDTO dto = new QuarkDTO();
                        dto.setName(title).setUrl(link).setDescription(desc).setEnding("true".equals(update)).setValid(true);
                        if (quarkService.addOne(dto)) {
                            send_to_user(fromUserName,"添加成功!");
                            log.info("{} 添加资源 {}, {}",fromUserName,title,link);
                        } else {
                            send_to_user(fromUserName,"添加失败!");
                            log.info("添加 {} 失败",title);
                            return;
                        }
                    }
                    // 需要持续关注更新
                    if ("false".equals(update)) {
                        UpdateListDTO dto = new UpdateListDTO();
                        dto.setName(title).setUrl(link).setShare(link);
                        quarkUtil.addRegularlyList(dto,null);
                    }
                } else if (content.startsWith("转存")) {
                    String[] split = content.split("转存并群发|转存")[1].trim().split(" ");
                    String title = split[0];
                    String desc = split[1];
                    String link = split[2];
                    String update = "true";
                    if (split.length == 4) update = split[3];
                    log.info("split: {}, update: {}",Arrays.toString(split), update);
                    String shareUrl;
                    if (content.contains("群发")) {
                        if (StrUtil.isBlank(title) || StrUtil.isBlank(link)) {
                            send_to_user(fromUserName,"格式错误,名称和链接不能为空");
                            return;
                        }
                        QuarkDTO dto = new QuarkDTO();
                        dto.setName(title).setDescription(desc).setValid(true).setEnding("true".equals(update));
                        shareUrl = quark.saveAndShareAndDel(link,savePathFid,title,"1",false);
                        if (StrUtil.isBlank(shareUrl)) {
                            send_to_user(fromUserName,"转存获取连接失败!");
                            log.error("转存获取连接失败!");
                            return;
                        }
                        dto.setUrl(shareUrl);
                        if (quarkService.addOne(dto)) {
                            notify.sendAllGroup(newMsg
                                    .replace("{{name}}",title)
                                    .replace("{{description}}",desc)
                                    .replace("{{link}}",shareUrl)
                                    .trim());
                            send_to_user(fromUserName,"添加成功,分享链接:" + shareUrl);
                            log.info("{} 添加资源 {}, {}",fromUserName,title,link);
                        }else {
                            send_to_user(fromUserName,"添加失败!");
                            log.info("添加 {} 失败",title);
                            return;
                        }

                    }else {
                        if (StrUtil.isBlank(title) || StrUtil.isBlank(link)) {
                            send_to_user(fromUserName,"格式错误,名称和链接不能为空");
                            return;
                        }
                        QuarkDTO dto = new QuarkDTO();
                        dto.setName(title).setDescription(desc).setValid(true).setEnding("true".equals(update));
                        shareUrl = quark.saveAndShareAndDel(link,savePathFid,title,"1",false);
                        if (StrUtil.isBlank(shareUrl)) {
                            send_to_user(fromUserName,"转存获取连接失败!");
                            log.error("转存获取连接失败!");
                            return;
                        }
                        dto.setUrl(shareUrl);
                        if (quarkService.addOne(dto)) {
                            send_to_user(fromUserName,"添加成功,分享链接:" + shareUrl);
                            log.info("{} 添加资源 {}, {}",fromUserName,title,link);
                        }else {
                            send_to_user(fromUserName,"添加失败!");
                            log.info("添加 {} 失败",title);
                        }

                    }
                    if ("false".equals(update)) {
                        UpdateListDTO dto = new UpdateListDTO();
                        dto.setName(title).setUrl(link).setShare(shareUrl);
                        quarkUtil.addRegularlyList(dto,null);
                    }
                } else if (content.startsWith("更新")) {
                    String[] split = content.split("更新")[1].trim().split(" ");
                    String oldShareUrl = split[0];
                    String newShareUrl = split[1];
                    String update = split[2];
                    if (StrUtil.isBlankIfStr(oldShareUrl)){
                        send_to_user(fromUserName,"你自己的分享链接 不能为空");
                        return;
                    }
                    if (StrUtil.isBlankIfStr(newShareUrl) && StrUtil.isBlank(update)) {
                        send_to_user(fromUserName,"新的分享链接 和 是否完结 不能同时为空");
                        return;
                    }
                    QueryWrapper<UpdateListEntity> wrapper = new QueryWrapper<>();
                    wrapper.eq("share",oldShareUrl);
                    UpdateListEntity entity = updateListMapper.selectOne(wrapper);
                    if (entity == null) {
                        send_to_user(fromUserName,"你自己的分享链接 查询失败");
                        return;
                    }
                    entity.setShare(StrUtil.isBlankIfStr(newShareUrl)?entity.getShare():"无".equals(newShareUrl)?entity.getShare():newShareUrl)
                            .setEnding(StrUtil.isBlankIfStr(update)? entity.getEnding() :
                                    "true".equals(update));

                    int i = updateListMapper.updateById(entity);

                    if (i > 0) {
                        send_to_user(fromUserName,"更新成功");
                        return;
                    }else {
                        send_to_user(fromUserName,"更新失败");
                        return;
                    }
                }


            }
        }else if (MsgType == 37) {
            // 添加好友通知消息
            String autoAdd = env.getProperty("bot.config.auto-accept-friends");
            boolean b = Boolean.parseBoolean(autoAdd);
            if (!b) {
                return;
            }
            Document xml = XmlUtil.parseXml(content);
            Element root = XmlUtil.getRootElement(xml);
            String v3 = root.getAttribute("encryptusername");
            String v4 = root.getAttribute("ticket");
            String scene = root.getAttribute("scene");
            JSONObject res = contactApi.search(this.data.getDataByString("appId"),
                    Integer.parseInt(scene),
                    3, v3, v4, addWelcomeContent);
            if (res.getInteger("ret") == 200) {
                Element e = XmlUtil.getRootElement(XmlUtil.parseXml(content));
                fromUserName = e.getAttribute("fromusername");
                String ad = """
                        官方直发大流量卡有保障 19元500G (推荐复制到浏览器打开)
                        https://172.lot-ml.com/ProductEn/Index/abc70ecbda21bd7a
                        """;
                this.send_to_user(fromUserName, addWelcomeContent + "\n" + """
                        ⭕进裙发搜加正确剧名到群里
                        ⭕群助手自动找剧
                        ⭕如：搜桃花马上请长缨
                        ⭕电视剧是同步官方免费更新！
                        ==================
                        """ + ad);
            }
        }
    }

    /**
     * 推送消息到 redis 频道
     */
    public boolean push(QuarkDTO quark) {
        if (quark == null || StrUtil.isEmptyIfStr(quark.getName()) || StrUtil.isEmptyIfStr(quark.getUrl())) {
            return false;
        }
        redisTemplate.opsForList().leftPush("push_list",quark);
        return true;
    }

}
