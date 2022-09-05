package com.github.nanoyou.maidnana.controller;

import com.github.nanoyou.maidnana.constant.Usage;
import com.github.nanoyou.maidnana.entity.Announcement;
import com.github.nanoyou.maidnana.entity.Body;
import com.github.nanoyou.maidnana.entity.PlainBody;
import com.github.nanoyou.maidnana.entity.Template;
import com.github.nanoyou.maidnana.entity.*;
import com.github.nanoyou.maidnana.service.AnnouncementService;
import com.github.nanoyou.maidnana.service.TemplateService;
import net.mamoe.mirai.event.events.FriendMessageEvent;

import java.util.*;
import java.util.stream.Collectors;

public class AnnouncementController {
    private static final AnnouncementController instance = new AnnouncementController();

    private final Map<Long, UUID> selectedAnnouncement = new HashMap<>();

    public static AnnouncementController getInstance() {
        return instance;
    }

    /**
     * 通过 UUID 或别名自动获取公告
     *
     * @param uuidOrAlias UUID 或别名
     * @return 公告
     */
    private Optional<Announcement> getAnnouncement(String uuidOrAlias) {
        try {
            var uuid = UUID.fromString(uuidOrAlias);
            return AnnouncementService.getInstance().get(uuid);
        } catch (IllegalArgumentException e) {
            return AnnouncementService.getInstance().get(uuidOrAlias);
        }
    }

    /**
     * 通过 UUID 或别名自动获取模板
     *
     * @param uuidOrAlias UUID 或别名
     * @return 模板
     */
    private Optional<Template> getTemplate(String uuidOrAlias) {
        try {
            var uuid = UUID.fromString(uuidOrAlias);
            return TemplateService.getInstance().get(uuid);
        } catch (IllegalArgumentException e) {
            return TemplateService.getInstance().get(uuidOrAlias);
        }
    }

    /**
     * 格式化公告, 将公告内容格式化为文本
     *
     * @param announcement 公告
     * @return 格式化后的文本
     */
    private String formatAnnouncement(Announcement announcement) {
        var sb = new StringBuilder();
        if (announcement.getAlias() == null) {
            sb.append(announcement.getUuid());
        } else {
            sb.append(announcement.getAlias());
            sb.append('(');
            sb.append(announcement.getUuid());
            sb.append(')');
        }
        sb.append('\n');
        sb.append("启用: ");
        sb.append(announcement.isEnabled() ? "✔" : "✖");
        sb.append('\n');
        sb.append("群: ");
        sb.append(announcement.getGroups().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ")));
        sb.append('\n');
        sb.append("触发器列表:\n");
        sb.append(announcement.getTriggers().stream()
                .map(trigger -> trigger.getCron() + "(" + trigger.getUuid() + ")")
                .collect(Collectors.joining("\n")));
        sb.append('\n');
        sb.append("公告体:\n");
        if (announcement.getBody() == null) {
            sb.append("[未设置]");
        } else if (announcement.getBody() instanceof PlainBody) {
            sb.append(announcement.getBody().getBodyString());
        } else {
            var body = (TemplateBody) announcement.getBody();
            TemplateService.getInstance().get(body.getTemplateID()).ifPresentOrElse(
                    template -> sb.append(template.getTemplate()),
                    () -> sb.append("模板被删除")
            );
            sb.append("\n变量列表:\n");
            body.getVar().forEach((k, v) -> {
                sb.append(k);
                sb.append(" = ");
                sb.append(v);
                sb.append('\n');
            });

        }

        return sb.toString();
    }

    /**
     * 获取该用户选择的公告, 若未选择或公告失效(已删除)会自动回复消息, 直接 return 即可<br />
     * 例:<br />
     * ...<br />
     * var announcement = getSelectedAnnouncement(event);<br />
     * if (announcement.isEmpty()) {<br />
     * &nbsp;&nbsp;&nbsp;&nbsp;return;<br />
     * }<br />
     * ...
     *
     * @param event 事件
     * @return 如果获取到返回公告, 已失效/未选择返回空
     */
    private Optional<Announcement> getSelectedAnnouncement(FriendMessageEvent event) {
        var ann = selectedAnnouncement.get(event.getSender().getId());
        if (ann == null) {
            event.getSender().sendMessage("请先选择公告");
            return Optional.empty();
        }
        var optAnn = AnnouncementService.getInstance().get(ann);
        if (optAnn.isEmpty()) {
            event.getSender().sendMessage("公告已被删除");
        }
        return optAnn;
    }

    /**
     * 新建一个公告
     *
     * @param event 好友信息事件
     */
    public void newAnnouncement(FriendMessageEvent event) {
        if (!event.getMessage().contentToString().startsWith("新建公告")) {
            return;
        }

        var line = event.getMessage().contentToString().split(" ");

        if (line.length < 1) {
            event.getSender().sendMessage("命令格式错误, 用法:\n" + Usage.NEW_ANNOUNCEMENT);
            return;
        }

        Announcement a;
        if (line.length == 1) {
            a = AnnouncementService.getInstance().create();
            event.getSender().sendMessage("新建了一个公告\nUUID=" + a.getUuid());
        } else {
            var optAnn = AnnouncementService.getInstance().create(line[1]);
            if (optAnn.isEmpty()) {
                event.getSender().sendMessage("公告别名已存在");
                return;
            }
            if ("".equals(line[1])) {
                event.getSender().sendMessage("别名不能为空");
                return;
            }
            a = optAnn.get();
            event.getSender().sendMessage("新建了一个公告\nUUID=" + a.getUuid() + "\n别名为“" + a.getAlias() + "”");
        }

        selectedAnnouncement.put(event.getSender().getId(), a.getUuid());

    }

    /**
     * 选择已有公告
     *
     * @param event 好友信息事件
     */
    public void selectAnnouncement(FriendMessageEvent event) {
        if (!event.getMessage().contentToString().startsWith("选择公告")) {
            return;
        }
        String[] line = event.getMessage().contentToString().split(" ");
        if (line.length < 2) {
            event.getSender().sendMessage("命令格式错误, 用法:\n" + Usage.SELECT_ANNOUNCEMENT);
            return;
        }
        getAnnouncement(line[1]).ifPresentOrElse(
                a -> {
                    selectedAnnouncement.put(event.getSender().getId(), a.getUuid());
                    if (a.getAlias() != null) {
                        event.getSender().sendMessage("选择公告: " + a.getAlias() + "(" + a.getUuid().toString() + ")");
                    } else {
                        event.getSender().sendMessage("选择公告: " + a.getUuid().toString());
                    }
                },
                () -> event.getSender().sendMessage("未找到公告")
        );
    }

    /**
     * 删除指定的公告。此方法会遍历所有选中的公告。
     *
     * @param event 好友信息事件
     */
    public void deleteAnnouncement(FriendMessageEvent event) {

        if (!event.getMessage().contentToString().startsWith("删除公告")) {
            return;
        }

        var optAnn = getSelectedAnnouncement(event);
        if (optAnn.isEmpty()) {
            return;
        }

        AnnouncementService.getInstance().delete(optAnn.get().getUuid());

        event.getSender().sendMessage("已删除选定公告");
    }

    /**
     * 查看公告列表
     *
     * @param event 好友信息事件
     */
    public void listAnnouncements(FriendMessageEvent event) {

        if (!event.getMessage().contentToString().startsWith("公告列表")) {
            return;
        }

        var la = AnnouncementService.getInstance().getAll();

        event.getSender().sendMessage("共有 " + la.size() + " 条公告");
        la.forEach(ann -> event.getSender().sendMessage(formatAnnouncement(ann)));

    }

    /**
     * 设置指定群为公告接收方
     *
     * @param event 好友信息事件
     */
    public void setGroupAnnouncement(FriendMessageEvent event) {
        if (!event.getMessage().contentToString().startsWith("设置群")) {
            return;
        }
        String[] line = event.getMessage().contentToString().split(" ");
        if (line.length < 2) {
            event.getSender().sendMessage("命令格式错误, 用法:\n" + Usage.SET_GROUP);
            return;
        }

        List<Long> groupIds = new ArrayList<>();

        try {
            for (int i = 1; i < line.length; i++) {
                groupIds.add(Long.valueOf(line[i]));
            }
        } catch (Exception e) {
            event.getSender().sendMessage("命令格式错误, 用法:\n" + Usage.SET_GROUP);
            return;
        }

        getSelectedAnnouncement(event).ifPresent(
                a -> {
                    groupIds.forEach(groupId -> AnnouncementService.getInstance().addGroup(a.getUuid(), groupId));
                    event.getSender().sendMessage("设置群成功");
                }
        );
    }

    /**
     * 取消指定群为公告接收方
     *
     * @param event 好友信息事件
     */
    public void unsetGroupAnnouncement(FriendMessageEvent event) {
        if (!event.getMessage().contentToString().startsWith("取消群")) {
            return;
        }
        String[] line = event.getMessage().contentToString().split(" ");
        if (line.length < 2) {
            event.getSender().sendMessage("命令格式错误, 用法:\n" + Usage.UNSET_GROUP);
            return;
        }

        List<Long> groupIds = new ArrayList<>();

        int i = 1;
        try {
            for (; i < line.length; i++) {
                groupIds.add(Long.valueOf(line[i]));
            }
        } catch (Exception e) {
            event.getSender().sendMessage("命令解析失败：\n“" + line[i] + "”不是一个正确的群号");
            return;
        }

        getSelectedAnnouncement(event).ifPresent(
                a -> groupIds.forEach(groupId -> AnnouncementService.getInstance().removeGroup(a.getUuid(), groupId).ifPresentOrElse(
                        ann -> event.getSender().sendMessage("取消群 " + groupId + " 成功"),
                        () -> event.getSender().sendMessage("未找到群 " + groupId)
                ))
        );
    }

    /**
     * 设置指定公告的纯文本体
     *
     * @param event 好友信息事件
     */
    public void setPlainBody(FriendMessageEvent event) {

        if (!event.getMessage().contentToString().startsWith("纯文本公告")) {
            return;
        }

        var pb = new PlainBody();
        var lines = event.getMessage().contentToString().split("\n", 2);
        if (lines.length < 2) {
            event.getSender().sendMessage("命令格式错误, 用法:\n" + Usage.SET_PLAIN_BODY);
            return;
        }
        var content = lines[1];
        pb.setContent(content);

        getSelectedAnnouncement(event).ifPresent(
                a -> {
                    AnnouncementService.getInstance().setBody(a.getUuid(), pb);
                    event.getSender().sendMessage("纯文本公告设置成功");
                }
        );
    }

    /**
     * 设置指定公告的模板体
     *
     * @param event 好友消息事件
     */
    public void setTemplateBody(FriendMessageEvent event) {
        if (!event.getMessage().contentToString().startsWith("模板公告")) {
            return;
        }
        String[] line = event.getMessage().contentToString().split("\n");
        String[] firstLine = line[0].split(" ");

        if (firstLine.length < 2) {
            event.getSender().sendMessage("命令格式错误, 用法:\n" + Usage.SET_TEMPLATE_BODY);
            return;
        }
        var optTemplate = getTemplate(firstLine[1]);
        if (optTemplate.isEmpty()) {
            event.getSender().sendMessage("模板不存在");
            return;
        }
        var optAnn = getSelectedAnnouncement(event);
        if (optAnn.isEmpty()) {
            return;
        }

        var body = new TemplateBody();
        body.setTemplateID(optTemplate.get().getUuid());
        var vars = new HashMap<String, String>();
        Arrays.stream(line).skip(1).forEach(row -> {
            var keyAndValue = row.split("\\s*=\\s*", 2);
            if (keyAndValue.length < 2) {
                return;
            }
            vars.put(keyAndValue[0], keyAndValue[1]);
        });
        body.setVar(vars);
        AnnouncementService.getInstance()
                .setBody(optAnn.get().getUuid(), body)
                .ifPresent(ann ->
                        event.getSender().sendMessage("设置成功! 预览:\n" + ann.getBody().getBodyString())
                );


    }

    /**
     * 开启定时公告的发布
     *
     * @param event 好友信息事件
     */
    public void enableAnnouncement(FriendMessageEvent event) {
        if (!event.getMessage().contentToString().startsWith("开启公告")) {
            return;
        }

        getSelectedAnnouncement(event)
                .flatMap(a -> AnnouncementService.getInstance().enable(a.getUuid()))
                .ifPresent(r -> event.getSender().sendMessage("开启成功"));
    }


    /**
     * 暂停定时公告的发布
     *
     * @param event 好友信息事件
     */
    public void disableAnnouncement(FriendMessageEvent event) {
        if (!event.getMessage().contentToString().startsWith("禁用公告")) {
            return;
        }

        getSelectedAnnouncement(event)
                .flatMap(a -> AnnouncementService.getInstance().disable(a.getUuid()))
                .ifPresent(r -> event.getSender().sendMessage("禁用成功"));
    }

    /**
     * 新建触发器
     *
     * @param event 好友信息事件
     */
    public void newTrigger(FriendMessageEvent event) {
        if (!event.getMessage().contentToString().startsWith("新建触发器")) {
            return;
        }
        var line = event.getMessage().contentToString().split(" ", 2);
        if (line.length != 2) {
            event.getSender().sendMessage("命令格式错误, 用法:\n" + Usage.NEW_TRIGGER);
            return;
        }
        getSelectedAnnouncement(event).ifPresent(
                a -> {
                    // 新建触发器
                    var t = new Trigger();
                    t.setUuid(UUID.randomUUID());
                    t.setCron(line[1]);
                    // 加入触发器
                    AnnouncementService.getInstance().addTrigger(a.getUuid(), t).ifPresent(
                            ann -> event.getSender().sendMessage("添加触发器成功")
                    );
                }
        );
    }

    /**
     * 删除触发器
     *
     * @param event 好友消息事件
     */
    public void deleteTrigger(FriendMessageEvent event) {
        if (!event.getMessage().contentToString().startsWith("删除触发器")) {
            return;
        }
        var line = event.getMessage().contentToString().split(" ");

        getSelectedAnnouncement(event).ifPresent(
                a -> {
                    if (line.length == 1) {
                        // forEach里不能修改原数组, 需要先进行克隆
                        new ArrayList<>(a.getTriggers()).forEach(t -> AnnouncementService.getInstance().removeTrigger(a.getUuid(), t.getUuid()));
                        event.getSender().sendMessage("已删除该公告的所有触发器");
                    } else {
                        Arrays.stream(line).skip(1)
                                .forEach(trigger -> AnnouncementService.getInstance().removeTrigger(a.getUuid(), UUID.fromString(trigger)));
                        event.getSender().sendMessage("已删除该公告中的指定的触发器");
                    }
                }
        );


    }

    // TODO: Test

    /**
     * @param event
     */
    public void preview(FriendMessageEvent event) {
        if (!event.getMessage().contentToString().startsWith("预览")) {
            return;
        }
        var line = event.getMessage().contentToString().split(" ");
        if (line.length != 1) {
            event.getSender().sendMessage("命令格式错误, 用法:\n" + Usage.PREVIEW);
            return;
        }
        getSelectedAnnouncement(event).ifPresentOrElse(
                a -> event.getSender().sendMessage(a.getBody().getBodyString()),
                () -> event.getSender().sendMessage("未选择任何公告")
        );
    }

    //TODO: Test

    /**
     * @param event
     */

    public void setVariable(FriendMessageEvent event) {
        if (!event.getMessage().contentToString().startsWith("设置变量")) {
            return;
        }
        var line = event.getMessage().contentToString().split("\n");

        if (line.length < 2) {
            event.getSender().sendMessage("命令格式错误, 用法:\n" + Usage.SET_VARIABLE);
            return;
        }

        getSelectedAnnouncement(event).ifPresentOrElse(
                a -> {
                    var body = a.getBody();
                    if (!(body instanceof TemplateBody)) {
                        event.getSender().sendMessage("当前选中的公告不能作为模板");
                        return;
                    }
                    var tb = ((TemplateBody) body);
                    Arrays.stream(line).skip(1).forEach(kv -> {
                        var skv = kv.split("=");
                        tb.getVar().put(skv[0], skv[1]);
                    });
                    a.setBody(tb);
                    event.getSender().sendMessage("变量设置成功");
                },
                () -> {
                }
        );
    }

    public void unsetVariable() {

    }
}
