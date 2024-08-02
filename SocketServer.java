import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;
import java.util.stream.Collectors;

public class SocketServer {
    ServerSocket server;
    Socket sk;
    InetAddress addr;
    ServerSocket fileServer;

    ArrayList<ServerThread> list = new ArrayList<ServerThread>();
    Map<String, ServerThread> userMap = new HashMap<String, ServerThread>(); // Add to keep track of users

    // Add Logger
    static final Logger logger = Logger.getLogger(SocketServer.class.getName());

    public SocketServer() {
        try {
            // Set up Logging
            setupLogging();
            fileServer = new ServerSocket(1235);  // New ServerSocket for file transfers
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
        list.remove(st);
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
//        if (userMap.isEmpty()) {
//            return "No users connected";
//        }
//        return String.join(", ", userMap.keySet());
        if (userMap.isEmpty()) {
            return "No users connected";
        }
        return userMap.keySet().stream()
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.joining(", "));
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
            if (name != null && !name.isEmpty()) {
                server.userMap.put(name, this);
                // Server logger joined message
                server.logger.info("New client joined: " + name);
                // Broadcast that a new client has entered
                server.broadCast("**[" + name + "] Entered**");
            } else {
                server.logger.warning("Client attempted to join with null or empty name");
                return;  // Exit the thread if the name is invalid
            }

            String data;
            while ((data = br.readLine()) != null) {
                if (data.equals("/list")) {
                    String userList = server.getUserList();
                    pw.println("Current users: " + userList);
                    server.logger.info("Sent user list to " + name);
                }  if (data.startsWith("/file ")) {
                    String fileName = data.substring(6);
                    receiveAndBroadcastFile(fileName);
                } else if (data.startsWith("/weather")) {
                    String location = data.substring(9).trim();
                    String weatherReport = getMockWeather(location);
                    pw.println(weatherReport);
                    server.logger.info("Sent weather report to " + name + " for location: " + location);
                } else {
                    // Broadcast the received message to all clients
                    server.broadCast("[" + name + "] " + data);
                    server.logger.info("Message from " + name + ": " + data);
                }
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

    private String getMockWeather(String location) {
        String[] conditions = {"Sunny", "Cloudy", "Rainy", "Snowy", "Windy"};
        String condition = conditions[(int) (Math.random() * conditions.length)];
        int temperature = (int) (Math.random() * 40) - 10;
        return String.format("Weather for %s: %s, %d°C", location, condition, temperature);
    }

    private void receiveAndBroadcastFile(String fileName) {
        try {
            Socket fileSocket = server.fileServer.accept();
            InputStream is = fileSocket.getInputStream();

            File receivedFile = new File("received_" + fileName);
            FileOutputStream fos = new FileOutputStream(receivedFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            fos.close();
            is.close();
            fileSocket.close();

            server.broadCast("File received: " + fileName + ". Size: " + receivedFile.length() + " bytes");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}