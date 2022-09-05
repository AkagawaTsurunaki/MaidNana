package com.github.nanoyou.maidnana.service;

import com.github.nanoyou.maidnana.dao.AnnouncementDao;
import com.github.nanoyou.maidnana.dao.TemplateDao;
import com.github.nanoyou.maidnana.entity.Announcement;
import com.github.nanoyou.maidnana.entity.Body;
import com.github.nanoyou.maidnana.entity.Template;
import com.github.nanoyou.maidnana.entity.Trigger;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.TestOnly;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AnnouncementService {
    private final static AnnouncementService instance = new AnnouncementService();
    private final static AnnouncementDao dao = AnnouncementDao.getInstance();

    public static AnnouncementService getInstance() {
        return instance;
    }

    /**
     * 创建公告
     * @return 创建的公告
     */
    public Announcement create() {
        var ansment = new Announcement();
        ansment.setUuid(UUID.randomUUID());
        dao.add(ansment);
        return ansment;
    }

    /**
     * 获取公告
     * @param announcementID ID
     * @return 找到返回值, 未找到返回空
     */
    public Optional<Announcement> get(UUID announcementID) {
        return dao.get(announcementID);
    }
    /**
     * 获取公告
     * @param alias 别名
     * @return 找到返回值, 未找到返回空
     */
    public Optional<Announcement> get(String alias) {
        return dao.get(alias);
    }

    /**
     * 删除公告
     * @param announcementID 公告ID
     * @return 被删的公告
     */
    public Optional<Announcement> delete(UUID announcementID) {
        return dao.delete(announcementID);
    }

    /**
     * 增加分组
     * @param announcementID 公告ID
     * @param groupID 分组ID
     */
    public Optional<Announcement> addGroup(UUID announcementID, long groupID) {
        var ansment = get(announcementID);
        if(ansment.isPresent()){
            var groups = ansment.get().getGroups();
            if (groups.stream().noneMatch(e -> e.equals(groupID)))
                groups.add(groupID);

            ansment.get().setGroups(groups);
            dao.modify(ansment.get());
        }
        return ansment;
    }
    /**
     * 增加分组
     * @param alias 公告ID
     * @param groupID 分组ID
     */
    public Optional<Announcement> addGroup(String alias, long groupID) {
        var ansment = get(alias);
        if(ansment.isPresent()){
            var groups = ansment.get().getGroups();
            if (groups.stream().noneMatch(e -> e.equals(groupID)))
                groups.add(groupID);

            ansment.get().setGroups(groups);
            dao.modify(ansment.get());
        }
        return ansment;
    }

    /**
     * 删除分组
     * @param announcementID 公告ID
     * @param groupID 分组ID
     * @return 被删除分组的公告
     */
    public Optional<Announcement> removeGroup(UUID announcementID, long groupID) {
        var ansment = get(announcementID);
        if(ansment.isPresent()){
            var groups = ansment.get().getGroups();
            if (groups.stream().anyMatch(e -> e.equals(groupID)))
                groups.removeIf(e -> e.equals(groupID));

            ansment.get().setGroups(groups);
            dao.modify(ansment.get());
        }
        return ansment;
    }
    /**
     * 删除分组
     *
     * @param alias   别名
     * @param groupID 组id
     * @return {@link Optional}<{@link Announcement}>
     */
    public Optional<Announcement> removeGroup(String alias, long groupID) {
        var ansment = get(alias);
        if(ansment.isPresent()){
            var groups = ansment.get().getGroups();
            if (groups.stream().anyMatch(e -> e.equals(groupID)))
                groups.removeIf(e -> e.equals(groupID));

            ansment.get().setGroups(groups);
            dao.modify(ansment.get());
        }
        return ansment;
    }

    /**
     * 添加触发器
     *
     * @param announcementID 公告ID
     * @param trigger        触发器
     */
    public Optional<Announcement> addTrigger(UUID announcementID, Trigger trigger) {
        var ansment = get(announcementID);
        if(ansment.isPresent()){
            var triggers = ansment.get().getTriggers();
            if (triggers.stream().noneMatch(e -> e.equals(trigger)))
                triggers.add(trigger);

            ansment.get().setTriggers(triggers);
            dao.modify(ansment.get());
        }

        return ansment;
    }
    /**
     * 添加触发器
     *
     * @param alias 别名
     * @param trigger        触发器
     */
    public Optional<Announcement> addTrigger(String alias, Trigger trigger) {
        var ansment = get(alias);
        if(ansment.isPresent()){
            var triggers = ansment.get().getTriggers();
            if (triggers.stream().noneMatch(e -> e.equals(trigger)))
                triggers.add(trigger);

            ansment.get().setTriggers(triggers);
            dao.modify(ansment.get());
        }

        return ansment;
    }

    /**
     * 移除触发器
     *
     * @param announcementID 公告ID
     * @param triggerID      触发器ID
     */
    public Optional<Announcement> removeTrigger(UUID announcementID, UUID triggerID) {
        var ansment = get(announcementID);
        if(ansment.isPresent()){
            var triggers = ansment.get().getTriggers();
            if (triggers.stream().anyMatch(e -> e.getUuid().equals(triggerID)))
                triggers.removeIf(e->e.getUuid().equals(triggerID));

            ansment.get().setTriggers(triggers);
            dao.modify(ansment.get());
        }
        return ansment;
    }
    /** 移除触发器
     *
     * @param alias 别名
     * @param triggerID 触发器ID
     */
    public Optional<Announcement> removeTrigger(String alias, UUID triggerID) {
        var ansment = get(alias);
        if(ansment.isPresent()){
            var triggers = ansment.get().getTriggers();
            if (triggers.stream().anyMatch(e -> e.getUuid().equals(triggerID)))
                triggers.removeIf(e->e.getUuid().equals(triggerID));

            ansment.get().setTriggers(triggers);
            dao.modify(ansment.get());
        }
        return ansment;
    }

    /**
     * 设置身体
     * @param announcementID 公告ID
     * @param body 身体
     */
    public Optional<Announcement> setBody(UUID announcementID, Body body) {
        var ansment = get(announcementID);
        ansment.ifPresent(a ->{
            a.setBody(body);
            dao.modify(a);
        });
        return ansment;
    }
    /**
     * 设置身体
     * @param alias 别名
     * @param body 身体
     */
    public Optional<Announcement> setBody(String alias, Body body){
        var ansment = get(alias);
        ansment.ifPresent(a ->{
            a.setBody(body);
            dao.modify(a);
        });
        return ansment;
    }

    /**
     * 启用公告
     * @param announcementID 公告ID
     */
    public Optional<Announcement> enable(UUID announcementID) {
        var ansment = get(announcementID);
        ansment.ifPresent(a ->{
            a.setEnabled(true);
            dao.modify(a);
        });
        return ansment;
    }
    /**
     * 启用公告
     * @param alias 别名捏
     */
    public Optional<Announcement> enable(String alias) {
        var ansment = get(alias);
        ansment.ifPresent(a ->{
            a.setEnabled(true);
            dao.modify(a);
        });
        return ansment;
    }

    /**
     * 禁用公告
     * @param announcementID 公告ID
     */
    public Optional<Announcement> disable(UUID announcementID) {
        var ansment = get(announcementID);
        ansment.ifPresent(a ->{
            a.setEnabled(false);
            dao.modify(a);
        });
        return ansment;
    }
    /**
     * 禁用公告
     * @param alias 别名
     */
    public Optional<Announcement> disable(String alias) {
        var ansment = get(alias);
        ansment.ifPresent(a ->{
            a.setEnabled(false);
            dao.modify(a);
        });
        return ansment;
    }

    /**
     * 得到所有
     *
     * @return {@link List}<{@link Announcement}>
     */
    public List<Announcement> getAll(){
        return dao.getAll();
    }
}
