package com.somepro.domain.hwaste.model;

/**
 * 年度转移计划状态机（纯领域枚举，落库时存 name() 字符串）。
 *
 * 流转：DRAFT → SUBMITTED → APPROVED / REJECTED；REJECTED 可改回 DRAFT 再报；
 * APPROVED 可发起追加申报回到 SUBMITTED，重新走一遍申报到批复。
 */
public enum PlanStatus {

    /** 草稿：新起的计划先落草稿，还没向监管申报。 */
    DRAFT,
    /** 已申报：已报监管，等批复。 */
    SUBMITTED,
    /** 已批复：监管批下额度，联单按批复量走。 */
    APPROVED,
    /** 已驳回：被监管驳回，可改了再报。 */
    REJECTED
}
