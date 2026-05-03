package com.dfca.common.utils;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class LogFilter {

    private boolean showCausedBy = true;
    private static final String itemSplit=":";
    //private int maxDepth = -1;

    private Pattern includePattern;
    private Pattern excludePattern;

    // 必须提供无参构造方法
    public LogFilter() {
        super();
    }

    public LogFilter(String[] options) {
        if (options != null) {
            Map<String,String> optionsMap=parseOptionToMap(options);
            getConfigFromMap(optionsMap);
        }
    }

    /**
    private void parseOption(String option) {
        if (isBlank(option)) {
            return;
        }
        // 去除首尾空格
        option = option.trim();
        // 解析 showCausedBy:false
        if (option.startsWith("showCausedBy:")) {
            String value = extractValue(option);
            if (value != null) {
                showCausedBy = Boolean.parseBoolean(value);
                //System.out.println("=== showCausedBy 设置为: " + showCausedBy); // 调试用
            }
        } else if (option.startsWith("includeRegex:")) {
            String regex = extractValue(option);
            if (regex != null && !regex.isEmpty()) {
                includePattern = Pattern.compile(regex);
            }
        } else if (option.startsWith("excludeRegex:")) {
            String regex = extractValue(option);
            if (regex != null && !regex.isEmpty()) {
                excludePattern = Pattern.compile(regex);
            }
        }
    } 
    */

    /**
     * options是以：作为分隔符的字符串配置项的数组，形式为
     * key:value
     * key:'value'
     * key:"value"
     * @param options
     * @return
     */

    private Map<String, String> parseOptionToMap(String[] options) {
        Map<String, String> result = new HashMap<>();
        for (String option : options) {
            if (isBlank(option)) {
                continue;
            }
            option = option.trim();
            // 查找分隔符位置
            int eqIndex = option.indexOf(itemSplit);
            if (eqIndex <= 0) {
                continue; // 跳过
            }
            // 提取 key 和 value
            String key = option.substring(0, eqIndex).trim();
            if (isBlank(key))
                continue;
            String value = option.substring(eqIndex + 1).trim();
            // 解析可能带引号的 value
            String parsedValue = parseQuotedValue(value);
            if (isBlank(parsedValue))
                continue;
            // 存入 Map
            result.put(key, parsedValue);
        }
        return result;
    }

    private void getConfigFromMap(Map<String, String> optionsMap) {
        String value = optionsMap.get("showCausedBy");
        if (value != null) {
            showCausedBy = Boolean.parseBoolean(value);
            //System.out.println("=== showCausedBy 设置为: " + showCausedBy); // 调试用
        }

        value = optionsMap.get("includeRegex");
        if (value != null && !value.isEmpty()) {
            includePattern = Pattern.compile(value);
        }

        value = optionsMap.get("excludeRegex");
        if (value != null && !value.isEmpty()) {
            excludePattern = Pattern.compile(value);
        }
    }

    /**
     * 解析可能带引号的值
     */
    private String parseQuotedValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // 双引号包裹
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }

        // 单引号包裹
        if (value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    /**
     * 从 option 字符串中提取值，处理引号和空格
     * 例如: "includeRegex:'com\.dfca\..*'" -> "com\.dfca\..*"
     */
    private String extractValue(String option) {
        int colonIdx = option.indexOf(':');
        if (colonIdx < 0 || colonIdx == option.length() - 1) {
            return null;
        }

        String value = option.substring(colonIdx + 1).trim();

        // 处理单引号
        if (value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
        }
        // 处理双引号
        else if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }

        return value;
    }

    public String formatStack(ch.qos.logback.classic.spi.IThrowableProxy tp) {
        StringBuilder sb = new StringBuilder();
        buildStackTrace(sb, tp, null, 0);
        return sb.toString();
    }

    //logback的堆栈输出
    public String formatStack(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        buildStackTrace(sb, throwable, null, 0);
        return sb.toString();
    }

    //log4J与logback的堆栈输出实现
    private void buildStackTrace(StringBuilder sb, Object t, String prefix, int depth) {
        if (t == null) return;

        if (prefix != null) sb.append(prefix);

        boolean hasBusinessLine = false;

        if (t instanceof Throwable) {
            //log4j
            Throwable throwable = (Throwable) t;
            sb.append(throwable.getClass().getName()).append(": ");
            String msg = throwable.getMessage();
            if (msg != null) {
                sb.append(msg);
            }
            sb.append(System.lineSeparator());

            for (StackTraceElement element : throwable.getStackTrace()) {
                String className = element.getClassName();
                if (shouldKeep(className)) {
                    sb.append("\t ").append(element.toString()).append(System.lineSeparator());
                    hasBusinessLine = true;
                }
            }
            // 提示信息（可选）
            if (!hasBusinessLine && prefix != null) {
                sb.append("\t at [非业务代码，堆栈已过滤]").append(System.lineSeparator());
            }
            if (showCausedBy && throwable.getCause() != null) {
                buildStackTrace(sb, throwable.getCause(), "Caused by: ", depth + 1);
            }
        } else if (t instanceof IThrowableProxy) {
            //logback
            IThrowableProxy proxy = (IThrowableProxy) t;
            sb.append(proxy.getClassName()).append(": ");
            String msg = proxy.getMessage();
            if (msg != null) {
                sb.append(msg);
            }
            sb.append(System.lineSeparator());

            for (StackTraceElementProxy step : proxy.getStackTraceElementProxyArray()) {
                String className = step.getStackTraceElement().getClassName();
                if (shouldKeep(className)) {
                    sb.append("\t ").append(step.toString()).append(System.lineSeparator());
                    hasBusinessLine = true;
                }
            }
            // 提示信息（可选）
            if (!hasBusinessLine && prefix != null) {
                sb.append("\tat [非业务代码，堆栈已过滤]").append(System.lineSeparator());
            }
            if (showCausedBy && proxy.getCause() != null) {
                buildStackTrace(sb, proxy.getCause(), "Caused by: ", depth + 1);
            }
        } else {
            sb.append("[未知异常类型: ").append(t.getClass().getName()).append("]").append(System.lineSeparator());
            System.out.println("错误的日志对象");
        }
    }

    private boolean shouldKeep(String className) {
        boolean shouldInclude = true;
        boolean shouldExclude = true;
        // 应用 exclude 正则（如果配置了）
        if (shouldInclude && excludePattern != null) {
            shouldExclude = !excludePattern.matcher(className).matches();
        }

        // 应用 include 正则（如果配置了）
        if (includePattern != null) {
            if (className.contains("CGLIB"))
                shouldInclude = false;
            else
                shouldInclude = includePattern.matcher(className).matches();
        }

        return (shouldInclude || shouldExclude);
    }

    private static boolean isBlank(String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}

