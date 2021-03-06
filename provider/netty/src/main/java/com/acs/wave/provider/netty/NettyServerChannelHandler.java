package com.acs.wave.provider.netty;

import com.acs.wave.router.*;
import com.acs.wave.router.constants.ProtocolVersion;
import com.acs.wave.router.constants.RequestMethod;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

class NettyServerChannelHandler extends ChannelInboundHandlerAdapter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final HTTPRouter httpRouter;

    NettyServerChannelHandler(HTTPRouter httpRouter) {
        this.httpRouter = httpRouter;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            if (HttpUtil.isKeepAlive(req)) {
                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
            }
            boolean keepAlive = HttpUtil.isKeepAlive(req);

            HTTPRequest waveRequest = getWaveRequest(req, ctx.channel().remoteAddress());
            HTTPResponse wareResponse = httpRouter.process(waveRequest);
            HttpResponse response = getNettyResponse(wareResponse);

            if (!keepAlive) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                ctx.write(response);
            }
        }
    }

    private HttpResponse getNettyResponse(HTTPResponse waveResponse) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                getNettyHttpVersion(waveResponse.protocolVersion),
                HttpResponseStatus.valueOf(waveResponse.responseStatus.code),
                Unpooled.wrappedBuffer(waveResponse.body)
        );

        waveResponse.headers.stream().forEach(header -> response.headers().set(header.key, header.value));

        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }


    private HTTPRequest getWaveRequest(HttpRequest request, SocketAddress socketAddress) {
        return new HTTPRequest(
                getWaveRequestMethod(request.method()),
                request.uri(),
                getWaveHTTPVersion(request.protocolVersion()),
                getHeaders(request),
                ((InetSocketAddress) socketAddress).getHostName(),
                getBody(request)
        );
    }

    private byte[] getBody(HttpRequest request) {
        byte[] body = new byte[0];

        if (request instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) request;
            ByteBuf content = httpContent.content();
            if (content.isReadable()) {
                body = ByteBufUtil.getBytes(content);
            }
        }

        return body;
    }

    private HTTPHeaders getHeaders(HttpRequest request) {
        HTTPHeaders result = new HTTPHeaders();
        request.headers().forEach(header -> result.add(header.getKey(), header.getValue()));
        return result;
    }

    private HttpVersion getNettyHttpVersion(ProtocolVersion protocolVersion) {
        return HttpVersion.valueOf(protocolVersion.toString());
    }

    private ProtocolVersion getWaveHTTPVersion(HttpVersion httpVersion) {
        return ProtocolVersion.fromString(httpVersion.toString().toUpperCase());
    }

    private RequestMethod getWaveRequestMethod(HttpMethod method) {
        return RequestMethod.fromString(method.name().toUpperCase());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Error during request", cause);
        ctx.close();
    }
}