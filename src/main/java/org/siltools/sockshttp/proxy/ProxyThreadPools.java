package org.siltools.sockshttp.proxy;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultExecutorServiceFactory;

import java.nio.channels.spi.SelectorProvider;

/**
 * 包含4个eventloopGroup
 */
public class ProxyThreadPools {

    private final EventLoopGroup clientToProxyAcceptorPool;

    private final NioEventLoopGroup clientToProxyWorkerPool;

    private final NioEventLoopGroup proxyToServerWorkerPool;

    public ProxyThreadPools(SelectorProvider selectorProvider, int inboundAcceptorThreads, int inboundWorkerThreads, int outboundWorkerThreads, String serverGroupName, int serverGroupId) {
        clientToProxyAcceptorPool = new NioEventLoopGroup(inboundAcceptorThreads, new DefaultExecutorServiceFactory(serverGroupName + "_" + "ClientToProxyAcceptor" + "_" + serverGroupId), selectorProvider);

        clientToProxyWorkerPool = new NioEventLoopGroup(inboundWorkerThreads, new DefaultExecutorServiceFactory(serverGroupName + "_" + "ClientToProxyWorker" + "_" + serverGroupId), selectorProvider);
        clientToProxyWorkerPool.setIoRatio(90);

        proxyToServerWorkerPool = new NioEventLoopGroup(outboundWorkerThreads, new DefaultExecutorServiceFactory(serverGroupName + "_" + "ProxyToServerWorker" + "_" + serverGroupId), selectorProvider);
        proxyToServerWorkerPool.setIoRatio(90);
    }

    public EventLoopGroup getClientToProxyAcceptorPool() {
        return clientToProxyAcceptorPool;
    }

    public EventLoopGroup getClientToProxyWorkerPool() {
        return clientToProxyWorkerPool;
    }

    public EventLoopGroup getProxyToServerWorkerPool() {
        return proxyToServerWorkerPool;
    }
}
