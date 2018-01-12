package tools.pdmgenerate;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 陈刚
 */
public class StringHelper {
	private static final Logger log = LoggerFactory.getLogger(StringHelper.class);

	private StringHelper() {
	}

	/**
	 * 获取一定长度的随机字符串
	 * @param length 指定字符串长度
	 * @return 一定长度的字符串
	 */
	public static String getRandomStringByLength(int length) {
		String base = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random random = new Random();
		StringBuffer sb = new StringBuffer(length);
		for (int i = 0; i < length; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		return sb.toString();
	}

	public static String substring(String str, int len) {
		if (str == null)
			return null;
		int length = str.length();
		if (length < len)
			return str;
		String s = str.substring(0, len);
		return (length > len + 3) ? s + "..." : s;
	}

	/**
	 * 将一个ip:port串,分解成独立的ip, port数组
	 * @param ipPortListStr
	 * @param defaultPort  默认端口
	 * @return
	 *      Object[] objects = StringHelper.splitIpPort(""127.0.0.1:80, 127.0.0.2:81, 127.0.0.3, 127.0.0.4"", 26379);
	 * 		String[] ips = (String[]) objects[0];
	 * 		int[] ports = (int[]) objects[1];
	 */
	public static Object[] splitIpPort(String ipPortListStr, int defaultPort) {
		List<String> ipPortList = split(ipPortListStr, ",");
		if (ipPortList == null || ipPortList.isEmpty()) {
			return null;
		}
		int len = ipPortList.size();
		String[] ips = new String[len];
		int[] ports = new int[len];
		for (int i = 0; i < len; i++) {
			String ipPortStr = ipPortList.get(i);
			String[] split = ipPortStr.split(":");
			ips[i] = split[0].trim();
			if (split.length == 1) {
				ports[i] = defaultPort;
			} else {
				ports[i] = Integer.parseInt(split[1].trim());
			}
		}
		return new Object[]{ips, ports};
	}

	/**
	 * 去掉前后空格(包括各元素的前后空格)
	 * 空元素会忽略
	 */
	public static List<String> split(String str, String splitChar) {
		ArrayList<String> result = new ArrayList<String>();
		if (StringUtils.isEmpty(str)) {
			return result;
		}
		String[] strs = StringUtils.split(str, splitChar);
		if (strs == null || strs.length == 0) {
			return result;
		}
		for (String s : strs) {
			s = s.trim();
			if (StringUtils.isNotEmpty(s)) {
				result.add(s);
			}
		}
		return result;
	}

	public static String join(Iterable list) {
		return join(list, ",");
	}

	public static String join(Iterable list, String splitChar) {
		if (list == null || !list.iterator().hasNext()) {
			return null;
		}
		StringBuilder buf = new StringBuilder();
		for (java.util.Iterator it = list.iterator(); it.hasNext(); ) {
			Object o = it.next();
			if (it.hasNext()) {
				buf.append(o.toString() + splitChar);
			} else {
				buf.append(o.toString());
			}
		}
		return buf.toString();
	}

	/**
	 * 去掉前后空格(包括各元素的前后空格)
	 * 空元素会忽略
	 */
	public static Set<String> splitToSet(String str, String splitChar) {
		HashSet<String> result = new HashSet<String>();
		if (StringUtils.isEmpty(str)) {
			return result;
		}
		String[] strs = StringUtils.split(str, splitChar);
		if (strs == null || strs.length == 0) {
			return result;
		}
		for (String s : strs) {
			s = s.trim();
			if (StringUtils.isNotEmpty(s)) {
				result.add(s);
			}
		}
		return result;
	}

	/**
	 * 去掉前后空格(包括各元素的前后空格)
	 * 空元素会忽略
	 * 相同的元素会忽略
	 */
	public static List splitNotSame(String str, String splitChar) {
		ArrayList<String> result = new ArrayList<String>();
		if (StringUtils.isEmpty(str)) {
			return result;
		}
		String[] strs = StringUtils.split(str, splitChar);
		if (strs == null || strs.length == 0) {
			return result;
		}
		for (String s : strs) {
			s = s.trim();
			if (StringUtils.isNotEmpty(s) && !result.contains(s)) {
				result.add(s);
			}
		}
		return result;
	}

	/**
	 * 在最后加一个斜杠
	 */
	public static String addSlash(String str) {
		if (str == null)
			return null;
		if (!str.endsWith("/")) {
			return str + "/";
		}
		return str;
	}

	/**
	 * 将传入消息的布尔值字串，统一转化为我们系统用的Y/N
	 */
	public static String toBooleanChar(String str) {
		if (StringUtils.isEmpty(str)) {
			return str;
		}
		if (str.equalsIgnoreCase("true") || str.equals("y")) {
			return "Y";
		} else if (str.equalsIgnoreCase("false") || str.equals("n")) {
			return "N";
		} else {
			log.error("It is not support boolean string: " + str);
			return str;
		}
	}

	/**
	 * 将输入流转换成字符串
	 */
	public static String toString(InputStream is) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int len;
		try {
			while ((len = is.read(buf)) != -1) {
				baos.write(buf, 0, len);
			}
		} catch (IOException e) {
			log.error("", e);
			throw new RuntimeException("read InputStream is error");
		}
		return new String(baos.toByteArray());
	}

	/**
	 * 将模板s中的占位符{}用参数逐一替换
	 */
	public static String format(String s, java.lang.Object... objects) {
		if (StringUtils.isEmpty(s) || objects == null || objects.length == 0 || s.indexOf("{}") == -1)
			return s;
		for (int i = 0; i < objects.length; i++) {
			Object o = objects[i];
			String value = o == null ? "null" : o.toString();
			s = StringUtils.replace(s, "{}", value, 1);
		}
		return s;
	}

	public static String notNull(String s, String defaultValue) {
		return s == null ? defaultValue : s;
	}

	public static String notEmpty(String s, String defaultValue) {
		return StringUtils.isEmpty(s) ? defaultValue : s;
	}

	public static String notBlank(String s, String defaultValue) {
		return StringUtils.isBlank(s) ? defaultValue : s;
	}

	public static Integer valueOf(String s, Integer defaultValue) {
		if (StringUtils.isBlank(s)) {
			return defaultValue;
		}
		try {
			return Integer.valueOf(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static int parseInt(String s, int defaultValue) {
		if (StringUtils.isBlank(s)) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static float parseFloat(String s, float defaultValue) {
		if (StringUtils.isBlank(s)) {
			return defaultValue;
		}
		try {
			return Float.parseFloat(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static <T> T valueOf(String s, Class<T> resultType, String defaultValue) {
		if (s == null) {
			return valueOf(defaultValue, resultType);
		}
		try {
			return valueOf(s, resultType);
		} catch (Exception e) {
			log.warn("转换出错,源={},类型={}, error message: {}", s, resultType, e.getMessage());
			return valueOf(defaultValue, resultType);
		}
	}

	public static <T> T valueOf(String s, Class<T> resultType) {
		if (s == null) {
			return null;
		}
		if (resultType.equals(String.class)) {
			return (T) s;
		} else if (resultType.equals(Integer.class)) {
			return (T) Integer.valueOf(s);
		} else if (resultType.equals(Long.class)) {
			return (T) Long.valueOf(s);
		} else if (resultType.equals(Float.class)) {
			return (T) Float.valueOf(s);
		} else if (resultType.equals(Boolean.class)) {
			return (T) Boolean.valueOf(s);
		} else if (resultType.equals(Date.class)) {
			throw new UnsupportedOperationException("");
//			return (T) DateHelper.parseDate(s);
		} else {
			throw new RuntimeException(resultType.toString());
		}
	}

	public static Float valueOf(String s, Float defaultValue) {
		if (StringUtils.isBlank(s)) {
			return defaultValue;
		}
		try {
			return Float.valueOf(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static Double valueOf(String s, Double defaultValue) {
		if (StringUtils.isBlank(s)) {
			return defaultValue;
		}
		try {
			return Double.valueOf(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static Long valueOf(String s, Long defaultValue) {
		if (StringUtils.isBlank(s)) {
			return defaultValue;
		}
		try {
			return Long.valueOf(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static long parseLong(String s, long defaultValue) {
		if (StringUtils.isBlank(s)) {
			return defaultValue;
		}
		try {
			return Long.parseLong(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static Boolean valueOf(String s, Boolean defaultValue) {
		if (StringUtils.isBlank(s)) {
			return defaultValue;
		}
		try {
			return Boolean.valueOf(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static boolean parseBoolean(String s, boolean defaultValue) {
		if (StringUtils.isBlank(s)) {
			return defaultValue;
		}
		try {
			return Boolean.parseBoolean(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * 骆驼命令转为分隔符命名
	 * converCamel("CommonData","-") ---> common-data
	 */
	public static String converCamel(String camelStr, String splitChar) {
		if (camelStr == null || camelStr.equals("")) {
			return "";
		}
		StringBuilder buf = new StringBuilder(camelStr);
		Pattern p = Pattern.compile("[A-Z]");
		Matcher mc = p.matcher(camelStr);
		int i = 0;
		while (mc.find()) {
			buf.replace(mc.start() + i, mc.end() + i, splitChar + mc.group().toLowerCase());
			i++;
		}
		if (buf.indexOf(splitChar) == 0) {
			buf.deleteCharAt(0);
		}
		return buf.toString();
	}

	/**
	 * 分隔符命名转为骆驼命令
	 * converCamel("common-data","-") ---> CommonData
	 */
	public static String toCamel(String name, String splitChar) {
		if (name == null || name.equals("")) {
			return "";
		}
		StringBuilder buf = new StringBuilder();
		name = name.toLowerCase();
		String[] array = StringUtils.split(name, splitChar);
		for (String s : array) {
			s = StringHelper.upperFirstCase(s);
			buf.append(s);
		}
		return buf.toString();
	}

	public static String upperFirstCase(String str) {
		if (StringUtils.isEmpty(str)) {
			return str;
		}
		String first = str.substring(0, 1).toUpperCase();
		String last = str.substring(1);
		return first + last;
	}

	public static String lowerFirstCase(String str) {
		if (StringUtils.isEmpty(str)) {
			return str;
		}
		String first = str.substring(0, 1).toLowerCase();
		String last = str.substring(1);
		return first + last;
	}

	/**
	 * 将字符串转unicode 
	 * @param s
	 * @return
	 * @author yxy 2013-08-20
	 */
	public static String convert(String s) {
		String unicode = "";
		char[] charAry = new char[s.length()];
		for (int i = 0; i < charAry.length; i++) {
			charAry[i] = s.charAt(i);
			if (Character.isLetter(charAry[i]) && (charAry[i] > 255))
				unicode += "/u" + Integer.toString(charAry[i], 16);
			else
				unicode += charAry[i];
		}
		return unicode;
	}

	public static void trim(String[] ss) {
		if (ss == null || ss.length == 0)
			return;
		for (int i = 0; i < ss.length; i++) {
			String s = ss[i];
			if (s != null) {
				ss[i] = s.trim();
			}
		}
	}

	/**
	 * 让字符串固定长度,不足补0
	 */
	public static String getFixLenStr(String str, int len) {
		if (StringUtils.isEmpty(str)) {
			return StringUtils.repeat("0", len);
		}
		int addLen = len - str.length();
		if (addLen > 0) {
			return StringUtils.repeat("0", addLen) + str;
		} else {
			return str;
		}
	}

}
