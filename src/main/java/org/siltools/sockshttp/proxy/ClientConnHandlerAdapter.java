package org.siltools.sockshttp.proxy;

/**
 * Created by Administrator on 3/5/2016.
 */
public class ClientConnHandlerAdapter extends ConnectionHandlerAdapter {

//    public void connecting(IConnectionHandlerContext ctx, Object object) {
//
//    }
//
//    public void disconnected(IConnectionHandlerContext ctx, Object object) {
//
//    }

    public int connectionType() {
        return IConnectionHandlerContext.CONNECTION_TYPE_CLIENT;
    }

}
