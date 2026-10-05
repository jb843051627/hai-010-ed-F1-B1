package com.fc.v2.service;

import com.fc.v2.model.auto.TEdAdjustBill;

/**
 * TEdAdjustBill 业务层处理接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITEdAdjustBillService {

    /** selectTEdAdjustBillById */
    TEdAdjustBill selectTEdAdjustBillById(Long id);

    /** approve */
    TEdAdjustBill approve(Long id, String approver, String comment);

    /** reject */
    TEdAdjustBill reject(Long id, String approver, String comment);

    /** rollback */
    TEdAdjustBill rollback(Long id, String comment);

}