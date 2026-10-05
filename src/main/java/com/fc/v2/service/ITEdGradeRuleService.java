package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TEdGradeRule;

/**
 * TEdGradeRule 业务层处理接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITEdGradeRuleService {

    /** selectTEdGradeRuleById */
    TEdGradeRule selectTEdGradeRuleById(Long id);

    /** listAvailable */
    List<TEdGradeRule> listAvailable(Date at);

    /** evaluate */
    int evaluate(String ruleCode, BigDecimal input, Date at);

    /** evaluateTop */
    int evaluateTop(BigDecimal input, Date at);

    /** usable */
    boolean usable(Long id, Date at);

    /** countAvailable */
    int countAvailable(Date at);

}