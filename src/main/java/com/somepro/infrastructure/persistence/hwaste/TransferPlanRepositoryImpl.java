package com.somepro.infrastructure.persistence.hwaste;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.hwaste.model.TransferPlan;
import com.somepro.domain.hwaste.repository.TransferPlanRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.persistence.base.BaseBlockingRepository;
import com.somepro.infrastructure.persistence.hwaste.converter.TransferPlanPoConverter;
import com.somepro.infrastructure.persistence.hwaste.po.TransferPlanPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 年度转移计划仓储适配器（基础设施层）。
 *
 * 并发约定：新起计划的「查重 + 取号 + 插入」与追加申报的「读状态 → 叠量 → 改写」
 * 都包在同一把 TP 锁里 —— 两个人几乎同时给同一家单位同一个类别同一年度递计划，只该成一份；
 * 同一份计划几乎同时递两笔追加，后到的在锁里重读状态，看到计划已回到已申报就挡回，
 * 最后只成一笔，额度不叠两回。库表 uk_plan_no 唯一约束是最后兜底。
 */
@Repository
public class TransferPlanRepositoryImpl extends BaseBlockingRepository implements TransferPlanRepository {

    private final TransferPlanMapper transferPlanMapper;
    private final BizNoService bizNoService;

    public TransferPlanRepositoryImpl(TransferPlanMapper transferPlanMapper, BizNoService bizNoService) {
        this.transferPlanMapper = transferPlanMapper;
        this.bizNoService = bizNoService;
    }

    @Override
    public Mono<TransferPlan> create(TransferPlan plan) {
        return blocking(() -> bizNoService.inLock("TP", () -> {
            Long dup = transferPlanMapper.selectCount(Wrappers.<TransferPlanPO>lambdaQuery()
                    .eq(TransferPlanPO::getSourceId, plan.getSourceId())
                    .eq(TransferPlanPO::getCategoryCode, plan.getCategoryCode())
                    .eq(TransferPlanPO::getPlanYear, plan.getPlanYear()));
            if (dup != null && dup > 0) {
                throw new BizException("该单位该类别该年度已存在转移计划，请勿重复申报");
            }
            TransferPlanPO po = TransferPlanPoConverter.toPo(plan);
            po.setId(IdUtil.getSnowflakeNextId());
            po.setPlanNo(bizNoService.nextPlanNo());
            transferPlanMapper.insert(po);
            return TransferPlanPoConverter.toDomain(po);
        }));
    }

    @Override
    public Mono<TransferPlan> findById(Long id) {
        return blocking(() -> {
            TransferPlanPO po = transferPlanMapper.selectById(id);
            return po == null ? null : TransferPlanPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<TransferPlan> findByPlanNo(String planNo) {
        return blocking(() -> {
            TransferPlanPO po = transferPlanMapper.selectOne(Wrappers.<TransferPlanPO>lambdaQuery()
                    .eq(TransferPlanPO::getPlanNo, planNo));
            return po == null ? null : TransferPlanPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<TransferPlan> save(TransferPlan plan) {
        return blocking(() -> {
            TransferPlanPO po = TransferPlanPoConverter.toPo(plan);
            transferPlanMapper.updateById(po);
            return TransferPlanPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<TransferPlan> supplement(Long planId, BigDecimal additionalWeight, BigDecimal inStockWeight) {
        return blocking(() -> bizNoService.inLock("TP", () -> {
            TransferPlanPO po = transferPlanMapper.selectById(planId);
            if (po == null) {
                throw new BizException("转移计划不存在");
            }
            // 锁里重读状态再走领域行为：并发追加时，后到的看到计划已不在已批复态就被挡回
            TransferPlan plan = TransferPlanPoConverter.toDomain(po);
            plan.applySupplement(additionalWeight, inStockWeight);
            transferPlanMapper.updateById(TransferPlanPoConverter.toPo(plan));
            return plan;
        }));
    }

    @Override
    public Mono<PageResult<TransferPlan>> page(int pageNum, int pageSize, Long sourceId, String categoryCode,
                                               Integer planYear, String status) {
        return this.<PageResult<TransferPlan>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<TransferPlanPO> wrapper = Wrappers.<TransferPlanPO>lambdaQuery()
                        .eq(sourceId != null, TransferPlanPO::getSourceId, sourceId)
                        .eq(categoryCode != null && !categoryCode.isBlank(),
                                TransferPlanPO::getCategoryCode, categoryCode)
                        .eq(planYear != null, TransferPlanPO::getPlanYear, planYear)
                        .eq(status != null && !status.isBlank(), TransferPlanPO::getStatus, status)
                        .orderByDesc(TransferPlanPO::getId);
                List<TransferPlanPO> rows = transferPlanMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<TransferPlan> content = rows.stream()
                        .map(TransferPlanPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                PageHelper.clearPage();
            }
        });
    }
}
