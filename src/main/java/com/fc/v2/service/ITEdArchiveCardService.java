package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TEdArchiveCard;

/**
 * TEdArchiveCard 业务层处理接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITEdArchiveCardService {

    /** selectTEdArchiveCardById */
    TEdArchiveCard selectTEdArchiveCardById(Long id);

    /** selectTEdArchiveCardList */
    List<TEdArchiveCard> selectTEdArchiveCardList(Wrapper<TEdArchiveCard> queryWrapper);

    /** insertTEdArchiveCard */
    int insertTEdArchiveCard(TEdArchiveCard record);

    /** updateTEdArchiveCard */
    int updateTEdArchiveCard(TEdArchiveCard record);

    /** advanceTEdArchiveCard */
    int advanceTEdArchiveCard(Long id);

    /** revertTEdArchiveCard */
    int revertTEdArchiveCard(Long id);

    /** deleteTEdArchiveCardByIds */
    int deleteTEdArchiveCardByIds(String ids);

    /** deleteTEdArchiveCardById */
    int deleteTEdArchiveCardById(Long id);

}