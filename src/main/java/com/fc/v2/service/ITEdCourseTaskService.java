package com.fc.v2.service;

import java.util.Date;
import java.util.List;
import com.fc.v2.model.auto.TEdCourseTask;
import com.fc.v2.model.custom.CourseTaskRoundResult;

/**
 * TEdCourseTask 业务层处理接口
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITEdCourseTaskService {

    /** selectTEdCourseTaskById */
    TEdCourseTask selectTEdCourseTaskById(Long id);

    /** listDue：到 at 这一刻该浮出来、仍待派的提醒（按说死的次序排好） */
    List<TEdCourseTask> listDue(Date at);

    /** runRound：以 at 为全校同一刻跑一轮派单，结果里三项计数（待派/已派/派不出去）与明细同一份快照 */
    CourseTaskRoundResult runRound(Date at);

    /** runOnce：跑一轮，返回本轮已派条数 */
    int runOnce(Date at);

    /** 立一条提醒：截止那一刻就此定死，情形落待派 */
    int insertTEdCourseTask(TEdCourseTask record);

    /** 调口径：只许调“还剩几天”（且须仍待派）与事由；截止与真派出的时刻动不得 */
    int updateTEdCourseTask(TEdCourseTask record);

    /** 就地挂起：记派不出去（2），原由写进留痕，翻走的不再参与后续派单 */
    int suspendTEdCourseTask(Long id, String reason);

    /** 撤单留痕：记派不出去（2），与挂起、到期没派出去同一档 */
    int cancelTEdCourseTask(Long id, String reason);

}
