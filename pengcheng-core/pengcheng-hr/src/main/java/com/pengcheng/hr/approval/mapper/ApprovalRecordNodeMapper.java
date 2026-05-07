package com.pengcheng.hr.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.hr.approval.entity.ApprovalRecordNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApprovalRecordNodeMapper extends BaseMapper<ApprovalRecordNode> {

    /**
     * 查询某用户当前可处理的待审批节点（result IS NULL 且其在 candidate_approver_ids 中）
     */
    @Select("""
            SELECT * FROM approval_record_node
            WHERE deleted = 0
              AND result IS NULL
              AND FIND_IN_SET(#{userId}, candidate_approver_ids) > 0
              AND (#{businessType} IS NULL OR business_type = #{businessType})
            ORDER BY create_time DESC
            """)
    List<ApprovalRecordNode> findPendingByApprover(@Param("userId") Long userId,
                                                   @Param("businessType") String businessType);
}
