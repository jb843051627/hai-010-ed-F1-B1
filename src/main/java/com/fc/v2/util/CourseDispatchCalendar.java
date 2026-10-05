package com.fc.v2.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.fc.v2.model.auto.TEdCourseTask;

/**
 * 选课催办派单口径（全校一套，只此一处）。
 *
 * <p>“到没到期”和“还剩几天”用同一把尺子：浮出时刻一律由
 * {@code 截止时刻 − 提前天数} 推出，判定一律对传入的同一时刻 {@code at} 做，
 * 浮出、派单、回写不许各算各的。截止那一刻（due_at）定后不许改，
 * 真要调只许调“还剩几天”（amount）。</p>
 *
 * <p>派单分时段：白天一档 {@link #WINDOW_DAY}（08:00–18:00），
 * 选课高峰三天单开一档 {@link #WINDOW_PEAK}（08:00–22:00，不走日常档），
 * 夜间与法定节假日整段不派（{@link #WINDOW_CLOSED}）。</p>
 *
 * @author fuce
 * @date 2026-10-05
 */
public final class CourseDispatchCalendar {

    private CourseDispatchCalendar() {
    }

    /** 全校统一时区：窗口、节假日、轮次号都按它取 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /** 白天档（按值班划的日常档） */
    public static final String WINDOW_DAY = "DAY";
    /** 选课高峰档（高峰三天单开，不走日常档） */
    public static final String WINDOW_PEAK = "PEAK";
    /** 窗口外：夜间或法定节假日，整段不派 */
    public static final String WINDOW_CLOSED = "CLOSED";

    /** 白天档起止（08:00 起，18:00 止不含） */
    private static final int DAY_START_HOUR = 8;
    private static final int DAY_END_HOUR = 18;
    /** 高峰档起止（08:00 起，22:00 止不含） */
    private static final int PEAK_START_HOUR = 8;
    private static final int PEAK_END_HOUR = 22;

    /** 一天的毫秒数（提前天数按自然日折算） */
    private static final BigDecimal DAY_MILLIS = new BigDecimal(24L * 60L * 60L * 1000L);
    /** 超三天未派出的顶到最前头：三天对应的毫秒数 */
    private static final long STALE_MILLIS = 3L * 24L * 60L * 60L * 1000L;

    /**
     * 法定节假日（整段不派）。按校历/国务院办公厅发布维护，调年份只改这里。
     */
    private static final Set<LocalDate> LEGAL_HOLIDAYS = new HashSet<LocalDate>(Arrays.asList(
            // 元旦
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 3),
            // 春节
            LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 18),
            LocalDate.of(2026, 2, 19), LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 21),
            LocalDate.of(2026, 2, 22),
            // 清明
            LocalDate.of(2026, 4, 4), LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 6),
            // 劳动节
            LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 3),
            LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5),
            // 端午
            LocalDate.of(2026, 6, 19), LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 21),
            // 中秋
            LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26), LocalDate.of(2026, 9, 27),
            // 国庆
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 3),
            LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6),
            LocalDate.of(2026, 10, 7)));

    /**
     * 选课高峰那三天（单开高峰档，不走日常档）。按教务处每学期选课安排维护。
     */
    private static final Set<LocalDate> PEAK_DAYS = new HashSet<LocalDate>(Arrays.asList(
            LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 9)));

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

    // ================= 派单分时段 =================

    /**
     * at 落在哪一档：法定节假日整段不派；高峰三天走高峰档；其余白天走日常档；夜间不派。
     */
    public static String windowOf(Date at) {
        if (at == null) {
            return WINDOW_CLOSED;
        }
        LocalDateTime t = at.toInstant().atZone(ZONE).toLocalDateTime();
        if (LEGAL_HOLIDAYS.contains(t.toLocalDate())) {
            return WINDOW_CLOSED;
        }
        int hour = t.getHour();
        if (PEAK_DAYS.contains(t.toLocalDate())) {
            return (hour >= PEAK_START_HOUR && hour < PEAK_END_HOUR) ? WINDOW_PEAK : WINDOW_CLOSED;
        }
        return (hour >= DAY_START_HOUR && hour < DAY_END_HOUR) ? WINDOW_DAY : WINDOW_CLOSED;
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
