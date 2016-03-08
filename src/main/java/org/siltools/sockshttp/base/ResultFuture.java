package org.siltools.sockshttp.base;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ResultFuture<V> implements Future<V> {

    V result;
    boolean success;

    public ResultFuture(V v, boolean success) {
        this.result = v;
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isCancellable() {
        return false;
    }

    public Throwable cause() {
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static void notifyListener0(Future future, GenericFutureListener l) {
        try {
            l.operationComplete(future);
        } catch (Throwable t) {

        }
    }

    public Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener) {
        notifyListener0(this, listener);
        return this;
    }

    public Future<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        return null;
    }

    public Future<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener) {
        return null;
    }

    public Future<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        return null;
    }

    public Future<V> sync() throws InterruptedException {
        return null;
    }

    public Future<V> syncUninterruptibly() {
        return null;
    }

    public Future<V> await() throws InterruptedException {
        return null;
    }

    public Future<V> awaitUninterruptibly() {
        return null;
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    public boolean await(long timeoutMillis) throws InterruptedException {
        return false;
    }

    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return false;
    }

    public boolean awaitUninterruptibly(long timeoutMillis) {
        return false;
    }

    public V getNow() {
        return null;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return false;
    }

    public V get() throws InterruptedException, ExecutionException {
        return this.result;
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
