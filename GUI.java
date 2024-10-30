package project471;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class GUI {
    private JFrame frame;
    private JTextField sharedFolderField;
    private JTextField sharedSecretField;
    private JButton startButton;
    Node node;

    public GUI() {
        frame = new JFrame("P2P File Sharing App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        sharedFolderField = new JTextField(20);
        sharedSecretField = new JTextField(20);
        startButton = new JButton("Start");
        startButton.addActionListener(this::startButtonActionPerformed);
        
        setupMenu();
        setupContent();

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem connectMenuItem = new JMenuItem("Connect");
        JMenuItem disconnectMenuItem = new JMenuItem("Disconnect");
        JMenuItem exitMenuItem = new JMenuItem("Exit");

        //connectMenuItem.addActionListener(this::connectToNetwork);
        //disconnectMenuItem.addActionListener(this::disconnectFromNetwork);
        exitMenuItem.addActionListener(this::exitApplication);

        fileMenu.add(connectMenuItem);
        fileMenu.add(disconnectMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);
        JMenu helpMenu = new JMenu("Help");
        JMenu aboutMenuItem = new JMenu("About");
        aboutMenuItem.addMouseListener(new MouseAdapter() {
        	@Override
        	public void mouseClicked(MouseEvent e) {
        		JOptionPane.showMessageDialog(frame,"Berk Günay 20190702015");
        	}
        });
        helpMenu.add(aboutMenuItem);
        menuBar.add(helpMenu);
        
        frame.setJMenuBar(menuBar);
    }

    private void setupContent() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        JPanel sharedFolderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sharedFolderPanel.add(new JLabel("Shared Folder Location:"));
        sharedFolderPanel.add(sharedFolderField);
        mainPanel.add(sharedFolderPanel);
        
        JPanel sharedSecretPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sharedSecretPanel.add(new JLabel("Shared Secret:"));
        sharedSecretPanel.add(sharedSecretField);
        mainPanel.add(sharedSecretPanel);
        
        JPanel startButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startButtonPanel.add(startButton);
        mainPanel.add(startButtonPanel);
        
        frame.add(mainPanel, BorderLayout.CENTER);
    }
    public void showHelpDialog() {
    	if(frame != null) {
    		JOptionPane.showMessageDialog(frame,"Berk Günay 20190702015");
    	}
    	else {
    		System.err.print("Error");
    	}
    }

    private void startButtonActionPerformed(ActionEvent e) {
        String sharedFolderPath = sharedFolderField.getText();
        String sharedSecret = sharedSecretField.getText();
        if (validateInput(sharedFolderPath, sharedSecret)) {
            // Close the current window
            frame.dispose(); 

            // Open the new, detailed GUI window
            EventQueue.invokeLater(() -> {
                DetailedGUI detailedGUI = new DetailedGUI(sharedFolderPath, sharedSecret);
                detailedGUI.display();
            });
        }
    }

    private boolean validateInput(String sharedFolderPath, String sharedSecret) {
        if (sharedFolderPath.isEmpty() || sharedSecret.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    

    private void exitApplication(ActionEvent e) {
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUI::new);
    }
}
