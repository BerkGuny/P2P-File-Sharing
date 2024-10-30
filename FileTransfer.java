package project471;

import java.io.*;
import java.net.*;

public class FileTransfer {
    private String fileName;
    private String filePath;
    private long fileSize;
    private long bytesTransferred;
    private InetAddress targetAddress; // Changed to InetAddress
    private int targetPort;

    // Constants
    private static final int CHUNK_SIZE = 512 * 1024; // 512 KB

    public FileTransfer(String fileName, String filePath, InetAddress targetAddress, int targetPort) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.targetAddress = targetAddress; // Set target address
        this.targetPort = targetPort; // Set target port
        // Initialize fileSize using the File object
        this.fileSize = new File(filePath).length();
        this.bytesTransferred = 0;
    }

    // Method to start file transfer (chunk by chunk)
    public void start() {
        // Open the file and set up networking
        try (DatagramSocket socket = new DatagramSocket();
             BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath))) {

            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int sequenceNumber = 0; // For ordering the chunks

            while ((bytesRead = bis.read(buffer)) > 0) {
                // Prepend or append sequence number, checksum, etc., as needed to buffer
                // Send chunk
                DatagramPacket packet = new DatagramPacket(buffer, bytesRead, targetAddress, targetPort);
                socket.send(packet);
                // Increment sequence number
                sequenceNumber++;

                // TODO: Implement wait for ACK and retransmission if needed

                // Rate limiting or flow control
                Thread.sleep(10); // Adjust as needed
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
