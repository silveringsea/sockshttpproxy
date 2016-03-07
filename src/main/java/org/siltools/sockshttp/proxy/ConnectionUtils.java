package org.siltools.sockshttp.proxy;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 */
public class ConnectionUtils {

    static final Logger logger = LoggerFactory.getLogger(DefaultConnectionPipeline.class);

    public static void invokeExceptionCaughtNow(final ChannelHandlerContext ctx, final Throwable cause) {
        try {
            ctx.handler().exceptionCaught(ctx, cause);
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("An exception was thrown by a user handler's exceptionCaught() method:", t);
                logger.warn(".. and the cause of the exceptionCaught() was:", cause);
            }
        }
    }

    static void notifyHandlerException(ChannelHandlerContext ctx, Throwable cause) {
        invokeExceptionCaughtNow(ctx, cause);
    }
}
