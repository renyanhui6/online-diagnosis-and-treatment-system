package cn.edu.ncu.medical.netty;

import cn.edu.ncu.medical.constant.RedisConstant;
import cn.edu.ncu.medical.utils.JwtUtil;
import cn.edu.ncu.medical.utils.RedisCache;
import io.jsonwebtoken.Claims;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 握手与认证处理：校验路径、token 与 redis 中的登录态，然后完成 WebSocket 握手。
 */
public class NettyHandshakeHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final NettyServerProperties properties;
    private final RedisCache redisCache;
    private final NettySessionRegistry sessionRegistry;

    public NettyHandshakeHandler(NettyServerProperties properties, RedisCache redisCache, NettySessionRegistry sessionRegistry) {
        this.properties = properties;
        this.redisCache = redisCache;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, request, HttpResponseStatus.BAD_REQUEST, "Bad Request");
            return;
        }

        URI uri = new URI(request.uri());
        String path = uri.getPath();
        if (!path.startsWith(properties.getPath())) {
            sendHttpResponse(ctx, request, HttpResponseStatus.NOT_FOUND, "Not Found");
            return;
        }

        String roomId = path.substring(properties.getPath().length());
        if (roomId.startsWith("/")) {
            roomId = roomId.substring(1);
        }
        if (roomId.isEmpty()) {
            sendHttpResponse(ctx, request, HttpResponseStatus.BAD_REQUEST, "roomId missing");
            return;
        }

        Map<String, String> params = new QueryStringDecoder(request.uri()).parameters().entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
        String token = params.getOrDefault("token", "");
        if (token.isEmpty()) {
            sendHttpResponse(ctx, request, HttpResponseStatus.UNAUTHORIZED, "token missing");
            return;
        }

        Claims claims;
        try {
            claims = JwtUtil.parseToken(token);
        } catch (Exception e) {
            sendHttpResponse(ctx, request, HttpResponseStatus.UNAUTHORIZED, "token invalid");
            return;
        }
        Long userId = claims.get("userId", Long.class);
        String username = claims.get("username", String.class);
        if (userId == null || username == null) {
            sendHttpResponse(ctx, request, HttpResponseStatus.UNAUTHORIZED, "token invalid");
            return;
        }

        String redisToken = redisCache.getString(RedisConstant.LOGIN_TOKEN_PREFIX + userId);
        if (redisToken == null || !redisToken.equals(token)) {
            sendHttpResponse(ctx, request, HttpResponseStatus.UNAUTHORIZED, "token expired");
            return;
        }

        Channel channel = ctx.channel();
        channel.attr(NettySessionRegistry.ATTR_USER_ID).set(userId);
        channel.attr(NettySessionRegistry.ATTR_USERNAME).set(username);
        channel.attr(NettySessionRegistry.ATTR_ROOM).set(roomId);
        boolean isLongConnection = roomId.startsWith("patient_") || roomId.startsWith("doctor_");
        channel.attr(NettySessionRegistry.ATTR_LONG_CONN).set(isLongConnection);

        sessionRegistry.addRoomChannel(roomId, channel);
        if (isLongConnection) {
            sessionRegistry.bindLongConnection(roomId, userId, channel);
        }

        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(request), null, true);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
        } else {
            handshaker.handshake(channel, request);
        }

        ctx.pipeline().remove(this);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sessionRegistry.remove(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status, String msg) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private String getWebSocketLocation(FullHttpRequest req) {
        String protocol = "ws";
        String host = req.headers().get(HttpHeaderNames.HOST);
        return protocol + "://" + host + properties.getPath();
    }
}
