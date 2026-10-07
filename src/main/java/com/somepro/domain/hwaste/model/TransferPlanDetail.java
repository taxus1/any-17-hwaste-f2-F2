package com.somepro.domain.hwaste.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 计划详情视图（领域值对象，不可变 record）：计划本体 + 该单位该类别当前在库总量。
 *
 * 看详情时申报量、批复量、在库总量一并带出，几个数能对得上。
 */
public record TransferPlanDetail(
        TransferPlan plan,
        BigDecimal inStockWeight) implements Serializable {
}
