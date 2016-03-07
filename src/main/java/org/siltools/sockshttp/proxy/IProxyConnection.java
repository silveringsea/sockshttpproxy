package org.siltools.sockshttp.proxy;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.concurrent.Future;
import org.siltools.sockshttp.def.ConnectionState;


/**
 * Created by Administrator on 1/13/2016.
 */
public interface IProxyConnection {

    public static final HttpResponseStatus CONNECTION_ESTABLISHED = new HttpResponseStatus(
            200, "HTTP/1.1 200 Connection established");
    public final static String CLIENT_ENCODER = "clientEncoder";
    public final static String SERVER_ENCODER = "serverEncoder";
    public final static String CLIENT_IDLE = "clientIdle";

    public static enum IProxyConnectionType {
        CLIENT,
        SERVER
    }

    void becomeState(ConnectionState connectionState);

//    void initChannelPipeline(ChannelPipeline pipeline);

    IProxyConnectionType getType();

    Future disconnect();

    ConnectionState getCurrentState();

    void stopAutoRead();

    void resumeAutoRead();

    void writeDataToChannel(Object msg);

//    SocksHttpProxyServer socksHttpProxyServer();

    boolean isSaturated();

    void submitChannelTask(Runnable r);
//    ConnectionFlowStep getStartTunnelingFlowStep();
}

