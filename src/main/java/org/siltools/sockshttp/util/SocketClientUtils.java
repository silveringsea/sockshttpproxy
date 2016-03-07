package org.siltools.sockshttp.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 * Created by Administrator on 2015/12/30.
 */
public class SocketClientUtils {

    public final static int BUFFER_SIZE = 10000;
    public static void writeStringToSocket(String string, Socket socket) throws IOException {
        OutputStream out = socket.getOutputStream();
        out.write(string.getBytes(Charset.defaultCharset()));
        out.flush();
    }

    public static String readStringFromSocket(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        byte[] bytes = new byte[BUFFER_SIZE];
        int bytesRead = in.read(bytes);
        if (bytesRead == -1) {
            throw new EOFException("the socket is closed");
        }

        String read = new String(bytes, 0, bytesRead, Charset.defaultCharset());
        return read;
    }

    public static Socket getSocketToProxyServer(String host, int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 1000);
        socket.setSoTimeout(300000);
        return socket;
    }
}
