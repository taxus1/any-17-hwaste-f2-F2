package com.somepro.interfaces.rest.hwaste.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转移计划对外返回对象（VO，用户接口层）—— 不可变 record。
 * 只暴露对外字段，delFlag / createBy / updateBy / updateTime 不进 API 契约。
 */
public record TransferPlanVO(
        Long id,
        String planNo,
        Long sourceId,
        String categoryCode,
        Integer planYear,
        BigDecimal plannedWeight,
        BigDecimal approvedWeight,
        String status,
        LocalDateTime createTime) implements Serializable {
}
