package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    public static void main(String[] args) {
        SocketChannel channel;
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);

            Selector selector = Selector.open();
            channel.register(selector, SelectionKey.OP_CONNECT);
            channel.connect(new InetSocketAddress("localhost", 8000));

            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();

                for (SelectionKey key : selectedKeys) {
                    if (key.isConnectable()) {
                        SocketChannel client = (SocketChannel) key.channel();

                        // Connection event in progress
                        if (client.isConnectionPending()) {
                            client.finishConnect();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            buffer.put(("Connected from client").getBytes());
                            buffer.flip();
                            channel.write(buffer);

                            // Create a new thread to monitor input
                            ExecutorService executorService = Executors
                                    .newSingleThreadExecutor(Executors.defaultThreadFactory());

                            executorService.submit(() -> {
                                while (true) {
                                    try {
                                        buffer.clear();
                                        InputStreamReader input = new InputStreamReader(System.in);
                                        BufferedReader br = new BufferedReader(input);

                                        String sendMessage = br.readLine();
                                        buffer.put(sendMessage.getBytes());
                                        buffer.flip();
                                        client.write(buffer);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                        client.register(selector, SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer readBuffer = ByteBuffer.allocate(1024);

                        int read = client.read(readBuffer);
                        if (read > 0) {
                            String message = new String(readBuffer.array());
                            System.out.println(message);
                        }

                    }
                }
                selectedKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
