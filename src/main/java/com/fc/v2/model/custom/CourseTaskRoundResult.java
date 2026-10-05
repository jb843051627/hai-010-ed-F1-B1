package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TEdCourseTask;

/**
 * 一轮派单的结果。三项计数（待派/已派/挂起）与明细同出一份快照：
 * 计数不是另起的 COUNT 查询，而是从 {@link #details} 这份明细上点出来的，
 * 屏上三处数与库里明细两边对的是同一份数。
 *
 * @author fuce
 * @date 2026-10-05
 */
public class CourseTaskRoundResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 本轮轮次号（档位-时刻） */
    private String roundId;

    /** 本轮落在哪一档 DAY白天档 PEAK高峰档 CLOSED窗口外 */
    private String windowCode;

    /** 本轮用的那一刻（全校同一把尺子） */
    private Date roundAt;

    /** 本轮明细：动过的与仍待派的都在里面，计数从它点出来 */
    private List<TEdCourseTask> details = new ArrayList<TEdCourseTask>();

    /** 本轮是否没活（没有到期的提醒，属正常情形，不算故障） */
    private boolean idle;

    /** 一句话交代本轮情形（没活直说没活） */
    private String message;

    public String getRoundId() {
        return roundId;
    }

    public void setRoundId(String roundId) {
        this.roundId = roundId;
    }

    public String getWindowCode() {
        return windowCode;
    }

    public void setWindowCode(String windowCode) {
        this.windowCode = windowCode;
    }

    public Date getRoundAt() {
        return roundAt;
    }

    public void setRoundAt(Date roundAt) {
        this.roundAt = roundAt;
    }

    public List<TEdCourseTask> getDetails() {
        return details;
    }

    public void setDetails(List<TEdCourseTask> details) {
        this.details = details == null ? new ArrayList<TEdCourseTask>() : details;
    }

    public boolean isIdle() {
        return idle;
    }

    public void setIdle(boolean idle) {
        this.idle = idle;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /** 仍待派几条（只算这一轮） */
    public long getPending() {
        return countByStatus(0);
    }

    /** 本轮已派几条（只算这一轮） */
    public long getDispatched() {
        return countByStatus(1);
    }

    /** 本轮撤单几条（只算这一轮） */
    public long getCancelled() {
        return countByStatus(2);
    }

    /** 本轮挂起几条（只算这一轮） */
    public long getSuspended() {
        return countByStatus(3);
    }

    private long countByStatus(int status) {
        long n = 0;
        for (TEdCourseTask t : details) {
            if (t.getStatus() != null && t.getStatus().intValue() == status) {
                n++;
            }
        }
        return n;
    }

}
