package org.siltools.sockshttp.proxy.log;

import io.netty.handler.codec.http.HttpObject;

/**
 * Created by Administrator on 3/8/2016.
 */
public enum LogFactory {
    INSTANCE;

    ILogger logger;
    boolean isLogEnabled;

    public static LogFactory getInstance() {
        return INSTANCE;
    }

    private LogFactory() {

    }

    public void logUrl(HttpObject httpObject) {
        if (!isLogEnabled) {
            return;
        }

        if (logger != null) {
            logger.logUrl(httpObject);
        }
    }

    public void setLogger(ILogger logger) {
        this.logger = logger;
    }
}
