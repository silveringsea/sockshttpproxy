package org.siltools.sockshttp;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import org.siltools.sockshttp.def.TransportProtocol;

import java.net.InetSocketAddress;

/**
 * Created by Administrator on 1/11/2016.
 */
public interface SocksHttpProxyServer {

    int getIdleConnectionTimeout();

    int getConnectTimeout();

//    ProxyAuthenticator getProxyAuthenticator();

    EventLoopGroup getProxyToServerThreadPool(TransportProtocol transportProtocol);

//    public HostResolver getHostResolver();
//
//    public void setHostResolver();

    public InetSocketAddress getLocalAddress();

    void registerChannel(Channel channel);

    void unRegisterChannel(Channel channel);

    void closeAllChannels(boolean gracefully);

    SocksHttpProxyServer start();

    void stop();

    void shutdown();
}
