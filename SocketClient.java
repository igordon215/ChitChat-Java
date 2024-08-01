import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@SuppressWarnings("serial")
public class SocketClient extends JFrame implements ActionListener, Runnable {
    // GUI components
    JTextArea textArea = new JTextArea();
    JScrollPane jp = new JScrollPane(textArea);
    JTextField input_Text = new JTextField();
    JMenuBar menuBar = new JMenuBar();
    JList<String> userList = new JList<>(); // Adding for userList

    // Networking components
    Socket sk;
    BufferedReader br;
    PrintWriter pw;


    public SocketClient() {
        super("Chit Chat");
        // Set up the main window
        setFont(new Font("Arial Black", Font.PLAIN, 12));
        setForeground(new Color(0, 0, 51));
        setBackground(new Color(51, 0, 0));

        // Configure the text area for chat history
        textArea.setToolTipText("Chat History");
        textArea.setForeground(new Color(50, 205, 50));
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.BOLD, 13));
        textArea.setBackground(new Color(0, 0, 0));

        userList.setBackground(new Color(230, 230, 250));
        userList.setFont(new Font("Tahoma", Font.PLAIN, 12));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jp, new JScrollPane(userList));
        splitPane.setResizeWeight(0.8);

        /*
         JMenu helpMenu = new JMenu("Help");
        JMenuItem update = new JMenuItem("Update Information");
        JMenuItem connect_List = new JMenuItem("Visitor List");

        helpMenu.add(update);
        helpMenu.add(connect_List);

        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
        */

        // Add components to the window
        getContentPane().add(jp, "Center");

        // Configure the input text field
        input_Text.setText("Enter your Message:");
        input_Text.setToolTipText("Enter your Message");
        input_Text.setForeground(new Color(0, 0, 0));
        input_Text.setFont(new Font("Tahoma", Font.BOLD, 11));
        input_Text.setBackground(new Color(230, 230, 250));

        getContentPane().add(input_Text, "South");

        JButton listButton = new JButton("User List");
        listButton.addActionListener(e -> {pw.println("/list");});
        getContentPane().add(listButton, "North");

        // Set window properties
        setSize(325, 411);
        setVisible(true);

        input_Text.requestFocus(); //Place cursor at run time, work after screen is shown

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Register action listener for the input field
        input_Text.addActionListener(this); //Event registration
    }

    public void serverConnection() {
        try {

            // Prompt for server IP
            String IP = JOptionPane.showInputDialog(this, "Please enter a server IP.", JOptionPane.INFORMATION_MESSAGE);
            sk = new Socket(IP, 1234);

            // Prompt for nickname
            String name = JOptionPane.showInputDialog(this, "Please enter a nickname", JOptionPane.INFORMATION_MESSAGE);
/*            while (name.length() > 7) {
                name = JOptionPane.showInputDialog(this, "Please enter a nickname.(7 characters or less)", JOptionPane.INFORMATION_MESSAGE);
            }
*/
            // Set up input and output streams
            br = new BufferedReader(new InputStreamReader(sk.getInputStream()));

            // Send nickname to server
            pw = new PrintWriter(sk.getOutputStream(), true);
            pw.println(name); // Send to server side

            // Start thread to receive messages
            new Thread(this).start();

        } catch (Exception e) {
            System.out.println(e + " Socket Connection error");
        }
    }

    public static void main(String[] args) {
        // Create SocketClient instance and connect to server
        new SocketClient().serverConnection(); //Method call at the same time object creation
    }

    @Override
    public void run() {
        String data = null;
        try {
            // Continuosly read and display incoming messages
            while ((data = br.readLine()) != null) {
                if (data.startsWith("Current users: ")) {
                    String[] users = data.substring("Current users: ".length()).split(", ");
                    SwingUtilities.invokeLater(() -> {userList.setListData(users);});
                } else {
                    textArea.append(data + "\n"); //textArea Decrease the position of the box's scroll bar by the length of the text entered
                    textArea.setCaretPosition(textArea.getText().length());
                }

            }
        } catch (Exception e) {
            System.out.println(e + "--> Client run fail");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Send message when Enter is pressed in the input field
        String data = input_Text.getText();
        pw.println(data); // Send to server side
        input_Text.setText("");
    }
}