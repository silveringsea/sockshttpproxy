package org.siltools.sockshttp.def;

/**
 */
public class ServerParams {
    /**
     * The default number of threads to accept incoming requests from clients. (Requests are serviced by worker threads,
     * not acceptor threads.)
     */
    public static final int DEFAULT_INBOUND_ACCEPTOR_THREADS = 2;

    /**
     * The default number of threads to service incoming requests from clients.
     */
    public static final int DEFAULT_INBOUND_WORKER_THREADS = 8;

    /**
     * The default number of threads to service outgoing requests to servers.
     */
    public static final int DEFAULT_OUTBOUND_WORKER_THREADS = 8;

    public static final int HTTP_DECODER_MAX_INITIAL_LINE_LENGTH = 8192;

    public static final int HTTP_DECODER_MAX_HEADER_SIZE = 8192 * 2;

    public static final int HTTP_DECODER_MAX_CHUNK_SIZE = 8192 * 2;

    public static final boolean HTTP_DECODER_VALIDATE_HEADER = true;

}
