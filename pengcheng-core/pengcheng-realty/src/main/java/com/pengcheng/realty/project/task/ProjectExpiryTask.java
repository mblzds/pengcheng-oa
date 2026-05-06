package com.pengcheng.realty.project.task;

import com.pengcheng.message.entity.SysNotice;
import com.pengcheng.message.mapper.SysNoticeMapper;
import com.pengcheng.message.service.SysNoticeService;
import com.pengcheng.realty.project.entity.Project;
import com.pengcheng.realty.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目到期定时任务
 * <p>
 * 每日检查代理到期项目并将状态标记为"已到期"。
 * 通过 Quartz 调度，调用目标：projectExpiryTask.execute
 * <p>
 * 到期提醒通知通过日志记录，后续可集成 pengcheng-message 的 SysNoticeService 推送给相关负责人。
 */
@Slf4j
@Component("projectExpiryTask")
@RequiredArgsConstructor
public class ProjectExpiryTask {

    private final ProjectService projectService;
    private final SysNoticeMapper sysNoticeMapper;
    private final SysNoticeService sysNoticeService;

    /**
     * 执行到期检查任务
     */
    public void execute() {
        log.info("开始执行项目到期检查任务...");
        List<Project> expiredProjects = projectService.markExpiredProjects();

        if (expiredProjects.isEmpty()) {
            log.info("项目到期检查完成，无到期项目");
            return;
        }

        for (Project project : expiredProjects) {
            log.info("项目已到期并标记：id={}, name={}, agencyEndDate={}",
                    project.getId(), project.getProjectName(), project.getAgencyEndDate());
            publishExpiryNotice(project);
        }

        log.info("项目到期检查完成，共标记 {} 个到期项目", expiredProjects.size());
    }

    private void publishExpiryNotice(Project project) {
        try {
            SysNotice notice = new SysNotice();
            notice.setTitle("项目代理到期提醒：" + project.getProjectName());
            notice.setContent(buildNoticeContent(project));
            notice.setNoticeType(1);
            notice.setStatus(0);
            notice.setCreateBy(1L);
            notice.setCreateName("系统任务");
            notice.setCreateTime(LocalDateTime.now());
            notice.setDeleted(0);
            sysNoticeMapper.insert(notice);

            sysNoticeService.publish(notice.getId());
            log.info("项目到期通知已发布：projectId={}, noticeId={}", project.getId(), notice.getId());
        } catch (Exception e) {
            log.error("项目到期通知发布失败：projectId={}, error={}", project.getId(), e.getMessage(), e);
        }
    }

    private String buildNoticeContent(Project project) {
        LocalDate endDate = project.getAgencyEndDate();
        return String.format(
                "项目【%s】代理已到期。到期日期：%s。驻场联系人：%s（%s）。请尽快处理续约或下架相关流程。",
                project.getProjectName(),
                endDate == null ? "-" : endDate,
                project.getContactPerson() == null ? "-" : project.getContactPerson(),
                project.getContactPhone() == null ? "-" : project.getContactPhone()
        );
    }
}
