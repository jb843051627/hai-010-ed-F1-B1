package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TEdCounselorMapper;
import com.fc.v2.mapper.auto.TEdCourseTaskMapper;
import com.fc.v2.mapper.auto.TEdStudentMapper;
import com.fc.v2.model.auto.TEdCounselor;
import com.fc.v2.model.auto.TEdCourseTask;
import com.fc.v2.model.auto.TEdStudent;
import com.fc.v2.model.custom.CourseTaskRoundResult;
import com.fc.v2.service.ITEdCourseTaskService;
import com.fc.v2.shiro.util.ShiroUtils;
import com.fc.v2.util.CourseDispatchCalendar;

/**
 * 选课催办派单 Service 业务层处理（scheduling-job 形状：周期执行）。
 *
 * <p>一轮派单的规矩：</p>
 * <ul>
 *   <li>同一把尺子：浮出、超三天、窗口、回写全用传入的同一刻，见
 *       {@link CourseDispatchCalendar}；截止那一刻定后不许改，只许调“还剩几天”。</li>
 *   <li>催办窗口就一档（定死的口径）：墙钟 02:00 起、05:00 止，02:00 算窗口内、
 *       05:00 算窗口外；窗口外到期的条目这一轮一律不派，一条不动，留到下一轮。</li>
 *   <li>条目情形只有三档：0 待派、1 已派、2 派不出去。撤单、挂起、到期没派出去的
 *       都记 2，原由写进留痕那一栏（stop_reason），不另起第四档。</li>
 *   <li>派给谁：在催越多权重越低、手头空的先接；超三天没催出去的顶到最前头。</li>
 *   <li>派出即写派发时刻并把情形从待派翻成已派，一条 SQL 同时落，缺一不算成；
 *       更新带“仍待派”条件，几个触发点撞在同一刻也只翻成一条，没抢到的本轮不计数
 *       （重跑幂等：已翻走的条目下一轮不会再当待派捞出来）。</li>
 *   <li>挂一条不拖整轮：单条出岔子记日志，其余照常往下派。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TEdCourseTaskServiceImpl implements ITEdCourseTaskService {

    private static final Logger log = LoggerFactory.getLogger(TEdCourseTaskServiceImpl.class);

    /** 条目情形：只有三档，派不出去不另起第四档 */
    private static final int ST_PENDING = 0;       // 待派
    private static final int ST_DISPATCHED = 1;    // 已派
    private static final int ST_UNDISPATCHABLE = 2; // 派不出去（撤单/挂起/到期没派出去，原由见留痕）

    /** 学生底档情形：0在籍 */
    private static final int STUDENT_ACTIVE = 0;
    /** 辅导员在岗情形：0在岗 */
    private static final int COUNSELOR_ON_DUTY = 0;

    @javax.annotation.Resource
    private TEdCourseTaskMapper edCourseTaskMapper;

    @javax.annotation.Resource
    private TEdStudentMapper edStudentMapper;

    @javax.annotation.Resource
    private TEdCounselorMapper edCounselorMapper;

    @Override
    public TEdCourseTask selectTEdCourseTaskById(Long id) {
        return this.edCourseTaskMapper.selectById(id);
    }

    /**
     * 本轮该浮出来的提醒：仍待派、到 at 这一刻已走过浮出时刻，
     * 按教务处说死的次序排好（超三天顶头、截止早的先、再按提醒号）。
     * 已派/派不出去的一律不再当待派捞出来。
     */
    @Override
    public List<TEdCourseTask> listDue(Date at) {
        List<TEdCourseTask> pending = this.edCourseTaskMapper.selectList(new QueryWrapper<TEdCourseTask>()
                .eq("status", ST_PENDING)
                .eq("del_flag", 0));
        List<TEdCourseTask> due = new ArrayList<TEdCourseTask>();
        for (TEdCourseTask r : pending) {
            if (r.getDueAt() != null && CourseDispatchCalendar.isSurfaced(r.getDueAt(), r.getAmount(), at)) {
                due.add(r);
            }
        }
        due.sort(CourseDispatchCalendar.dispatchOrder(at));
        return due;
    }

    /**
     * 以 at 为全校同一刻跑一轮：窗口外一条不动（到期的留到下一轮）；
     * 窗口内逐条来——截止已过的记派不出去留痕，办不成的记派不出去写明原由，
     * 其余派出。这一轮没有到期的就直说没活，不算故障。
     */
    @Override
    public CourseTaskRoundResult runRound(Date at) {
        Date now = at == null ? new Date() : at;
        String window = CourseDispatchCalendar.windowOf(now);
        CourseTaskRoundResult result = new CourseTaskRoundResult();
        result.setRoundAt(now);
        result.setWindowCode(window);
        result.setRoundId(CourseDispatchCalendar.roundId(window, now));

        List<TEdCourseTask> due = listDue(now);
        if (due.isEmpty()) {
            result.setIdle(true);
            result.setMessage("本轮没有到期的催办提醒，选课窗口没攒下人，属正常情形");
            return result;
        }

        if (CourseDispatchCalendar.WINDOW_CLOSED.equals(window)) {
            // 窗口外一律不派：到期的一条不动（撤单、挂起也只在窗口内落），留到下一轮
            result.setDetails(due);
            result.setMessage("窗口外（催办窗口为墙钟02:00起、05:00止，05:00算窗口外）一律不派，到期的 "
                    + due.size() + " 条留到下一轮");
            return result;
        }

        // 窗口内：辅导员名册与在催数一次点清，本轮内自增
        List<TEdCounselor> counselors = listOnDutyCounselors();
        Map<String, Long> inUrging = countInUrging();
        String operator = operator();
        List<TEdCourseTask> details = new ArrayList<TEdCourseTask>();
        int skipped = 0;
        for (TEdCourseTask r : due) {
            try {
                TEdCourseTask after;
                if (CourseDispatchCalendar.isPastDue(r.getDueAt(), now)) {
                    // 到期没派出去的：记派不出去，原由留痕
                    after = markUndispatchable(r, now, "截止已过仍未派出，到期没派出去，留痕", operator);
                } else {
                    String stopReason = suspendReasonOf(r);
                    if (stopReason == null && counselors.isEmpty()) {
                        stopReason = "辅导员整组调走，无在岗辅导员可派";
                    }
                    if (stopReason != null) {
                        after = markUndispatchable(r, now, stopReason, operator);
                    } else {
                        after = dispatchOne(r, now, window, result.getRoundId(), counselors, inUrging, operator);
                    }
                }
                if (after == null) {
                    // 同一刻被别的触发点抢先翻走：本轮不算它，免得同一条在两轮里都记一遍
                    skipped++;
                    log.warn("选课催办派单同一刻撞车，本条由先到的触发点处理 id={}", r.getId());
                } else {
                    details.add(after);
                }
            } catch (Exception e) {
                // 挂一条不拖整轮：单条出岔子记日志，情形留在待派，下一轮再来
                log.error("选课催办派单单条处理失败 id={}", r.getId(), e);
                details.add(r);
            }
        }
        result.setDetails(details);
        String message = "本轮派完：待派 " + result.getPending() + "、已派 " + result.getDispatched()
                + "、派不出去 " + result.getUndispatchable();
        if (skipped > 0) {
            message += "（另有 " + skipped + " 条同一刻被别的触发点处理，见日志）";
        }
        result.setMessage(message);
        return result;
    }

    @Override
    public int runOnce(Date at) {
        return (int) runRound(at).getDispatched();
    }

    // ================= 立条与调口径 =================

    /**
     * 立一条提醒：五样记齐，截止那一刻就此定死，情形落待派。
     */
    @Override
    public int insertTEdCourseTask(TEdCourseTask record) {
        if (record == null || record.getItemNo() == null || record.getDueAt() == null) {
            return 0;
        }
        if (record.getAmount() == null) {
            record.setAmount(BigDecimal.ZERO);
        }
        if (record.getAmount().signum() < 0) {
            return 0;
        }
        record.setStatus(ST_PENDING);
        record.setDelFlag(0);
        record.setAssignee(null);
        record.setDispatchedAt(null);
        record.setWindowCode(null);
        record.setRoundId(null);
        record.setStopReason(null);
        return this.edCourseTaskMapper.insert(record);
    }

    /**
     * 调口径：真要调只许调“还剩几天”（且须仍待派）；事由在待派/派不出去（改用途）时可改。
     * 截止那一刻与真派出的时刻一律以库里的为准，不许被覆盖。
     */
    @Override
    public int updateTEdCourseTask(TEdCourseTask record) {
        if (record == null || record.getId() == null) {
            return 0;
        }
        TEdCourseTask cur = this.edCourseTaskMapper.selectById(record.getId());
        if (cur == null || cur.getDelFlag() == null || cur.getDelFlag().intValue() == 1) {
            return 0;
        }
        Integer st = cur.getStatus() == null ? ST_PENDING : cur.getStatus();
        TEdCourseTask upd = new TEdCourseTask();
        upd.setId(cur.getId());
        // 截止与派发留痕动不得：恒以库里的为准
        upd.setDueAt(cur.getDueAt());
        upd.setDispatchedAt(cur.getDispatchedAt());
        upd.setAssignee(cur.getAssignee());
        upd.setWindowCode(cur.getWindowCode());
        upd.setRoundId(cur.getRoundId());
        upd.setStopReason(cur.getStopReason());
        upd.setStatus(cur.getStatus());
        // 还剩几天：只许在仍待派时调
        if (st.intValue() == ST_PENDING && record.getAmount() != null && record.getAmount().signum() >= 0) {
            upd.setAmount(record.getAmount());
        } else {
            upd.setAmount(cur.getAmount());
        }
        // 事由：待派可改；派不出去可改（改用途）；其余情形不动
        if (st.intValue() == ST_PENDING || st.intValue() == ST_UNDISPATCHABLE) {
            upd.setContent(record.getContent() != null ? record.getContent() : cur.getContent());
            upd.setRemark(record.getRemark() != null ? record.getRemark() : cur.getRemark());
        } else {
            upd.setContent(cur.getContent());
            upd.setRemark(cur.getRemark());
        }
        upd.setUpdateBy(operator());
        upd.setUpdateTime(new Date());
        return this.edCourseTaskMapper.update(upd, new UpdateWrapper<TEdCourseTask>()
                .eq("id", cur.getId())
                .eq("del_flag", 0));
    }

    /**
     * 就地挂起：记派不出去（2），原由写进留痕；只从待派翻，翻走的不再参与后续派单。
     */
    @Override
    public int suspendTEdCourseTask(Long id, String reason) {
        if (id == null) {
            return 0;
        }
        TEdCourseTask cur = this.edCourseTaskMapper.selectById(id);
        if (cur == null || cur.getStatus() == null || cur.getStatus().intValue() != ST_PENDING) {
            return 0;
        }
        TEdCourseTask after = markUndispatchable(cur, new Date(), reason == null ? "人工挂起，记派不出去" : reason, operator());
        if (after == null) {
            // 同一刻被别的触发点翻了：以库里的为准，已在派不出去档也算成
            after = this.edCourseTaskMapper.selectById(id);
        }
        return after != null && after.getStatus() != null && after.getStatus().intValue() == ST_UNDISPATCHABLE ? 1 : 0;
    }

    /**
     * 撤单留痕：记派不出去（2），与挂起、到期没派出去同一档；只从待派翻；
     * 截止日与真派出的时刻不动。
     */
    @Override
    public int cancelTEdCourseTask(Long id, String reason) {
        if (id == null) {
            return 0;
        }
        TEdCourseTask cur = this.edCourseTaskMapper.selectById(id);
        if (cur == null || cur.getStatus() == null || cur.getStatus().intValue() != ST_PENDING) {
            return 0;
        }
        TEdCourseTask after = markUndispatchable(cur, new Date(), reason == null ? "人工撤单，记派不出去留痕" : reason, operator());
        if (after == null) {
            // 同一刻被别的触发点翻了：以库里的为准，已在派不出去档也算成
            after = this.edCourseTaskMapper.selectById(id);
        }
        return after != null && after.getStatus() != null && after.getStatus().intValue() == ST_UNDISPATCHABLE ? 1 : 0;
    }

    // ================= 一轮里的单条动作（都带“仍待派”条件，撞车只成一条） =================

    /**
     * 派出一条：派发时刻与情形翻转同一条 SQL 落，缺一不算成；
     * 条件里钉死“仍待派”，几个触发点撞在同一刻也只派成一条。
     * 派出去的带本轮轮次号，下一轮不会再当待派捞出来。
     * 没抢到的返回 null：本轮不算它，计数与明细都不重复记。
     */
    private TEdCourseTask dispatchOne(TEdCourseTask r, Date at, String window, String roundId,
            List<TEdCounselor> counselors, Map<String, Long> inUrging, String operator) {
        String assignee = pickCounselor(counselors, inUrging);
        int rows = this.edCourseTaskMapper.update(null, new UpdateWrapper<TEdCourseTask>()
                .eq("id", r.getId())
                .eq("status", ST_PENDING)
                .eq("del_flag", 0)
                .set("status", ST_DISPATCHED)
                .set("assignee", assignee)
                .set("dispatched_at", at)
                .set("window_code", window)
                .set("round_id", roundId)
                .set("update_by", operator)
                .set("update_time", at));
        if (rows == 0) {
            // 同一刻已被别的触发点派走：本轮不算它，先前派的那次原样留着
            return null;
        }
        r.setStatus(ST_DISPATCHED);
        r.setAssignee(assignee);
        r.setDispatchedAt(at);
        r.setWindowCode(window);
        r.setRoundId(roundId);
        inUrging.put(assignee, inUrging.get(assignee) == null ? 1L : inUrging.get(assignee) + 1L);
        return r;
    }

    /**
     * 记一条派不出去：撤单、挂起、到期没派出去的就这一个档（2），原由写进留痕那一栏，
     * 三条路同一份动作（同源）。带“仍待派”条件，同一刻撞车只成一条；
     * 没抢到的返回 null：本轮不算它。截止日与真派出的时刻不动。
     */
    private TEdCourseTask markUndispatchable(TEdCourseTask r, Date at, String reason, String operator) {
        int rows = this.edCourseTaskMapper.update(null, new UpdateWrapper<TEdCourseTask>()
                .eq("id", r.getId())
                .eq("status", ST_PENDING)
                .eq("del_flag", 0)
                .set("status", ST_UNDISPATCHABLE)
                .set("stop_reason", reason)
                .set("update_by", operator)
                .set("update_time", at));
        if (rows == 0) {
            // 同一刻已被别的触发点翻走：本轮不算它
            return null;
        }
        r.setStatus(ST_UNDISPATCHABLE);
        r.setStopReason(reason);
        return r;
    }

    // ================= 派给谁 / 派不派得出去 =================

    /**
     * 派给谁：手头在催越少越先接，空的先接；一样空按工号字典序（说死，不许插队）。
     */
    private String pickCounselor(List<TEdCounselor> counselors, Map<String, Long> inUrging) {
        String best = null;
        long bestLoad = Long.MAX_VALUE;
        for (TEdCounselor c : counselors) {
            Long load = inUrging.get(c.getCounselorNo());
            long n = load == null ? 0L : load.longValue();
            if (n < bestLoad) {
                bestLoad = n;
                best = c.getCounselorNo();
            }
        }
        return best;
    }

    /**
     * 这条该不该记派不出去：学生在提醒浮出来前退学休学（底档注销）→ 写明原由。
     * 未挂底档的老数据不挡路，照常往下派。
     */
    private String suspendReasonOf(TEdCourseTask r) {
        if (r.getStudentId() == null) {
            return null;
        }
        TEdStudent stu = this.edStudentMapper.selectOne(new QueryWrapper<TEdStudent>()
                .eq("id", r.getStudentId())
                .eq("del_flag", 0));
        if (stu == null || stu.getStatus() == null || stu.getStatus().intValue() != STUDENT_ACTIVE) {
            return "提醒浮出前学生已退学休学（底档注销）";
        }
        return null;
    }

    /** 在岗辅导员名册，按工号排好（空手先接的 tie-break 说死） */
    private List<TEdCounselor> listOnDutyCounselors() {
        return this.edCounselorMapper.selectList(new QueryWrapper<TEdCounselor>()
                .eq("status", COUNSELOR_ON_DUTY)
                .eq("del_flag", 0)
                .orderByAsc("counselor_no"));
    }

    /** 各辅导员手头在催几条：已派未了的都算在催 */
    private Map<String, Long> countInUrging() {
        List<TEdCourseTask> urging = this.edCourseTaskMapper.selectList(new QueryWrapper<TEdCourseTask>()
                .select("assignee")
                .eq("status", ST_DISPATCHED)
                .eq("del_flag", 0));
        Map<String, Long> map = new HashMap<String, Long>();
        for (TEdCourseTask t : urging) {
            if (t.getAssignee() == null) {
                continue;
            }
            Long n = map.get(t.getAssignee());
            map.put(t.getAssignee(), n == null ? 1L : n + 1L);
        }
        return map;
    }

    /** 操作者：页面上来的是登录人，定时任务里没会话就记 quartz */
    private String operator() {
        try {
            return ShiroUtils.getLoginName();
        } catch (Exception e) {
            return "quartz";
        }
    }

}
