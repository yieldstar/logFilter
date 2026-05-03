package com.dfca.common.utils;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.IThrowableProxy;

import java.util.List;

public class LogFilterForLogBack extends ThrowableProxyConverter {

    private LogFilter logFilter;

    // 必须提供无参构造方法
    public LogFilterForLogBack() {
        super();
    }

    @Override
    public void start() {
        List<String> options = getOptionList();
        if (options!=null)
          logFilter = new LogFilter(options.toArray(new String[0]));
        super.start();
    }

    @Override
    protected String throwableProxyToString(IThrowableProxy tp) {
        return logFilter.formatStack(tp);
    }

}

