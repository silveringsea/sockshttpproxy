package org.siltools.sockshttp.proxy.connection;//package org.siltools.sockshttp.proxy.connection;
//
//import io.netty.util.concurrent.Future;
//import org.siltools.sockshttp.def.ConnectionState;
//import org.siltools.sockshttp.proxy.ProxyConnection;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * Created by Administrator on 1/12/2016.
// */
//public abstract class ConnectionFlowStep {
//    private Logger LOG = LoggerFactory.getLogger(ConnectionFlowStep.class);
//    private final ProxyConnection connection;
//    private final ConnectionState state;
//    public ConnectionFlowStep(ProxyConnection connection,
//                              ConnectionState state) {
//        super();
//        this.connection = connection;
//        this.state = state;
//    }
//
//    ProxyConnection getConnection() {
//        return connection;
//    }
//
//    ConnectionState getState() {
//        return state;
//    }
//
//    /**
//     * Indicates whether or not to suppress the initial request. Defaults to
//     * false, can be overridden.
//     *
//     * @return
//     */
//    boolean shouldSuppressInitialRequest() {
//        return false;
//    }
//
//    /**
//     * <p>
//     * Indicates whether or not this step should be executed on the channel's
//     * event loop. Defaults to false, can be overridden.
//     * </p>
//     *
//     * <p>
//     * If this step modifies the pipeline, for example by adding/removing
//     * handlers, it's best to make it execute on the event loop.
//     * </p>
//     *
//     *
//     * @return
//     */
//    boolean shouldExecuteOnEventLoop() {
//        return false;
//    }
//
//    /**
//     * Implement this method to actually do the work involved in this step of
//     * the flow.
//     *
//     * @return
//     */
//    protected abstract Future execute();
//
//    /**
//     * When the flow determines that this step was successful, it calls into
//     * this method. The default implementation simply continues with the flow.
//     * Other implementations may choose to not continue and instead wait for a
//     * message or something like that.
//     *
//     * @param flow
//     */
//    void onSuccess(ConnectionFlow flow) {
//        flow.advance();
//    }
//
//    /**
//     * <p>
//     * Any messages that are read from the underlying connection while we're at
//     * this step of the connection flow are passed to this method.
//     * </p>
//     *
//     * <p>
//     * The default implementation ignores the message and logs this, since we
//     * weren't really expecting a message here.
//     * </p>
//     *
//     * <p>
//     * Some {@link ConnectionFlowStep}s do need to read the messages, so they
//     * override this method as appropriate.
//     * </p>
//     *
//     * @param flow
//     *            our {@link ConnectionFlow}
//     * @param msg
//     *            the message read from the underlying connection
//     */
//    void read(ConnectionFlow flow, Object msg) {
//        LOG.debug("Received message while in the middle of connecting: {}", msg);
//    }
//
//    @Override
//    public String toString() {
//        return state.toString();
//    }
//}
