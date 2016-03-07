package org.siltools.sockshttp.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.WeakHashMap;

/**
 */
public abstract class AbstractConnHandlerContext implements IConnectionHandlerContext {

    volatile AbstractConnHandlerContext prev;
    volatile AbstractConnHandlerContext next;
    boolean removed = false;

    final DefaultConnectionPipeline pipeline;
    final String name;
    private static final int MASK_INBOUND = MASK_IN_EXCEPTION_CAUGHT |
            MASK_CONN_MESSAGERECEIVE |
            MASK_CONN_PROXYSTATE_CHG;

    private static final int MASK_OUTBOUND = MASK_OUT_EXCEPTION_CAUGHT |
            MASK_CONN_READ_HTTPCHUNK |
            MASK_CONN_READ_RAW |
            MASK_CONN_PROXY_AUTHENTICATION ;

    final int skipFlags;
    protected static ThreadLocal<WeakHashMap<Class<?>, Integer>> skipFlagsCache
            = new ThreadLocal<WeakHashMap<Class<?>, Integer>>();

    protected AbstractConnHandlerContext(DefaultConnectionPipeline pipeline,
                                         String name, int skipFlags) {
        this.pipeline = pipeline;
//        this.proxyConnection = pipeline.connection();
        this.name = name;
        this.skipFlags = skipFlags;
    }

    public String name() {
        return name;
    }

    public IProxyToServerConnection proxyToServerConnection() {
        return (IProxyToServerConnection)pipeline.proxyConnection();
    }

    public IClientToProxyConnection clientToProxyConnection() {
        return (IClientToProxyConnection)pipeline.clientConnection();
    }

    public IConnectionPipeline pipeline() {
        return pipeline;
    }

    @SuppressWarnings("rawtypes")
    private static boolean isSkippable(
            Class<?> handlerType, String methodName, Class<?>... paramTypes) throws Exception {

        Class[] newParamTypes = new Class[paramTypes.length + 1];
        newParamTypes[0] = IConnectionHandlerContext.class;
        System.arraycopy(paramTypes, 0, newParamTypes, 1, paramTypes.length);

        return handlerType.getMethod(methodName, newParamTypes).isAnnotationPresent(IConnectionHandler.Skip.class);
    }

    /**
     * Returns an integer bitset that tells which handler methods were annotated with {@link ChannelHandler.Skip}.
     * It gets the value from {@link #skipFlagsCache} if an handler of the same type were queried before.
     * Otherwise, it delegates to {@link #skipFlags0(Class)} to get it.
     */
    static int skipFlags(IConnectionHandler handler) {
        WeakHashMap<Class<?>, Integer> cache = skipFlagsCache.get();
        Class<? extends IConnectionHandler> handlerType = handler.getClass();
        int flagsVal;
        Integer flags = cache.get(handlerType);
        if (flags != null) {
            flagsVal = flags;
        } else {
            flagsVal = skipFlags0(handlerType);
            cache.put(handlerType, Integer.valueOf(flagsVal));
        }

        return flagsVal;
    }

    /**
     * Determines the {@link #skipFlags} of the specified {@code handlerType} using the reflection API.
     */
    static int skipFlags0(Class<? extends IConnectionHandler> handlerType) {
        int flags = 0;
        try {
            if (isSkippable(handlerType, "exceptionCaught", Throwable.class)) {
                flags |= MASK_IN_EXCEPTION_CAUGHT;
            }
            if (isSkippable(handlerType, "readHTTPInitial", Object.class)) {
                flags |= MASK_CONN_READ_HTTPINITIAL;
            }
            if (isSkippable(handlerType, "readHTTPChunk", Object.class)) {
                flags |= MASK_CONN_READ_HTTPCHUNK;
            }
            if (isSkippable(handlerType, "readRaw", Object.class)) {
                flags |= MASK_CONN_READ_RAW;
            }
            if (isSkippable(handlerType, "proxyAuthentication", Object.class)) {
                flags |= MASK_CONN_PROXY_AUTHENTICATION;
            }
        } catch (Exception e) {
            // Should never reach here.
            throw new RuntimeException("skipFlags0", e);
        }

        return flags;
    }

    private AbstractConnHandlerContext findContextInbound() {
        AbstractConnHandlerContext ctx = this;
        do {
            ctx = ctx.next;
        } while ((ctx.skipFlags & MASK_INBOUND) == MASK_INBOUND);
        return ctx;
    }

    private AbstractConnHandlerContext findContextOutbound() {
        AbstractConnHandlerContext ctx = this;
        do {
            ctx = ctx.prev;
        } while ((ctx.skipFlags & MASK_OUTBOUND) == MASK_OUTBOUND);
        return ctx;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved() {
        this.removed = true;
    }

    public IConnectionHandlerContext fireInitChannelPipeline(ChannelPipeline channelPipeline, int connectionType) {
        final AbstractConnHandlerContext ctx = findContextInbound();
        final IConnectionHandler handler = ctx.handler();
        handler.initChannelPipeline(ctx, channelPipeline, connectionType);
        return this;
    }

    public IConnectionHandlerContext fireMessageReceive(final Object... objects) {
        final AbstractConnHandlerContext ctx = findContextInbound();
        final IConnectionHandler handler = ctx.handler();
        if (handler.shouldExecuteOnEventLoop(MASK_CONN_MESSAGERECEIVE)) {
            ctx.connection().submitChannelTask(new Runnable() {
                public void run() {
                    handler.messageReceive(ctx, objects);
                }
            });
        }
        handler.messageReceive(ctx, objects);
        return this;
    }

    public IConnectionHandlerContext fireProxyStateChange(final Object... objects) {
        final AbstractConnHandlerContext ctx = findContextInbound();
        final IConnectionHandler handler = ctx.handler();
        if (handler.shouldExecuteOnEventLoop(MASK_CONN_PROXYSTATE_CHG)) {
            ctx.connection().submitChannelTask(new Runnable() {
                public void run() {
                    handler.proxyStateChange(ctx, objects);
                }
            });
        }
        handler.messageReceive(ctx, objects);
        return this;
    }

    public IConnectionHandlerContext fireInboundExceptionCaught(Throwable cause, Object objects) throws Exception {
        final AbstractConnHandlerContext ctx = findContextInbound();
        final IConnectionHandler handler = ctx.handler();
        handler.inboundExceptionCaught(ctx, cause);
        return this;
    }

    public IConnectionHandlerContext fireReadHTTPChunk(final Object... objects) {
        final AbstractConnHandlerContext ctx = findContextOutbound();
        final IConnectionHandler handler = ctx.handler();
        if (handler.shouldExecuteOnEventLoop(MASK_CONN_READ_HTTPCHUNK)) {
            ctx.connection().submitChannelTask(new Runnable() {
                public void run() {
                    handler.proxyStateChange(ctx, objects);
                }
            });
        }
        handler.proxyStateChange(ctx, objects);
        return this;
    }

    public IConnectionHandlerContext fireServerConnectedSucc(final Object... objects) {
        final AbstractConnHandlerContext ctx = findContextOutbound();
        final IConnectionHandler handler = ctx.handler();
        handler.serverConnectedSucc(ctx, objects);
        return this;
    }

    public IConnectionHandlerContext fireServerConnectedFail(final Object... objects) {
        final AbstractConnHandlerContext ctx = findContextOutbound();
        final IConnectionHandler handler = ctx.handler();
        handler.serverConnectedFail(ctx, objects);
        return this;
    }

    public IConnectionHandlerContext fireOutboundExceptionCaught(Throwable cause, Object object) throws Exception {
        final AbstractConnHandlerContext ctx = findContextOutbound();
        final IConnectionHandler handler = ctx.handler();
        handler.outboundExceptionCaught(ctx, cause);
        return this;
    }

    public IConnectionHandlerContext fireClientConnectTimeout(Object objects) {
        final AbstractConnHandlerContext ctx = findContextOutbound();
        final IConnectionHandler handler = ctx.handler();
        handler.clientConnectTimeout(ctx, null);
        return this;
    }

    public IConnectionHandlerContext fireReadRaw(Object object) {
        final AbstractConnHandlerContext ctx = findContextOutbound();
        final IConnectionHandler handler = ctx.handler();
        handler.readRaw(ctx, (ByteBuf) object);
        return this;
    }


    public Future<InetSocketAddress> fireRemoteInetSocketAddress(HttpRequest initialHttpRequest) {
        final AbstractConnHandlerContext ctx = findContextInbound();
        final IConnectionHandler handler = ctx.handler();
        return handler.remoteInetSocketAddress(ctx, initialHttpRequest);
    }
//    void doProcessCurrentStep(ContextStep currentStep) {
//        currentStep.execute().addListener(
//                new GenericFutureListener<Future<?>>() {
//                    public void operationComplete(
//                            io.netty.util.concurrent.Future<?> future)
//                            throws Exception {
//                        synchronized (connectLock) {
//                            if (future.isSuccess()) {
//
//                            } else {
////                                LOG.debug("ConnectionFlowStep failed",
////                                        future.cause());
////                                fail(future.cause());
//                            }
//                        }
//                    };
//                });
//    }

//    public static abstract class ConnHandlerCallback {
//        IProxyConnection connection;
//        ConnectionState state;
//
//        ConnHandlerCallback(AbstractConnHandlerContext ctx, ConnectionState state) {
//            super();
//            this.connection = connection;
//            this.state = state;
//        }
//
//        abstract Future execute();
//
//        abstract void onExecuteEnd();
//    }
}
