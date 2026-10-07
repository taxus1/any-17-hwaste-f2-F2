package com.somepro.interfaces.rest.hwaste.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转移计划详情对外返回对象（VO，用户接口层）—— 不可变 record。
 * 比列表行多带一个该单位该类别当前在库总量，申报量 / 批复量 / 在库量几个数能对得上。
 */
public record TransferPlanDetailVO(
        Long id,
        String planNo,
        Long sourceId,
        String categoryCode,
        Integer planYear,
        BigDecimal plannedWeight,
        BigDecimal approvedWeight,
        BigDecimal inStockWeight,
        String status,
        LocalDateTime createTime) implements Serializable {
}
