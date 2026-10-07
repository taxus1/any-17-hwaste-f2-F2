package com.somepro.domain.hwaste.repository;

import com.somepro.domain.hwaste.model.TransferPlan;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 年度转移计划仓储端口：领域层定义，基础设施层实现。
 *
 * 新起计划的「同组合唯一 + 编号分配」与追加申报的「读状态 → 改写」要保证并发安全，
 * 由实现侧用锁与唯一约束兜底（两个人同时递同组合计划只该成一份，
 * 同一份计划几乎同时递两笔追加最后只该成一笔）。
 */
public interface TransferPlanRepository {

    /**
     * 新起计划：同一单位 + 类别 + 年度只准挂一份计划，重复申报抛业务异常。
     * 编号（TP-年份-序号）由实现侧分配。
     */
    Mono<TransferPlan> create(TransferPlan plan);

    Mono<TransferPlan> findById(Long id);

    Mono<TransferPlan> findByPlanNo(String planNo);

    /** 状态流转落库（按 id 更新）。 */
    Mono<TransferPlan> save(TransferPlan plan);

    /**
     * 追加申报：在锁里重读计划，只有已批复的才叠量并回到已申报；
     * 并发追加只成一笔，别把额度叠两回。inStockWeight 为当前在库总量，由应用层取数传入。
     */
    Mono<TransferPlan> supplement(Long planId, BigDecimal additionalWeight, BigDecimal inStockWeight);

    /** 多条件分页：单位 / 类别 / 年度 / 状态均可选，都不传则分页列全。 */
    Mono<PageResult<TransferPlan>> page(int pageNum, int pageSize, Long sourceId, String categoryCode,
                                        Integer planYear, String status);
}
