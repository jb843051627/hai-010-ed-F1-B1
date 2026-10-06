package com.fc.v2.common.quartz.task;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fc.v2.model.custom.CourseTaskRoundResult;
import com.fc.v2.service.ITEdCourseTaskService;

/**
 * 选课催办派单定时入口。quartz 里配 {@code courseTaskDispatchTask.runRound()} 即可。
 * 催办窗口就一档：墙钟 02:00 起、05:00 止（02:00 算窗口内、05:00 算窗口外）；
 * 窗口外那一轮自然空跑，到期的一条不动，留到下一轮。
 *
 * @CLASSNAME CourseTaskDispatchTask
 * @author fuce
 * @date 2026-10-05
 */
@Component("courseTaskDispatchTask")
public class CourseTaskDispatchTask {

    private static final Logger log = LoggerFactory.getLogger(CourseTaskDispatchTask.class);

    @Autowired
    private ITEdCourseTaskService edCourseTaskService;

    /**
     * 无参任务：以当前这一刻为全校同一刻跑一轮派单
     */
    public void runRound() {
        CourseTaskRoundResult result = edCourseTaskService.runRound(new Date());
        log.info("选课催办派单一轮跑完 roundId={} window={} 待派={} 已派={} 派不出去={}：{}",
                result.getRoundId(), result.getWindowCode(),
                result.getPending(), result.getDispatched(), result.getUndispatchable(),
                result.getMessage());
    }

}
