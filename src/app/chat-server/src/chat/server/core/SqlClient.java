package chat.server.core;

import java.sql.*;

public class SqlClient {

    private static Connection connection;
    private static Statement statement;


    synchronized  static void connect(){

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:src/lesson2/chat_app/chat-server/chat.db");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized  static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static String getNickname(String login, String password) {
        String query = String.format("select nickname from users where login='%s'" + //Запрос в бд
                " and password='%s'", login, password);
        try (ResultSet set = statement.executeQuery(query)) {  //Если вопрос вернет множество результатов
            if (set.next())
                return set.getString(1/*номер столбца или название столбца, например "nickname"*/);//забираем строку
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null; //вернем null, если запрос вернулся пустым
    }

    synchronized static void changeNickname(String newNickname, String oldNickname)  {
        String update = String.format("update users set nickname = '%s' where nickname = '%s'",  //Запрос в бд
                newNickname, oldNickname);
        try {
            statement.executeUpdate(update);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }}
