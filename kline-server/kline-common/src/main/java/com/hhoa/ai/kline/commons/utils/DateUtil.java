package com.hhoa.ai.kline.commons.utils;

import cn.hutool.core.date.LocalDateTimeUtil;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalQueries;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 日期工具类
 *
 * @author shenjie
 * @date 2017/11/07
 */
@Slf4j
public class DateUtil {
    private static final Pattern TIMESTAMP_FORMAT_WITH_TIMEZONE_PATTERN =
            Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2})(.*)");

    private static final Pattern TIMESTAMP_FORMAT_PATTERN =
            Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}?.*");
    private static final int MILLIS_PER_SECOND = 1000;
    public static final int LENGTH_SECOND = 10;
    public static final int LENGTH_MILLISECOND = 13;
    public static final int LENGTH_MICROSECOND = 16;
    public static final int LENGTH_NANOSECOND = 19;
    private static final String TIME_ZONE = "GMT+8";

    private static final String STANDARD_DATETIME_FORMAT = "standardDatetimeFormatter";

    private static final String STANDARD_DATETIME_FORMAT_FOR_MILLISECOND =
            "standardDatetimeFormatterForMillisecond";

    private static final String UN_STANDARD_DATETIME_FORMAT = "unStandardDatetimeFormatter";

    private static final String T_DATETIME_FORMAT = "datetimeFormatterWithT";

    private static final String TZ_DATETIME_FORMAT = "datetimeFormatterWithTZ";

    private static final String DATE_FORMAT = "dateFormatter";

    private static final String TIME_FORMAT = "timeFormatter";

    private static final String YEAR_FORMAT = "yearFormatter";

    private static final String START_TIME = "1970-01-01";

    public static final String DATE_REGEX = "(?i)date";

    public static final String TIMESTAMP_REGEX = "(?i)timestamp";

    public static final String DATETIME_REGEX = "(?i)datetime";

    private DateUtil() {}

    public static ThreadLocal<Map<String, SimpleDateFormat>> datetimeFormatter =
            ThreadLocal.withInitial(
                    () -> {
                        TimeZone timeZone = TimeZone.getTimeZone(TIME_ZONE);

                        Map<String, SimpleDateFormat> formatterMap = new HashMap<>();
                        SimpleDateFormat standardDatetimeFormatter =
                                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        standardDatetimeFormatter.setTimeZone(timeZone);
                        formatterMap.put(STANDARD_DATETIME_FORMAT, standardDatetimeFormatter);

                        SimpleDateFormat unStandardDatetimeFormatter =
                                new SimpleDateFormat("yyyyMMddHHmmss");
                        unStandardDatetimeFormatter.setTimeZone(timeZone);
                        formatterMap.put(UN_STANDARD_DATETIME_FORMAT, unStandardDatetimeFormatter);

                        SimpleDateFormat simpleDateFormatWithT =
                                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                        simpleDateFormatWithT.setTimeZone(timeZone);
                        formatterMap.put(T_DATETIME_FORMAT, simpleDateFormatWithT);

                        SimpleDateFormat simpleDateFormatWithTZ =
                                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                        simpleDateFormatWithTZ.setTimeZone(timeZone);
                        formatterMap.put(TZ_DATETIME_FORMAT, simpleDateFormatWithTZ);

                        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
                        dateFormatter.setTimeZone(timeZone);
                        formatterMap.put(DATE_FORMAT, dateFormatter);

                        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
                        timeFormatter.setTimeZone(timeZone);
                        formatterMap.put(TIME_FORMAT, timeFormatter);

                        SimpleDateFormat yearFormatter = new SimpleDateFormat("yyyy");
                        yearFormatter.setTimeZone(timeZone);
                        formatterMap.put(YEAR_FORMAT, yearFormatter);

                        SimpleDateFormat standardDatetimeFormatterOfMillisecond =
                                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                        standardDatetimeFormatterOfMillisecond.setTimeZone(timeZone);
                        formatterMap.put(
                                STANDARD_DATETIME_FORMAT_FOR_MILLISECOND,
                                standardDatetimeFormatterOfMillisecond);

                        return formatterMap;
                    });

    /** 日期格式 */
    public interface DATE_PATTERN {
        String YYYY_MM_DD_HH = "yyyy-MM-dd HH";
        String HHMMSS = "HHmmss";
        String HH_MM_SS = "HH:mm:ss";
        String YYYYMMDD = "yyyyMMdd";
        String YYYY_MM_DD = "yyyy-MM-dd";
        String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
        String YYYYMMDDHHMMSSSSS = "yyyyMMddHHmmssSSS";
        String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
        String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";

        String dd_MMM_yyyy_HH_mm_ss_Z = "dd/MMM/yyyy:HH:mm:ss Z";
    }

    public enum DateFormat {
        yyyyMMdd(DateTimeFormatter.ofPattern(DATE_PATTERN.YYYYMMDD)),
        yyyy_MM_dd(DateTimeFormatter.ofPattern(DATE_PATTERN.YYYY_MM_DD)),
        yyyy_MM_dd_HH_mm_ss(DateTimeFormatter.ofPattern(DATE_PATTERN.YYYY_MM_DD_HH_MM_SS)),

        dd_MMM_yyyy_HH_mm_ss_Z(DateTimeFormatter.ofPattern(DATE_PATTERN.dd_MMM_yyyy_HH_mm_ss_Z)),

        HH_mm_ss(DateTimeFormatter.ofPattern(DATE_PATTERN.HH_MM_SS));

        private DateTimeFormatter formatter;

        DateFormat(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        public DateTimeFormatter getFormatter() {
            return this.formatter;
        }
    }

    public static String formatLocalDateTime(LocalDateTime localDateTime, String formatStr) {
        return localDateTime.format(DateTimeFormatter.ofPattern(formatStr));
    }

    public static String formatTimestamp(long timestamp, String formatStr) {
        LocalDateTime localDateTime =
                LocalDateTime.ofEpochSecond(timestamp / 1000, 0, ZoneOffset.ofHours(8));
        return formatLocalDateTime(localDateTime, formatStr);
    }

    /**
     * 格式化日期
     *
     * @param date
     * @param
     * @return
     */
    public static String format(Object date) {
        return format(date, DATE_PATTERN.YYYY_MM_DD);
    }

    /**
     * 格式化日期
     *
     * @param time
     * @param
     * @return
     */
    public static String formatDate(Long time) {
        return format(time, DATE_PATTERN.YYYY_MM_DD);
    }

    /**
     * 格式化日期
     *
     * @param date
     * @param pattern
     * @return
     */
    public static String format(Object date, String pattern) {
        if (date == null) {
            return null;
        }
        if (pattern == null) {
            return format(date);
        }
        return new SimpleDateFormat(pattern).format(date);
    }

    public static Timestamp convertToTimestampWithZone(String timestamp) {
        Matcher matcher = TIMESTAMP_FORMAT_WITH_TIMEZONE_PATTERN.matcher(timestamp);
        if (matcher.find()) {
            return Timestamp.valueOf(matcher.group(1));
        }
        return null;
    }

    /**
     * 获取日期
     *
     * @return
     */
    public static String getDate() {
        return format(new Date());
    }

    /**
     * 获取日期时间
     *
     * @return
     */
    public static String getDateTime() {
        return format(new Date(), DATE_PATTERN.YYYY_MM_DD_HH_MM_SS);
    }

    public static long timeToStamp(String time) {
        long stamp = 0L;
        SimpleDateFormat df0 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;

        try {
            date = df0.parse(time);
            stamp = date.getTime();
        } catch (ParseException var6) {
            var6.printStackTrace();
        }

        return stamp;
    }

    public static String addDayForTime(String time, int day) {
        SimpleDateFormat df0 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        Date date = null;

        try {
            date = df0.parse(time);
        } catch (ParseException var6) {
            var6.printStackTrace();
        }

        calendar.setTime(date);
        calendar.add(5, day);
        date = calendar.getTime();
        return df0.format(date);
    }

    /**
     * 获取日期
     *
     * @param pattern
     * @return
     */
    public static String getDateTime(String pattern) {
        return format(new Date(), pattern);
    }

    /**
     * 日期计算
     *
     * @param date
     * @param field
     * @param amount
     * @return
     */
    public static Date addDate(Date date, int field, int amount) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(field, amount);
        return calendar.getTime();
    }

    /**
     * 日期计算
     *
     * @param date
     * @param field
     * @param amount
     * @return
     */
    public static Date reduceDate(Date date, int field, int amount) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(field, amount);
        return calendar.getTime();
    }

    /**
     * 字符串转换为日期:不支持yyM[M]d[d]格式
     *
     * @param date
     * @return
     */
    public static Date stringToDate(String date) {
        if (date == null) {
            return null;
        }
        String separator = String.valueOf(date.charAt(4));
        String pattern = "yyyyMMdd";
        if (!separator.matches("\\d*")) {
            pattern = "yyyy" + separator + "MM" + separator + "dd";
            if (date.length() < 10) {
                pattern = "yyyy" + separator + "M" + separator + "d";
            }
        } else if (date.length() < 8) {
            pattern = "yyyyMd";
        }
        pattern += " HH:mm:ss.SSS";
        pattern = pattern.substring(0, Math.min(pattern.length(), date.length()));
        try {
            return new SimpleDateFormat(pattern).parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 间隔天数
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public static Integer getDayBetween(Date startDate, Date endDate) {
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);
        end.set(Calendar.HOUR_OF_DAY, 0);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);

        long n = end.getTimeInMillis() - start.getTimeInMillis();
        return (int) (n / (60 * 60 * 24 * 1000L));
    }

    /**
     * 间隔月
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public static Integer getMonthBetween(Date startDate, Date endDate) {
        if (startDate == null || endDate == null || !startDate.before(endDate)) {
            return null;
        }
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);
        int year1 = start.get(Calendar.YEAR);
        int year2 = end.get(Calendar.YEAR);
        int month1 = start.get(Calendar.MONTH);
        int month2 = end.get(Calendar.MONTH);
        int n = (year2 - year1) * 12;
        n = n + month2 - month1;
        return n;
    }

    /**
     * 根据Date类型生成yyyyMMdd格式时间戳
     *
     * @param currentTime
     * @return
     */
    public static String getStringDay(Date currentTime) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

        String dateString = formatter.format(currentTime);

        return dateString;
    }

    /**
     * 间隔月，多一天就多算一个月
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public static Integer getMonthBetweenWithDay(Date startDate, Date endDate) {
        if (startDate == null || endDate == null || !startDate.before(endDate)) {
            return null;
        }
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        end.setTime(endDate);
        int year1 = start.get(Calendar.YEAR);
        int year2 = end.get(Calendar.YEAR);
        int month1 = start.get(Calendar.MONTH);
        int month2 = end.get(Calendar.MONTH);
        int n = (year2 - year1) * 12;
        n = n + month2 - month1;
        int day1 = start.get(Calendar.DAY_OF_MONTH);
        int day2 = end.get(Calendar.DAY_OF_MONTH);
        if (day1 <= day2) {
            n++;
        }
        return n;
    }

    public static String date2str(Date date, String pattern) {
        return format(date, pattern);
    }

    /**
     * String 类型的时间转换为时间戳
     *
     * @param time
     * @return
     */
    public static Long getTime(String time) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN.YYYY_MM_DD_HH_MM_SS);
            return format.parse(time).getTime();
        } catch (ParseException e) {
            throw new RuntimeException("时间转换错误");
        }
    }

    /**
     * String 类型的时间转换为时间戳
     *
     * @param time
     * @return
     */
    public static Long getTime2(String time) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN.YYYYMMDD);
            return format.parse(time).getTime();
        } catch (ParseException e) {
            throw new RuntimeException("时间转换错误");
        }
    }

    /**
     * String 类型的时间转换为时间戳
     *
     * @param time
     * @return
     */
    public static Long getTime3(String time) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN.YYYY_MM_DD_HH_MM);
            return format.parse(time).getTime();
        } catch (ParseException e) {
            throw new RuntimeException("时间转换错误");
        }
    }

    /**
     * 时间格式转换，从{fromPattern}格式转化为{toPattern}格式
     *
     * @param time
     * @param fromPattern
     * @param toPattern
     * @return
     */
    public static String format(String time, String fromPattern, String toPattern) {
        SimpleDateFormat format = new SimpleDateFormat(fromPattern);
        Long ts = null;
        try {
            ts = format.parse(time).getTime();
        } catch (ParseException e) {
            log.warn(
                    "时间转换出错，time: {}, fromPattern: {}, toPattern: {}",
                    new Object[] {time, fromPattern});
            throw new RuntimeException("时间转换出错");
        }
        return format(ts, toPattern);
    }

    /**
     * 时间转时间戳
     *
     * @param tm
     * @return
     */
    public static Long time2tm(String tm) {
        return getTime(tm);
    }

    /**
     * 时间转日期
     *
     * @param time
     * @return
     */
    public static String time2date(String time) {
        return time.split(" ")[0];
    }

    /**
     * 时间戳转为日期
     *
     * @param timestamp
     * @return
     */
    public static String tm2date(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN.YYYYMMDD);
        return sdf.format(timestamp);
    }

    /**
     * 时间戳转为日期
     *
     * @param timestamp
     * @return
     */
    public static String tm3date(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN.YYYY_MM_DD_HH_MM_SS);
        return sdf.format(timestamp);
    }

    /**
     * 时间戳转为日期
     *
     * @param timestamp
     * @return
     */
    public static String tm2date2(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN.YYYY_MM_DD);
        return sdf.format(timestamp);
    }

    /**
     * 时间戳转时间
     *
     * @param timestamp
     * @return
     */
    public static String tm2time(Long timestamp) {
        if (DataUtil.isEmpty(timestamp)) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN.YYYY_MM_DD_HH_MM_SS);
        return sdf.format(timestamp);
    }

    /**
     * 时间戳转字符串 yyyyMMddHHmmss
     *
     * @param timestamp
     * @return
     */
    public static String tm2timeTight(Long timestamp) {
        if (DataUtil.isEmpty(timestamp)) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN.YYYYMMDDHHMMSS);
        return sdf.format(timestamp);
    }

    /**
     * 时间戳转日期
     *
     * @param timestamp
     * @return
     */
    public static Date tm2Date(Long timestamp) {
        String dateStr = tm2time(timestamp);
        return DateUtil.stringToDate(dateStr);
    }

    public static Long date2tm(String date) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN.YYYYMMDD);
            return format.parse(date).getTime();
        } catch (ParseException e) {
            log.warn("事件转换出错, date: {}", date);
            throw new RuntimeException("时间转换出错");
        }
    }

    /**
     * 获取当前时间
     *
     * @param pattern 日期样式
     * @return
     */
    public static String getCurrentTimeStr(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).format(LocalDateTime.now());
    }

    public static long getTime(String date, String pattern) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            return format.parse(date).getTime();
        } catch (ParseException e) {
            throw new RuntimeException("时间转换错误");
        }
    }

    /**
     * @param startTime
     * @param endTime
     * @param timeFormat 开始时间和结束时间的时间格式
     * @param axisFormat 横坐标的时间格式
     * @return
     */
    public static Map<String, Long> getBetweenDates(
            String startTime, String endTime, String timeFormat, String axisFormat) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(timeFormat);
        Map<String, Long> result = new TreeMap<>();
        Calendar startCalendar = Calendar.getInstance();
        Calendar endCalendar = Calendar.getInstance();
        try {
            startCalendar.setTime(simpleDateFormat.parse(startTime));
            endCalendar.setTime(simpleDateFormat.parse(endTime));
        } catch (ParseException e) {
            log.warn("时间解析错误，startTime: {}, endTime: {}", new Object[] {startTime, endTime, e});
            throw new RuntimeException("时间转换错误");
        }
        SimpleDateFormat axisDateFormat = new SimpleDateFormat(axisFormat);
        while (startCalendar.before(endCalendar) || startCalendar.equals(endCalendar)) {
            result.put(axisDateFormat.format(startCalendar.getTime()), 0L);
            startCalendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return result;
    }

    public static Map<String, Integer> getBetweenDates(String startDate, String endDate) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        Map<String, Integer> result = new TreeMap<>();
        Calendar startCalendar = Calendar.getInstance();
        try {
            startCalendar.setTime(simpleDateFormat.parse(startDate));
        } catch (Exception e) {
            log.warn("startTime格式不正确，startDate: {}", startDate, e);
            throw new RuntimeException("startDate invalid");
        }
        Calendar endCalendar = Calendar.getInstance();
        try {
            endCalendar.setTime(simpleDateFormat.parse(endDate));
        } catch (Exception e) {
            log.warn("endTime格式不正确，endDate: {}", endDate, e);
            throw new RuntimeException("endDate invalid");
        }
        while (startCalendar.before(endCalendar) || startCalendar.equals(endCalendar)) {
            result.put(simpleDateFormat.format(startCalendar.getTime()), 0);
            startCalendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return result;
    }

    public static List<String> getDays(String startTime, String endTime, String format) {
        // 返回的日期集合
        List<String> days = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat dateFormat1 = new SimpleDateFormat(format);
        try {
            Date start = dateFormat.parse(startTime);
            Date end = dateFormat.parse(endTime);
            Calendar tempStart = Calendar.getInstance();
            tempStart.setTime(start);
            Calendar tempEnd = Calendar.getInstance();
            tempEnd.setTime(end);
            tempEnd.add(Calendar.DATE, +1);
            while (tempStart.before(tempEnd)) {
                days.add(dateFormat1.format(tempStart.getTime()));
                tempStart.add(Calendar.DAY_OF_YEAR, 1);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return days;
    }

    public static List<String> getDays(Long startTime, Long endTime, String format) {
        try {
            // 返回的日期集合
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            String startDate = dateFormat.format(startTime);
            String endDate = dateFormat.format(endTime);
            return getDays(startDate, endDate, format);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static List<String> getBetweenDateList(Long startTime, Long endTime, String format) {
        List<String> dateList = new ArrayList<>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            Date startDate = new Date(startTime);
            Date endDate = new Date(endTime);
            String startDateString = sdf.format(startDate);
            String endDateString = sdf.format(endDate);
            Date parseStartDate = sdf.parse(startDateString);
            Date parseEndDate = sdf.parse(endDateString);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parseStartDate);
            dateList.add(startDateString);
            while (parseEndDate.after(calendar.getTime())) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                dateList.add(sdf.format(calendar.getTime()));
            }
        } catch (Exception e) {
            // nothing
        }
        return dateList;
    }

    public static List<String> getBetweenDateList(String startDate, String endDate, String format) {
        List<String> dateList = new ArrayList<>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            Date parseStartDate = sdf.parse(startDate);
            Date parseEndDate = sdf.parse(endDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parseStartDate);
            dateList.add(startDate);
            while (parseEndDate.after(calendar.getTime())) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                dateList.add(sdf.format(calendar.getTime()));
            }
        } catch (Exception e) {
            // nothing
        }
        return dateList;
    }

    /**
     * 获取两个时间戳相隔的天数
     *
     * @param startTimestamp
     * @param endTimestamp
     * @return
     */
    public static int getBetweenDay(Long startTimestamp, Long endTimestamp) {
        int day = (int) ((endTimestamp - startTimestamp) / (24 * 3600 * 1000));
        return day;
    }

    public static List<String> getBetweenDateList(String startDate, String endDate) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        Calendar startCalendar = Calendar.getInstance();
        try {
            startCalendar.setTime(simpleDateFormat.parse(startDate));
        } catch (ParseException e) {
            log.warn("startTime格式不正确，startDate: {}", startDate, e);
            throw new RuntimeException("startDate invalid");
        }
        Calendar endCalendar = Calendar.getInstance();
        try {
            endCalendar.setTime(simpleDateFormat.parse(endDate));
        } catch (ParseException e) {
            log.warn("endTime格式不正确，endDate: {}", endDate, e);
            throw new RuntimeException("endDate invalid");
        }

        List<String> dates = new ArrayList<>();
        while (startCalendar.before(endCalendar) || startCalendar.equals(endCalendar)) {
            dates.add(simpleDateFormat.format(startCalendar.getTime()));
            startCalendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return dates;
    }

    /**
     * 获取当前日期时间
     *
     * @return 当前日期时间字符串 yyyy-MM-dd HH:mm:ss
     */
    public static String currentDateTime() {
        return LocalDateTime.now().format(DateFormat.yyyy_MM_dd_HH_mm_ss.getFormatter());
    }

    /**
     * 获取当前日期
     *
     * @return 当前日期字符串 yyyyMMdd
     */
    public static String currentDate() {
        return LocalDate.now().format(DateFormat.yyyyMMdd.getFormatter());
    }

    /**
     * 获取当前时间戳
     *
     * @return 当前时间戳 精确到毫秒
     */
    public static long currentTimestamp() {
        return Instant.now().toEpochMilli();
    }

    /**
     * @param mss
     * @return 该毫秒数转换为 * days * hours * minutes * seconds 后的格式
     * @author fy.zhang
     */
    public static String formatDuring(long mss) {
        long days = mss / (1000 * 60 * 60 * 24);
        long hours = (mss % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (mss % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (mss % (1000 * 60)) / 1000;
        if (days == 0 && hours == 0) {
            return minutes + "分钟";
        } else if (days == 0) {
            return hours + "小时" + minutes + "分钟";
        }
        return days + "天" + hours + "小时" + minutes + "分钟";
    }

    /** 获取今天星期几 */
    public static Integer getWeek() {

        Integer[] weekDays = {7, 1, 2, 3, 4, 5, 6};
        Calendar calendar = Calendar.getInstance();
        return weekDays[calendar.get(Calendar.DAY_OF_WEEK) - 1];
    }

    /** 获取当前小时 */
    public static Integer getHour() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        return cal.get(Calendar.HOUR);
    }

    /** 获取当天是几号 */
    public static Integer getDayOfMonth() {
        LocalDate nowDate = LocalDate.now();
        return nowDate.getDayOfMonth();
    }

    /** 获取是几月 */
    public static Integer getMonthOfYear() {
        LocalDate nowDate = LocalDate.now();
        return nowDate.getMonthValue();
    }

    /**
     * 获取当天时间戳的最小最大范围
     *
     * @return
     */
    public static long[] getTodayTimeStampBetween() {
        long current = System.currentTimeMillis(); // 当前时间毫秒数
        long zero = current - (current + TimeZone.getDefault().getRawOffset()) % (1000 * 3600 * 24);
        long end = zero + 24 * 60 * 60 * 1000 - 1; // 今天23点59分59秒的毫秒数
        return new long[] {zero, end};
    }

    /**
     * 获取当天零点时间戳
     *
     * @return
     */
    public static long getTodayZeroTimeStamp() {
        long current = System.currentTimeMillis();
        // 会有时区问题
        long zero = current - (current + TimeZone.getDefault().getRawOffset()) % (1000 * 3600 * 24);
        return zero;
    }

    public static Long getStartOfToday() {
        LocalDate parse = LocalDate.now();
        return parse.atStartOfDay().toInstant(ZoneOffset.of("+8")).toEpochMilli();
    }

    public static String getStartTimeOfDay(LocalDateTime localDateTime) {
        return localDateTime
                .with(LocalTime.MIN)
                .format(DateFormat.yyyy_MM_dd_HH_mm_ss.getFormatter());
    }

    public static String getEndTimeOfDay(LocalDateTime localDateTime) {
        return localDateTime
                .with(LocalTime.MAX)
                .format(DateFormat.yyyy_MM_dd_HH_mm_ss.getFormatter());
    }

    /**
     * 获取昨天最后的时间戳 23:59:59
     *
     * @return
     */
    public static long getYesterdayLastTimestamp() {
        long todayZero = getTodayTimeStampBetween()[0];
        return todayZero - 1000;
    }

    /**
     * 获取date往前/往后推几个月的日期
     *
     * @param n
     * @return
     */
    public static Date addMonth(Date date, int n) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, n);
        return cal.getTime();
    }

    /**
     * 根据Date类型生成yyyyMMdd格式时间戳
     *
     * @param currentTime
     * @return
     */
    public static String getDay(Date currentTime) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

        String dateString = formatter.format(currentTime);

        return dateString;
    }

    /**
     * 根据日期类型 WEEK、MONTH、YEAR 获取日期的起止日期 示例： WEEK: [20230110,]
     *
     * @param dateType
     * @return
     */
    public static List<String> getDateAreaList(String dateType) {
        List<String> dateList = new ArrayList<>();
        switch (dateType) {
            case "WEEK":
                dateList.add(DateUtil.getStringDay(DateUtil.addDate(new Date(), 3, -1)));
                break;
            case "MONTH":
                dateList.add(DateUtil.getStringDay(DateUtil.addDate(new Date(), 2, -1)));
                break;
            case "YEAR":
                dateList.add(DateUtil.getStringDay(DateUtil.addDate(new Date(), 1, -1)));
                break;
            default:
                return dateList;
        }
        dateList.add(DateUtil.getStringDay(new Date()));
        return dateList;
    }

    /**
     * 根据日期类型获取所对应的起始时间
     *
     * @param dateType
     * @return
     */
    public static Long getStartTimeByDateType(String dateType) {
        Long startTime = 0L;
        if ("WEEK".equalsIgnoreCase(dateType)) {
            startTime = DateUtil.addDate(new Date(), 3, -1).getTime();
        } else if ("MONTH".equalsIgnoreCase(dateType)) {
            startTime = DateUtil.addDate(new Date(), 2, -1).getTime();
        } else if ("YEAR".equalsIgnoreCase(dateType)) {
            startTime = DateUtil.addDate(new Date(), 1, -1).getTime();
        }
        return startTime;
    }

    public static String formatDateString(Long timestamp, String formatter) {
        SimpleDateFormat sdf = new SimpleDateFormat(formatter);
        Date date = new Date();
        date.setTime(timestamp);
        return sdf.format(date);
    }

    public static java.sql.Date columnToDate(Object column, SimpleDateFormat customTimeFormat) {
        if (column == null) {
            return null;
        } else if (column instanceof String) {
            if (((String) column).length() == 0) {
                return null;
            }

            Date date = stringToDate((String) column, customTimeFormat);
            if (null == date) {
                return null;
            }
            return new java.sql.Date(date.getTime());
        } else if (column instanceof Integer) {
            Integer rawData = (Integer) column;
            return new java.sql.Date(getMillSecond(rawData.toString()));
        } else if (column instanceof Long) {
            Long rawData = (Long) column;
            return new java.sql.Date(getMillSecond(rawData.toString()));
        } else if (column instanceof java.sql.Date) {
            return (java.sql.Date) column;
        } else if (column instanceof Timestamp) {
            Timestamp ts = (Timestamp) column;
            return new java.sql.Date(ts.getTime());
        } else if (column instanceof Date) {
            Date d = (Date) column;
            return new java.sql.Date(d.getTime());
        }

        throw new IllegalArgumentException(
                "Can't convert " + column.getClass().getName() + " to Date");
    }

    public static Timestamp columnToTimestamp(String data, String format) {
        LocalDateTime parse = LocalDateTimeUtil.parse(data, format);
        LocalTime localTime = parse.query(TemporalQueries.localTime());
        LocalDate localDate = parse.query(TemporalQueries.localDate());
        return Timestamp.valueOf(LocalDateTime.of(localDate, localTime));
    }

    public static Timestamp columnToTimestampByFormat(
            Object column, SimpleDateFormat customTimeFormat) {
        if (column == null) {
            return null;
        } else if (column instanceof String) {
            if (((String) column).length() == 0) {
                return null;
            }

            Date date = stringToDate((String) column, customTimeFormat);
            if (null == date) {
                return null;
            }
            return Timestamp.from(date.toInstant());
        } else if (column instanceof Integer) {
            Integer rawData = (Integer) column;
            return new Timestamp(getMillSecond(rawData.toString()));
        } else if (column instanceof Long) {
            Long rawData = (Long) column;
            return new Timestamp(getMillSecond(rawData.toString()));
        } else if (column instanceof java.sql.Date) {
            return new Timestamp(((java.sql.Date) column).getTime());
        } else if (column instanceof Timestamp) {
            return (Timestamp) column;
        } else if (column instanceof Date) {
            Date d = (Date) column;
            return new Timestamp(d.getTime());
        }

        throw new UnsupportedOperationException(
                "Can't convert " + column.getClass().getName() + " to Date");
    }

    /** 将 2020-09-07 14:49:10.0 Timestamp */
    public static Timestamp convertToTimestamp(String timestamp) {
        if (TIMESTAMP_FORMAT_PATTERN.matcher(timestamp).find()) {
            return Timestamp.valueOf(timestamp);
        }
        return null;
    }

    public static long getMillSecond(String data) {
        long time = Long.parseLong(data);
        if (data.length() == LENGTH_SECOND) {
            time = Long.parseLong(data) * 1000;
        } else if (data.length() == LENGTH_MILLISECOND) {
            time = Long.parseLong(data);
        } else if (data.length() == LENGTH_MICROSECOND) {
            time = Long.parseLong(data) / 1000;
        } else if (data.length() == LENGTH_NANOSECOND) {
            time = Long.parseLong(data) / 1000000;
        } else if (data.length() < LENGTH_SECOND) {
            try {
                long day = Long.parseLong(data);
                Date date = datetimeFormatter.get().get(DATE_FORMAT).parse(START_TIME);
                Calendar cal = Calendar.getInstance();
                long addMill = date.getTime() + day * 24 * 3600 * 1000;
                cal.setTimeInMillis(addMill);
                time = cal.getTimeInMillis();
            } catch (Exception ignore) {
            }
        }
        return time;
    }

    public static java.sql.Date stringColumnToDate(String column, String customTimeFormat) {
        SimpleDateFormat format = null;
        if (StringUtils.isNotBlank(customTimeFormat)) {
            format = new SimpleDateFormat(customTimeFormat);
        }
        return columnToDate(column, format);
    }

    public static Date stringToDate(String strDate, SimpleDateFormat customTimeFormat) {
        if (strDate == null || strDate.trim().length() == 0) {
            return null;
        }

        if (customTimeFormat != null) {
            try {
                return customTimeFormat.parse(strDate);
            } catch (ParseException ignored) {
            }
        }

        try {
            return datetimeFormatter.get().get(STANDARD_DATETIME_FORMAT).parse(strDate);
        } catch (ParseException ignored) {
        }

        try {
            return datetimeFormatter.get().get(UN_STANDARD_DATETIME_FORMAT).parse(strDate);
        } catch (ParseException ignored) {
        }

        try {
            return datetimeFormatter.get().get(T_DATETIME_FORMAT).parse(strDate);
        } catch (ParseException ignored) {
        }

        try {
            return datetimeFormatter.get().get(TZ_DATETIME_FORMAT).parse(strDate);
        } catch (ParseException ignored) {
        }

        try {
            return datetimeFormatter.get().get(DATE_FORMAT).parse(strDate);
        } catch (ParseException ignored) {
        }

        try {
            return datetimeFormatter.get().get(TIME_FORMAT).parse(strDate);
        } catch (ParseException ignored) {
        }

        try {
            return datetimeFormatter.get().get(YEAR_FORMAT).parse(strDate);
        } catch (ParseException ignored) {
        }

        throw new RuntimeException("can't parse date");
    }
}
