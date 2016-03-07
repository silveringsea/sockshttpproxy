package org.siltools.sockshttp.util;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * Created by Administrator on 12/30/2015.
 */
public class ChannelUtils {

    public static void channelCloseOnFlush(Channel chl) {
        if (chl.isActive()) {
            chl.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
