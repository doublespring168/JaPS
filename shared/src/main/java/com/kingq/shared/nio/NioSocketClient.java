package com.kingq.shared.nio;

import com.kingq.shared.config.ClusterServer;
import com.kingq.shared.net.ConnectException;
import com.kingq.shared.pipeline.ChannelUtil;
import com.kingq.shared.pipeline.PipelineUtils;
import com.kingq.shared.pipeline.initialize.ClientChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by spring on 16.06.2017.
 */
@ChannelHandler.Sharable
public abstract class NioSocketClient extends SimpleChannelInboundHandler<JSONObject> {

    private static final int CONNECT_TIMEOUT = 2000;
    protected String name;
    private List<ClusterServer> clusterServers = new ArrayList<>();
    private Channel channel;
    private boolean connected;
    private AtomicBoolean reconnecting = new AtomicBoolean(false);
    private Queue<JSONObject> sendQueue = new ConcurrentLinkedQueue<>();
    private String host;

    private int port;

    public NioSocketClient(List<ClusterServer> clusterServers) {

        this(clusterServers, "server");
    }

    public NioSocketClient(List<ClusterServer> clusterServers, String name) {

        // Randomize the list to give a chance for a better use of the cluster
        Collections.shuffle(clusterServers);

        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }

        this.clusterServers = clusterServers;
        this.name = name;

        ClusterServer first = clusterServers.get(0);

        this.host = first.host();
        this.port = first.port();

        if (!connect(host, port)) {
            throw new ConnectException("cannot initially connect to " + first.host() + ":" + first.port());
        }
    }

    public boolean connect(String host, int port) {

        ChannelFuture channelFuture = new Bootstrap()
                .group(PipelineUtils.newEventLoopGroup(1))
                .channel(PipelineUtils.getChannel())
                .handler(new ClientChannelInitializer(this))
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
                .connect(host, port);

        channelFuture.awaitUninterruptibly();

        channel = channelFuture.channel();

        CountDownLatch countDownLatch = new CountDownLatch(1);

        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {

                connected = channelFuture.isSuccess();

                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return connected;
    }

    public abstract void clientReconnected();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        clientConnected();
    }

    public abstract void clientConnected();

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        reconnect();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        ChannelUtil.closeOnFlush(channel);

        if (!(cause instanceof IOException)) {
            cause.printStackTrace();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JSONObject jsonObject) throws Exception {

        received(jsonObject);
    }

    public abstract void received(JSONObject jsonObject);

    private void addToQueue(JSONObject jsonObject) {

        // Only queue up to 100 messages
        if (sendQueue.size() < 100) {
            sendQueue.offer(jsonObject);
        }
    }

    private void reconnect() {

        if (!reconnecting.get()) {
            reconnecting.set(true);

            connected = false;

            channel.eventLoop().schedule(() -> {

                if (!connect(host, port)) {
                    reconnecting.set(false);

                    reconnect();
                } else {
                    reconnecting.set(false);

                    clientReconnected();

                    try {
                        // Give the subscriber a chance to connect first
                        Thread.sleep(1200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //Resend the queued messages if available
                    while (sendQueue.size() > 0) {
                        JSONObject jsonObject = sendQueue.poll();
                        if (jsonObject != null) {
                            write(jsonObject);
                        }
                    }
                }
            }, 1, TimeUnit.SECONDS);
        }
    }

    public void close(boolean force) {

        if (connected) {
            connected = false;

            channel.close();

            if (!force) {
                reconnect();
            }
        }
    }

    public void write(JSONObject jsonObject, boolean queueEnabled) {

        if (!channel.isActive() && queueEnabled) {
            addToQueue(jsonObject);
            return;
        }

        channel.writeAndFlush(jsonObject);
    }

    public void write(JSONObject jsonObject) {

        write(jsonObject, true);
    }

    public List<ClusterServer> clusterServers() {

        return clusterServers;
    }

    public boolean isConnected() {

        return connected;
    }
}
