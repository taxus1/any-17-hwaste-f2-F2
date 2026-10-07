package com.somepro.domain.hwaste.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 年度转移计划（聚合根，纯领域，无框架注解）。
 *
 * 业务规则集中在这里：
 * - 一份计划盯一家产废单位 + 一类危废 + 一个年度；同组合只准挂一份（由仓储层挡重）。
 * - 申报的量不能凭空写：申报重量不得盖过该单位该类别当前在库总量，创建与申报时都对着在库数校验。
 * - 状态机：DRAFT → SUBMITTED → APPROVED / REJECTED；REJECTED 可改回 DRAFT 再报；
 *   APPROVED 只允许走追加申报回到 SUBMITTED，别的方向一律不动。
 * - 批复量不得多过申报量；追加量叠进申报总量，追加未批下来之前批复量（额度）保持原样。
 */
@Getter
@Setter
public class TransferPlan extends BaseEntity {

    private Long id;

    /** 计划编号，全局唯一，形如 TP-2026-0001（由仓储层分配）。 */
    private String planNo;

    private Long sourceId;

    private String categoryCode;

    /** 计划年度，如 2026。 */
    private Integer planYear;

    /** 申报转移重量（千克）；追加时累计叠加。 */
    private BigDecimal plannedWeight;

    /** 批复转移重量（千克）；监管批下来的额度，追加在途期间不加宽。 */
    private BigDecimal approvedWeight;

    private PlanStatus status;

    /**
     * 工厂方法：新起一份计划（落草稿）。inStockWeight 为该单位该类别当前在库总量，
     * 由应用层按在库批次合计取数后传入；申报重量不得盖过它。
     */
    public static TransferPlan create(Long sourceId, String categoryCode, Integer planYear,
                                      BigDecimal plannedWeight, BigDecimal inStockWeight) {
        if (sourceId == null) {
            throw new BizException("产废单位不能为空");
        }
        if (categoryCode == null || categoryCode.isBlank()) {
            throw new BizException("危废类别不能为空");
        }
        if (planYear == null || planYear < 2000 || planYear > 2100) {
            throw new BizException("计划年度不合法");
        }
        requirePositive(plannedWeight, "申报重量必须大于 0");
        requireWithinStock(plannedWeight, inStockWeight, "申报重量不能超过该单位该类别当前在库总量");
        TransferPlan plan = new TransferPlan();
        plan.setSourceId(sourceId);
        plan.setCategoryCode(categoryCode.trim());
        plan.setPlanYear(planYear);
        plan.setPlannedWeight(plannedWeight);
        plan.setApprovedWeight(BigDecimal.ZERO);
        plan.setStatus(PlanStatus.DRAFT);
        return plan;
    }

    /**
     * 申报：草稿 → 已申报；被驳回的计划改了之后也可直接再报。
     * 申报时顺带再对一遍在库总量，没那么多货别报那么多。
     */
    public void submit(BigDecimal inStockWeight) {
        require(status == PlanStatus.DRAFT || status == PlanStatus.REJECTED,
                "只有草稿或已驳回的计划才能申报");
        requireWithinStock(plannedWeight, inStockWeight, "申报重量不能超过该单位该类别当前在库总量");
        this.status = PlanStatus.SUBMITTED;
    }

    /**
     * 改计划：只有草稿或已驳回的计划能改申报重量；已驳回的改完回到草稿，可重新申报。
     * 新重量同样不得盖过当前在库总量。
     */
    public void revise(BigDecimal newPlannedWeight, BigDecimal inStockWeight) {
        require(status == PlanStatus.DRAFT || status == PlanStatus.REJECTED,
                "只有草稿或已驳回的计划才能修改");
        requirePositive(newPlannedWeight, "申报重量必须大于 0");
        requireWithinStock(newPlannedWeight, inStockWeight, "申报重量不能超过该单位该类别当前在库总量");
        this.plannedWeight = newPlannedWeight;
        this.status = PlanStatus.DRAFT;
    }

    /** 批复：已申报 → 已批复，把批下来的重量落上；批下来的量不得多过申报量。 */
    public void approve(BigDecimal approved) {
        require(status == PlanStatus.SUBMITTED, "只有已申报的计划才能批复");
        requirePositive(approved, "批复重量必须大于 0");
        if (approved.compareTo(plannedWeight) > 0) {
            throw new BizException("批复重量不能超过申报重量");
        }
        this.approvedWeight = approved;
        this.status = PlanStatus.APPROVED;
    }

    /** 驳回：已申报 → 已驳回，必须写明理由。 */
    public void reject(String reason) {
        require(status == PlanStatus.SUBMITTED, "只有已申报的计划才能驳回");
        if (reason == null || reason.isBlank()) {
            throw new BizException("驳回必须写明理由");
        }
        this.status = PlanStatus.REJECTED;
    }

    /**
     * 追加申报：已批复的计划额度不够时，当年可再发起一次追加。
     * 追加量叠到申报总量上，计划回到已申报重走批复；批复量（额度）保持原样，
     * 追加没批下来之前额度不加宽。累计申报量不得盖过当前在库总量。
     */
    public void applySupplement(BigDecimal additionalWeight, BigDecimal inStockWeight) {
        require(status == PlanStatus.APPROVED, "只有已批复的计划才能发起追加申报");
        requirePositive(additionalWeight, "追加重量必须大于 0");
        BigDecimal cumulative = plannedWeight.add(additionalWeight);
        requireWithinStock(cumulative, inStockWeight, "累计申报重量不能超过该单位该类别当前在库总量");
        this.plannedWeight = cumulative;
        this.status = PlanStatus.SUBMITTED;
    }

    private static void requirePositive(BigDecimal weight, String message) {
        if (weight == null || weight.signum() <= 0) {
            throw new BizException(message);
        }
    }

    private static void requireWithinStock(BigDecimal weight, BigDecimal inStockWeight, String message) {
        BigDecimal stock = inStockWeight == null ? BigDecimal.ZERO : inStockWeight;
        if (weight.compareTo(stock) > 0) {
            throw new BizException(message);
        }
    }

    private static void require(boolean ok, String message) {
        if (!ok) {
            throw new BizException(message);
        }
    }
}
