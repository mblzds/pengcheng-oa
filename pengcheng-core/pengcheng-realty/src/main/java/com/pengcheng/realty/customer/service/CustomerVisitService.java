package com.pengcheng.realty.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.common.exception.InvalidStateTransitionException;
import com.pengcheng.realty.customer.dto.CustomerVisitDTO;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerVisit;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import com.pengcheng.realty.customer.mapper.CustomerVisitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 客户到访管理服务
 */
@Service
@RequiredArgsConstructor
public class CustomerVisitService {

    private final CustomerVisitMapper customerVisitMapper;
    private final RealtyCustomerMapper customerMapper;

    /** 客户状态：已报备 */
    private static final int STATUS_REPORTED = 1;
    /** 客户状态：已到访 */
    private static final int STATUS_VISITED = 2;

    /**
     * 录入到访数据
     * <p>
     * 校验客户状态为"已报备"或"已到访"（支持多次到访），
     * 首次到访时更新客户状态为"已到访"。
     */
    @Transactional
    public Long createVisit(CustomerVisitDTO dto) {
        validateVisitDTO(dto);

        // 查询客户
        Customer customer = customerMapper.selectById(dto.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }

        // 校验客户状态：必须为已报备或已到访（支持多次到访）
        if (customer.getStatus() != STATUS_REPORTED && customer.getStatus() != STATUS_VISITED) {
            throw new InvalidStateTransitionException("客户当前状态不允许录入到访数据，需要先完成客户报备");
        }

        // 创建到访记录
        CustomerVisit visit = CustomerVisit.builder()
                .customerId(dto.getCustomerId())
                .actualVisitTime(dto.getActualVisitTime())
                .actualVisitCount(dto.getActualVisitCount())
                .receptionist(dto.getReceptionist())
                .remark(dto.getRemark())
                .build();
        customerVisitMapper.insert(visit);

        // 首次到访时更新客户状态为"已到访"
        if (customer.getStatus() == STATUS_REPORTED) {
            customer.setStatus(STATUS_VISITED);
            customerMapper.updateById(customer);
        }

        return visit.getId();
    }

    /**
     * 查询客户的所有到访记录
     */
    public List<CustomerVisit> listVisitsByCustomerId(Long customerId) {
        LambdaQueryWrapper<CustomerVisit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerVisit::getCustomerId, customerId)
                .orderByDesc(CustomerVisit::getActualVisitTime);
        return customerVisitMapper.selectList(wrapper);
    }

    /**
     * 校验到访 DTO 必填字段
     */
    private void validateVisitDTO(CustomerVisitDTO dto) {
        if (dto.getCustomerId() == null) {
            throw new IllegalArgumentException("客户ID不能为空");
        }
        if (dto.getActualVisitTime() == null) {
            throw new IllegalArgumentException("实际到访时间不能为空");
        }
        if (dto.getActualVisitCount() == null) {
            throw new IllegalArgumentException("实际到访人数不能为空");
        }
        if (!StringUtils.hasText(dto.getReceptionist())) {
            throw new IllegalArgumentException("接待人员不能为空");
        }
    }
}
