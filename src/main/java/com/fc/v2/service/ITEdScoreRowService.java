package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TEdScoreRow;

/**
 * TEdScoreRow 业务层处理接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITEdScoreRowService {

    /** selectTEdScoreRowById */
    TEdScoreRow selectTEdScoreRowById(Long id);

    /** submitBatch */
    int submitBatch(String batchNo, List<TEdScoreRow> rows);

    /** listErrors */
    List<TEdScoreRow> listErrors(String batchNo);

}