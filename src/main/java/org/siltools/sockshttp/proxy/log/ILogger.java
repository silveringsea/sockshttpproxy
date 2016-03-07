package org.siltools.sockshttp.proxy.log;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpObject;

/**
 */
public interface ILogger {

    void logUrl(HttpObject httpObject);

    void logRaw(ByteBuf msg);
}
