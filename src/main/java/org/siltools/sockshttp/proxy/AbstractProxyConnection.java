package org.siltools.sockshttp.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.siltools.sockshttp.def.ConnectionState;

/**
 * Created by Administrator on 3/3/2016.
 */
public class AbstractProxyConnection extends SimpleChannelInboundHandler<Object>   {
    protected ConnectionState curConnectionState;
    IConnectionPipeline connPipeline;

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
//        connPipeline.fir
    }
}
