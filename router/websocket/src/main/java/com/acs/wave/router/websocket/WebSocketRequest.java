package com.acs.wave.router.websocket;

import java.net.SocketAddress;

public class WebSocketRequest {

    public final String id;
    public final String uri;
    public final SocketAddress localAddress;
    public final SocketAddress remoteAddress;

    public WebSocketRequest(String id, String uri, SocketAddress localAddress, SocketAddress remoteAddress) {
        this.id = id;
        this.uri = uri;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }
}
