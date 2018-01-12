package tools.pdmgenerate;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 陈刚
 */
public class DateHelper {
	private static final Logger log = LoggerFactory.getLogger(DateHelper.class);
	public static final long SECOND = 1000;
	public static final long MINUTE = 60 * 1000; //60 * SECOND;
	public static final long HOUR = 60 * 60 * 1000;//60 * MINUTE;
	public static final long DAY = 24 * 60 * 60 * 1000; //24 * HOUR;
	private static final DecimalFormat timeDisplayDecimalFormat = (DecimalFormat) NumberFormat.getInstance();

	public static final Map<String, ThreadLocal<SimpleDateFormat>> PATTERN_MAP = new HashMap<String, ThreadLocal<SimpleDateFormat>>();
	public static final String PATTERN_DATE = "yyyy-MM-dd";
	public static final String PATTERN_DATE_TIME = "yyyy-MM-dd HH:mm:ss";
	public static final String PATTERN_MILLS = "yyyyMMddHHmmssSSS";
	public static final String PATTERN_YMD = "yyyyMMdd";

	public static final String PATTERN_UTC_MINUTE = "yyyy-MM-dd'T'HH:mm";
	public static final String PATTERN_UTC_TIME = "yyyy-MM-dd'T'HH:mm:ss";
	public static final String PATTERN_UTC_MILLS = "yyyy-MM-dd'T'HH:mm:ss.SSS";

	public static enum PeriodType {
		MINUTE, HOUR, DAY, WEEK, MONTH, YEAR
	}

	private DateHelper() {
	}

	static {
		DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
		timeDisplayDecimalFormat.setDecimalFormatSymbols(dfs);
		timeDisplayDecimalFormat.applyPattern("#.#");
	}

	public static SimpleDateFormat getDateFormat(final String pattern) {
		ThreadLocal<SimpleDateFormat> local = PATTERN_MAP.get(pattern);
		if (local == null) {
			/**
			 * SimpleDateFormat不是线程安全的, 但其对象又庞大(含有Calendar), 所以采用线程变量方式, 让它在线程内共享
			 */
			local = new ThreadLocal<SimpleDateFormat>() { //线程副本
				@Override
				protected synchronized SimpleDateFormat initialValue() {
					return new SimpleDateFormat(pattern);
				}
			};
			PATTERN_MAP.put(pattern, local);
		}
		return local.get();
	}

	public static String getTimeDisplay(long time, long displayType) {
		double v = (double) time / (double) displayType;
		return timeDisplayDecimalFormat.format(v);
	}

	public static String getTimeDisplay(long time) {
		if (time < SECOND) {
			return time + " mill";
		} else if (time < MINUTE) {
			return getTimeDisplay(time, SECOND) + " sec";
		} else if (time < HOUR) {
			return getTimeDisplay(time, MINUTE) + " minute";
		} else if (time < DAY) {
			return getTimeDisplay(time, HOUR) + " hour";
		} else {
			return getTimeDisplay(time, DAY) + " day";
		}
	}

	public static String formatDate(Date date, String pattern) {
		if (date == null) {
			return null;
		}
		return getDateFormat(pattern).format(date);
	}

	public static String formatDate(long date, String pattern) {
		return getDateFormat(pattern).format(new Date(date));
	}

	/**
	 * 将日期对象输出为日期时间字串(例: 2012-12-12 12:12:12)
	 */
	public static String formatDateTime(Date date) {
		return date == null ? null : getDateFormat(PATTERN_DATE_TIME).format(date);
	}

	/**
	 * 将日期对象输出为日期字串(例: 2012-12-12 不包括时间)
	 */
	public static String formatDate(Date date) {
		return date == null ? null : getDateFormat(PATTERN_DATE).format(date);
	}

	public static Date parseDate(String source, String pattern) {
		if (StringUtils.isBlank(source)) {
			return null;
		}
		try {
			return getDateFormat(pattern).parse(source);
		} catch (ParseException e) {
			throw new IllegalArgumentException("格式错误, 无法转化成日期对象: " + source + ", pattern=" + pattern, e);
		}
	}

	public static Date parseDate(String source) {
		if (StringUtils.isBlank(source)) {
			return null;
		}
		int len = source.length();
		if (NumberUtils.isNumber(source)) {
			return new Date(Long.parseLong(source));
		}

		if (source.indexOf("T") != -1) {
			if (len == 23 && source.indexOf(".") == 19) { //2017-03-11T10:45:26.891
				//do nothing
			} else if (len == 19) { //2017-03-11T10:45:26
				source += ".000";
			} else if (len == 16) { //2017-03-16T01:01
				source += ":00.000";
			} else {
				throw new IllegalArgumentException("格式错误(长度不对), 无法转化成日期对象: " + source);
			}
			try {
				return getDateFormat(PATTERN_UTC_MILLS).parse(source);
			} catch (ParseException ex) {
				throw new IllegalArgumentException("格式错误, 无法转化成日期对象: " + source, ex);
			}
		}

		if (len == 10) {//日期
			source += " 00:00:00";
		} else if (len == 13) { //小时
			source += ":00:00";
		} else if (len == 16) { //分钟
			source += ":00";
		} else if (len == 19) {//秒
			//do nothing
		} else {
			throw new IllegalArgumentException("格式错误(长度不对), 无法转化成日期对象: " + source);
			//throw new IllegalArgumentException("Invalid date format (length error): " + source);
		}
		try {
			return getDateFormat(PATTERN_DATE_TIME).parse(source);
		} catch (ParseException ex) {
			throw new IllegalArgumentException("格式错误, 无法转化成日期对象: " + source, ex);
			//throw new IllegalArgumentException("Invalid date format: " + source, ex);
		}
	}

	/**
	 * 取得分解开的年月日
	 * @return [0]=2013 [1]=02 [2]=25
	 */
	public static String[] getDateStrs() {
		String str = formatDate(new Date());
		return StringUtils.split(str, "-");
	}

	public static Date getDate(int year) {
		return getDate(year, 1, 1, 0, 0, 0);
	}

	public static Date getDate(int year, int month) {
		return getDate(year, month, 1, 0, 0, 0);
	}

	public static Date getDate(int year, int month, int day) {
		return getDate(year, month, day, 0, 0, 0);
	}

	public static Date getDate(int year, int month, int day, int hour) {
		return getDate(year, month, day, hour, 0, 0);
	}

	public static Date getDate(int year, int month, int day, int hour, int minute) {
		return getDate(year, month, day, hour, minute, 0);
	}

	public static Date getDate(int year, int month, int day, int hour, int minute, int second) {
		if (hour < 0 || hour > 23) {
			throw new IllegalArgumentException("error hour(0~23):" + hour);
		}
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month - 1);
		cal.set(Calendar.DAY_OF_MONTH, day);
		cal.set(Calendar.HOUR, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, second);
		return cal.getTime();
	}

	/**
	 * 取某种时间类型的最始时间
	 * 例：getMaxTime(Date(2007-3-1), PeriodType.YEAR)=2007-1-1
	 *
	 * @param periodType 年，月，日，小时，分钟, 周
	 * @return
	 */
	public static Date getMinTime(Date date, PeriodType periodType) {
		return getMinTime(date, periodType, 1);
	}

	public static Date getMinTime(Date date, PeriodType periodType, int period) {
		if (PeriodType.MINUTE != periodType && period != 1) {
			throw new IllegalArgumentException("本方法只支持PeriodType.MINUTE(分钟)时, period可以不为1");
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		if (periodType == PeriodType.YEAR) {
			return getDate(year);
		} else if (periodType == PeriodType.MONTH) {
			return getDate(year, month);
		} else if (periodType == PeriodType.WEEK) {
			return getFirstDayOfWeek(getDate(year, month, day));
		} else if (periodType == PeriodType.DAY) {
			return getDate(year, month, day);
		} else if (periodType == PeriodType.HOUR) {
			return getDate(year, month, day, hour);
		} else if (periodType == PeriodType.MINUTE) {
			return getDate(year, month, day, hour, period * (minute / period));
		} else {
			throw new IllegalArgumentException("field is error:" + periodType);
		}

	}

	/**
	 * 取得下一个时间。
	 * 例：getTime(date, PeriodType.WEEK, 2)=取得date的两周后的日期
	 *
	 * @param periodType 年，月，日，小时，分钟, 周
	 * @param period 周期数
	 * @return
	 */
	public static Date getDate(Date date, PeriodType periodType, int period) {
		if (periodType == PeriodType.YEAR) {
			return DateUtils.addYears(date, period);
		} else if (periodType == PeriodType.MONTH) {
			return DateUtils.addMonths(date, period);
		} else if (periodType == PeriodType.WEEK) {
			return DateUtils.addDays(date, 7 * period);
		} else if (periodType == PeriodType.DAY) {
			return DateUtils.addDays(date, period);
		} else if (periodType == PeriodType.HOUR) {
			return DateUtils.addHours(date, period);
		} else if (periodType == PeriodType.MINUTE) {
			return DateUtils.addMinutes(date, period);
		} else {
			throw new IllegalArgumentException("field is error:" + periodType);
		}
	}

	/**
	 * 取某种时间类型的最未时间
	 * 例：getMaxTime(2007-3-1, PeriodType.YEAR)=2008-1-1
	 *
	 * @param periodType 年，月，日，小时，分钟, 周
	 * @return
	 */
	public static Date getMaxTime(Date date, PeriodType periodType) {
		return getMaxTime(date, periodType, 1);
	}

	public static Date getMaxTime(Date date, PeriodType periodType, int period) {
		Date minTime = getMinTime(date, periodType, period);
		return getDate(minTime, periodType, period);
	}

	/**
	 * 取得本周的第一天的日期（周一为第一天)
	 *
	 * @param date
	 * @return
	 */
	public static Date getFirstDayOfWeek(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		//周日=1 ~ 周六=7
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		if (dayOfWeek == 1) {
			dayOfWeek = 8;
		}
		cal.add(Calendar.DATE, -(dayOfWeek - 2));
		return cal.getTime();
	}

	/**
	 * 本周最后一天的日期（周日为最后一天）
	 * @return 本周最后一天的日期。
	 */
	public static Date getLastDayOfWeek(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		if (dayOfWeek == 1)
			dayOfWeek = 8;
		cal.add(Calendar.DATE, 8 - dayOfWeek);
		return cal.getTime();
	}

	/**
	 * 功能：获得指定日期所在周(第几周)
	 */
	public static int getWeekOfYear(Date setDate) {
		Calendar ca = Calendar.getInstance();
		ca.setTime(setDate != null ? setDate : new Date());
		return ca.get(Calendar.WEEK_OF_YEAR);
	}

	/**
	 * 功能：获得指定周号
	 */
	public static int getWeekIndex(Date setDate) {
		Calendar ca = Calendar.getInstance();
		ca.setTime(setDate != null ? setDate : new Date());
		int i = ca.get(Calendar.DAY_OF_WEEK);
		if (i == 1) {
			return 7;
		} else {
			return i - 1;
		}
	}

	/**
	 * 功能：获得指定日期年份
	 */
	public static int getYear(Date setDate) {
		Calendar ca = Calendar.getInstance();
		ca.setTime(setDate != null ? setDate : new Date());
		return ca.get(Calendar.YEAR);
	}

	/**
	 * 功能：得到指定日期的后n天日期,n可以为负
	 */
	public static Date getNextDay(Date setDate, int nextDays) {
		Calendar ca = Calendar.getInstance();
		ca.setTime(setDate != null ? setDate : new Date());
		ca.add(Calendar.DATE, nextDays);
		return ca.getTime();
	}

	/**
	 * 获取发布时间差
	 */
	public static String getBetweenTime(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
		SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");
		Date now = new Date(System.currentTimeMillis());
		long l = now.getTime() - date.getTime();
		long day = l / (24 * 60 * 60 * 1000);
		long hour = (l / (60 * 60 * 1000) - day * 24);
		long min = ((l / (60 * 1000)) - day * 24 * 60 - hour * 60);
		long s = (l / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);

		if (day == 0 && hour == 0 && min == 0) {
			return "刚刚";
		} else if (day == 0 && hour == 0 && min != 0) {
			return min + "分钟前";
		} else if (day == 0 && hour != 0) {
			return hour + "小时前";
		} else if (day == 1) {
			return "昨天" + sdf2.format(date);
		} else if (day == 2) {
			return "前天" + sdf2.format(date);
		} else {
			//return "多余两天";
			return sdf.format(date);
		}
	}

	/**
	 * 获取时间差
	 */
	public static String getTimeDesc(Date time1, Date time2) {
		long l = time2.getTime() - time1.getTime();
		long days = l / DAY;
		long hours = l / HOUR - days * 24;
		long mins = l / MINUTE - days * 24 * 60 - hours * 60;
		long secs = l / SECOND - days * 24 * 60 * 60 - hours * 60 * 60 - mins * 60;

		StringBuilder buf = new StringBuilder();
		if (days > 0) {
			buf.append(days + "天");
		}
		if (hours > 0) {
			buf.append(hours + "小时");
		}
		if (mins > 0) { //一天之内时,显示分钟
			buf.append(mins + "分钟");
		}
		if (secs > 0 && days == 0 && hours == 0 && mins == 0) {
			buf.append(secs + "秒"); //1分钟之内, 才显示秒
		}
		return buf.toString();
	}

	/**
	 * 获取过期时间提示
	 */
	public static String getExpireTimeDesc(long expireTime) {
		long l = expireTime - System.currentTimeMillis();
		if (l <= 0) {
			return "<span class=\"striking2\">已过期</span>";
		}
		long day = l / (24 * 60 * 60 * 1000);
		long hour = (l / (60 * 60 * 1000) - day * 24);
		long min = ((l / (60 * 1000)) - day * 24 * 60 - hour * 60);
		if (day == 0 && hour == 0 && min == 0) {
			return "即将过期";
		} else if (day == 0 && hour == 0 && min != 0) {
			return min + "分钟后过期";
		} else if (day == 0 && hour != 0) {
			return hour + "小时后过期";
		} else {
			return day + "天后过期";
		}
	}

	/**
	 * 当时分秒为零时,取得该天的最后时间(如 2012-08-08 00:00:00 则返回 2012-08-08 23:59:59)
	 */
	public static Date toEndDate(Date date) {
		if (date == null)
			return null;
		Date d = DateHelper.parseDate(DateHelper.formatDate(date));
		return DateUtils.addMilliseconds(d, (int) (DAY - 1));
//		return DateUtils.addDays(d, 1);
	}

	/**
	 * 取得两时间差的分钟数
	 */
	public static int getMinutes(Date startTime, Date endTime) {
		return (int) ((endTime.getTime() - startTime.getTime()) / MINUTE);
	}

	/**
	 *	两段时间([a1,a2]和[b1,b2])是否有重叠的部分
	 */
	public static boolean isOverlap(Date a1, Date a2, Date b1, Date b2) {
		return (b1.after(a1) && b1.before(a2)) //
				|| (b2.after(a1) && b2.before(a2))//
				|| (b1.before(a1) && b2.after(a2));
	}

	/**
	 * 获取最近同步日期
	 */
	public static Date getLatestDate(Date createdDate, Date updatedDate) {
		if (updatedDate == null) {
			return createdDate;
		}
		if (createdDate == null) {
			return updatedDate;
		}
		if (updatedDate.after(createdDate)) {
			return updatedDate;
		} else {
			return createdDate;
		}
	}

	/**
	 * 去掉小时
	 * 2017-04-04 01:02:03 --> 2017-04-04 00:00:00
	 */
	public static Date clearDay(Date date) {
		if (date == null) {
			return null;
		}
		Date result = DateUtils.truncate(date, Calendar.DAY_OF_MONTH);
		log.info("clear day,   args: {}", DateHelper.formatDateTime(date));
		log.info("clear day, result: {}", DateHelper.formatDateTime(result));
		return result;
	}

	public static String getDistanceDesc(double distance) {
		double l = distance;

		long kl = (long) l / 1000; //公里

		StringBuilder buf = new StringBuilder();
		if (kl > 0) {

			DecimalFormat df = new DecimalFormat("######0.00");
			double dt = Double.parseDouble(df.format(distance / 1000));

			buf.append(dt + "公里");
		} else {
			DecimalFormat df = new DecimalFormat("######0.00");
			double dt = Double.parseDouble(df.format(distance));

			buf.append(dt + "米");
		}

		return buf.toString();
	}

}
