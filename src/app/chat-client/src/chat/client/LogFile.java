package chat.client;

import java.io.*;
import java.util.Scanner;

public class LogFile extends File {
    private String nameFile;
    private String contentLogFile;
  Scanner scanner;

    public LogFile(String nameFile, String contentLogFile) {
        super("log");
        this.nameFile = nameFile;
        this.contentLogFile = contentLogFile;

    }



    public LogFile(String nameFile) {
        super("log");
        this.nameFile = nameFile;

    }

    protected boolean createTextFile(String content) throws FileNotFoundException {
        File file = new File(this.nameFile);
        PrintStream printStream;
        printStream = new PrintStream(this.nameFile);
        printStream.print(content);
        printStream.flush();
        printStream.close();
        return file.exists();
    }

    protected boolean addTextToFile(String content) throws FileNotFoundException {
        String s;
        File file = new File(this.nameFile);
        s = readFile(this.nameFile);
        PrintStream printStream;
        printStream = new PrintStream(this.nameFile);
        printStream.print(s + content);
        printStream.flush();
        printStream.close();
        return file.exists();
    }

    protected  String readFile(String nameFile) throws FileNotFoundException {
        StringBuilder stringBuilder = new StringBuilder();
        scanner = new Scanner(new FileInputStream(nameFile));
        while (scanner.hasNext()) {
            stringBuilder.append(scanner.nextLine()).append("\n");
        }
        scanner.close();
        return stringBuilder.toString();
    }

    protected boolean createFile(String name){
        File file = new File(this.nameFile);
      return   file.mkdir();

    }

    protected void wrtMsgToLogFile(String msg) {
        try (FileWriter out = new FileWriter("log", true)) {
            out.write(msg + "\n");
            out.flush();
        } catch (IOException e) {
           e.printStackTrace();
        }
    }
}
