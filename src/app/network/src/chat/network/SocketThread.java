package chat.network;

import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


public class SocketThread extends Thread implements Closeable {

    private final SocketThreadListener listener;
    private final Socket socket;
    private DataOutputStream out;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss ");

    public SocketThread(SocketThreadListener listener, String name, Socket socket) {
        super(name);
        this.socket = socket;
        this.listener = listener;
        start();
    }

    @Override
    public void run() {
        try {
            listener.onSocketStart(this, socket);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            listener.onSocketReady(this, socket);
            while (!isInterrupted()) {
                String msg = "";
                try {
                    msg = in.readUTF();
                    listener.onReceiveString(this, socket, msg);
                } catch (EOFException e) {
                    in.close();
                    out.flush();
                    out.close();
                    interrupt();
                }
            }

        } catch (IOException e) {
            listener.onSocketException(this, e);
        } finally {
            close();
            listener.onSocketStop(this);
        }
    }

    public synchronized boolean sendMessage(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
            return true;
        } catch (IOException e) {
            listener.onSocketException(this, e);
            close();
            return false;
        }
    }


    public synchronized void close() {
        interrupt();
        try {
            out.flush();
            socket.close();
        } catch (IOException e) {
            listener.onSocketException(this, e);
        }
    }
}
