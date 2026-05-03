package com.dfca.common.utils;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;
/**
 * Pattern定义如下
 %CustomStack{showCausedBy:false}{includeRegex:'com\.dfca\..*'}{excludeRegex:'java\..*|org\..*|com\..*'}
 */
@Plugin(name = "Log4jThrowableConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"CustomStack"})
public class LogFilterForLog4j extends ThrowablePatternConverter {

    private LogFilter logFilter;

    protected LogFilterForLog4j(Configuration config, String[] options) {

        super("CustomStack", "stack", null, config);

        // 使用公共解析器
        //logFilter.Options opts = logFilter.Options.parse(options);
        logFilter = new LogFilter(options);
    }

    public static LogFilterForLog4j newInstance(Configuration config, String[] options) {
        return new LogFilterForLog4j(config, options);
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        Throwable throwable = event.getThrown();
        if (throwable == null) return;
        // 使用公共逻辑
        toAppendTo.append(logFilter.formatStack(throwable));
    }
}
