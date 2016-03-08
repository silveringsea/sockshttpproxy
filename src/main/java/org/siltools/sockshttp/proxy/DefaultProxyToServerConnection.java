package org.siltools.sockshttp.proxy;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.apache.commons.lang3.StringUtils;
import org.siltools.sockshttp.def.ConnectionState;
import org.siltools.sockshttp.def.TransportProtocol;
import org.siltools.sockshttp.util.SocksHttpProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Administrator on 1/12/2016.
 */
public class DefaultProxyToServerConnection extends ProxyConnection<HttpResponse> implements IProxyToServerConnection  {
//    AbstractClientToProxyConnection
//    IClientToProxyConnection clientConnection;
//    protected ConnectionFlow connectionFlow;
    volatile HttpRequest initialHttpRequest;
    protected volatile String serverHostAndPort;
    private volatile InetSocketAddress remoteAddress;
    private volatile InetSocketAddress localAddress;
    private volatile TransportProtocol transportProtocol;

    private static transient Logger LOG = LoggerFactory.getLogger(DefaultProxyToServerConnection.class);

    private volatile Lock connectionLock = new ReentrantLock();
    private volatile Condition connectionLockCond = connectionLock.newCondition();
    volatile boolean isTunneling = false;

    public void setTransportProtocol(TransportProtocol transportProtocol) {
        this.transportProtocol = transportProtocol;
    }

    public DefaultProxyToServerConnection(IConnectionPipeline connectionPipeline,
                                          HttpRequest initialHttpRequest, String serverHostAndPort) {
        this(connectionPipeline, initialHttpRequest, serverHostAndPort, TransportProtocol.TCP);
    }

    public DefaultProxyToServerConnection(IConnectionPipeline connectionPipeline,
                                          HttpRequest initialHttpRequest, String serverHostAndPort,
                                          TransportProtocol transportProtocol) {
        super(connectionPipeline);
        curConnectionState = ConnectionState.DISCONNECTED;
        this.initialHttpRequest = initialHttpRequest;
        this.serverHostAndPort = serverHostAndPort;
        this.transportProtocol = transportProtocol;
        this.isTunneling = true;
        setupConnectionParameters();
    }

    protected void setupConnectionParameters() {
        String[] serverHostPortStrs = StringUtils.split(serverHostAndPort, ":");
        String host = serverHostPortStrs[0];
        int port = (serverHostPortStrs.length > 1 && serverHostPortStrs[1] != null) ? Integer.parseInt(serverHostPortStrs[1]): 80;
        remoteAddress = new InetSocketAddress(host, port);
    }

    public String getServerHostAndPort() {
        return serverHostAndPort;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void resentHttpRequest(HttpRequest httpRequest) {
        if (getCurrentState().isPartOfConnectionFlow()) {
            connectionLock.lock();
            if (getCurrentState().isPartOfConnectionFlow()) {
                stopAutoRead();
                try {
                    connectionLockCond.await(1000000, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    LOG.error(" if is connecting fail ", e);
                }
            }
            connectionLock.unlock();
        }

        if (getCurrentState().isPartOfConnectionFlow() || getCurrentState().isDisconnectingOrDisconnected()) {
            // connect fail
            LOG.error("connect fail");
            return;
        }

        writeDataToChannel(httpRequest);
    }

    protected ConnectionState readHTTPInitial(HttpResponse httpResponse) {
        LOG.debug("Received raw response: {}", httpResponse);
        if (SocksHttpProxyUtils.isChunked(httpResponse)) {
            return ConnectionState.AWAITING_CHUNK;
        } else {
            return ConnectionState.AWAITING_INITIAL;
        }
    }

    public void connectionSucceeded(boolean shouldForwardInitialRequest) {
        becomeState(ConnectionState.AWAITING_INITIAL);
        if (shouldForwardInitialRequest) {
            if (this.channel.isActive()) {
                writeDataToChannel(this.initialHttpRequest);
            } else {
                LOG.error("channel is not active");
            }
        }
    }

    public boolean connectionFailed(Throwable cause) throws UnknownHostException {
        return false;
    }

    public HttpRequest getInitialRequest() {
        return this.initialHttpRequest;
    }

    public IProxyConnectionType getType() {
        return IProxyConnectionType.SERVER;
    }
}
