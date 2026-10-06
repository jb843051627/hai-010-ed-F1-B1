package com.fc.v2.util;

import java.math.BigDecimal;
import java.time.LocalTime;
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
 * <p>催办窗口是墙钟 {@code [02:00, 05:00)}（{@link #WINDOW_URGING}）：
 * 02:00 整算窗口内（含起点），05:00 整算窗口外（不含终点），其余时刻一律窗口外
 * （{@link #WINDOW_CLOSED}）。只看墙钟，不再分白天档/高峰档/节假日，
 * 端点也只许在 {@link #windowOf(Date)} 这一处判，谁都不许另写一套比较。
 * 窗口外到期的条目这一轮一律不动，留到下一轮。</p>
 *
 * @author fuce
 * @date 2026-10-06
 */
public final class CourseDispatchCalendar {

    private CourseDispatchCalendar() {
    }

    /** 全校统一时区：窗口、轮次号都按它取 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /** 催办窗口内：墙钟 02:00（含）到 05:00（不含） */
    public static final String WINDOW_URGING = "URGING";
    /** 窗口外：02:00 之前或 05:00 及以后，整段不派 */
    public static final String WINDOW_CLOSED = "CLOSED";

    /** 催办窗口起点（含）：02:00:00 */
    private static final LocalTime WINDOW_START = LocalTime.of(2, 0);
    /** 催办窗口终点（不含）：05:00:00 */
    private static final LocalTime WINDOW_END = LocalTime.of(5, 0);

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

    /** 到没到期：at 已走到浮出时刻（含恰好撞上：到期时刻==执行时刻算到期）即该浮出来 */
    public static boolean isSurfaced(Date dueAt, BigDecimal amount, Date at) {
        Date surface = surfaceMoment(dueAt, amount);
        return surface != null && at != null && !at.before(surface);
    }

    /** 截止过没过：at 晚于截止那一刻（最后一刻本身仍算期内，端点同样含等号） */
    public static boolean isPastDue(Date dueAt, Date at) {
        return dueAt != null && at != null && at.after(dueAt);
    }

    /** 超三天没催出去：at 距浮出时刻已超过三个整天 */
    public static boolean isStaleOver3Days(Date dueAt, BigDecimal amount, Date at) {
        Date surface = surfaceMoment(dueAt, amount);
        return surface != null && at != null && at.getTime() - surface.getTime() > STALE_MILLIS;
    }

    // ================= 催办窗口（端点只此一处） =================

    /**
     * at 落在线上还是线外，只看墙钟：02:00（含）到 05:00（不含）为窗口内，
     * 其余时刻（含 05:00 整）一律窗口外。起点用“不早于”（含等号），
     * 终点用“早于”（不含等号），两端口径钉死，别在别处再判一遍。
     */
    public static String windowOf(Date at) {
        if (at == null) {
            return WINDOW_CLOSED;
        }
        LocalTime t = at.toInstant().atZone(ZONE).toLocalTime();
        boolean inside = !t.isBefore(WINDOW_START) && t.isBefore(WINDOW_END);
        return inside ? WINDOW_URGING : WINDOW_CLOSED;
    }

    /** 轮次号：窗口标记 + 本轮时刻（全校同一时区），同一刻撞进来的触发点共用同一个轮次号 */
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
