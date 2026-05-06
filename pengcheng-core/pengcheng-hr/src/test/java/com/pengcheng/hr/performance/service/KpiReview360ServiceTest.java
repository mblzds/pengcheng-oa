package com.pengcheng.hr.performance.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.pengcheng.hr.performance.dto.Kpi360WeightConfig;
import com.pengcheng.hr.performance.entity.KpiPeerReview;
import com.pengcheng.hr.performance.entity.KpiReview360;
import com.pengcheng.hr.performance.entity.KpiReviewRelation;
import com.pengcheng.hr.performance.mapper.KpiPeerReviewMapper;
import com.pengcheng.hr.performance.mapper.KpiReview360Mapper;
import com.pengcheng.hr.performance.mapper.KpiReviewRelationMapper;
import com.pengcheng.system.entity.SysConfigGroup;
import com.pengcheng.system.mapper.SysUserMapper;
import com.pengcheng.system.service.SysConfigGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P1-1 360 度评估 3 处 TODO 修复验证
 */
@DisplayName("KpiReview360Service — 3 TODO 修复")
class KpiReview360ServiceTest {

    private KpiReview360Mapper reviewMapper;
    private KpiReviewRelationMapper relationMapper;
    private KpiPeerReviewMapper peerMapper;
    private SysUserMapper userMapper;
    private SysConfigGroupService configGroupService;
    private KpiReview360Service service;

    @BeforeEach
    void setUp() {
        reviewMapper = mock(KpiReview360Mapper.class);
        relationMapper = mock(KpiReviewRelationMapper.class);
        peerMapper = mock(KpiPeerReviewMapper.class);
        userMapper = mock(SysUserMapper.class);
        configGroupService = mock(SysConfigGroupService.class);
        service = new KpiReview360Service(reviewMapper, relationMapper, peerMapper,
                userMapper, configGroupService);
    }

    // ---------- TODO #1：createReviewTasks 生成四向任务 ----------

    @Test
    @DisplayName("createReviewTasks：user 有 manager+1 peer，按 4 向生成 4 条任务（自评/上级/同事/下级）")
    void createReviewTasks_fourDirections() {
        KpiReviewRelation rel = new KpiReviewRelation();
        rel.setPeriodId(1L); rel.setUserId(100L); rel.setManagerId(200L); rel.setStatus(1);
        when(relationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(rel));

        KpiPeerReview peer = new KpiPeerReview();
        peer.setPeriodId(1L); peer.setUserId(100L); peer.setPeerId(300L); peer.setStatus(1);
        when(peerMapper.selectList(any(Wrapper.class))).thenReturn(List.of(peer));

        // upsertTask 的 selectCount 返回 0 表示全新
        when(reviewMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        int n = service.createReviewTasks(1L, List.of(100L, 200L));

        // 100: 自评+上级(200)+同事(300)+没有下级 = 3
        // 200: 自评+无上级+无同事+下级(100 反查) = 2
        // 合计 5
        assertThat(n).isEqualTo(5);
        verify(reviewMapper, times(5)).insert(any(KpiReview360.class));
    }

    @Test
    @DisplayName("createReviewTasks：user 无关系 → 只生成自评 1 条，告警日志记录但不抛异常")
    void createReviewTasks_noRelations() {
        when(relationMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(peerMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(reviewMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        int n = service.createReviewTasks(1L, List.of(100L));
        assertThat(n).isEqualTo(1);
    }

    @Test
    @DisplayName("createReviewTasks：重复调用 → upsertTask 去重，不重复 insert")
    void createReviewTasks_idempotent() {
        when(relationMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(peerMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        // selectCount 返回 1 表示已存在
        when(reviewMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        int n = service.createReviewTasks(1L, List.of(100L));
        assertThat(n).isZero();
        verify(reviewMapper, never()).insert(any());
    }

    // ---------- TODO #2：权重配置读写 ----------

    @Test
    @DisplayName("getWeightConfig：配置缺失 → 返回默认 0.1/0.4/0.3/0.2")
    void getWeightConfig_defaultWhenMissing() {
        when(configGroupService.getByGroupCode("kpi360Config")).thenReturn(null);
        Kpi360WeightConfig c = service.getWeightConfig();
        assertThat(c.getSelfWeight()).isEqualTo(0.1);
        assertThat(c.getManagerWeight()).isEqualTo(0.4);
        assertThat(c.getPeerWeight()).isEqualTo(0.3);
        assertThat(c.getSubordinateWeight()).isEqualTo(0.2);
    }

    @Test
    @DisplayName("getWeightConfig：解析 V33 初始 JSON")
    void getWeightConfig_parseV33Json() {
        SysConfigGroup g = new SysConfigGroup();
        g.setGroupCode("kpi360Config");
        g.setConfigValue("{\"weights\":{\"self\":0.2,\"manager\":0.5,\"peer\":0.2,\"subordinate\":0.1},\"minReviewers\":5,\"anonymous\":false}");
        when(configGroupService.getByGroupCode("kpi360Config")).thenReturn(g);

        Kpi360WeightConfig c = service.getWeightConfig();
        assertThat(c.getSelfWeight()).isEqualTo(0.2);
        assertThat(c.getManagerWeight()).isEqualTo(0.5);
        assertThat(c.getMinReviewers()).isEqualTo(5);
        assertThat(c.getAnonymous()).isFalse();
    }

    @Test
    @DisplayName("updateWeightConfig：合计 != 1 → 抛异常")
    void updateWeightConfig_sumMustBeOne() {
        Kpi360WeightConfig bad = Kpi360WeightConfig.builder()
                .selfWeight(0.1).managerWeight(0.1)
                .peerWeight(0.1).subordinateWeight(0.1)
                .minReviewers(3).anonymous(true).build();
        assertThatThrownBy(() -> service.updateWeightConfig(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("合计必须等于 1.0");
        verifyNoInteractions(configGroupService);
    }

    @Test
    @DisplayName("updateWeightConfig：合法 → 调 saveConfig 写 JSON")
    void updateWeightConfig_savesJson() {
        Kpi360WeightConfig ok = Kpi360WeightConfig.builder()
                .selfWeight(0.1).managerWeight(0.4)
                .peerWeight(0.3).subordinateWeight(0.2)
                .minReviewers(3).anonymous(true).build();

        service.updateWeightConfig(ok);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(configGroupService).saveConfig(eq("kpi360Config"), json.capture());
        assertThat(json.getValue()).contains("\"self\":0.1")
                .contains("\"manager\":0.4")
                .contains("\"peer\":0.3")
                .contains("\"subordinate\":0.2")
                .contains("\"anonymous\":true");
    }

    // ---------- TODO #3：getCurrentUserId 从 StpUtil 取 ----------

    @Test
    @DisplayName("getCurrentUserId：未登录 → 返回 null（不再硬编码 1L）")
    void getCurrentUserId_nullWhenNotLoggedIn() {
        // 单测环境下 Sa-Token 没有 session，StpUtil.isLogin() == false 或抛异常被吞
        Long id = service.getCurrentUserId();
        assertThat(id).isNull();
    }
}
