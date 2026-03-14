package com.hhoa.ai.kline.commons.utils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class DataUtil {
    private DataUtil() {}

    /**
     * 十进制字节数组转十六进制字符串
     *
     * @param b
     * @return
     */
    public static final String byte2hex(byte[] b) { // 一个字节数，转成16进制字符串
        StringBuilder hs = new StringBuilder(b.length * 2);
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            // 整数转成十六进制表示
            stmp = Integer.toHexString(b[n] & 0XFF);
            if (stmp.length() == 1) {
                hs.append("0").append(stmp);
            } else {
                hs.append(stmp);
            }
        }
        return hs.toString(); // 转成大写
    }

    /**
     * 十六进制字符串转十进制字节数组
     *
     * @param
     * @return
     */
    public static final byte[] hex2byte(String hs) {
        byte[] b = hs.getBytes();
        if ((b.length % 2) != 0) throw new IllegalArgumentException("长度不是偶数");
        byte[] b2 = new byte[b.length / 2];
        for (int n = 0; n < b.length; n += 2) {
            String item = new String(b, n, 2);
            // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个十进制字节
            b2[n / 2] = (byte) Integer.parseInt(item, 16);
        }
        return b2;
    }

    /**
     * 这个方法可以通过与某个类的class文件的相对路径来获取文件或目录的绝对路径。 通常在程序中很难定位某个相对路径，特别是在B/S应用中。
     * 通过这个方法，我们可以根据我们程序自身的类文件的位置来定位某个相对路径。 比如：某个txt文件相对于程序的Test类文件的路径是../../resource/test.txt，
     * 那么使用本方法Path.getFullPathRelateClass("../../resource/test.txt",Test.class)
     * 得到的结果是txt文件的在系统中的绝对路径。
     *
     * @param relatedPath 相对路径
     * @param cls 用来定位的类
     * @return 相对路径所对应的绝对路径
     * @throws IOException 因为本方法将查询文件系统，所以可能抛出IO异常
     */
    public static final String getFullPathRelateClass(String relatedPath, Class<?> cls) {
        String path = null;
        if (relatedPath == null) {
            throw new NullPointerException();
        }
        String clsPath = getPathFromClass(cls);
        File clsFile = new File(clsPath);
        String tempPath = clsFile.getParent() + File.separator + relatedPath;
        File file = new File(tempPath);
        try {
            path = file.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    /**
     * 获取class文件所在绝对路径
     *
     * @param cls
     * @return
     * @throws IOException
     */
    public static final String getPathFromClass(Class<?> cls) {
        String path = null;
        if (cls == null) {
            throw new NullPointerException();
        }
        URL url = getClassLocationURL(cls);
        if (url != null) {
            path = url.getPath();
            if ("jar".equalsIgnoreCase(url.getProtocol())) {
                try {
                    path = new URL(path).getPath();
                } catch (MalformedURLException e) {
                }
                int location = path.indexOf("!/");
                if (location != -1) {
                    path = path.substring(0, location);
                }
            }
            File file = new File(path);
            try {
                path = file.getCanonicalPath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return path;
    }

    /**
     * 判断对象是否Empty(null或元素为0)<br>
     * 实用于对如下对象做判断:String Collection及其子类 Map及其子类
     *
     * @param pObj 待检查对象
     * @return boolean 返回的布尔值
     */
    public static final boolean isEmpty(Object pObj) {
        if (pObj == null) return true;
        if (pObj == "") return true;
        if (pObj instanceof String) {
            if (((String) pObj).trim().length() == 0) {
                return true;
            }
        } else if (pObj instanceof Collection<?>) {
            if (((Collection<?>) pObj).size() == 0) {
                return true;
            }
        } else if (pObj instanceof Map<?, ?>) {
            if (((Map<?, ?>) pObj).size() == 0) {
                return true;
            }
        } else if (pObj instanceof Object[]) {
            if (((Object[]) pObj).length == 0) {
                return true;
            }
        }
        return false;
    }

    public static final boolean isNull(Object pObj) {
        if (pObj == null) return true;
        if (pObj == "") return true;
        if (pObj instanceof String) {
            if (((String) pObj).trim().length() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断对象是否为NotEmpty(!null或元素>0)<br>
     * 实用于对如下对象做判断:String Collection及其子类 Map及其子类
     *
     * @param pObj 待检查对象
     * @return boolean 返回的布尔值
     */
    public static final boolean isNotEmpty(Object pObj) {
        if (pObj == null) return false;
        if (pObj == "") return false;
        if (pObj instanceof String) {
            if (((String) pObj).trim().length() == 0) {
                return false;
            }
        } else if (pObj instanceof Collection<?>) {
            if (((Collection<?>) pObj).size() == 0) {
                return false;
            }
        } else if (pObj instanceof Map<?, ?>) {
            if (((Map<?, ?>) pObj).size() == 0) {
                return false;
            }
        } else if (pObj instanceof Object[]) {
            if (((Object[]) pObj).length == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * JS输出含有\n的特殊处理
     *
     * @param pStr
     * @return
     */
    public static final String replace4JsOutput(String pStr) {
        pStr = pStr.replace("\r\n", "<br/>&nbsp;&nbsp;");
        pStr = pStr.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
        pStr = pStr.replace(" ", "&nbsp;");
        return pStr;
    }

    /**
     * 分别去空格
     *
     * @param paramArray
     * @return
     */
    public static final String[] trim(String[] paramArray) {
        if (paramArray == null || paramArray.length == 0) {
            return paramArray;
        }
        String[] resultArray = new String[paramArray.length];
        for (int i = 0; i < paramArray.length; i++) {
            String param = paramArray[i];
            resultArray[i] = param == null ? param : param.trim();
        }
        return resultArray;
    }

    /**
     * 获取类的class文件位置的URL
     *
     * @param cls
     * @return
     */
    private static URL getClassLocationURL(final Class<?> cls) {
        if (cls == null) throw new IllegalArgumentException("null input: cls");
        URL result = null;
        final String clsAsResource = cls.getName().replace('.', '/').concat(".class");
        final ProtectionDomain pd = cls.getProtectionDomain();
        if (pd != null) {
            final CodeSource cs = pd.getCodeSource();
            if (cs != null) result = cs.getLocation();
            if (result != null) {
                if ("file".equals(result.getProtocol())) {
                    try {
                        if (result.toExternalForm().endsWith(".jar")
                                || result.toExternalForm().endsWith(".zip"))
                            result =
                                    new URL(
                                            "jar:"
                                                    .concat(result.toExternalForm())
                                                    .concat("!/")
                                                    .concat(clsAsResource));
                        else if (new File(result.getFile()).isDirectory())
                            result = new URL(result, clsAsResource);
                    } catch (MalformedURLException ignore) {
                    }
                }
            }
        }
        if (result == null) {
            final ClassLoader clsLoader = cls.getClassLoader();
            result =
                    clsLoader != null
                            ? clsLoader.getResource(clsAsResource)
                            : ClassLoader.getSystemResource(clsAsResource);
        }
        return result;
    }

    /** 初始化设置默认值 */
    public static final <K> K ifNull(K k, K defaultValue) {
        if (k == null) {
            return defaultValue;
        }
        return k;
    }

    /**
     * 获取随机字符串
     *
     * @param count
     * @return
     */
    public static String getRandomStr(int count) {
        String ku = "23456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder str = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < count; i++) {
            int index = r.nextInt(ku.length());
            str.append(ku.charAt(index));
        }
        return str.toString();
    }

    public static String getRandomNum(int count) {
        String ku = "123456789";
        StringBuilder str = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < count; i++) {
            int index = r.nextInt(ku.length());
            str.append(ku.charAt(index));
        }
        return str.toString();
    }

    /**
     * 检查是否是数字
     *
     * @param content
     * @return
     */
    public static boolean isNumeric(String content) {
        Pattern pattern = Pattern.compile("^([0-9]*)$");
        Matcher isNum = pattern.matcher(content);
        if (!isNum.matches()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Unicode 转换成utf-8
     *
     * @param str
     * @return
     */
    public static String utf8Encoding(String str) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(str);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            str = str.replace(matcher.group(1), ch + "");
        }
        return str;
    }

    public static String desensiTextsToStr(List<String> texts) {
        if (DataUtil.isEmpty(texts)) {
            return null;
        }

        return texts.stream().map(DataUtil::desensiText).collect(Collectors.joining(", "));
    }

    /**
     * 对文本进行脱敏 左右两边最多显示6个字符 a a aa a* aaa a** aaaa a**a aaaaa a***a aaaaaa aa***a aaaaaaa aa****a
     * aaaaaaaa aa****aa aaaaaaaaa aa*****aa aaaaaaaaaa aaa*****aa
     *
     * @param text
     * @return
     */
    public static String desensiText(String text) {
        if (DataUtil.isEmpty(text)) {
            return text;
        }

        int len = text.length();
        if (len == 1) {
            return text;
        }

        int left = (len - 2) / 4 + 1;
        int right = left + (len % 2 == 0 ? len / 2 : len / 2 + 1);

        if (left > 6) {
            left = 6;
            right = len - left;
        }

        char[] chars = text.toCharArray();
        for (int i = left; i < right; i++) {
            chars[i] = '#';
        }
        return new String(chars);
    }

    /**
     * 对文本列表进行脱敏
     *
     * @param texts
     * @return
     */
    public static List<String> desensiTexts(List<String> texts) {
        if (DataUtil.isEmpty(texts)) {
            return null;
        }

        return texts.stream()
                .map(
                        i -> {
                            return desensiText(i);
                        })
                .collect(Collectors.toList());
    }

    /**
     * 对js中escape后的内容进行界面
     *
     * @param src
     * @return
     */
    private static String unescape(String src) {
        StringBuffer tmp = new StringBuffer();
        tmp.ensureCapacity(src.length());
        int lastPos = 0, pos = 0;
        char ch;
        while (lastPos < src.length()) {
            pos = src.indexOf("%", lastPos);
            if (pos == lastPos) {
                if (src.charAt(pos + 1) == 'u') {
                    ch = (char) Integer.parseInt(src.substring(pos + 2, pos + 6), 16);
                    tmp.append(ch);
                    lastPos = pos + 6;
                } else {
                    ch = (char) Integer.parseInt(src.substring(pos + 1, pos + 3), 16);
                    tmp.append(ch);
                    lastPos = pos + 3;
                }
            } else {
                if (pos == -1) {
                    tmp.append(src.substring(lastPos));
                    lastPos = src.length();
                } else {
                    tmp.append(src.substring(lastPos, pos));
                    lastPos = pos;
                }
            }
        }
        return tmp.toString();
    }

    /**
     * 将路径取消转义 $.user\[username\] -> user[username]
     *
     * @param path
     * @return
     */
    public static String unescape(String path, Boolean isKeepLast) {
        final char ESCAPE_CHAR = '\\';

        if (DataUtil.isEmpty(path)) {
            return null;
        }

        if (path.startsWith("$.")) {
            path = path.substring(2);
        }

        char[] pathChar = path.toCharArray();
        StringBuilder keyBuild = new StringBuilder();

        int keyIndex = 0;
        for (int i = 0; i < pathChar.length; i++) {

            // 如果当前字符是普通字符，则直接保留
            if (pathChar[i] != ESCAPE_CHAR) {
                keyBuild.append(pathChar[i]);
            }

            // 判断前一个字符是否是转义字符，若是则保留
            if (pathChar[i] == ESCAPE_CHAR && i - 1 >= 0 && pathChar[i - 1] == ESCAPE_CHAR) {
                keyBuild.append(pathChar[i]);

                // 需要设置为普通字符，防止后续判断是认为该字符为转义字符
                pathChar[i] = 'a';
            }

            if (isKeepLast) {

                // 如果遇到点号，且前置字符不是转义符，那么需要清空keyBuild，因为$.a.b.c -> c
                if (pathChar[i] == '.' && i - 1 >= 0 && pathChar[i - 1] != ESCAPE_CHAR) {
                    keyBuild.delete(0, keyBuild.length());
                }
            }
        }

        return keyBuild.toString();
    }

    /**
     * 首字母大写
     *
     * @param str
     * @return
     */
    public static String upperCase(String str) {
        char[] ch = str.toCharArray();
        if (ch[0] >= 'a' && ch[0] <= 'z') {
            ch[0] = (char) (ch[0] - 32);
        }
        return new String(ch);
    }

    public static List subList(List list, Integer from, Integer to) {
        if (DataUtil.isEmpty(list)) {
            return null;
        }

        int size = list.size();
        if (from >= size) {
            return null;
        }
        if (to >= size) {
            to = size - 1;
        }

        return list.subList(from, to);
    }

    /**
     * 保留2位小数
     *
     * @param value
     * @return
     */
    public static Object keepFloatFix(Object value, int num) {
        if (DataUtil.isEmpty(value)) {
            return value;
        }
        BigDecimal bd = null;
        if (value instanceof Float) {
            bd = new BigDecimal((Float) value);
        }
        if (value instanceof Double) {
            bd = new BigDecimal((Double) value);
        }

        return bd.setScale(num, RoundingMode.HALF_UP).floatValue();
    }

    /**
     * 去除字符串首尾空格
     *
     * @param str
     * @return
     */
    public static String trim(String str) {
        if (DataUtil.isEmpty(str)) {
            return str;
        }

        return str.trim();
    }

    /**
     * 正则表达式中的特殊字符
     *
     * @param fieldValue
     * @return
     */
    public static String regexStrEscape(String fieldValue) {
        if (DataUtil.isEmpty(fieldValue)) {
            return fieldValue;
        }
        return fieldValue
                .replaceAll("\\\\", "\\\\\\\\")
                .replaceAll("\\?", "\\\\\\?")
                .replaceAll("\\^", "\\\\\\^")
                .replaceAll("\\$", "\\\\\\$")
                .replaceAll("\\{", "\\\\\\{")
                .replaceAll("\\}", "\\\\\\}")
                .replaceAll("\\[", "\\\\\\[")
                .replaceAll("\\]", "\\\\\\]")
                .replaceAll("\\(", "\\\\\\(")
                .replaceAll("\\)", "\\\\\\)")
                .replaceAll("\\*", "\\\\\\*")
                .replaceAll("\\+", "\\\\\\+")
                .replaceAll("\\.", "\\\\\\.");
    }

    public static String urlDecode(String s) {

        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (Exception e) {
            // ignore
        }

        try {
            return unescape(s);
        } catch (Exception e) {
            // ignore
        }

        // 解码失败，返回原始内容
        return s;
    }

    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            // ignore
        }

        // 编码失败，返回原始内容
        return s;
    }

    public static <T> List<T> array2list(T[] array) {
        if (array == null || array.length == 0) {
            return null;
        }

        List<T> list = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            list.add(array[i]);
        }
        return list;
    }

    private static String replaceKey(String key, String... replaces) {
        if (replaces == null || replaces.length == 0) {
            return key;
        }

        for (int i = 0; i < replaces.length; i = i + 2) {
            key = key.replaceAll(replaces[i], replaces[i + 1]);
        }
        return key;
    }

    /**
     * ip地址转成long型数字 将IP地址转化成整数的方法如下： 1、通过String的split方法按.分隔得到4个长度的数组
     * 2、通过左移位操作（<<）给每一段的数字加权，第一段的权为2的24次方，第二段的权为2的16次方，第三段的权为2的8次方，最后一段的权为1
     *
     * @param strIp
     * @return
     */
    public static long ipToLong(String strIp) {
        String[] ip = strIp.split("\\.");
        return (Long.parseLong(ip[0]) << 24)
                + (Long.parseLong(ip[1]) << 16)
                + (Long.parseLong(ip[2]) << 8)
                + Long.parseLong(ip[3]);
    }

    /**
     * 将十进制整数形式转换成127.0.0.1形式的ip地址 将整数形式的IP地址转化成字符串的方法如下：
     * 1、将整数值进行右移位操作（>>>），右移24位，右移时高位补0，得到的数字即为第一段IP。 2、通过与操作符（&）将整数值的高8位设为0，再右移16位，得到的数字即为第二段IP。
     * 3、通过与操作符吧整数值的高16位设为0，再右移8位，得到的数字即为第三段IP。 4、通过与操作符吧整数值的高24位设为0，得到的数字即为第四段IP。
     *
     * @param longIp
     * @return
     */
    public static String longToIP(long longIp) {
        StringBuffer sb = new StringBuffer("");
        // 直接右移24位
        sb.append(String.valueOf((longIp >>> 24)));
        sb.append(".");
        // 将高8位置0，然后右移16位
        sb.append(String.valueOf((longIp & 0x00FFFFFF) >>> 16));
        sb.append(".");
        // 将高16位置0，然后右移8位
        sb.append(String.valueOf((longIp & 0x0000FFFF) >>> 8));
        sb.append(".");
        // 将高24位置0
        sb.append(String.valueOf((longIp & 0x000000FF)));
        return sb.toString();
    }

    public static boolean isADateFormat(int formatIndex, String formatString) {
        if (isInternalDateFormat(formatIndex)) {
            return true;
        }

        if ((formatString == null) || (formatString.length() == 0)) {
            return false;
        }

        String fs = formatString;
        // 下面这一行是自己手动添加的 以支持汉字格式wingzing
        fs = fs.replaceAll("[\"|\']", "").replaceAll("[年|月|日|时|分|秒|毫秒|微秒]", "");

        fs = fs.replaceAll("\\\\-", "-");

        fs = fs.replaceAll("\\\\,", ",");

        fs = fs.replaceAll("\\\\.", ".");

        fs = fs.replaceAll("\\\\ ", " ");

        fs = fs.replaceAll(";@", "");

        fs = fs.replaceAll("^\\[\\$\\-.*?\\]", "");

        fs = fs.replaceAll("^\\[[a-zA-Z]+\\]", "");

        return (fs.matches("^[yYmMdDhHsS\\-/,. :]+[ampAMP/]*$"));
    }

    public static boolean isInternalDateFormat(int format) {
        switch (format) {
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 45:
            case 46:
            case 47:
                return true;
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
        }
        return false;
    }

    public static boolean isValidExcelDate(double value) {
        return (value > -4.940656458412465E-324D);
    }

    public static String formatPercent(double number, int scale) {
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMaximumFractionDigits(scale);
        return format.format(number);
    }

    public static double div(float v1, float v2) {
        return div(v1, v2, 10);
    }

    public static double div(float v1, float v2, int scale) {
        return div(v1, v2, scale, RoundingMode.HALF_UP);
    }

    public static double div(float v1, float v2, int scale, RoundingMode roundingMode) {
        return div(Float.toString(v1), Float.toString(v2), scale, roundingMode).doubleValue();
    }

    public static BigDecimal div(String v1, String v2, int scale, RoundingMode roundingMode) {
        return div(toBigDecimal(v1), toBigDecimal(v2), scale, roundingMode);
    }

    public static BigDecimal div(
            BigDecimal v1, BigDecimal v2, int scale, RoundingMode roundingMode) {
        Objects.requireNonNull(v2, "Divisor must be not null !");
        if (null == v1) {
            return BigDecimal.ZERO;
        } else {
            if (scale < 0) {
                scale = -scale;
            }

            return v1.divide(v2, scale, roundingMode);
        }
    }

    public static BigDecimal toBigDecimal(String numberStr) {
        if (isEmpty(numberStr)) {
            return BigDecimal.ZERO;
        } else {
            try {
                Number number = parseNumber(numberStr);
                return number instanceof BigDecimal
                        ? (BigDecimal) number
                        : new BigDecimal(number.toString());
            } catch (Exception var2) {
                return new BigDecimal(numberStr);
            }
        }
    }

    public static Number parseNumber(String numberStr) throws NumberFormatException {
        try {
            NumberFormat format = NumberFormat.getInstance();
            if (format instanceof DecimalFormat) {
                ((DecimalFormat) format).setParseBigDecimal(true);
            }

            return format.parse(numberStr);
        } catch (ParseException var3) {
            NumberFormatException nfe = new NumberFormatException(var3.getMessage());
            nfe.initCause(var3);
            throw nfe;
        }
    }

    private static final char[] HEX_CHARS =
            new char[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
            };

    /**
     * 字节数组转16进制字符串
     *
     * @param input
     * @return
     */
    public static String bytes2HexString(byte[] input) {
        StringBuilder builder = new StringBuilder(input.length);

        for (int i = 0; i < input.length; ++i) {
            builder.append(HEX_CHARS[input[i] >>> 4 & 15]);
            builder.append(HEX_CHARS[input[i] & 15]);
        }

        return builder.toString();
    }

    /**
     * 16进制字符串转字节数组
     *
     * @param s
     * @return
     */
    public static byte[] hexString2Bytes(String s) {
        char[] rawChars = s.toUpperCase().toCharArray();
        int hexChars = 0;

        for (int i = 0; i < rawChars.length; ++i) {
            if (rawChars[i] >= '0' && rawChars[i] <= '9'
                    || rawChars[i] >= 'A' && rawChars[i] <= 'F') {
                ++hexChars;
            }
        }

        byte[] byteString = new byte[hexChars + 1 >> 1];
        int pos = hexChars & 1;

        for (int i = 0; i < rawChars.length; ++i) {
            if (rawChars[i] >= '0' && rawChars[i] <= '9') {
                byteString[pos >> 1] = (byte) (byteString[pos >> 1] << 4);
                byteString[pos >> 1] = (byte) (byteString[pos >> 1] | rawChars[i] - 48);
            } else {
                if (rawChars[i] < 'A' || rawChars[i] > 'F') {
                    continue;
                }

                byteString[pos >> 1] = (byte) (byteString[pos >> 1] << 4);
                byteString[pos >> 1] = (byte) (byteString[pos >> 1] | rawChars[i] - 65 + 10);
            }

            ++pos;
        }

        return byteString;
    }
}
