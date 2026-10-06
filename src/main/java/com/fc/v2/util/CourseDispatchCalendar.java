package com.fc.v2.util;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

import com.fc.v2.model.auto.TEdCourseTask;

/**
 * 选课催办派单口径（全校一套，只此一处）。
 *
 * <p>“到没到期”和“还剩几天”用同一把尺子：浮出时刻一律由
 * {@code 截止时刻 − 提前天数} 推出，判定一律对传入的同一时刻 {@code at} 做，
 * 浮出、派单、回写不许各算各的。截止那一刻（due_at）定后不许改，
 * 真要调只许调“还剩几天”（amount）。</p>
 *
 * <p>催办窗口就一档（定死的口径）：墙钟 02:00 起、05:00 止，
 * 02:00 算窗口内、05:00 算窗口外（{@link #WINDOW_URGE}／{@link #WINDOW_CLOSED}）。
 * 窗口外到期的条目这一轮一律不派，留到下一轮再说。</p>
 *
 * @author fuce
 * @date 2026-10-05
 */
public final class CourseDispatchCalendar {

    private CourseDispatchCalendar() {
    }

    /** 全校统一时区：窗口、轮次号都按它取 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /** 催办窗口（墙钟 02:00 起、05:00 止；02:00 算窗口内、05:00 算窗口外） */
    public static final String WINDOW_URGE = "URGE";
    /** 窗口外：一律不派，到期的留到下一轮 */
    public static final String WINDOW_CLOSED = "CLOSED";

    /** 窗口起点（墙钟 02:00，算窗口内） */
    private static final int URGE_START_HOUR = 2;
    /** 窗口止点（墙钟 05:00，算窗口外） */
    private static final int URGE_END_HOUR = 5;

    /** 一天的毫秒数（提前天数按自然日折算） */
    private static final BigDecimal DAY_MILLIS = new BigDecimal(24L * 60L * 60L * 1000L);
    /** 超三天未派出的顶到最前头：三天对应的毫秒数 */
    private static final long STALE_MILLIS = 3L * 24L * 60L * 60L * 1000L;

    // ================= 同一把尺子 =================

    /**
     * 浮出时刻 = 截止那一刻 − 提前的自然日数。判定与回写都只许用这一处算。
     */
    public static Date surfaceMoment(Date dueAt, BigDecimal amount) {
        if (dueAt == null) {
            return null;
        }
        BigDecimal days = amount == null ? BigDecimal.ZERO : amount;
        long shift = days.multiply(DAY_MILLIS).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        return new Date(dueAt.getTime() - shift);
    }

    /** 到没到期：at 已走过浮出时刻（含恰好撞上）即该浮出来 */
    public static boolean isSurfaced(Date dueAt, BigDecimal amount, Date at) {
        Date surface = surfaceMoment(dueAt, amount);
        return surface != null && at != null && !at.before(surface);
    }

    /** 截止过没过：at 晚于截止那一刻（最后一刻本身仍算期内） */
    public static boolean isPastDue(Date dueAt, Date at) {
        return dueAt != null && at != null && at.after(dueAt);
    }

    /** 超三天没催出去：at 距浮出时刻已超过三个整天 */
    public static boolean isStaleOver3Days(Date dueAt, BigDecimal amount, Date at) {
        Date surface = surfaceMoment(dueAt, amount);
        return surface != null && at != null && at.getTime() - surface.getTime() > STALE_MILLIS;
    }

    // ================= 催办窗口 =================

    /**
     * at 在不在催办窗口里：墙钟 02:00 起（算窗口内）、05:00 止（算窗口外），
     * 只认墙钟，全系统只此一处。窗口外一律不派，到期的留到下一轮。
     */
    public static String windowOf(Date at) {
        if (at == null) {
            return WINDOW_CLOSED;
        }
        int hour = at.toInstant().atZone(ZONE).getHour();
        return (hour >= URGE_START_HOUR && hour < URGE_END_HOUR) ? WINDOW_URGE : WINDOW_CLOSED;
    }

    /** 轮次号：档位 + 本轮时刻（全校同一时区），同一刻撞进来的触发点共用同一个轮次号 */
    public static String roundId(String windowCode, Date at) {
        Calendar c = Calendar.getInstance(java.util.TimeZone.getTimeZone(ZONE));
        c.setTime(at);
        return windowCode + "-" + String.format("%1$tY%1$tm%1$td%1$tH%1$tM%1$tS", c);
    }

    // ================= 谁先谁后（教务处说死的排序） =================

    /**
     * 本轮派单次序，全系统只认这一条：
     * 超三天没催出去的顶到最前头；其余按截止早的先来；再撞车按提醒号字典序；
     * 最后还撞按主键。普通提醒不许插队，一个学生候补几门也按这条走。
     */
    public static Comparator<TEdCourseTask> dispatchOrder(Date at) {
        final Date now = at;
        return new Comparator<TEdCourseTask>() {
            @Override
            public int compare(TEdCourseTask a, TEdCourseTask b) {
                boolean sa = isStaleOver3Days(a.getDueAt(), a.getAmount(), now);
                boolean sb = isStaleOver3Days(b.getDueAt(), b.getAmount(), now);
                if (sa != sb) {
                    return sa ? -1 : 1;
                }
                int c = compareNullsLast(a.getDueAt(), b.getDueAt());
                if (c != 0) {
                    return c;
                }
                c = compareNullsLast(a.getItemNo(), b.getItemNo());
                if (c != 0) {
                    return c;
                }
                return compareNullsLast(a.getId(), b.getId());
            }
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compareNullsLast(Comparable a, Comparable b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return a.compareTo(b);
    }

}
