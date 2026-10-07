package com.somepro.interfaces.rest.hwaste;

import com.somepro.application.hwaste.TransferPlanAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.hwaste.converter.TransferPlanVoConverter;
import com.somepro.interfaces.rest.hwaste.vo.PageVO;
import com.somepro.interfaces.rest.hwaste.vo.TransferPlanDetailVO;
import com.somepro.interfaces.rest.hwaste.vo.TransferPlanVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 年度转移计划接口（用户接口层）：新起 → 申报 → 批复 / 驳回 →（改后再报）/ 追加申报，以及查询。
 *
 * 状态流转类接口的计划定位参数：planId 或 planNo 传其一即可。
 */
@RestController
@RequestMapping("/api/hwaste/plan")
public class TransferPlanController {

    private final TransferPlanAppService transferPlanAppService;

    public TransferPlanController(TransferPlanAppService transferPlanAppService) {
        this.transferPlanAppService = transferPlanAppService;
    }

    /** 新起计划：落草稿；申报量不得盖过该单位该类别当前在库总量；同单位同类别同年度只准挂一份。 */
    @PostMapping("/create")
    public Mono<Result<TransferPlanVO>> create(@RequestParam(required = false) Long sourceId,
                                               @RequestParam(required = false) String categoryCode,
                                               @RequestParam(required = false) Integer planYear,
                                               @RequestParam(required = false) BigDecimal plannedWeight) {
        return transferPlanAppService.create(sourceId, categoryCode, planYear, plannedWeight)
                .map(TransferPlanVoConverter::toVo)
                .map(Result::ok);
    }

    /** 申报：草稿 → 已申报（被驳回的改了再报）；申报时顺带对着在库总量再校验一遍。 */
    @PostMapping("/submit")
    public Mono<Result<TransferPlanVO>> submit(@RequestParam(required = false) Long planId,
                                               @RequestParam(required = false) String planNo) {
        return transferPlanAppService.submit(planId, planNo)
                .map(TransferPlanVoConverter::toVo)
                .map(Result::ok);
    }

    /** 改计划：草稿或已驳回才能改申报重量，改完落回草稿；新重量不得盖过在库总量。 */
    @PostMapping("/revise")
    public Mono<Result<TransferPlanVO>> revise(@RequestParam(required = false) Long planId,
                                               @RequestParam(required = false) String planNo,
                                               @RequestParam(required = false) BigDecimal plannedWeight) {
        return transferPlanAppService.revise(planId, planNo, plannedWeight)
                .map(TransferPlanVoConverter::toVo)
                .map(Result::ok);
    }

    /** 批复：已申报 → 已批复，把批下来的重量落上；批下来的量不得多过申报量。 */
    @PostMapping("/approve")
    public Mono<Result<TransferPlanVO>> approve(@RequestParam(required = false) Long planId,
                                                @RequestParam(required = false) String planNo,
                                                @RequestParam(required = false) BigDecimal approvedWeight) {
        return transferPlanAppService.approve(planId, planNo, approvedWeight)
                .map(TransferPlanVoConverter::toVo)
                .map(Result::ok);
    }

    /** 驳回：已申报 → 已驳回，必须写明理由。 */
    @PostMapping("/reject")
    public Mono<Result<TransferPlanVO>> reject(@RequestParam(required = false) Long planId,
                                               @RequestParam(required = false) String planNo,
                                               @RequestParam(required = false) String reason) {
        return transferPlanAppService.reject(planId, planNo, reason)
                .map(TransferPlanVoConverter::toVo)
                .map(Result::ok);
    }

    /** 追加申报：已批复的计划当年可再追加一次，追加量叠进申报总量重走批复；追加未批前额度不加宽。 */
    @PostMapping("/supplement")
    public Mono<Result<TransferPlanVO>> supplement(@RequestParam(required = false) Long planId,
                                                   @RequestParam(required = false) String planNo,
                                                   @RequestParam(required = false) BigDecimal additionalWeight) {
        return transferPlanAppService.supplement(planId, planNo, additionalWeight)
                .map(TransferPlanVoConverter::toVo)
                .map(Result::ok);
    }

    /** 单查明细：planId 或 planNo 传其一；申报量、批复量、在库总量一并带出。 */
    @GetMapping("/detail")
    public Mono<Result<TransferPlanDetailVO>> detail(@RequestParam(required = false) Long planId,
                                                     @RequestParam(required = false) String planNo) {
        return transferPlanAppService.detail(planId, planNo)
                .map(TransferPlanVoConverter::toDetailVo)
                .map(Result::ok);
    }

    /** 分页查询：单位 / 类别 / 年度 / 状态均可选，都不传则分页列全，每行带计划编号。 */
    @GetMapping("/page")
    public Mono<Result<PageVO<TransferPlanVO>>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "20") int pageSize,
                                                     @RequestParam(required = false) Long sourceId,
                                                     @RequestParam(required = false) String categoryCode,
                                                     @RequestParam(required = false) Integer planYear,
                                                     @RequestParam(required = false) String status) {
        return transferPlanAppService.page(pageNum, pageSize, sourceId, categoryCode, planYear, status)
                .map(TransferPlanVoConverter::toPageVo)
                .map(Result::ok);
    }
}
