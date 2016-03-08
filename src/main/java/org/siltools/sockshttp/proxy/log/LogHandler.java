package org.siltools.sockshttp.proxy.log;

import io.netty.handler.codec.http.HttpRequest;
import org.siltools.sockshttp.proxy.ClientConnHandlerAdapter;
import org.siltools.sockshttp.proxy.IConnectionHandlerContext;

/**
 * Created by Administrator on 3/8/2016.
 */
public class LogHandler extends ClientConnHandlerAdapter {

    @Override
    public void messageReceive(IConnectionHandlerContext ctx, Object... objects) {
        if (objects == null && !(objects[0] instanceof HttpRequest)) {
            return;
        }

        LogFactory.getInstance().logUrl((HttpRequest)objects[0]);
    }
}
