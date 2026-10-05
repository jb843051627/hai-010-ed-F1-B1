package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TEdRemakeFlow;

/**
 * TEdRemakeFlow 业务层处理接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITEdRemakeFlowService {

    /** selectTEdRemakeFlowById */
    TEdRemakeFlow selectTEdRemakeFlowById(Long id);

    /** selectTEdRemakeFlowList */
    List<TEdRemakeFlow> selectTEdRemakeFlowList(QueryWrapper<TEdRemakeFlow> queryWrapper);

    /** advance */
    TEdRemakeFlow advance(Long id, String remark);

    /** rollback */
    TEdRemakeFlow rollback(Long id, String remark);

    /** updateContent */
    boolean updateContent(Long id, String remark);

    /** remove */
    boolean remove(Long id);

}