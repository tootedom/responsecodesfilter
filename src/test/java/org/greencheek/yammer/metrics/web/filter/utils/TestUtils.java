package org.greencheek.yammer.metrics.web.filter.utils;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * User: dominictootell
 * Date: 28/11/2012
 * Time: 19:11
 */
public class TestUtils {

    public static int findFreePort()
            throws IOException {
        ServerSocket server;
        server = new ServerSocket(0);
        int port = server.getLocalPort();
        server.close();
        return port;
    }
}
