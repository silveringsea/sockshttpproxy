package org.siltools.sockshttp.def.exception;

/**
 * Created by Administrator on 1/13/2016.
 */
public class ProxyException extends Exception {

    public enum ProxyExceptionType {
        FAIL_INIT_SERVER_CONNECTION
    }

    public ProxyException(String message, ProxyExceptionType proxyExceptionType) {

    }
}
