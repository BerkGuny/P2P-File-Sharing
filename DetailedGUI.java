package project471;

import javax.swing.*;
import java.io.File;
import java.awt.*;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List; // Correct import for java.util.List
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.*;
// /home/banzai/Masaüstü/wayd  /home/banzai/Masaüstü/deneme
public class DetailedGUI implements NewPeerListener, Node.FileNamesListener {
    private JFrame frame;
    private boolean extractUsed =false;
    private JTextArea computersInNetworkTextArea;
    private JTextArea filesFoundTextArea;
    private JTextArea fileTransfersTextArea;
    private JLabel statusLabel;
    private JLabel computerHostnameLabel;
    private JLabel computerIPLabel;
    public Node node; // Reference to your Node class
    private JList<String> filesFoundList;
    private JTextField sharedFolderField;
    private String sharedFolderPath;
    public List<String> singleFileList = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
   

    
    public DetailedGUI(String sharedFolderPath, String sharedSecret) {
        this.sharedFolderPath = sharedFolderPath;
    
        try {
            node = new Node(this.sharedFolderPath, sharedSecret,this);
            node.setFileNamesListener(this);
        } catch (Exception e) {
            e.printStackTrace(); // Handle exception properly
        }
        initializeComponents();
        startPeriodicUpdate();
    }
   
    private void startPeriodicUpdate() {
        scheduler.scheduleAtFixedRate(this::updatePeersList, 0, 6, TimeUnit.SECONDS);
    }
   
    private void updatePeersList() {
        SwingUtilities.invokeLater(() -> {
            computersInNetworkTextArea.setText(""); // Clear the text area
            for (String peer : node.getPeers()) { 
                computersInNetworkTextArea.append(peer + "\n"); // Append each peer to the text area
            }
        });
    }

    private void initializeComponents() {
        frame = new JFrame("P2P Network Details");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        setupMenu();
        setupContentPanels();

        frame.pack();
        frame.setLocationRelativeTo(null);  // Center the frame
        updateComputerInformation();

        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem connectMenuItem = new JMenuItem("Connect");
        connectMenuItem.addActionListener(this::connectToNetwork);
        fileMenu.add(connectMenuItem);

        JMenuItem disconnectMenuItem = new JMenuItem("Disconnect");
        disconnectMenuItem.addActionListener(this::disconnectFromNetwork);
        fileMenu.add(disconnectMenuItem);

        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenu aboutMenuItem = new JMenu("About");
        aboutMenuItem.addMouseListener(new MouseAdapter(){
        	@Override
        	public void mouseClicked(MouseEvent e) {
        		JOptionPane.showMessageDialog(frame,"Berk Günay 20190702015");
        	}
        });
        helpMenu.add(aboutMenuItem);
        menuBar.add(helpMenu);
        
        JMenu extractMenu = new JMenu("extract");
        JMenu extractMenuItem = new JMenu("Selection");
        extractMenuItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectAndExtractFiles();
            }
        });
        extractMenu.add(extractMenuItem);
        menuBar.add(extractMenu);
        frame.setJMenuBar(menuBar);
    }
    
    private void selectAndExtractFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            for (File file : selectedFiles) {
                String fileName = file.getName();
                String fullPath = file.getAbsolutePath();
                this.node.populateSharedFilesList(fileName);
                this.node.hold_path(fullPath, fileName);
                singleFileList.add(fileName);
            }

            // Set the flag to true since files have been selected using Extract
            extractUsed = true;

            updateFilesFound(singleFileList); // Update GUI with the selected files
        }
    }

    private void setupContentPanels() {
        // Status Label
        statusLabel = new JLabel("Status: Ready");
        frame.add(statusLabel, BorderLayout.SOUTH);  // Adding the status label at the bottom

        // Computers in Network Panel
        computersInNetworkTextArea = new JTextArea(10, 20);
        computersInNetworkTextArea.setEditable(false);
        JScrollPane computersInNetworkScrollPane = new JScrollPane(computersInNetworkTextArea);  // Scrollable pane
        JPanel computersInNetworkPanel = new JPanel(new BorderLayout());
        computersInNetworkPanel.add(new JLabel("Computers in Network:"), BorderLayout.NORTH);
        computersInNetworkPanel.add(computersInNetworkScrollPane, BorderLayout.CENTER);

        // Files Found Panel
        filesFoundList = new JList<>();  // Initializing the JList
        filesFoundList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Ensure only one file can be selected at a time
        JScrollPane filesFoundScrollPane = new JScrollPane(filesFoundList);
        JPanel filesFoundPanel = new JPanel(new BorderLayout());
        filesFoundPanel.add(new JLabel("Files Found:"), BorderLayout.NORTH);
        filesFoundPanel.add(filesFoundScrollPane, BorderLayout.CENTER);

        // Add double-click listener to the filesFoundList for file selection
        filesFoundList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList)evt.getSource();
                if (evt.getClickCount() == 2) {  // Double-click
                    int index = list.locationToIndex(evt.getPoint());
                    if (index >= 0) {  // Valid index
                        String selectedFileName = (String) list.getModel().getElementAt(index);
                        onFileSelected(selectedFileName);
                    }
                }
            }
        });

        // File Transfers Panel
        fileTransfersTextArea = new JTextArea(5, 20);
        fileTransfersTextArea.setEditable(false);
        JScrollPane fileTransfersScrollPane = new JScrollPane(fileTransfersTextArea);
        JPanel fileTransfersPanel = new JPanel(new BorderLayout());
        fileTransfersPanel.add(new JLabel("File Transfers:"), BorderLayout.NORTH);
        fileTransfersPanel.add(fileTransfersScrollPane, BorderLayout.CENTER);

        // Computer Information Panel
        JPanel computerInfoPanel = new JPanel(new GridLayout(1, 2));
        computerHostnameLabel = new JLabel("Computer Hostname: Unknown");  // Will be updated when GUI starts
        computerIPLabel = new JLabel("Computer IP: Unknown");  // Will be updated when GUI starts
        computerInfoPanel.add(computerHostnameLabel);
        computerInfoPanel.add(computerIPLabel);

        // Main Content Panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(computerInfoPanel, BorderLayout.NORTH);
        contentPanel.add(computersInNetworkPanel, BorderLayout.WEST);
        contentPanel.add(filesFoundPanel, BorderLayout.CENTER);
        contentPanel.add(fileTransfersPanel, BorderLayout.EAST);

        // Adding the main content panel to the frame
        frame.add(contentPanel, BorderLayout.CENTER);
    }

    @Override
    public void onNewPeerDetected(String peerAddress) {
        SwingUtilities.invokeLater(this::updatePeersList); // Call updatePeersList directly
    }

    private void updateComputerInformation() {
        try {
            String localIP = NetworkUtils.getLocalAddress();
            if(localIP != null) {
                computerIPLabel.setText("Computer IP: " + localIP);
            } else {
                computerIPLabel.setText("Computer IP: Not found");
            }
        } catch (Exception e) {
            computerIPLabel.setText("Computer IP: Error");
            e.printStackTrace();
        }
    }


   
    public void onFileSelected(String fileName) {
        if(singleFileList.contains(fileName)) {
        	JOptionPane.showMessageDialog(frame,"Can't select it: "+ fileName);
        }
        else {
        	JFileChooser fileChooser = new JFileChooser();
        	fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        	int option = fileChooser.showOpenDialog(frame);
        	if(option == JFileChooser.APPROVE_OPTION) {
        		File selectedFolder = fileChooser.getSelectedFile();
        		Node.downloadPath = selectedFolder.getAbsolutePath();
        		this.node.get_the_filename(fileName);
            	this.node.start2();
        	}
        }
    	//node.requestFile(fileName); // Assume Node class has a method to handle file requests
        //updateStatus("Requested file: " + fileName); // Optionally update the status
    }
    
   
    public void updateFilesFound(List<String> files) {
        SwingUtilities.invokeLater(() -> {
            DefaultListModel<String> model = new DefaultListModel<>();
            for (String file : files) {
                model.addElement(file);  // Add each file as a separate element
            }
            filesFoundList.setModel(model);
        });
    }
    private void connectToNetwork(ActionEvent e) {
        try {
            File sharedFolder = new File(this.sharedFolderPath);

            if (!extractUsed) { // Check if Extract was not used
                if (!sharedFolder.exists()) {
                    System.out.println("Shared folder does not exist.");
                } else if (sharedFolder.isFile()) {
                    String fileName = extractFileName(this.sharedFolderPath);
                    this.node.populateSharedFilesList(fileName);
                    this.node.hold_path(this.sharedFolderPath, fileName);
                    singleFileList.add(fileName);
                } else {
                    File[] filesInDirectory = sharedFolder.listFiles();
                    if (filesInDirectory != null) {
                        for (File file : filesInDirectory) {
                            String fileName = file.getName();
                            String fullPath = file.getAbsolutePath();
                            this.node.populateSharedFilesList(fileName);
                            this.node.hold_path(fullPath, fileName);
                            singleFileList.add(fileName);
                        }
                    }
                }
            }

            this.node.start();
            updateFilesFound(singleFileList);
            updateStatus("Connected to network. Listening for peers...");
        } catch (Exception ex) {
            updateStatus("Failed to connect to network: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Utility method to extract file name from the full path
    private String extractFileName(String fullPath) {
        // Handle both Unix/Linux and Windows paths
        int lastUnixPos = fullPath.lastIndexOf('/');
        int lastWindowsPos = fullPath.lastIndexOf('\\');
        int lastPos = Math.max(lastUnixPos, lastWindowsPos);

        if (lastPos == -1) {
            return fullPath; // No path separator found, return the fullPath itself
        } else {
            return fullPath.substring(lastPos + 1);
        }
    }

    private void disconnectFromNetwork(ActionEvent e) {
        if (node != null) {
            node.shutdown(); // Assuming Node class has a shutdown method
            updateStatus("Disconnected from network.");
        }
        // Further logic to update GUI based on disconnection status
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    public void display() {
        frame.setVisible(true);
    }

    @Override
    public void onFileNamesReceived(List<String> fileNames) {
        SwingUtilities.invokeLater(() -> {
            updateFilesFound(fileNames);
        });
    }
    public void showHelpDialog() {
    	if(frame != null) {
    		JOptionPane.showMessageDialog(frame,"Berk Günay 20190702015");
    	}
    	else {
    		System.err.print("Error");
    	}
    }

}