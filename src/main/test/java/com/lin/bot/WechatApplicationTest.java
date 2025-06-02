package com.lin.bot;


import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lin.bot.api.base.LoginApi;
import com.lin.bot.config.MagicRegexConfig;
import com.lin.bot.data.TempData;
import com.lin.bot.mapper.DuanjuMapper;
import com.lin.bot.mapper.UpdateListMapper;
import com.lin.bot.model.DTO.QuarkDTO;
import com.lin.bot.model.QuarkNode;
import com.lin.bot.model.VO.CommonVO;
import com.lin.bot.model.entity.DuanjuEntity;
import com.lin.bot.model.entity.UpdateListEntity;
import com.lin.bot.service.AsyncService;
import com.lin.bot.service.DuanjuService;
import com.lin.bot.service.SearchService;
import com.lin.bot.util.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.StopWatch;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @Author Lin.
 * @Date 2025/1/3
 */
@SpringBootTest
@EnableRedisRepositories
@Slf4j
public class WechatApplicationTest {
//    @Resource
//    private QuarkTest quarkTest;
    @Autowired
    private Quark quark;
    @Autowired
    private TreeDiffUtil treeDiffUtil;
    @Autowired
    private SearchService searchService;
    @Autowired
    private DuanjuMapper duanjuMapper;
    @Autowired
    private Notify notify;
//    @Autowired
//    private SearchService searchService;

    //    @Autowired
//    private Environment env;
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//    @Autowired
//    private SearchService searchService;
//    @Autowired
//    private TempData data;
//    @Autowired
//    private Quark quark;
//    @Autowired
//    private MagicRegexConfig magicRegexConfig;
//
//    @Test
//    public void xmlTest() {
//        String string = "<msg fromusername=\"wxid_phyyedw9xap22\" encryptusername=\"v3_020b3826fd03010000000000feba078fc1e760000000501ea9a3dba12f95f6b60a0536a1adb6f6352c38d0916c9c74045d85aa602efa2d81b84adde05d285124e8a54b9fcd039f725d6ac0d3bd651c7c74503a@stranger\" fromnickname=\"朝夕。\" content=\"我是朝夕。\" fullpy=\"chaoxi\" shortpy=\"CX\" imagestatus=\"3\" scene=\"6\" country=\"\" province=\"\" city=\"\" sign=\"\" percard=\"0\" sex=\"1\" alias=\"\" weibo=\"\" albumflag=\"3\" albumstyle=\"0\" albumbgimgid=\"\" snsflag=\"273\" snsbgimgid=\"http://shmmsns.qpic.cn/mmsns/FzeKA69P5uIdqPfQxp59LvOohoE2iaiaj86IBH1jl0F76aGvg8AlU7giaMtBhQ3bPibunbhVLb3aEq4/0\" snsbgobjectid=\"14216284872728580667\" mhash=\"d36f4cc1c8bba1df41b93d2215133cdb\" mfullhash=\"d36f4cc1c8bba1df41b93d2215133cdb\" bigheadimgurl=\"http://wx.qlogo.cn/mmhead/ver_1/G3G6r1OBfCIO40FTribZ3WvrLQbnMibfT5PyRaxeyjXgLqA8M94lKic3ibOztlrawo2xpVQaH7V6yhYATia3GKbVH8MhRbnKQGfNZ4EY8Zc85uy49P5WSZZrntbECUpQfrjRu/0\" smallheadimgurl=\"http://wx.qlogo.cn/mmhead/ver_1/G3G6r1OBfCIO40FTribZ3WvrLQbnMibfT5PyRaxeyjXgLqA8M94lKic3ibOztlrawo2xpVQaH7V6yhYATia3GKbVH8MhRbnKQGfNZ4EY8Zc85uy49P5WSZZrntbECUpQfrjRu/132\" ticket=\"v4_000b708f0b040000010000000000c502ff3b59b31c08394fdaefa0651000000050ded0b020927e3c97896a09d47e6e9eec84bb6bebe542fb120b366298a0157c280337855083f4a87fc4b15cfba311a11720041ce2d9f8a575cf7b432a2c0bebc5ed9c9a70bf7784c54ebbfb816e54e0fda2befcf2f873d162f5ed54108c76ce53310321077ced22420c5fbd199cff57d8e0a583f155e7e558@stranger\" opcode=\"2\" googlecontact=\"\" qrticket=\"\" chatroomusername=\"\" sourceusername=\"\" sourcenickname=\"\" sharecardusername=\"\" sharecardnickname=\"\" cardversion=\"\" extflag=\"0\"><brandlist count=\"0\" ver=\"640356091\"></brandlist></msg>";
//        Document xml = XmlUtil.parseXml(string);
//        Element rootElement = XmlUtil.getRootElement(xml);
//        String fromusername = rootElement.getAttribute("fromusername");
//        String v3 = rootElement.getAttribute("encryptusername");
//        String v4 = rootElement.getAttribute("ticket");
//        log.info("fromusername:{} \n v3:{} \n v4:{}",fromusername,v3,v4);
//    }
//
//    @Test
//    public void reTest() {
//        boolean match = ReUtil.isMatch("(?i)^[\\w\\u4e00-\\u9fa5]+\\.(png|jpeg|jpg|docx|xlsx)$", "公众号.png");
//        log.info("{}",match);
//    }
//
//    @Test
//    public void ymlTest() {
//        List<String> property = env.getProperty("bot.config.invite-group-name", List.class);
//        assert property != null;
//        log.info("property:{}",property);
//    }
//
//    @Test
//    public void redisTest() {
//        QuarkDTO quarkDTO = new QuarkDTO();
//        quarkDTO.setName("aaa");
//        quarkDTO.setUrl("httpsss");
//        redisTemplate.opsForList().leftPush("test",quarkDTO);
//        QuarkDTO test = (QuarkDTO)redisTemplate.opsForList().rightPop("test");
//        log.info("test:{}",test);
//    }
//
//    @Test
//    public void searchTest() {
//        CommonVO<?> vo = searchService.search("我是刑警");
//        List<Map<String, String>> list = (List<Map<String, String>>) vo.getData();
//        for (Map<String, String> map : list) {
//            log.info("map:{}",map);
//        }
//    }
//
//    @Test
//    public void pyTest() {
//        log.info(Objects.requireNonNull(Py.execute("search", "爱情公寓")).toString());
//    }
//
//    @Test
//    public void replyTest() {
//        String content = "搜剧校花";
//        String reg = env.getProperty("bot.config.search-keywords", String.class);
//        String keyword = ReUtil.findAll(reg, content ,2, new ArrayList<>()).get(0).trim();
//        log.info("keyword: {}",keyword);
//        if (StrUtil.isNotBlank(keyword)) {
//            List<Map<String, String>> list = (List<Map<String, String>>) searchService.search(keyword).getData();
//            if (list.isEmpty())
//                log.info( "⚠暂未找到该资源 建议更换详细的关键字\n 或者去🌏 https://1soso.cc 进行反馈哦 \n😘");
//            list = list.stream().filter(item -> !MapUtil.isEmpty(item)).toList();
//            String uname = "Lin.";
//            String replyContent = """
//                    @{{username}} 😃为您查询到以下相关资源⬇
//                    ⚠ 观看口令如：看1
//                    {{data}}
//                    第{{now}}/{{page}}页
//                    翻页：上一页|下一页
//                    """;
//            replyContent = replyContent.replace("{{username}}", uname);
//            Map<String, String> map;
//            for (int i = 0; i < list.size();i++) {
//                log.info("当前第{}页",i);
//                map = list.get(i);
//                String reply = replyContent.replace("{{data}}", Common.toStringForMap(map))
//                        .replace("{{now}}",String.valueOf(i + 1))
//                        .replace("{{page}}",String.valueOf(list.size()));
//                log.info(reply);
////                    Scanner scanner = new Scanner(System.in);
////                    String line = scanner.nextLine();
////                    if ("上一页".equals(line) && i > 0) {
////                        i--;
////                    } else if ("下一页".equals(line) && i <= list.size() - 1) {
////                        i++;
////                    }else {
////                        log.error("没有更多了~");
////                    }
//            }
//
//        }
//    }
//    @Test
//    public void onlineTest() {
//        Map<Object, Object> tempMap = data.getDataByMap("ㅤ ");
//        if (MapUtil.isNotEmpty(tempMap)) {
//            String s = null;
//            int now = Integer.parseInt(tempMap.get("now").toString());
//            int page = Integer.parseInt(tempMap.get("page").toString());
//            log.info("now:{}, page: {}",now,page);
//            List<Map<String, String>> list = (List) tempMap.get("data");
//            log.info("list: {}",list);
//        }
//    }
//
//    @Value("${quark.config.cookie}")
//    private String cookie;
//    @Test
//    public void quarkTest() throws IOException, InterruptedException {
//        String string = quark.saveAndShare("https://pan.quark.cn/s/74c068e8273e", "44b90aa662e34f3c8bb9df2314c1e862", "测试标题");
////        ArrayList<String> list = new ArrayList<>();
////        list.add("21e864bdc8174c0a9bee13f01a957910");
////        String share = quark.share(list, "测试标题");
//        log.info("分享链接 {}",string);
//    }
//
//    @Test
//    public void magicTest() {
//        Map<String, MagicRegexConfig.RegexRule> rules = magicRegexConfig.getRules();
//        Set<String> strings = rules.keySet();
//        for (String string : strings) {
//            log.info("string:{}",string);
//        }
//        String $AD = magicRegexConfig.getRules().get("AD").getPattern();
//        log.info("$AD:{}",$AD);
//    }
//
//    @Autowired
//    private AsyncService asyncService;
//    @Test
//    public void factTest() throws IOException {
//        List<JSONObject> list = asyncService.lsDir("https://drive-m.quark.cn",cookie,"15b78521b56b4bd19968c202e3ae673b");
//        String reAD = magicRegexConfig.getRules().get("AD").getPattern();
//        String replaceAD = magicRegexConfig.getRules().get("AD").getReplace();
//        for (JSONObject o : list) {
//            String fileName = o.getString("file_name");
//            String fid = o.getString("fid");
//            if (ReUtil.isMatch(reAD,fileName)) {
//                // 匹配含有广告词的文件
//                if (ReUtil.isMatch("(?i)^[\\w\\u4e00-\\u9fa5]+\\.(png|jpeg|jpg|docx|xlsx)$",fileName)) {
//                    ArrayList<String> fidList = new ArrayList<>();
//                    fidList.add(fid);
//                    quark.delete(fidList);
//                    log.warn("⚠匹配到广告文件:{} , 已删除",fileName);
//                    continue;
//                }
//                String newFileName = ReUtil.replaceAll(fileName, reAD, replaceAD);
//                if (!quark.renameFile(fid,newFileName)) {
//                    log.error("❌文件重命名失败！filename: {}, newFileName: {}", fileName, newFileName);
//                    return;
//                }
//                log.warn("⚠匹配到广告词语:{} , 已替换为:{}",fileName,newFileName);
//            }
//        }
//    }
//
//    @Autowired
//    private OkhttpUtil okhttpUtil;
//    @Test
//    public void shareTest() throws IOException {
//        String taskId = "50cd8a6f4651485fbe3b778980ef9310";
//        String url = "https://drive-pc.quark.cn/1/clouddrive/task?pr=ucpro&fr=pc&uc_param_str&retry_index=0&task_id=" + taskId;
//        JSONObject jsonObject = okhttpUtil.getJsonObject(url, quark.header(), okhttpUtil.getOkHttpClient());
//        log.info("shareObj2: {}",jsonObject.toString());
//    }
//    @Test
//    public void deleteTest() throws IOException {
//        String fid = "17b6592f82604c5a8ae342783aab4603";
//        ArrayList<String> list = new ArrayList<>();
//        list.add(fid);
//        boolean delete = quark.delete(list);
//        log.info(String.valueOf(delete));
//    }
//
//    @Test
//    public void searchOnWebTest() throws IOException {
//
//        StopWatch watch = new StopWatch();
//        watch.start("watcher");
//        String name = "爱情公寓";
//        JSONObject jsonObject = okhttpUtil.getJsonObject("http://127.0.0.1:8000/" + name, quark.header(), okhttpUtil.getOkHttpClient());
//        log.info(jsonObject.toString());
//        watch.stop();
//        log.info(watch.prettyPrint());
//    }
    @Test
    public void strTest() {
        String str = "名字  链接";
        String[] split = str.split(" ");
        log.info(Arrays.toString(split));
    }

    @Test
    public void treeTest() throws IOException {
        String directory = quark.createDirectory("/test/测试");
        log.info(directory);
    }

//    @Test
//    public void QuarkTest() throws IOException {
//
//        String pwdId = "95ec6e6b727f";
//        String stoken = "1k+p2PiitLzhcUrZaK5U1iWkZzuym9Jbhe7UQgPv0ZU=";
//        String initialPdirFid = "0";
//        // 获取分享链接的 Tree
//        Tree<QuarkNode> fileTree = quarkTest.getAllFileList(pwdId, stoken, initialPdirFid);
//        Scanner sc = new Scanner(System.in);
//        log.info("请输入");
////         阻塞进程
//        String next = sc.next();
//        // 拿到本地的 Tree
////        newFileTree = quarkTest.getAllFileList("37363b37d4a14625b276fde897996a3e");
//        Tree<QuarkNode> newFileTree = quarkTest.getAllFileList(pwdId, stoken, initialPdirFid);
//        Tree<QuarkNode> diffTree = treeDiffUtil.findDiffTree(fileTree, newFileTree);
//        // 无新增内容
//        if (!diffTree.hasChild()) return;
//
//        log.info("新增内容: {}",diffTree.toString());
//
//        diffTree.walk(node -> {
//            String id = node.get("id").toString();
//            if (!"root".equals(id)) {
//                QuarkNode quarkNode = QuarkNode.fromTree(node);
//                if (quarkNode != null) {
//                    // 判断是否为目录
//                    boolean isDirectory = false;
//                    List<Tree<QuarkNode>> children = node.getChildren();
//                    if (children != null && !children.isEmpty()) {
//                        isDirectory = true;
//                    }
//
//                    // 获取完整路径和父路径
//                    String fullPath = treeDiffUtil.getFullPath(diffTree, node);
//                    String parentPath = node.getParent() != null
//                            ? treeDiffUtil.getFullPath(diffTree, node.getParent())
//                            : "/";
//
//                    if (isDirectory) {
//                        // 输出目录的完整路径
//                        log.info("dirFullPath: {}", fullPath);
//                        try {
//                            // 创建文件夹
//                            quark.createDirectory(fullPath);
//                        } catch (IOException e) {
//                            log.error("创建文件夹失败: {}",e.getMessage());
//                        }
//                    } else {
//                        // 输出文件的父路径和文件名
//                        String fileName = (String) node.get("name");
//                        log.info("fileParentPath: {}, fileName: {}", parentPath, fileName);
//                        try {
//                            String directory = quark.createDirectory(parentPath);
//                            String fid = (String) node.get("id");
//                            log.info("pFid: {} ,fid: {},shareFidToken: {}",directory, fid, (String) node.get("shareFidToken"));
//                            quark.updateSave(pwdId,
//                                    stoken,
//                                    directory,
//                                    fid,
//                                    (String) node.get("shareFidToken"));
//                        } catch (IOException e) {
//                            log.error("创建文件夹失败: {}",e.getMessage());
//                        }
//                    }
//                }
//            }
//        });
//
//    }

    @Autowired
    private UpdateListMapper updateListMapper;
    @Test
    public void sqlTest() {
//        UpdateListEntity entity = new UpdateListEntity();
//        String pwdId = "95ec6e6b727f";
//        String stoken = "UG2csltyxvq9BdafMTGBa478uRMFiAABfr60Fyd8hiY=";
//        String initialPdirFid = "0";
//        // 获取分享链接的 Tree
//        Tree<QuarkNode> fileTree = quarkTest.getAllFileList(pwdId, stoken, initialPdirFid);
//        entity.setId(1).setName("测试").setTree(fileTree).setShare("share");
//        updateListMapper.insert(entity);
        UpdateListEntity entity = updateListMapper.selectById(2);
        log.info(entity.getTree().toString());
    }

    @Autowired
    private JavaMailSender mailSender;
    @Test
    public void mailTest() {
        //创建简单的邮件发送对象
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom("admin@lin03.cn");           // 设置发件人邮箱（若配置默认邮箱则不用再设置）
        mail.setTo("1428637891@qq.com");            // 设置收件人邮箱
        mail.setSubject("测试主题");                  // 设置邮件主题
        mail.setText("测试内容"); // 设置邮件文本内容
        mail.setSentDate(new Date());                // 设置邮件发送时间
        //发送
        mailSender.send(mail);
    }
    @Autowired
    private LoginApi loginApi;
    @Test
    public void loginTest() {
        String appId = "wx_VGmVwz1dSW5aRp0k8GR9X";
//        JSONObject jsonObject = loginApi.checkOnline(appId);
//        log.info(jsonObject.toString());
        loginApi.reconnection(appId);
    }

    @Test
    public void regTest() {
        String content = "搜妻好终生福";
        String reg = "^(搜短剧|搜索|搜剧|搜)\\s*(.+)$";
        String keyword = ReUtil.findAll(reg, content ,2, new ArrayList<>()).get(0).trim();
        log.info(keyword);
    }

    @Test
    public void searchTest() throws IOException {
        CommonVO<?> commonVO = searchService.search("名侦探柯南");
        log.info(commonVO.getData().toString());
    }

    @Autowired
    private DuanjuService duanjuService;
    @Test
    public void dailyTest() throws IOException, InterruptedException {
        duanjuService.updDuanjuDaily();
    }

    @Test
    public void sqlTest1() {
        QueryWrapper<DuanjuEntity> wrapper = new QueryWrapper<>();
        wrapper.apply(true,"TO_DAYS(NOW()) - TO_DAYS(create_time) = 0");
        List<DuanjuEntity> list = duanjuMapper.selectList(wrapper);
        log.info(list.toString());
    }

    @Test
    public void notifyTest() {
        notify.sendAllGroup("1");
    }

    @Test
    public void checkByUrl() throws IOException {
        String string = quark.checkByUrl("https://pan.quark.cn/s/bf03d0b4aff6","");
        log.info(string);
        log.info(String.valueOf(StrUtil.isBlank(string)));
    }

    @Test
    public void sqlDelTest() {
        CommonVO<List<Map<String, String>>> vo = searchService.searchDuanju("北上");
        log.info(vo.getData().toString());
    }
}
