package com.somepro.infrastructure.persistence.hwaste;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.hwaste.po.TransferPlanPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * t_transfer_plan 的 MyBatis-Plus Mapper（基础设施层，阻塞 JDBC，只能在 boundedElastic 线程上调用）。
 *
 * 编号取数说明：MAX 查询故意不过滤 del_flag —— 已删除计划的编号也不许复用；
 * FOR UPDATE 走当前已提交数据，避免事务快照里读到旧的最大号。
 */
@Mapper
public interface TransferPlanMapper extends BaseMapper<TransferPlanPO> {

    @Select("SELECT MAX(plan_no) FROM t_transfer_plan WHERE plan_no LIKE CONCAT(#{prefix}, '%') FOR UPDATE")
    String maxPlanNo(@Param("prefix") String prefix);
}
