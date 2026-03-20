package cn.edu.ncu.medical.netty;

import cn.edu.ncu.medical.service.ChatMessageService;
import cn.edu.ncu.medical.service.RoomService;
import cn.edu.ncu.medical.utils.RedisCache;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 独立的 Netty WebSocket 服务。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NettyWebSocketServer implements InitializingBean, DisposableBean {

    private final NettyServerProperties properties;
    private final RedisCache redisCache;
    private final NettySessionRegistry sessionRegistry;
    private final ChatMessageService chatMessageService;
    private final RoomService roomService;
    @Value("${app.auth.enabled:true}")
    private boolean authEnabled;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @Override
    public void afterPropertiesSet() {
        if (!properties.isEnabled()) {
            log.info("Netty WebSocket server is disabled by configuration.");
            return;
        }
        start();
    }

    private void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new HttpServerCodec())
                                    .addLast(new HttpObjectAggregator(65536))
                                    .addLast(new IdleStateHandler(properties.getIdleSeconds(), properties.getIdleSeconds(), properties.getIdleSeconds(), TimeUnit.SECONDS))
                                    .addLast(new WebSocketServerCompressionHandler())
                                    .addLast(new NettyHandshakeHandler(properties, redisCache, sessionRegistry, authEnabled))
                                    .addLast(new NettyTextFrameHandler(sessionRegistry, chatMessageService, roomService));
                        }
                    });

            ChannelFuture future = bootstrap.bind(properties.getPort()).sync();
            serverChannel = future.channel();
            log.info("Netty WebSocket server started at ws://localhost:{}{}", properties.getPort(), properties.getPath() + "/{roomId}");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Netty WebSocket server start interrupted", e);
        } catch (Exception e) {
            log.error("Failed to start Netty WebSocket server", e);
        }
    }

    @Override
    public void destroy() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
