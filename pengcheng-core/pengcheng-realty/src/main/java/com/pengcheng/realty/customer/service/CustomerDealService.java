package com.pengcheng.realty.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.realty.common.exception.InvalidStateTransitionException;
import com.pengcheng.realty.customer.dto.CustomerDealDTO;
import com.pengcheng.realty.customer.dto.CustomerDealUpdateDTO;
import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.entity.CustomerDeal;
import com.pengcheng.realty.customer.mapper.CustomerDealMapper;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 客户成交管理服务
 */
@Service
@RequiredArgsConstructor
public class CustomerDealService {

    private final CustomerDealMapper customerDealMapper;
    private final RealtyCustomerMapper customerMapper;

    /** 客户状态：已到访 */
    private static final int STATUS_VISITED = 2;
    /** 客户状态：已成交 */
    private static final int STATUS_DEAL = 3;

    /**
     * 录入成交数据
     * <p>
     * 校验客户状态为"已到访"，录入成交信息后更新客户状态为"已成交"。
     */
    @Transactional
    public Long createDeal(CustomerDealDTO dto) {
        validateDealDTO(dto);

        // 查询客户
        Customer customer = customerMapper.selectById(dto.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }

        // 校验客户状态：必须为已到访
        if (customer.getStatus() != STATUS_VISITED) {
            throw new InvalidStateTransitionException("客户当前状态不允许录入成交数据，需要先录入到访数据");
        }

        // 创建成交记录
        CustomerDeal deal = CustomerDeal.builder()
                .customerId(dto.getCustomerId())
                .roomNo(dto.getRoomNo())
                .dealAmount(dto.getDealAmount())
                .dealTime(dto.getDealTime())
                .signStatus(dto.getSignStatus())
                .subscribeType(dto.getSubscribeType())
                .onlineSignStatus(0)
                .filingStatus(0)
                .loanStatus(0)
                .paymentStatus(0)
                .build();
        customerDealMapper.insert(deal);

        // 更新客户状态为"已成交"
        customer.setStatus(STATUS_DEAL);
        customerMapper.updateById(customer);

        return deal.getId();
    }

    /**
     * 更新成交后续手续状态（网签、备案、贷款、回款）
     */
    @Transactional
    public void updateDeal(CustomerDealUpdateDTO dto) {
        if (dto.getDealId() == null) {
            throw new IllegalArgumentException("成交记录ID不能为空");
        }

        CustomerDeal deal = customerDealMapper.selectById(dto.getDealId());
        if (deal == null) {
            throw new IllegalArgumentException("成交记录不存在");
        }

        if (dto.getOnlineSignStatus() != null) {
            deal.setOnlineSignStatus(dto.getOnlineSignStatus());
        }
        if (dto.getFilingStatus() != null) {
            deal.setFilingStatus(dto.getFilingStatus());
        }
        if (dto.getLoanStatus() != null) {
            deal.setLoanStatus(dto.getLoanStatus());
        }
        if (dto.getPaymentStatus() != null) {
            deal.setPaymentStatus(dto.getPaymentStatus());
        }

        customerDealMapper.updateById(deal);
    }

    /**
     * 查询客户的成交记录（按成交时间倒序）。
     */
    public List<CustomerDeal> listDealsByCustomerId(Long customerId) {
        return customerDealMapper.selectList(
                new LambdaQueryWrapper<CustomerDeal>()
                        .eq(CustomerDeal::getCustomerId, customerId)
                        .orderByDesc(CustomerDeal::getDealTime)
        );
    }

    /**
     * 校验成交 DTO 必填字段
     */
    private void validateDealDTO(CustomerDealDTO dto) {
        if (dto.getCustomerId() == null) {
            throw new IllegalArgumentException("客户ID不能为空");
        }
        if (!StringUtils.hasText(dto.getRoomNo())) {
            throw new IllegalArgumentException("成交房号不能为空");
        }
        if (dto.getDealAmount() == null) {
            throw new IllegalArgumentException("成交金额不能为空");
        }
        if (dto.getDealTime() == null) {
            throw new IllegalArgumentException("成交时间不能为空");
        }
        if (dto.getSubscribeType() == null) {
            throw new IllegalArgumentException("认购类型不能为空");
        }
    }
}
