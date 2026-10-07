package com.somepro.infrastructure.persistence.hwaste.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * t_transfer_plan 表的持久化对象（PO，基础设施层）。只描述表形状，不放业务规则。
 */
@Getter
@Setter
@TableName("t_transfer_plan")
public class TransferPlanPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("plan_no")
    private String planNo;

    @TableField("source_id")
    private Long sourceId;

    @TableField("category_code")
    private String categoryCode;

    @TableField("plan_year")
    private Integer planYear;

    @TableField("planned_weight")
    private BigDecimal plannedWeight;

    @TableField("approved_weight")
    private BigDecimal approvedWeight;

    @TableField("status")
    private String status;
}
