package com.lin.bot.util;

import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lin.bot.mapper.UpdateListMapper;
import com.lin.bot.model.DTO.UpdateListDTO;
import com.lin.bot.model.QuarkNode;
import com.lin.bot.model.entity.UpdateListEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * @Author Lin.
 * @Date 2025/1/23
 */
@Slf4j
@Component
public class QuarkUtil {
    private final Quark quark;
    private final TreeDiffUtil treeDiffUtil;
    private final Notify notify;
    private final UpdateListMapper updateListMapper;

    public QuarkUtil(Quark quark, TreeDiffUtil treeDiffUtil, Notify notify, UpdateListMapper updateListMapper, @Qualifier("redisTemplate") RedisTemplate redisTemplate) {
        this.quark = quark;
        this.treeDiffUtil = treeDiffUtil;
        this.notify = notify;
        this.updateListMapper = updateListMapper;
    }

    /**
     * 添加自动更新任务
     */
    public void addRegularlyList(UpdateListDTO dto,String passwd) throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, IOException {
        UpdateListEntity entity = BeanConverter.convert(dto, UpdateListEntity.class);
        String url = entity.getUrl();
        if (entity.getTree() == null) {
            // 获取当前的文件tree并存储
            entity.setTree(quark.getAllFileList(quark.getPwdId(entity.getUrl()), quark.checkByUrl(url,passwd), "0"));
        }
        QueryWrapper<UpdateListEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("url", url);
        long i = updateListMapper.selectCount(wrapper);
        if (i == 0) updateListMapper.insert(entity);
    }

    /**
     * 更新资源函数
     * 记录源链接的tree，下次更新时根据此tree寻找更新的文件
     */
    @Transactional
    public boolean updateAndSave(UpdateListEntity entity, String passwd, Tree<QuarkNode> oldTree) throws IOException {
        String url = entity.getUrl();
        String pwdId = quark.getPwdId(url);
        String stoken = quark.checkByUrl(url,passwd);
        String initialPdirFid = "0";

        Tree<QuarkNode> newFileTree = quark.getAllFileList(pwdId, stoken, initialPdirFid);
        if (newFileTree == null) {
            log.error("获取源地址列表失败 {}",url);
            notify.send("获取源地址列表失败 " + url);
            return false;
        }
        Tree<QuarkNode> diffTree = treeDiffUtil.findDiffTree(oldTree, newFileTree);
        // 无新增内容
        if (!diffTree.hasChild() || diffTree.getChildren().isEmpty()) {
            log.debug("{} 无新增内容",entity.getName());
            return false;
        }

        log.debug("新增内容: {}",diffTree);
        notify.send(diffTree.getName() + "更新新内容" + diffTree);

        diffTree.walk(node -> {
            String id = node.get("id").toString();
            if (!"root".equals(id)) {
                QuarkNode quarkNode = QuarkNode.fromTree(node);
                if (quarkNode != null) {
                    // 判断是否为目录
                    boolean isDirectory = false;
                    List<Tree<QuarkNode>> children = node.getChildren();
                    if (children != null && !children.isEmpty()) {
                        isDirectory = true;
                    }

                    // 获取完整路径和父路径
                    String fullPath = treeDiffUtil.getFullPath(diffTree, node);
                    String parentPath = node.getParent() != null
                            ? treeDiffUtil.getFullPath(diffTree, node.getParent())
                            : "/";

                    if (isDirectory) {
                        try {
                            // 创建文件夹
                            quark.createDirectory(fullPath);
                        } catch (IOException e) {
                            log.error("创建文件夹失败: {}",e.getMessage());
                        }
                    } else {
                        // 输出文件的父路径和文件名
                        String fileName = (String) node.get("name");
                        try {
                            String directory = quark.createDirectory(parentPath);
                            String fid = (String) node.get("id");
                            quark.updateSave(pwdId,
                                    stoken,
                                    directory,
                                    fid,
                                    (String) node.get("shareFidToken"));
                        } catch (IOException e) {
                            log.error("创建文件夹失败: {}",e.getMessage());
                        }
                    }
                }
            }
        });
        // 更新 Tree
        entity.setTree(newFileTree);
        updateListMapper.insertOrUpdate(entity);
        return true;
    }
}
