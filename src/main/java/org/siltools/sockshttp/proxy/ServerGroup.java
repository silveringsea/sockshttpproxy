package org.siltools.sockshttp.proxy;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.udt.nio.NioUdtProvider;
import org.siltools.sockshttp.def.TransportProtocol;
import org.siltools.sockshttp.def.exception.UnknownTransportProtocolException;
import org.siltools.sockshttp.util.SocksHttpProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.spi.SelectorProvider;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Administrator on 1/11/2016.
 */
public class ServerGroup {
    private static Logger LOG = LoggerFactory.getLogger(ServerGroup.class);

    String name;
    private AtomicInteger serverGroupCount = new AtomicInteger(0);
    /**
     * The ID of this server group. Forms part of the name of each thread created for this server group. Useful for
     * differentiating threads when multiple proxy instances are running.
     */
    private final int serverGroupId;

    private final int incomingAcceptorThreads;
    private final int incomingWorkerThreads;
    private final int outgoingWorkerThreads;


    private final Object THREAD_POOL_INIT_LOCK = new Object();

    private final EnumMap<TransportProtocol, ProxyThreadPools> protocolThreadPools = new EnumMap<TransportProtocol, ProxyThreadPools>(TransportProtocol.class);

    private static final EnumMap<TransportProtocol, SelectorProvider> TRANSPORT_PROTOCOL_SELECTOR_PROVIDERS = new EnumMap<TransportProtocol, SelectorProvider>(TransportProtocol.class);
    static {
        TRANSPORT_PROTOCOL_SELECTOR_PROVIDERS.put(TransportProtocol.TCP, SelectorProvider.provider());

        // allow the proxy to operate without UDT support. this allows clients that do not use UDT to exclude the barchart
        // dependency completely.
        if (SocksHttpProxyUtils.isUdtAvailable()) {
            TRANSPORT_PROTOCOL_SELECTOR_PROVIDERS.put(TransportProtocol.UDT, NioUdtProvider.BYTE_PROVIDER);
        } else {
            LOG.debug("UDT provider not found on classpath. UDT transport will not be available.");
        }
    }

    public ServerGroup(String name, int incomingAcceptorThreads, int incomingWorkerThreads, int outgoingWorkerThreads) {
        this.name = name;
        this.serverGroupId = serverGroupCount.getAndIncrement();
        this.incomingAcceptorThreads = incomingAcceptorThreads;
        this.incomingWorkerThreads = incomingWorkerThreads;
        this.outgoingWorkerThreads = outgoingWorkerThreads;
    }

    private ProxyThreadPools getThreadPoolsForProtocol(TransportProtocol protocol) {
        // if the thread pools have not been initialized for this protocol, initialize them
        if (protocolThreadPools.get(protocol) == null) {
            synchronized (THREAD_POOL_INIT_LOCK) {
                if (protocolThreadPools.get(protocol) == null) {
                    LOG.warn("Initializing thread pools for {} with {} acceptor threads, {} incoming worker threads, and {} outgoing worker threads",
                            protocol, incomingAcceptorThreads, incomingWorkerThreads, outgoingWorkerThreads);

                    SelectorProvider selectorProvider = TRANSPORT_PROTOCOL_SELECTOR_PROVIDERS.get(protocol);
                    if (selectorProvider == null) {
                        throw new UnknownTransportProtocolException(protocol);
                    }

                    ProxyThreadPools threadPools = new ProxyThreadPools(selectorProvider,
                            incomingAcceptorThreads,
                            incomingWorkerThreads,
                            outgoingWorkerThreads,
                            name,
                            serverGroupId);
                    protocolThreadPools.put(protocol, threadPools);
                }
            }
        }

        return protocolThreadPools.get(protocol);
    }

    public EventLoopGroup getProxyToServerWorkerPoolForTransport(TransportProtocol protocol) {
        return getThreadPoolsForProtocol(protocol).getProxyToServerWorkerPool();
    }

    public EventLoopGroup getClientToProxyAcceptorPoolForTransport(TransportProtocol protocol) {
        return getThreadPoolsForProtocol(protocol).getClientToProxyAcceptorPool();
    }

    public EventLoopGroup getClientToProxyWorkerPoolForTransport(TransportProtocol protocol) {
        return getThreadPoolsForProtocol(protocol).getClientToProxyWorkerPool();
    }
}
