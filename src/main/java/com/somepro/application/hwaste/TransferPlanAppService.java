package com.somepro.application.hwaste;

import com.somepro.common.exception.BizException;
import com.somepro.domain.hwaste.model.TransferPlan;
import com.somepro.domain.hwaste.model.TransferPlanDetail;
import com.somepro.domain.hwaste.repository.TransferPlanRepository;
import com.somepro.domain.hwaste.repository.WasteCategoryRepository;
import com.somepro.domain.hwaste.repository.WasteSourceRepository;
import com.somepro.domain.hwaste.repository.WasteStockRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 年度转移计划用例编排（应用层）：新起 → 申报 → 批复 / 驳回 →（改后再报）/ 追加申报，以及查询。
 *
 * 业务规则在领域对象 {@link TransferPlan} 里，这里只做编排：
 * 校验单位与类别存在、取在库总量、加载聚合、调用领域行为、落库。
 */
@Service
public class TransferPlanAppService {

    private final TransferPlanRepository transferPlanRepository;
    private final WasteSourceRepository wasteSourceRepository;
    private final WasteCategoryRepository wasteCategoryRepository;
    private final WasteStockRepository wasteStockRepository;

    public TransferPlanAppService(TransferPlanRepository transferPlanRepository,
                                  WasteSourceRepository wasteSourceRepository,
                                  WasteCategoryRepository wasteCategoryRepository,
                                  WasteStockRepository wasteStockRepository) {
        this.transferPlanRepository = transferPlanRepository;
        this.wasteSourceRepository = wasteSourceRepository;
        this.wasteCategoryRepository = wasteCategoryRepository;
        this.wasteStockRepository = wasteStockRepository;
    }

    /**
     * 新起计划：单位 / 类别先校验存在，再取在库总量对着申报量校验，落草稿。
     * 同单位 + 同类别 + 同年度只准挂一份，重复申报由仓储层挡回。
     */
    public Mono<TransferPlan> create(Long sourceId, String categoryCode, Integer planYear,
                                     BigDecimal plannedWeight) {
        return Mono.defer(() -> {
            if (sourceId == null) {
                return Mono.error(new BizException("产废单位不能为空"));
            }
            if (categoryCode == null || categoryCode.isBlank()) {
                return Mono.error(new BizException("危废类别不能为空"));
            }
            String code = categoryCode.trim();
            return wasteSourceRepository.findById(sourceId)
                    .switchIfEmpty(Mono.error(new BizException("产废单位不存在")))
                    .then(wasteCategoryRepository.findByCode(code))
                    .switchIfEmpty(Mono.error(new BizException("危废类别不存在")))
                    .then(wasteStockRepository.sumInStock(sourceId, code))
                    .flatMap(inStock -> {
                        TransferPlan plan = TransferPlan.create(sourceId, code, planYear, plannedWeight, inStock);
                        return transferPlanRepository.create(plan);
                    });
        });
    }

    /** 申报：草稿 → 已申报（被驳回的改了再报）；申报时顺带把在库总量算出来对一遍。 */
    public Mono<TransferPlan> submit(Long planId, String planNo) {
        return load(planId, planNo).flatMap(plan ->
                inStockOf(plan).flatMap(inStock -> {
                    plan.submit(inStock);
                    return transferPlanRepository.save(plan);
                }));
    }

    /** 改计划：草稿或已驳回才能改申报重量，改完落回草稿；新重量不得盖过在库总量。 */
    public Mono<TransferPlan> revise(Long planId, String planNo, BigDecimal plannedWeight) {
        return load(planId, planNo).flatMap(plan ->
                inStockOf(plan).flatMap(inStock -> {
                    plan.revise(plannedWeight, inStock);
                    return transferPlanRepository.save(plan);
                }));
    }

    /** 批复：已申报 → 已批复，把批下来的重量落上；批下来的量不得多过申报量。 */
    public Mono<TransferPlan> approve(Long planId, String planNo, BigDecimal approvedWeight) {
        return load(planId, planNo).flatMap(plan -> {
            plan.approve(approvedWeight);
            return transferPlanRepository.save(plan);
        });
    }

    /** 驳回：已申报 → 已驳回，必须写明理由。 */
    public Mono<TransferPlan> reject(Long planId, String planNo, String reason) {
        return load(planId, planNo).flatMap(plan -> {
            plan.reject(reason);
            return transferPlanRepository.save(plan);
        });
    }

    /**
     * 追加申报：已批复的计划当年可再发起一次追加，追加量叠到申报总量上重走申报到批复。
     * 累计申报量不得盖过当前在库总量；追加没批下来之前批复量（额度）不加宽。
     * 同一份计划几乎同时递两笔追加，由仓储层串行挡回，只成一笔。
     */
    public Mono<TransferPlan> supplement(Long planId, String planNo, BigDecimal additionalWeight) {
        return load(planId, planNo).flatMap(plan ->
                inStockOf(plan).flatMap(inStock ->
                        transferPlanRepository.supplement(plan.getId(), additionalWeight, inStock)));
    }

    /** 计划详情：申报量、批复量、该单位该类别当前在库总量一并带出。 */
    public Mono<TransferPlanDetail> detail(Long planId, String planNo) {
        return load(planId, planNo).flatMap(plan ->
                inStockOf(plan).map(inStock -> new TransferPlanDetail(plan, inStock)));
    }

    /** 分页查询：单位 / 类别 / 年度 / 状态均可选，都不传则分页列全。 */
    public Mono<PageResult<TransferPlan>> page(int pageNum, int pageSize, Long sourceId, String categoryCode,
                                               Integer planYear, String status) {
        return transferPlanRepository.page(pageNum, pageSize, sourceId, categoryCode, planYear, status);
    }

    /** 该单位该类别当前在库总量。 */
    private Mono<BigDecimal> inStockOf(TransferPlan plan) {
        return wasteStockRepository.sumInStock(plan.getSourceId(), plan.getCategoryCode());
    }

    /** 按 id 或编号加载计划；两个都不传或查不到都视为业务失败。 */
    private Mono<TransferPlan> load(Long planId, String planNo) {
        Mono<TransferPlan> found;
        if (planId != null) {
            found = transferPlanRepository.findById(planId);
        } else if (planNo != null && !planNo.isBlank()) {
            found = transferPlanRepository.findByPlanNo(planNo.trim());
        } else {
            return Mono.error(new BizException("planId 或 planNo 必传其一"));
        }
        return found.switchIfEmpty(Mono.error(new BizException("转移计划不存在")));
    }
}
