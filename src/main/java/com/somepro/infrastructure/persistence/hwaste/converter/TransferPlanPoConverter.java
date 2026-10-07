package com.somepro.infrastructure.persistence.hwaste.converter;

import com.somepro.domain.hwaste.model.PlanStatus;
import com.somepro.domain.hwaste.model.TransferPlan;
import com.somepro.infrastructure.persistence.hwaste.po.TransferPlanPO;

/**
 * TransferPlanPO（表）↔ TransferPlan（领域）转换器（基础设施层）。
 * 状态在库里存字符串，在领域里是枚举，互转在这里收口。
 */
public final class TransferPlanPoConverter {

    private TransferPlanPoConverter() {
    }

    public static TransferPlanPO toPo(TransferPlan domain) {
        TransferPlanPO po = new TransferPlanPO();
        po.setId(domain.getId());
        po.setPlanNo(domain.getPlanNo());
        po.setSourceId(domain.getSourceId());
        po.setCategoryCode(domain.getCategoryCode());
        po.setPlanYear(domain.getPlanYear());
        po.setPlannedWeight(domain.getPlannedWeight());
        po.setApprovedWeight(domain.getApprovedWeight());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static TransferPlan toDomain(TransferPlanPO po) {
        TransferPlan domain = new TransferPlan();
        domain.setId(po.getId());
        domain.setPlanNo(po.getPlanNo());
        domain.setSourceId(po.getSourceId());
        domain.setCategoryCode(po.getCategoryCode());
        domain.setPlanYear(po.getPlanYear());
        domain.setPlannedWeight(po.getPlannedWeight());
        domain.setApprovedWeight(po.getApprovedWeight());
        domain.setStatus(po.getStatus() == null ? null : PlanStatus.valueOf(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
