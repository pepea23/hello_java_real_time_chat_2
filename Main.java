package com.company;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Main {

    private static Map<String, SocketChannel> clientMap = new HashMap();

    private static int CLIENT_ID = 0;

    public static void main(String[] args) throws Exception {
        ServerSocketChannel serverChannel;

        serverChannel = ServerSocketChannel.open();

        serverChannel.configureBlocking(false);
        ServerSocket serverSocket = serverChannel.socket();
        serverSocket.bind(new InetSocketAddress(8000));

        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            try {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> itera = selectedKeys.iterator();
                while (itera.hasNext()) {
                    SelectionKey key = itera.next();
                    final SocketChannel client;
                    try {
                        if (key.isAcceptable()) {
                            ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                            client = channel.accept();
                            // Set non-blocking mode
                            client.configureBlocking(false);

                            // Register concerned events
                            client.register(selector, SelectionKey.OP_READ);
                            String clientId = "Client [" + CLIENT_ID++ + "]";
                            clientMap.put(clientId, client);
                        } else if (key.isReadable()) {
                            client = (SocketChannel) key.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            int count = client.read(buffer);
                            if (count > 0) {
                                buffer.flip();
                                Charset charset = Charset.forName("utf-8");
                                String message = String.copyValueOf(charset.decode(buffer).array());
                                System.out.println(client.getLocalAddress() + ":" + message);

                                String senderKey = null;
                                for (Map.Entry<String, SocketChannel> entry : clientMap.entrySet()) {
                                    if (client == entry.getValue()) {
                                        senderKey = entry.getKey();
                                        break;
                                    }
                                }

                                for (Map.Entry<String, SocketChannel> entry : clientMap.entrySet()) {
                                    SocketChannel socket = entry.getValue();
                                    ByteBuffer writeBuffer = ByteBuffer.allocate(1024);

                                    writeBuffer.put((senderKey + ":" + message).getBytes());
                                    writeBuffer.flip();

                                    socket.write(writeBuffer);
                                }
                            }
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                selectedKeys.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}