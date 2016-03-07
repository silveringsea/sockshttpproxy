package org.siltools.sockshttp.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.StringUtil;
import org.siltools.sockshttp.SocksHttpProxyServer;
import org.siltools.sockshttp.def.ConnectionState;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class DefaultConnectionPipeline implements IConnectionPipeline {

    IProxyConnection clientConnection;
    IProxyConnection proxyConnection;

    final AbstractConnHandlerContext head;
    final AbstractConnHandlerContext tail;
    public static final String HEAD_CONTEXT = "HEAD_CONTEXT";
    public static final String TAIL_CONTEXT = "TAIL_CONTEXT";

    SocksHttpProxyServer socksHttpProxyServer;
    AtomicInteger namePosfix = new AtomicInteger(0);
    ConnectionState currentState;
    Map<String, AbstractConnHandlerContext> name2ctx = new ConcurrentHashMap<String, AbstractConnHandlerContext>();
//    Stack<IProxyConnection> stackConnection = new Stack<IProxyConnection>();

    public DefaultConnectionPipeline(SocksHttpProxyServer socksHttpProxyServer) {
        head = new EmptyConnectionContext(this, HEAD_CONTEXT);
        tail = new EmptyConnectionContext(this, TAIL_CONTEXT);
        this.socksHttpProxyServer = socksHttpProxyServer;
    }

    public SocksHttpProxyServer proxyServer() {
        return socksHttpProxyServer;
    }

    private String filterName(String name, IConnectionHandler handler) {
        if (name == null) {
            return StringUtil.simpleClassName(handler.getClass()) + "#" + namePosfix.incrementAndGet();
        }

        if (!name2ctx.containsKey(name)) {
            return name;
        }

        throw new IllegalArgumentException("Duplicate handler name: " + name);
    }

    protected AbstractConnHandlerContext getContext(String name) {
        synchronized (this) {
            return name2ctx.get(name);
        }
    }

    protected AbstractConnHandlerContext getContextOrDie(IConnectionHandler handler) {
        Objects.requireNonNull(handler, "handlers must not null");
        synchronized (this) {
            AbstractConnHandlerContext ctx = head.next;
            for (;;) {

                if (ctx == null) {
                    return null;
                }

                if (ctx.handler() == handler) {
                    return ctx;
                }

                ctx = ctx.next;
            }
        }
    }

    protected AbstractConnHandlerContext getContextOrDie(String name) {
        synchronized (this) {
            AbstractConnHandlerContext ctx = name2ctx.get(name);
            if (ctx == null) {
                throw new NoSuchElementException(name);
            }

            return ctx;
        }
    }

    public IConnectionPipeline addFirst(IConnectionHandler... handlers) {
        if (handlers == null) {
            throw new NullPointerException("handlers");
        }
        if (handlers.length == 0 || handlers[0] == null) {
            return this;
        }

        for (int i = handlers.length -1; i >= 0; i--) {
            if (handlers[i] == null) {
                throw new NullPointerException("handlers");
            }
            addFirst(handlers[i].getName(), handlers[i]);
        }
        return this;
    }

    public IConnectionPipeline addLast(IConnectionHandler... handlers) {
        if (handlers == null) {
            throw new NullPointerException("handlers");
        }
        if (handlers.length == 0 || handlers[0] == null) {
            return this;
        }

        for (int i = 0; i < handlers.length; i++) {
            if (handlers[i] == null) {
                throw new NullPointerException("handlers");
            }
            addLast(handlers[i].getName(), handlers[i]);
        }
        return this;
    }

    public IConnectionPipeline addFirst(String name, IConnectionHandler connectionHandler) {
        synchronized (this) {
            name = filterName(name, connectionHandler);
            AbstractConnHandlerContext newCtx = new DefaultConnHandlerContext(this, name, connectionHandler);
            addFirst0(name, newCtx);
        }
        return this;
    }

    public IConnectionPipeline addLast(String name, IConnectionHandler connectionHandler) {
        synchronized (this) {
            name = filterName(name, connectionHandler);
            AbstractConnHandlerContext newCtx = new DefaultConnHandlerContext(this, name, connectionHandler);
            addLast0(name, newCtx);
        }
        return this;
    }

    public IConnectionPipeline addBefore(String baseName, String name, IConnectionHandler connectionHandler) {
        synchronized (this) {
            AbstractConnHandlerContext ctx = getContextOrDie(baseName);
            name = filterName(name, connectionHandler);
            AbstractConnHandlerContext newCtx = new DefaultConnHandlerContext(this, name, connectionHandler);
            addBefore0(name, ctx, newCtx);
        }
        return this;
    }

    public IConnectionPipeline addAfter(String baseName, String name, IConnectionHandler connectionHandler) {
        synchronized (this) {
            AbstractConnHandlerContext ctx = getContextOrDie(baseName);
            name = filterName(name, connectionHandler);
            AbstractConnHandlerContext newCtx = new DefaultConnHandlerContext(this, name, connectionHandler);
            addAfter0(name, ctx, newCtx);
        }
        return this;
    }

    public IConnectionPipeline remove(IConnectionHandler handler) {
        remove(getContextOrDie(handler)).handler();
        return this;
    }

    public IConnectionHandler remove(String name) {
        return remove(getContextOrDie(name)).handler();
    }

    public IConnectionHandler replace(String oldName, String newName, IConnectionHandler newHandler) {
        return replace(getContextOrDie(oldName), newName, newHandler);
    }

    public IConnectionHandler first() {
        IConnectionHandlerContext first = firstContext();
        return first == null? null:first.handler();
    }

    public IConnectionHandlerContext firstContext() {
        IConnectionHandlerContext first = head.next;
        if (first == tail) {
            return null;
        }
        return first;
    }

    public IConnectionHandler last() {
        IConnectionHandlerContext last = lastContext();
        return last == null? null: last.handler();
    }

    public IConnectionHandlerContext lastContext() {
        IConnectionHandlerContext last = tail.prev;
        if (last == head) {
            return null;
        }
        return last;
    }

    public IConnectionHandler get(String name) {
        return null;
    }

    public IProxyConnection clientConnection() {
        return clientConnection;
    }

    public void clientConnection(IProxyConnection connection) {
        this.clientConnection = connection;
    }

    public IProxyConnection proxyConnection() {
        return this.proxyConnection;
    }

    public void proxyConnection(IProxyConnection connection) {
        this.proxyConnection = connection;
    }

    public Map<String, IConnectionHandler> toMap() {
        Map<String, IConnectionHandler> map = new LinkedHashMap<String, IConnectionHandler>();
        AbstractConnHandlerContext ctx = head.next;
        for (;;) {
            if (ctx.next == tail)
                return map;

            map.put(ctx.name, ctx.handler());
            ctx = ctx.next;
        }
    }

    public IConnectionPipeline fireInitChannelPipeline(ChannelPipeline cp, int connectionType) {
        head.fireInitChannelPipeline(cp, connectionType);
        return this;
    }

    public IConnectionPipeline fireMessageReceive(Object... objects) {
        head.fireMessageReceive(objects);
        return this;
    }

    public IConnectionPipeline fireProxyStateChange(Object... objects) {
        tail.fireProxyStateChange(objects);
        return this;
    }

    public IConnectionPipeline fireReadHTTPChunk(Object... objects) {
        tail.fireReadHTTPChunk(objects);
        return this;
    }

    public IConnectionPipeline fireReadRaw(Object... objects) {
        tail.fireReadRaw(objects);
        return this;
    }


    public IConnectionPipeline fireServerConnectedSucc(Object... objects) {
        tail.fireServerConnectedSucc(objects);
        return null;
    }

    public IConnectionPipeline fireClientConnectTimeout(Object... objects) {
        tail.fireClientConnectTimeout(objects);
        return this;
    }

    public IConnectionPipeline fireServerConnectedFail(Object... objects) {
        tail.fireServerConnectedFail(objects);
        return this;
    }

    public List<String> names() {
        return Arrays.asList(toMap().keySet().toArray(new String[0]));
    }

    public Iterator<Map.Entry<String, IConnectionHandler>> iterator() {
        return toMap().entrySet().iterator();
    }

    private void addFirst0(final String name, AbstractConnHandlerContext newCtx) {
        AbstractConnHandlerContext nextCtx = head.next;
        newCtx.prev = head;
        newCtx.next = nextCtx;
        head.next = newCtx;
        nextCtx.prev = newCtx;

        name2ctx.put(name, newCtx);
    }

    private void addLast0(final String name, AbstractConnHandlerContext newCtx) {
        AbstractConnHandlerContext prevCtx = tail.prev;
        newCtx.next = tail;
        newCtx.prev = prevCtx;
        tail.prev = newCtx;
        prevCtx.next = newCtx;

        name2ctx.put(name, newCtx);
    }

    private void addBefore0(final String name, AbstractConnHandlerContext ctx, AbstractConnHandlerContext newCtx) {
        newCtx.prev = ctx.prev;
        newCtx.next = ctx;
        ctx.prev.next = newCtx;
        ctx.prev = newCtx;

        name2ctx.put(name, newCtx);
    }
    
    private void addAfter0(String name, AbstractConnHandlerContext ctx, AbstractConnHandlerContext newCtx) {
        newCtx.prev = ctx;
        newCtx.next = ctx.next;
        ctx.next.prev = newCtx;
        ctx.next = newCtx;

        name2ctx.put(name, newCtx);
    }

    private AbstractConnHandlerContext remove(final AbstractConnHandlerContext ctx) {
        assert ctx != head && ctx != tail;
        remove0(ctx);
        AbstractConnHandlerContext context = ctx;
        return context;
    }

    void remove0(AbstractConnHandlerContext ctx) {
        AbstractConnHandlerContext prev = ctx.prev;
        AbstractConnHandlerContext next = ctx.next;
        prev.next = next;
        next.prev = prev;
        name2ctx.remove(ctx.name());
        callConnContextRemoved(ctx);
    }

    private IConnectionHandler replace(final AbstractConnHandlerContext ctx, String newName,
                                       IConnectionHandler newHandler) {
        assert ctx != head && ctx != tail;

        synchronized (this) {
            if (newName == null) {
                newName = ctx.name();
            } else if (!ctx.name().equals(newName)) {
                newName = filterName(newName, newHandler);
            }

            final AbstractConnHandlerContext newCtx =
                    new DefaultConnHandlerContext(this, newName, newHandler);
                replace0(ctx, newName, newCtx);

        }

        return ctx.handler();
    }

    private void replace0(AbstractConnHandlerContext oldCtx, String newName,
                          AbstractConnHandlerContext newCtx) {
        AbstractConnHandlerContext prev = oldCtx.prev;
        AbstractConnHandlerContext next = oldCtx.next;
        newCtx.prev = prev;
        newCtx.next = next;

        prev.next = newCtx;
        next.prev = newCtx;

        if (!oldCtx.name().equals(newName)) {
            name2ctx.remove(oldCtx.name());
        }
        name2ctx.put(newName, newCtx);

        // update the reference to the replacement so forward of buffered content will work correctly
        oldCtx.prev = newCtx;
        oldCtx.next = newCtx;
        callConnContextRemoved(oldCtx);
    }

    private void callConnContextRemoved(AbstractConnHandlerContext oldCtx) {
        oldCtx.setRemoved();
    }

    static class EmptyConnectionContext extends AbstractConnHandlerContext implements IConnectionHandler {
        private static final int SKIP_FLAGS = skipFlags0(EmptyConnectionContext.class);

        protected EmptyConnectionContext(DefaultConnectionPipeline pipeline, String name) {
            super(pipeline, name, SKIP_FLAGS);
        }

        @Skip
        public void initChannelPipeline(IConnectionHandlerContext ctx, ChannelPipeline channelPipeline, int connectionType) {

        }

        @Skip
        public void messageReceive(IConnectionHandlerContext ctx, Object... objects) {

        }

        @Skip
        public void proxyStateChange(IConnectionHandlerContext ctx, Object... objects) {

        }

        @Skip
        public void readHTTPChunk(IConnectionHandlerContext ctx, Object... chunk) {

        }

        @Skip
        public void readRaw(IConnectionHandlerContext ctx, ByteBuf buf) {

        }

        @Skip
        public void serverConnectedSucc(IConnectionHandlerContext ctx, Object... object) {

        }

        @Skip
        public void serverConnectedFail(IConnectionHandlerContext ctx, Object... objects) {

        }

        @Skip
        public void inboundExceptionCaught(IConnectionHandlerContext ctx, Throwable cause) throws Exception {

        }

        @Skip
        public void outboundExceptionCaught(IConnectionHandlerContext ctx, Throwable cause) throws Exception {

        }

        @Skip
        public void clientConnectTimeout(IConnectionHandlerContext ctx, Throwable cause) {

        }

        @Skip
        public Future<InetSocketAddress> remoteInetSocketAddress(IConnectionHandlerContext ctx, HttpRequest httpRequest) {
            return null;
        }

        @Skip
        public boolean shouldExecuteOnEventLoop(int stepFlag) {
            return false;
        }

        @Skip
        public int connectionType() {
            return 0;
        }

        @Skip
        public String getName() {
            return null;
        }

        @Skip
        public IProxyConnection connection() {
            return null;
        }

        @Skip
        public IProxyConnection connectionSibling() {
            return null;
        }

        @Skip
        public void connection(IProxyConnection connection) {

        }

        @Skip
        public IConnectionHandler handler() {
            return null;
        }
        @Skip
        public Future<InetSocketAddress> fireRemoteInetSocketAddress(HttpRequest initialHttpRequest) {
            return null;
        }
    }
}
