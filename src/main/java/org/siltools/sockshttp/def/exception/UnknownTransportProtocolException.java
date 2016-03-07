package org.siltools.sockshttp.def.exception;

import org.siltools.sockshttp.def.TransportProtocol;

public class UnknownTransportProtocolException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnknownTransportProtocolException(TransportProtocol transportProtocol) {
        super(String.format("Unknown TransportProtocol: %1$s", transportProtocol));
    }
}
