package com.somepro.interfaces.rest.hwaste.converter;

import com.somepro.domain.hwaste.model.TransferPlan;
import com.somepro.domain.hwaste.model.TransferPlanDetail;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.hwaste.vo.PageVO;
import com.somepro.interfaces.rest.hwaste.vo.TransferPlanDetailVO;
import com.somepro.interfaces.rest.hwaste.vo.TransferPlanVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TransferPlan（领域）→ 对外 VO 转换器（用户接口层）。
 */
public final class TransferPlanVoConverter {

    private TransferPlanVoConverter() {
    }

    public static TransferPlanVO toVo(TransferPlan domain) {
        return new TransferPlanVO(
                domain.getId(),
                domain.getPlanNo(),
                domain.getSourceId(),
                domain.getCategoryCode(),
                domain.getPlanYear(),
                domain.getPlannedWeight(),
                domain.getApprovedWeight(),
                domain.getStatus() == null ? null : domain.getStatus().name(),
                domain.getCreateTime());
    }

    public static TransferPlanDetailVO toDetailVo(TransferPlanDetail detail) {
        TransferPlan plan = detail.plan();
        return new TransferPlanDetailVO(
                plan.getId(),
                plan.getPlanNo(),
                plan.getSourceId(),
                plan.getCategoryCode(),
                plan.getPlanYear(),
                plan.getPlannedWeight(),
                plan.getApprovedWeight(),
                detail.inStockWeight(),
                plan.getStatus() == null ? null : plan.getStatus().name(),
                plan.getCreateTime());
    }

    public static PageVO<TransferPlanVO> toPageVo(PageResult<TransferPlan> page) {
        List<TransferPlanVO> content = page.content().stream()
                .map(TransferPlanVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
