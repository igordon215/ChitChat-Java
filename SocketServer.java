import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

public class SocketServer {
    ServerSocket server;
    Socket sk;
    InetAddress addr;

    ArrayList<ServerThread> list = new ArrayList<ServerThread>();
    Map<String, ServerThread> userMap = new HashMap<String, ServerThread>(); // Add to keep track of users

    // Add Logger
    static final Logger logger = Logger.getLogger(SocketServer.class.getName());

    public SocketServer() {
        try {
            // Set up Logging
            setupLogging();

            // Set up the server address (localhost)
            addr = InetAddress.getByName("127.0.0.1");
            //addr = InetAddress.getByName("192.168.43.1");

            //Create a ServerSocket on port 1234 with a backlog of 50 connections
            server = new ServerSocket(1234, 50, addr);
            // Adding some logger text
            logger.info("Server started. Waiting for client connections...");
            System.out.println("\n Waiting for Client connection");
            // Start the client
            SocketClient.main(null);
            while (true) {
                // Accept incoming client connections
                sk = server.accept();
                // Adding connected message
                logger.info("New connection from: " + sk.getInetAddress());
                System.out.println(sk.getInetAddress() + " connect");

                //Create and start a new thread for the connected client
                ServerThread st = new ServerThread(this);
                addThread(st);
                st.start();
            }
        } catch (IOException e) {
            // logger fail message
            logger.severe("ServerSocket failed: " + e.getMessage());
            System.out.println(e + "-> ServerSocket failed");
        }
    }

    // Setup logging method
    private void setupLogging() throws IOException {
        // Create file handler
        FileHandler fileHandler = new FileHandler("server_log.txt", true);

        // Create formatter
        SimpleFormatter formatter = new SimpleFormatter() {
            @Override
            public synchronized String format(LogRecord lr) {
                return String.format("[%s] %s: %s%n",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage());
            }
        };
        fileHandler.setFormatter(formatter);

        logger.addHandler(fileHandler);

        logger.setLevel(Level.ALL);
    }


    // Add a new client thread from the list
    public void addThread(ServerThread st) {
        list.add(st);
        userMap.put(st.name, st);
        logger.info("New client thread added. Total clients: " + list.size());
    }

    // Remove a client thread from the list
    public void removeThread(ServerThread st) {
        list.remove(st); //remove
        userMap.remove(st.name);
        logger.info("Client thread removed. Total clients: " + list.size());
    }

    // Broadcast a message to all connected clients
    public void broadCast(String message) {
        for (ServerThread st : list) {
            st.pw.println(message);
        }
        //logger.info("Broadcasted message: " + message);
    }

    public String getUserList() {
        return String.join(", ", userMap.keySet());
    }
    // This is our MAIN METHOD
    public static void main(String[] args) {
        new SocketServer();
    }
}

//Thread connected clients to ArrayList
class ServerThread extends Thread {
    SocketServer server;
    PrintWriter pw;
    String name;

    public ServerThread(SocketServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // Set up reader for incoming messages
            BufferedReader br = new BufferedReader(new InputStreamReader(server.sk.getInputStream()));

            // Set up writer for the outgoing messages
            pw = new PrintWriter(server.sk.getOutputStream(), true);
            // Read the client's name
            name = br.readLine();
            // Server logger joined message
            server.logger.info("New client joined: " + name);
            // Broadcast that a new client has entered
            server.broadCast("**[" + name + "] Entered**");

            String data;
            while ((data = br.readLine()) != null) {
                // Check for list Command
                if (data.equals("/list")) {
                    String userList = server.getUserList();
                    pw.println("Current users: " + userList);
                    server.logger.info("Message from " + name + ": " + data);
                }
                // Broadcast the received message to all clients
                server.broadCast("[" + name + "] " + data);
            }
        } catch (Exception e) {
            //Remove the current thread from the ArrayList.
            server.removeThread(this);
            // Broadcast that the client has left
            server.broadCast("**[" + name + "] Left**");
            server.logger.info("Client disconnected: " + name);
            System.out.println(server.sk.getInetAddress() + " - [" + name + "] Exit");
            server.logger.severe("Error in client thread: " + e.getMessage());
            System.out.println(e + "---->");
        }
    }
}