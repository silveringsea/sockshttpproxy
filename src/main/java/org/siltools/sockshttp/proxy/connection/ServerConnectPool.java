package org.siltools.sockshttp.proxy.connection;

import org.siltools.sockshttp.proxy.IClientToProxyConnection;
import org.siltools.sockshttp.proxy.IProxyConnection;
import org.siltools.sockshttp.proxy.IProxyToServerConnection;
import org.siltools.sockshttp.proxy.ProxyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 3/4/2016.
 */
public enum ServerConnectPool {
    INSTANCE;

    public static ServerConnectPool getInstance() {
        return INSTANCE;
    }

    private static Logger logger = LoggerFactory.getLogger(ServerConnectPool.class);
    private static final Map<String, IProxyConnection> serverConnectionsByHostAndPort = new ConcurrentHashMap<String, IProxyConnection>();

    public synchronized IProxyConnection get(String hostAndPort) {
        return null;// serverConnectionsByHostAndPort.get(hostAndPort);
    }

    public synchronized void put(String hostAndPort, IProxyConnection connection) {
        //serverConnectionsByHostAndPort.put(hostAndPort, connection);
    }

    public synchronized void remove(String hostAndPort) {
        serverConnectionsByHostAndPort.remove(hostAndPort);
    }
    /**
     * When a server becomes saturated, we stop reading from the client.
     * @param serverConnection
     */
    synchronized protected void serverBecameSaturated(IProxyToServerConnection serverConnection,
                                                      IClientToProxyConnection clientToProxyConnection) {
        if (serverConnection.isSaturated()) {
            logger.info("Connection to server became saturated, stopping reading");
            clientToProxyConnection.stopAutoRead();
        }
    }

    /**
     * When the ClientToProxyConnection becomes saturated, stop reading on all
     * associated ProxyToServerConnections.
     */
    synchronized protected void becameSaturated(IProxyConnection clientConn) {
        for (IProxyConnection serverConnection : serverConnectionsByHostAndPort
                .values()) {
            synchronized (serverConnection) {
                if (clientConn.isSaturated()) {
                    serverConnection.stopAutoRead();
                }
            }
        }
    }

    /**
     * When the ClientToProxyConnection becomes writable, resume reading on all
     * associated ProxyToServerConnections.
     */
    synchronized protected void becameWritable(IProxyConnection clientConn) {
        for (IProxyConnection serverConnection : serverConnectionsByHostAndPort
                .values()) {
            synchronized (serverConnection) {
                if (!clientConn.isSaturated()) {
                    serverConnection.resumeAutoRead();
                }
            }
        }
    }
    /**
     * When a server becomes writeable, we check to see if all servers are
     * writeable and if they are, we resume reading.
     */
    synchronized protected void serverBecameWriteable(IProxyConnection clientConn) {
        boolean anyServersSaturated = false;
        for (IProxyConnection otherServerConnection : serverConnectionsByHostAndPort.values()) {
            if (otherServerConnection instanceof ProxyConnection && ((ProxyConnection)otherServerConnection).isSaturated()) {
                anyServersSaturated = true;
                break;
            }
        }
        if (!anyServersSaturated) {
            logger.info("All server connections writeable, resuming reading");
            clientConn.resumeAutoRead();
        }
    }
}
