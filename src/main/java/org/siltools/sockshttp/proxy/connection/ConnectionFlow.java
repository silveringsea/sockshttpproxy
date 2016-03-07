package org.siltools.sockshttp.proxy.connection;//package org.siltools.sockshttp.proxy.connection;
//
//
//import io.netty.util.concurrent.Future;
//import io.netty.util.concurrent.GenericFutureListener;
//import org.siltools.sockshttp.def.ConnectionState;
//import org.siltools.sockshttp.proxy.IProxyToServerConnection;
//import org.siltools.sockshttp.proxy.ProxyConnection;
//
//import java.util.Queue;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.locks.Condition;
//import java.util.concurrent.locks.Lock;
//
///**
// */
//public class ConnectionFlow {
//    private Queue<ConnectionFlowStep> flowSteps = new ConcurrentLinkedQueue<ConnectionFlowStep>();
//    private final IProxyToServerConnection proxyToServerConnection;
//    private final Lock connectionLock;
//    private final Condition connectionLockCond;
//
//    private volatile ConnectionFlowStep curFlowStep;
//    private volatile boolean suspendInitialRequest = false;
//
//    public ConnectionFlow(/*IClientToProxyConnection clientToProxyConnection, */
//                          IProxyToServerConnection proxyToServerConnection, Lock connectionLock, Condition connectionLockCond) {
//        this.proxyToServerConnection = proxyToServerConnection;
//        this.connectionLock = connectionLock;
//        this.connectionLockCond = connectionLockCond;
//    }
//
//    public ConnectionFlow then(ConnectionFlowStep step) {
//        flowSteps.add(step);
//        return this;
//    }
//
//    public void start() {
//        proxyToServerConnection.serverConnectionFlowStarted();
//        advance();
//    }
//
//    void advance() {
//        curFlowStep = flowSteps.poll();
//        if (curFlowStep == null) {
//            succeed();
//        } else {
//            processCurrentStep();
//        }
//    }
//
//    void succeed() {
//        connectionLock.lock();
//        proxyToServerConnection.connectionSucceeded(true);
//        notifyThreadAwaitConnection();
//        connectionLock.unlock();
//    }
//
//    void fail(final Throwable cause) {
//        final ConnectionState lastStateBeforeFailure = proxyToServerConnection
//                .getCurrentState();
//        proxyToServerConnection.disconnect().addListener(new GenericFutureListener<Future>() {
//            public void operationComplete(Future future) throws Exception {
//                //future.isSuccess()
//                //synchronized (connectionLock) {
//                connectionLock.lock();
//                if (proxyToServerConnection.serverConnectionFailed(
//                        lastStateBeforeFailure,
//                        cause))
//                    return;
//                // the connection to the server failed and we are not retrying, so transition to the
//                // DISCONNECTED state
//                proxyToServerConnection.becomeState(ConnectionState.DISCONNECTED);
//
//                // We are not retrying our connection, let anyone waiting for a connection know that we're done
//                notifyThreadAwaitConnection();
//                connectionLock.unlock();
//            //    }
//            }
//        });
//    }
//
//    void processCurrentStep() {
//        final ProxyConnection proxyConnection = curFlowStep.getConnection();
//        proxyConnection.becomeState(curFlowStep.getState());
//        if (curFlowStep.shouldExecuteOnEventLoop()) {
//            proxyConnection.submitChannelTask(new Runnable() {
//                public void run() {
//                    doProcessCurrentStep();
//                }
//            });
//        } else {
//            doProcessCurrentStep();
//        }
//    }
//
//    private void doProcessCurrentStep() {
//        curFlowStep.execute().addListener(new GenericFutureListener() {
//            public void operationComplete(Future future) throws Exception {
//                if (future.isSuccess()) {
//                    curFlowStep.onSuccess(ConnectionFlow.this);
//                } else {
//                    fail(future.cause());
//                }
//            }
//        });
//    }
//
//    void notifyThreadAwaitConnection() {
//        connectionLockCond.signalAll();
//    }
//}
