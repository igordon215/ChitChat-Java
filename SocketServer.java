import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class SocketServer {
    ServerSocket server;
    Socket sk;
    InetAddress addr;

    ArrayList<ServerThread> list = new ArrayList<ServerThread>();

    public SocketServer() {
        try {
            // Set up the server address (localhost)
            addr = InetAddress.getByName("127.0.0.1");
            //addr = InetAddress.getByName("192.168.43.1");

            //Create a ServerSocket on port 1234 with a backlog of 50 connections
            server = new ServerSocket(1234, 50, addr);
            System.out.println("\n Waiting for Client connection");
            // Start the client
            SocketClient.main(null);
            while (true) {
                // Accept incoming client connections
                sk = server.accept();
                System.out.println(sk.getInetAddress() + " connect");

                //Create and start a new thread for the connected client
                ServerThread st = new ServerThread(this);
                addThread(st);
                st.start();
            }
        } catch (IOException e) {
            System.out.println(e + "-> ServerSocket failed");
        }
    }

    // Add a new client thread from the list
    public void addThread(ServerThread st) {
        list.add(st);
    }

    // Remove a client thread from the list
    public void removeThread(ServerThread st) {
        list.remove(st); //remove
    }

    // Broadcast a message to all connected clients
    public void broadCast(String message) {
        for (ServerThread st : list) {
            st.pw.println(message);
        }
    }

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
            // Broadcast that a new client has entered
            server.broadCast("**[" + name + "] Entered**");

            String data;
            while ((data = br.readLine()) != null) {
                // Check for list Command
                if (data == "/list") {
                    pw.println("a");
                }
                // Broadcast the received message to all clients
                server.broadCast("[" + name + "] " + data);
            }
        } catch (Exception e) {
            //Remove the current thread from the ArrayList.
            server.removeThread(this);
            // Broadcast that the client has left
            server.broadCast("**[" + name + "] Left**");
            System.out.println(server.sk.getInetAddress() + " - [" + name + "] Exit");
            System.out.println(e + "---->");
        }
    }
}