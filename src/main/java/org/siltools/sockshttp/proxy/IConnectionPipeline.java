package org.siltools.sockshttp.proxy;

import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.Future;
import org.siltools.sockshttp.SocksHttpProxyServer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 */
public interface IConnectionPipeline extends Iterable<Map.Entry<String, IConnectionHandler>> {

    SocksHttpProxyServer proxyServer();
    /**
     * Inserts a {@link IConnectionHandler}s at the first position of this pipeline.
     *
     * @param handlers  the handlers to insert first
     *
     */
    IConnectionPipeline addFirst(IConnectionHandler... handlers);

    /**
     * Inserts a {@link IConnectionHandler}s at the last position of this pipeline.
     *
     * @param handlers  the handlers to insert last
     *
     */
    IConnectionPipeline addLast(IConnectionHandler... handlers);

    /**
     * Inserts a {@link IConnectionHandler} at the first position of this pipeline.
     * @param name the name of the handler to append. {@code null} to let the name auto-generated.
     * @param connectionHandler the handler to append
     */
    IConnectionPipeline addFirst(String name, IConnectionHandler connectionHandler);

    /**
     * Inserts a {@link IConnectionHandler} at the last position of this pipeline.
     * @param name  the name of the handler to append. {@code null} to let the name auto-generated.
     * @param connectionHandler the handler to append
     */
    IConnectionPipeline addLast(String name, IConnectionHandler connectionHandler);

    /**
     * Inserts a {@link IConnectionHandler} before an existing handler of this
     *
     * @param baseName  the name of the existing handler
     * @param name      the name of the handler to insert before. {@code null} to let the name auto-generated.
     * @param connectionHandler   the handler to insert before
     * @throws NoSuchElementException
     *         if there's no such entry with the specified {@code baseName}
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified baseName or handler is {@code null}
     */
    IConnectionPipeline addBefore(String baseName, String name, IConnectionHandler connectionHandler);

    /**
     * Inserts a {@link IConnectionHandler} after an existing handler of this
     *
     * @param baseName  the name of the existing handler
     * @param name      the name of the handler to insert before. {@code null} to let the name auto-generated.
     * @param connectionHandler   the handler to insert before
     * @throws NoSuchElementException
     *         if there's no such entry with the specified {@code baseName}
     * @throws IllegalArgumentException
     *         if there's an entry with the same name already in the pipeline
     * @throws NullPointerException
     *         if the specified baseName or handler is {@code null}
     */
    IConnectionPipeline addAfter(String baseName, String name, IConnectionHandler connectionHandler);

    /**
     * Removes the specified {@link IConnectionHandler} from this pipeline.
     *
     * @param  handler          the {@link IConnectionHandler} to remove
     *
     * @throws NoSuchElementException
     *         if there's no such handler in this pipeline
     * @throws NullPointerException
     *         if the specified handler is {@code null}
     */
    IConnectionPipeline remove(IConnectionHandler handler);

    /**
     * Removes the {@link IConnectionHandler} with the specified name from this pipeline.
     *
     * @param  name             the name under which the {@link IConnectionHandler} was stored.
     *
     * @return the removed handler
     *
     * @throws NoSuchElementException
     *         if there's no such handler with the specified name in this pipeline
     * @throws NullPointerException
     *         if the specified name is {@code null}
     */
    IConnectionHandler remove(String name);

    /**
     * Replaces the {@link IConnectionHandler} of the specified name with a new handler in this pipeline.
     *
     * @param  oldName       the name of the {@link IConnectionHandler} to be replaced
     * @param  newName       the name under which the replacement should be added.
     *                       {@code null} to use the same name with the handler being replaced.
     * @param  newHandler    the {@link IConnectionHandler} which is used as replacement
     *
     * @return the removed handler
     *
     * @throws NoSuchElementException
     *         if the handler with the specified old name does not exist in this pipeline
     * @throws IllegalArgumentException
     *         if a handler with the specified new name already exists in this
     *         pipeline, except for the handler to be replaced
     * @throws NullPointerException
     *         if the specified old handler or new handler is {@code null}
     */
    IConnectionHandler replace(String oldName, String newName, IConnectionHandler newHandler);

    /**
     * Returns the first {@link IConnectionHandler} in this pipeline.
     *
     * @return the first handler.  {@code null} if this pipeline is empty.
     */
    IConnectionHandler first();

    /**
     * Returns the context of the first {@link IConnectionHandler} in this pipeline.
     *
     * @return the context of the first handler.  {@code null} if this pipeline is empty.
     */
    IConnectionHandlerContext firstContext();

    /**
     * Returns the last {@link IConnectionHandler} in this pipeline.
     *
     * @return the last handler.  {@code null} if this pipeline is empty.
     */
    IConnectionHandler last();

    /**
     * Returns the context of the last {@link IConnectionHandler} in this pipeline.
     *
     * @return the context of the last handler.  {@code null} if this pipeline is empty.
     */
    IConnectionHandlerContext lastContext();

    /**
     * Returns the {@link IConnectionHandler} with the specified name in this
     * pipeline.
     *
     * @return the handler with the specified name.
     *         {@code null} if there's no such handler in this pipeline.
     */
    IConnectionHandler get(String name);


    /**
     * Returns the {@link IProxyConnection} that this pipeline is attached to.
     *
     * @return the connection. {@code null} if this pipeline is not attached yet.
     */
    IProxyConnection clientConnection();

    /**
     *
    */
    void clientConnection(IProxyConnection connection);

    /**
     * Returns the {@link IProxyConnection} that this pipeline is attached to.
     *
     * @return the connection. {@code null} if this pipeline is not attached yet.
     */
    IProxyConnection proxyConnection();

    /**
     *
     */
    void proxyConnection(IProxyConnection connection);

    /**
     * Returns the {@link List} of the handler names.
     */
    List<String> names();

    /**
     * Converts this pipeline into an ordered {@link Map} whose keys are
     * handler names and whose values are handlers.
     */
    Map<String, IConnectionHandler> toMap();

    IConnectionPipeline fireInitChannelPipeline(ChannelPipeline cp, int connectionType);

    IConnectionPipeline fireProxyStateChange(Object ...objects);

    IConnectionPipeline fireMessageReceive(Object ...objects);

//    IConnectionPipeline fireReadHTTPInitial(Object ...objects);

    IConnectionPipeline fireReadHTTPChunk(Object ...objects);

    IConnectionPipeline fireReadRaw(Object ...objects);

    IConnectionPipeline fireServerConnectedSucc(Object ...objects);

    IConnectionPipeline fireServerConnectedFail(Object ...objects);
//    IConnectionPipeline fireDisconnect(Object ...objects);

    IConnectionPipeline fireClientConnectTimeout(Object ...objects);

    Future<InetSocketAddress> fireRemoteInetSocketAddress(Object ...objects);
//    IConnectionPipeline fireConnect(Object ...objects);


}
