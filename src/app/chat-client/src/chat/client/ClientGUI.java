package chat.client;

import chat.common.Common;
import chat.network.SocketThread;
import chat.network.SocketThreadListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * chat_app
 * @author Valeriy Lazarev
 * @since 14.09.2020
 */
/**
 * Класс создает клиентские сокеты, которые при создании подключаются к серверу.
 * Оборачивает созданные сокеты в сокетТреады для работы в отдельном потоке.
 * СокетТреад через СТЛ отправляет свои события в клиентГуи тк он реализует интерфейс СТЛ.
 */
public class ClientGUI extends JFrame implements ActionListener, Thread.UncaughtExceptionHandler, SocketThreadListener {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;
    private static final int BORDER_SIZE = 5;
    private static final int LIMIT_LINES = 100;

    private final JPanel mainPanel = new JPanel();

    private final JPanel panelTop = new JPanel(new GridLayout(2, 3));
    private final JPanel panelBottom = new JPanel(new BorderLayout());
    private final JTextArea log = new JTextArea();

    private final JTextField tfIPAddress = new JTextField("127.0.0.1");
    private final JTextField tfPort = new JTextField("8189");
    private final JCheckBox cbAlwaysOnTop = new JCheckBox("Always on top", true);
    private final JTextField tfLogin = new JTextField("LV");
    private final JPasswordField tfPassword = new JPasswordField("123");
    private final JButton btnLogin = new JButton("Login");
    private final JButton btnDisconnect = new JButton("Disconnect");
    private final JTextField tfMessage = new JTextField();
    private final JButton btnSend = new JButton("Send");

    private final JList<String> userList = new JList<>();
    private boolean shownIoErrors = false;
    private SocketThread socketThread;
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss: ");
    private final String WINDOW_TITLE = "Chat";
    private Socket socket;
    private LogFile logFile = new LogFile("log");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }

    private ClientGUI() {

        if (logFile.exists()) { //обновление чата из лога
            try {
                StringBuilder stringBuilder = new StringBuilder();
                List<String> strings = Files.lines(Paths.get("log"))
                        .map(s -> s + " \n")
                        .collect(Collectors.toList());

                int i = strings.size() > LIMIT_LINES ? strings.size() - LIMIT_LINES : 0;
                for (int j = i; j < strings.size(); j++) {
                    stringBuilder.append(strings.get(j));
                }
                log.setText(stringBuilder.toString());
            } catch (IOException e) {
                throw new RuntimeException("Error method ClientGUI()");
            }
        }

        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setTitle(WINDOW_TITLE);
        setAlwaysOnTop(true);
        setSize(WIDTH, HEIGHT);
        log.setEditable(false);
        JScrollPane scrollUsers = new JScrollPane(userList);
        JScrollPane scrollLog = new JScrollPane(log);

        scrollUsers.setPreferredSize(new Dimension(100, 0));
        cbAlwaysOnTop.addActionListener(this);
        btnSend.addActionListener(this);
        tfMessage.addActionListener(this);
        btnLogin.addActionListener(this);
        btnDisconnect.addActionListener(this);

        panelTop.add(tfIPAddress);
        panelTop.add(tfPort);
        panelTop.add(cbAlwaysOnTop);
        panelTop.add(tfLogin);
        panelTop.add(tfPassword);
        panelTop.add(btnLogin);

        panelBottom.setVisible(false);
        panelBottom.add(btnDisconnect, BorderLayout.WEST);
        panelBottom.add(tfMessage, BorderLayout.CENTER);
        panelBottom.add(btnSend, BorderLayout.EAST);

        mainPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.setBackground(Color.DARK_GRAY);
        mainPanel.add(scrollLog, BorderLayout.CENTER);
        mainPanel.add(scrollUsers, BorderLayout.EAST);
        mainPanel.add(panelTop, BorderLayout.NORTH);
        mainPanel.add(panelBottom, BorderLayout.SOUTH);

        add(mainPanel);

        setVisible(true);
    }

    private EmptyBorder getBorder() {
        return new EmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == cbAlwaysOnTop) {
            setAlwaysOnTop(cbAlwaysOnTop.isSelected());
        } else if (src == btnSend || src == tfMessage) {
            sendMessage();
        } else if (src == btnLogin) {
            connect();
        } else if (src == btnDisconnect) {
            socketThread.close();
        } else {
            throw new RuntimeException("Unknown source: " + src);
        }
    }

    private void disconnect() {
        onSocketStop(socketThread);
    }

    private void connect() {
        try {
            socket = new Socket(tfIPAddress.getText(), Integer.parseInt(tfPort.getText()));
            socketThread = new SocketThread(this, "Client", socket);
        } catch (IOException exception) {
            showException(Thread.currentThread(), exception);
        }
    }

    private void sendMessage() {
        String msg = tfMessage.getText();
        if ("".equals(msg)) return;
        tfMessage.setText(null);
        tfMessage.grabFocus();
        socketThread.sendMessage(Common.getTypeBcastClient(msg));
    }

    private void wrtMsgToLogFile(String msg, String username) {
        try (FileWriter out = new FileWriter("log", true)) {
            out.write(username + ": " + msg + "\n");
            out.flush();
        } catch (IOException e) {
            if (!shownIoErrors) {
                shownIoErrors = true;
                showException(Thread.currentThread(), e);
            }
        }
    }

    private void putLog(String msg) {
        if ("".equals(msg)) return;
        SwingUtilities.invokeLater(() -> {
            log.append(msg + "\n");
            log.setCaretPosition(log.getDocument().getLength());//Устанавливает каретку на конец документа
        });
    }

    private void showException(Thread t, Throwable e) {
        String msg;
        StackTraceElement[] ste = e.getStackTrace();
        if (ste.length == 0)
            msg = "Empty Stacktrace";
        else {
            msg = String.format("Exception in \"%s\" %s: %s\n\tat %s",
                    t.getName(), e.getClass().getCanonicalName(), e.getMessage(), ste[0]);
            JOptionPane.showMessageDialog(this, msg, "Exception", JOptionPane.ERROR_MESSAGE);
        }
        JOptionPane.showMessageDialog(null, msg, "Exception", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        showException(t, e);
        System.exit(1);
    }

    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {
        putLog("Start");
    }

    @Override
    public void onSocketStop(SocketThread thread) {
        panelBottom.setVisible(false);
        panelTop.setVisible(true);
        setTitle(WINDOW_TITLE);
        userList.setListData(new String[0]);

    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        panelBottom.setVisible(true);
        panelTop.setVisible(false);
        String login = tfLogin.getText();
        String password = new String(tfPassword.getPassword());
        thread.sendMessage(Common.getAuthRequest(login, password));
        putLog("Ready");
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        handleMessage(msg);
//        putLog(msg);
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        putLog("Client terminated the connection");
    }


    private void handleMessage(String msg) {
        String[] arr = msg.split(Common.DELIMITER);
        String msgType = arr[0];

        switch (msgType) {
            case Common.AUTH_ACCEPT:
                setTitle(WINDOW_TITLE + " entered with nickname: " + arr[1]);  //тайтл устанавливается по факту авторизации
                break;
            case Common.AUTH_DENIED:
                putLog(msg);
                break;
            case Common.MSG_FORMAT_ERROR:
                putLog(msg);
                socketThread.close();
                break;
            case Common.TYPE_BROADCAST:
                String formatMsg = DATE_FORMAT.format(Long.parseLong(arr[1])) +
                        arr[2] + ": " + arr[3];
                putLog(formatMsg);
                logFile.wrtMsgToLogFile(formatMsg);

                break;
            case Common.CHANGE_LOGIN:

                break;
            case Common.USER_LIST:
                String users = msg.substring(Common.USER_LIST.length() +
                        Common.DELIMITER.length());
                String[] usersArr = users.split(Common.DELIMITER);
                Arrays.sort(usersArr);
                userList.setListData(usersArr);
                break;
            default:
                throw new RuntimeException("Unknown message type: " + msg);
        }
    }


}
