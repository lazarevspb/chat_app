package chat.server.gui;

import chat.server.core.ChatServer;
import chat.server.core.ChatServerListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Класс, создает чат сервер
 */
public class ServerGui extends JFrame implements ActionListener, Thread.UncaughtExceptionHandler,ChatServerListener {

    private static final int POS_X = 800;
    private static final int POS_Y = 200;
    private static final int WIDTH = 600;
    private static final int HEIGHT = 300;
    private final ChatServer chatServer = new ChatServer(this);
    private final JButton btnStart = new JButton("Start");
    private final JButton btnStop = new JButton("Stop");
    private final JPanel panelTop = new JPanel(new GridLayout(1, 2));
    private final JTextArea log = new JTextArea();

    public static void main(String[] args) {
        /*Создается окно в потоке event dispatching thread (EDT) */
        /*параметры метода *//*Вызов конструктора, создание нового объекта, реализующего интерфейс(анонимный класс),
        передается в качестве аргумента*//*переопределяемый метод взятый из интерфейса Runnable()*/
        /*Создаем объект, вызывая конструктор-*/
        SwingUtilities/*Класс*/.invokeLater/*Статический метод*/(/*аргумент*/ ServerGui::/*вызов конструктора класса*/new);

//        /*Классический способ*/
//        /*Создается окно в потоке event dispatching thread (EDT) */
//         */
//        SwingUtilities/*Класс*/.invokeLater/*Статический метод*/(/*аргумент*/ new Runnable(/*параметры метода */) {
//            /*Вызов конструктора, создание нового объекта, реализующего интерфейс(анонимный класс),
//            передается в качестве аргумента*/
//            @Override
//            public void run()/*переопределяемый метод взятый из интерфейса Runnable()*/ {
//                new ServerGui();/*Создаем объект, вызывая конструктор-*/
//            }
//        });
//
//
//    }
    }

    private ServerGui() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(POS_X, POS_Y, WIDTH, HEIGHT);
        setResizable(false);
        setTitle("Chat server");
        setAlwaysOnTop(true);
        log.setEditable(false);
        log.setLineWrap(true);
        JScrollPane scrollLog = new JScrollPane(log);

        btnStart.addActionListener(this);
        btnStop.addActionListener(this);

        panelTop.add(btnStart);
        panelTop.add(btnStop);
        add(panelTop, BorderLayout.NORTH);
        add(scrollLog, BorderLayout.CENTER);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnStop)
            chatServer.stop();
        else if (src == btnStart)
//            throw new RuntimeException("Hello from EDT");
            chatServer.start(8189);
        else
            throw new RuntimeException("Unknown source: " + src);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        String msg;
        StackTraceElement[] ste = e.getStackTrace();
        msg = "Exception in " + t.getName() + " " +
                e.getClass().getCanonicalName() + ": " +
                e.getMessage() + "\n\t at " + ste[0];
        JOptionPane.showMessageDialog(this, msg, "Exception", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    @Override
    public void onChatServerMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            log.append(msg + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }
}
