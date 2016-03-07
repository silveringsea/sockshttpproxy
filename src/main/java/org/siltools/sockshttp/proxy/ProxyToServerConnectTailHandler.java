package org.siltools.sockshttp.proxy;

/**
 * Created by Administrator on 3/4/2016.
 */
public class ProxyToServerConnectTailHandler extends ConnectionHandlerAdapter {
    public int connectionType() {
        return IConnectionHandlerContext.CONNECTION_TYPE_PROXY;
    }
}
