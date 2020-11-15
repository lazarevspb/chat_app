package chat.server.core;

import chat.common.Common;
import chat.network.ServerSocketThread;
import chat.network.ServerSocketThreadListener;
import chat.network.SocketThread;
import chat.network.SocketThreadListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.*;


/*
 2. На серверной стороне сетевого чата реализовать управление потоками через ExecutorService.
 * */

/*
 *
 * 1. Добавить на серверную сторону чата логирование, с выводом информации о действиях на сервере
 *  (запущен, произошла ошибка, клиент подключился, клиент прислал сообщение/команду).
 * */

/**
 * Класс рождает сокеты реализует SSTL, STL. Класс отвечает за события всей серверной части
 * приложения.
 */
public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {

    private static final Logger LOGGER = Logger.getLogger(ChatServer.class.getName());

    public Vector<SocketThread> listOfClients = new Vector<>();
    private ServerSocketThread server;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss ");
    private ChatServerListener listener;
    ExecutorService executorService = Executors.newCachedThreadPool();

    public ChatServer(ChatServerListener listener) {
        this.listener = listener;
        try {
            Handler h = new FileHandler("logServer.log", true);
            h.setLevel(Level.ALL);
            h.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(h);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start(int port) {
        if (server != null && server.isAlive()) {
            LOGGER.log(Level.INFO, "logger: Сервер уже запущен");
            putLog("Server already stared");
        } else {
            server = new ServerSocketThread(this, "Chat server", port, 2000);
            LOGGER.log(Level.INFO, "logger: Сервер перешел в состояние \"Включен\".");
        }
    }

    public void stop() {
        if (server == null || !server.isAlive()) {
            LOGGER.log(Level.WARNING, "logger: Сервер еще не запущен.");
            putLog("Server is not running");
        } else {
            server.interrupt();
            executorService.shutdown();
            LOGGER.log(Level.INFO, "logger: Сервер перешел в состояние \"Выключен\".");


        }
    }

    private void putLog(String msg) {
        msg = DATE_FORMAT.format(System.currentTimeMillis()) +
                Thread.currentThread().getName() + ": " + msg;
        listener.onChatServerMessage(msg);
    }

    /**
     * Server Socket Thread Listener methods
     */
    @Override
    public void onServerStart(ServerSocketThread thread) {
        putLog("Server started");
        SqlClient.connect();
    }

    @Override
    public void onServerStop(ServerSocketThread thread) {
        listOfClients.remove(thread);
        putLog("Server stopped");
        SqlClient.disconnect();
    }


    @Override
    public void onServerSocketCreated(ServerSocketThread thread, ServerSocket server) {
        putLog("Listening to port");
    }

    @Override
    public void onServerTimeout(ServerSocketThread thread, ServerSocket server) {
        putLog("Waiting for connection...");
    }


    /**
     * Метод создает новый соектТреад на основе сокета полученный ивентом от ССТЛ, как бы оборачивает сокеты в сокетТреады
     *
     * @param thread
     * @param server
     * @param socket
     */
    @Override
    synchronized public void onSocketAccepted(ServerSocketThread thread, ServerSocket server, Socket socket) {
        String name = "Client " + socket.getInetAddress() + ":" + socket.getPort();

        executorService.execute(() -> {
            new ClientThread(this, name, socket);
        });

    }

    @Override
    public void onServerException(ServerSocketThread thread, Throwable exception) {
        exception.printStackTrace();
    }

    @Override
    synchronized public void broadcastMessage(String msg) {
    }

    /**
     * Socket Thread Listener methods
     */
    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {
        putLog("Client thread started");

    }


    @Override
    public void onSocketStop(SocketThread thread) {
        ClientThread client = (ClientThread) thread;
        listOfClients.remove(thread);
        if (client.isAuthorized() && !client.isReconnecting()) {
            sendToAllAuthorizedClients(Common.getTypeBroadcast("Server", client.getNickname() + " disconnected"));
            LOGGER.log(Level.INFO, "logger: Пользователь "+ client.getNickname() +" отключен.");
        }
        sendToAllAuthorizedClients(Common.getUserList(getUsers()));
    }


    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        listOfClients.add(thread);
        putLog("Client is ready to chat");

    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        ClientThread client = (ClientThread) thread;
        if (client.isAuthorized()) {
            try {
                handleAuthMessage(client, msg);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            handleNonAuthMessage(client, msg);
        }

    }


    private void handleNonAuthMessage(ClientThread client, String msg) {
        String[] arr = msg.split(Common.DELIMITER);
        if (arr.length != 3 || !arr[0].equals(Common.AUTH_REQUEST)) {
            client.msgFormatError(msg);
            return;
        }
        String login = arr[1];
        String password = arr[2];
        String nickname = SqlClient.getNickname(login, password);
        if (nickname == null) {
            putLog("Invalid login attempt: " + login);
            LOGGER.log(Level.WARNING, "logger: Логин не распознан");
            client.authFail();
            return;
        } else {
            ClientThread oldClient = findClientByNickname(nickname);
            client.authAccept(nickname);
            if (oldClient == null) {
                sendToAllAuthorizedClients(Common.getTypeBroadcast("Server", nickname + " connected"));
                LOGGER.log(Level.INFO, "logger: Пользователь " + nickname + " подключился.");
            } else {
                oldClient.reconnect();
                listOfClients.remove(oldClient);
            }
        }
        sendToAllAuthorizedClients(Common.getUserList(getUsers()));
    }

    private void handleAuthMessage(ClientThread client, String msg) throws SQLException {
        String[] arr = msg.split(Common.DELIMITER);
        String msgType = arr[0];
        if (!arr[1].contains("/")) {
            switch (msgType) {
                case Common.TYPE_BCAST_CLIENT:
                    sendToAllAuthorizedClients(
                            Common.getTypeBroadcast(client.getNickname(), arr[1]));
                    LOGGER.log(Level.INFO, "logger: Пользователь "+ client.getNickname() + " отправил сообщение.");

                    break;
                default:
                    client.msgFormatError(msg);
                    LOGGER.log(Level.WARNING, "logger: Неверный формат сообщения.");

            }
        } else {
            String[] arrServiceMsg = arr[1].split(" ");
            String serviceMsgType = arrServiceMsg[0];

            switch (serviceMsgType) {// TODO: 26.09.2020 добавить проверка на повтор нового никнейма
                case Common.CHANGE_LOGIN:
                    sendToAllAuthorizedClients(
                            Common.getTypeBroadcast(client.getNickname(), " changes nickname to:" + arrServiceMsg[1]));
                    SqlClient.changeNickname(arrServiceMsg[1], client.getNickname());

                    client.authAccept(arrServiceMsg[1]);
                    sendToAllAuthorizedClients(Common.getUserList(getUsers()));
                    client.updNickname();
                    break;
                default:
                    client.msgFormatError(msg);
                    LOGGER.log(Level.WARNING, "logger: Неверный формат сообщения");
            }
        }
    }

    private void sendToAllAuthorizedClients(String msg) {
        for (int i = 0; i < listOfClients.size(); i++) {
            ClientThread client = (ClientThread) listOfClients.get(i);
            if (!client.isAuthorized()) continue;
            client.sendMessage(msg);
        }
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
    }

    private String getUsers() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < listOfClients.size(); i++) {
            ClientThread client = (ClientThread) listOfClients.get(i);
            if (!client.isAuthorized()) continue;
            sb.append(client.getNickname()).append(Common.DELIMITER);
        }
        return sb.toString();
    }

    private synchronized ClientThread findClientByNickname(String nickname) {
        for (int i = 0; i < listOfClients.size(); i++) {
            ClientThread client = (ClientThread) listOfClients.get(i);
            if (!client.isAuthorized()) continue;
            if (client.getNickname().equals(nickname))
                return client;
        }
        return null;
    }

}

