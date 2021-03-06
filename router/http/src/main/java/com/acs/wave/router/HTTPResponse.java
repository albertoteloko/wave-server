package com.acs.wave.router;

import com.acs.wave.router.constants.ProtocolVersion;
import com.acs.wave.router.constants.ResponseStatus;

public class HTTPResponse extends HTTPItem {
    public final ResponseStatus responseStatus;
    public final byte[] body;

    HTTPResponse(ProtocolVersion protocolVersion, ResponseStatus responseStatus, HTTPHeaders headers, byte[] body) {
        super(protocolVersion, headers);
        this.responseStatus = responseStatus;
        this.body = body;
    }

    @Override
    public String toString() {
        return "HTTPResponse{" +
                "protocolVersion=" + protocolVersion +
                ", responseStatus=" + responseStatus +
                ", headers=" + headers +
                ", body=" + body.getClass().getName() +
                '}';
    }
}

