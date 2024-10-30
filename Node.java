package project471;

import java.io.*;
import java.util.Base64;
import java.util.concurrent.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Node {
	public class ChunkInfo {
	    private final long chunkCount;
	    private final int lastChunkSize;

	    public ChunkInfo(long chunkCount, int lastChunkSize) {
	        this.chunkCount = chunkCount;
	        this.lastChunkSize = lastChunkSize;
	    }

	    public long getChunkCount() {
	        return chunkCount;
	    }

	    public int getLastChunkSize() {
	        return lastChunkSize;
	    }
	}
    private String sharedFolderPath;
    private static boolean send_for_getfile_boolean;
    private static boolean on_ill;
    public static String downloadPath;
    public static String sendingfilename;
    private String sharedSecret;
    private DatagramSocket sendSocket;
    private DatagramSocket iHaveSocket;
    private DatagramSocket youHaveSocket;
    private DatagramSocket fileNameReceiveSocket;
    private DatagramSocket fileNameSendSocket;
    private DatagramSocket receiveSocket;
    private DatagramSocket whoHasThisFile;
    private DatagramSocket doHaveFile;
    private DatagramSocket you_theseChunks;
    private DatagramSocket i_getChunks;
    private DatagramSocket get_thisFile;
    private DatagramSocket finalSocket;
    private Map<String, Set<String>> holdit = new ConcurrentHashMap<>();
    private Map<String, ChunkInfo> file_chunk_count = new HashMap<>();
    private List<String> sharedFiles;
    private List<String> downloaded;
    private List<String> receivedFileNames;
    private List<String> ipToFileList;   
    private Map<String,String> filePathMap;
    private byte[] buf = new byte[1024]; // Adjust buffer size as needed
    private final static int CHUNK_SIZE = 512 * 1024; // 512 KB for chunk size
    private NewPeerListener listener;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private List<String> peers = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean isRunning = true;
    private FileNamesListener fileNamesListener;
    private Map<String, byte[][]> files_to_concatenate = new HashMap<>();
    private Thread listenThread;
    private final long LISTEN_DURATION = 30000;
    
    public Node(String sharedFolderPath, String sharedSecret, NewPeerListener listener) throws SocketException {
        this.sharedFolderPath = sharedFolderPath;
        this.send_for_getfile_boolean = false;
        this.on_ill = false;
        this.sharedSecret = sharedSecret;
        this.sendSocket = new DatagramSocket(4455); // Socket for sending messages
        this.receiveSocket = new DatagramSocket(1234); // Socket for receiving messages
        this.fileNameSendSocket = new DatagramSocket(4400);
        this.fileNameReceiveSocket = new DatagramSocket(4321);
        this.whoHasThisFile = new DatagramSocket(5551);
        this.doHaveFile = new DatagramSocket(1555);
        this.iHaveSocket = new DatagramSocket(8001);
        this.youHaveSocket = new DatagramSocket(1998);
        this.you_theseChunks = new DatagramSocket(1902);
        this.i_getChunks = new DatagramSocket(2091);
        this.get_thisFile = new DatagramSocket(9876);
        this.finalSocket = new DatagramSocket(6789);
        this.sharedFiles = new ArrayList<>();
        this.ipToFileList = new ArrayList<>();
        this.receivedFileNames = new ArrayList<>();
        this.downloaded = new ArrayList<>();
        this.listener = listener; // Set the listener
        this.filePathMap = new HashMap<>();
        //i_getChunks.setSoTimeout(50000);
        
        //new Thread(this::initialPacketCheck).start();
       
       
    }
    public void start() {
    	System.out.println("in start");
        new Thread(this::listenForPeers).start();
        new Thread(this::broadcastSharedFiles).start();
        new Thread(this::listenForFileNames).start();
        //new Thread(this::broadcastSharedFiles).start();
        new Thread(this::broadcastPresence).start();
        new Thread(this::listenHaveFile).start();
        new Thread(this::listen_ill_sendChunks).start();
        //new Thread(this::broadcastSharedFiles).start(); 

        // Start the periodic broadcasting task
        startNetworkTasks();
    }
    
    public void start2() {
        	System.out.println("in start2");
            new Thread(this::broadcastWhoHas).start();
            //new Thread(this::listenHaveFile).start();
            //new Thread(this::listenFileReturns).start();
            new Thread(this::send_for_getfile).start();
            new Thread(this::listenFileReturns).start();
            new Thread(this::send_for_getfile).start();
            new Thread(this::take_chunks_Concatenate).start();
            
            //new Thread(this::broadcastSharedFiles).start(); 

            // Start the periodic broadcasting task
            startNetworkTasks();
        }    
   
    public void setFileNamesListener(FileNamesListener listener) {
    this.fileNamesListener = listener;
    }
    
    private void initialPacketCheck() {
    	long startTime = System.currentTimeMillis();
    	while(System.currentTimeMillis()-startTime < LISTEN_DURATION) {
    		try {
                byte[] buf = new byte[1024]; // Adjust buffer size as needed
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                i_getChunks.receive(packet); // This will block until a packet is received

                if (packet.getAddress() != null) {
                    // Start the listenThread once a packet with non-null address is received
                    listenThread = new Thread(this::listen_ill_sendChunks);
                    listenThread.start();
                }
            } catch (SocketTimeoutException e) {
                // Handle timeout if no packet is received within the timeout period
                // You might want to retry or take other actions
            } catch (IOException e) {
                e.printStackTrace();
                // Handle other I/O errors
            }
    	}
        
    }


    
    private void refreshPeers() {
    peers.clear();
    broadcastPresence();
    }
    
    public void hold_path(String fullpath, String filename) {
    	filePathMap.put(filename,fullpath);
    }
   
    private void startNetworkTasks() {
    System.out.println("in startNetwrokTasks");
    scheduler.scheduleAtFixedRate(this::refreshPeers, 0, 7, TimeUnit.SECONDS);
    }

    public void requestFile(String fileName) {
        try {
            String requestMessage = "FILE_REQUEST " + sharedSecret + " " + fileName;
            byte[] message = requestMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName("10.0.2.255"), 1234);
            sendSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (sendSocket != null && !sendSocket.isClosed()) {
            sendSocket.close();
        }
        if (receiveSocket != null && !receiveSocket.isClosed()) {
            receiveSocket.close();
        }
        // Further cleanup logic can go here
    }
   
    private String getMyIPAddress() {
    try {
            return NetworkUtils.getLocalAddress(); // Get the real local IP address
         
        }catch (SocketException e) {
        e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
   
    public void addPeer(String peerAddress) {
        if (!peers.contains(peerAddress) && !peerAddress.equals(getMyIPAddress()) && !peerAddress.equals("10.0.2.1") && !peerAddress.equals("10.0.2.45")) {
            peers.add(peerAddress);
            listener.onNewPeerDetected(peerAddress);
        }
    }
   // /home/banzai/Masaüstü/deneme
    private void broadcastSharedFiles() {
        try {
        	String myIP = NetworkUtils.getLocalAddress();
            StringBuilder fileListBuilder = new StringBuilder("SHARED_FILES " + sharedSecret + " "+myIP+" ");
            for (String fileName : sharedFiles) {
                fileListBuilder.append(fileName).append(","); // Using comma as delimiter
            }
            String fileListMessage = fileListBuilder.toString();
            byte[] message = fileListMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName("10.0.2.255"), 4321);
            fileNameSendSocket.send(packet); // Broadcast the shared file list
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForFileNames() {
        try {
        	//broadcastSharedFiles();
            System.out.println("in listenForFileNames");
            while (true) {
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                fileNameReceiveSocket.receive(packet); // Receive packet
                String received = new String(packet.getData(), 0, packet.getLength());
               
                if (received.startsWith("SHARED_FILES")) {
                    String[] parts = received.split(" ");
                    //System.out.println("Received in listenForFileNames: " + parts[2] + " Message length: "+parts.length);
                    if (parts.length >= 4 && parts[1].equals(sharedSecret) && !NetworkUtils.getLocalAddress().equals(parts[2])) {
                    	//System.out.println("Received in listenForFileNames: " + parts[2] +" ITS PART3 "+parts[3]);
                    	String[] fileNames = parts[3].split(",");                       
                        for (String fileName : fileNames) {
                            if (NetworkUtils.getLocalAddress() != parts[2] && !sharedFiles.contains(fileName)) {                      
                                receivedFileNames.add(fileName);
                                int a = fileNames.length;
                                if (fileNamesListener != null && receivedFileNames.size() == a) {
                                    fileNamesListener.onFileNamesReceived(new ArrayList<>(receivedFileNames));
                                }
                            }
                        }
                        /*if (fileNamesListener != null) {
                            fileNamesListener.onFileNamesReceived(new ArrayList<>(receivedFileNames));
                        }*/
                        receivedFileNames.clear();
                        broadcastSharedFiles();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    private void broadcastPresence() {
    while(true) {
    try {
            String myIP = NetworkUtils.getLocalAddress(); // Get the real local IP address
            if(myIP == null) {
                System.err.println("Local IP Address not found.");
                return;
            }

            String broadcastMessage = "NODE_PRESENT " + sharedSecret + " " + myIP;
            byte[] message = broadcastMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName("10.0.2.255"), 1234);
            sendSocket.send(packet); // Send using the sendSocket
        } catch (UnknownHostException e) {
            System.err.println("Host could not be determined: " + e.getMessage());
        } catch (SocketException e) {
            System.err.println("Socket error occurred: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IO error occurred: " + e.getMessage());
        }
    }
       
    }


    public List<String> getPeers() {
        return new ArrayList<>(peers); // Returns a defensive copy of the peers list
    }

    private void listenForPeers() {
        try {
            while (isRunning) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                receiveSocket.receive(packet);  // Using the receiveSocket to listen for incoming packets
                String received = new String(packet.getData(), 0, packet.getLength());
                String[] parts = received.split(" ");
                //System.out.println(parts[2]);

                if (parts[0].equals("NODE_PRESENT") && parts[1].equals(sharedSecret)) {
                    String peerAddress = parts[2]; // The IP address of the peer is the third part of the message
                    addPeer(peerAddress); // Add to peer list

                    // Respond back to the peer with this node's IP
                    String myIP = InetAddress.getLocalHost().getHostAddress();
                    String responseMessage = "NODE_RESPONSE " + sharedSecret + " " + myIP;
                    byte[] responseBytes = responseMessage.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                            responseBytes, responseBytes.length, InetAddress.getByName(peerAddress), packet.getPort());
                    sendSocket.send(responsePacket); // Send using the sendSocket
                }
                // Handle other messages such as SHARED_FILES or FILE_REQUEST...
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    
    
    public void get_the_filename(String filename) {
    	sendingfilename = filename;
    }

    public void broadcastWhoHas() {//Bu dosyaya kimlere ait diye bir broadcast yapar
    	try {
    		System.out.println("in broadcastWhoHas");
    		String fileName = sendingfilename;
    		System.out.println("Aranan dosya: "+sendingfilename);
    		String myIP = NetworkUtils.getLocalAddress();   		
    		String messageToSend = "WHO_HAS "+sharedSecret+" "+fileName+" "+myIP;
    		byte[] message = messageToSend.getBytes();
    		DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName("10.0.2.255"), 1555);
    		whoHasThisFile.send(packet);
    	}catch(IOException e) {
    		e.printStackTrace();
    	}
    }
    
    public void listenHaveFile() {// Dosyaya kim sahip  diye sorulduğunda bunu algılar ve ben sahibim diye mesaj döner
        try {
        	System.out.println("in listenHaveFile");
            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            String myIP = NetworkUtils.getLocalAddress(); // Get the local IP address

            while (true) {
            	System.out.println("in listenHaveFile in while");
            	new Thread(this::send_for_getfile).start();
                doHaveFile.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                String[] parts = received.split(" ");
                //System.out.println("Gelen pakeetin uzunluğu ve geldiği ip "+parts.length+" "+parts[3]);
                if (parts.length >= 4 && parts[0].equals("WHO_HAS") && parts[1].equals(sharedSecret) && !parts[3].equals(NetworkUtils.getLocalAddress())) {
                	System.out.println("in listenHaveFile in 1.if");
                	String fileName = parts[2];
                	String filePath = filePathMap.get(fileName);
                	System.out.println("Aranan path: "+filePath);
                    String requesterIP = parts[3];

                    // Check if the requester IP is not one of the listening peers and the file is in filePathMap
                    if (!myIP.equals(requesterIP) && filePathMap.containsKey(fileName)) {
                    	System.out.println("in listenHaveFile 2.if");
                    	File file = new File(filePath);
                    	System.out.println("Bunun long sizeı"+file.length());
                        if (file.exists()) {
                        	System.out.println("in listenHaveFile 3.if");
                        	long fileSize = file.length();
                            String response = "I_HAVE " + myIP + " " + fileSize + " " + fileName;
                            System.out.println("INSIDE listenHaveFile gönderilecek olan filesize büyüklüğü: "+fileSize);
                            byte[] responseBytes = response.getBytes();
                            DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, InetAddress.getByName(requesterIP), 1998);
                            iHaveSocket.send(responsePacket);
                            on_ill = true;
                           
                        }
                    }
                }
                //new Thread(this::listen_ill_sendChunks).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //listen_ill_sendChunks();
    }
    
    private void listenFileReturns() {//Dosyaya sahip olan peerların adını ve iplerini holdit içinde tutar
        try {
        	System.out.println("in listenFileReturns");
            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            String myIP = NetworkUtils.getLocalAddress(); // Get the local IP address

            while (true) {
            	System.out.println("in listenFileReturns in while");           	
                youHaveSocket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                String[] parts = received.split(" ");

                if (parts.length >= 4 && parts[0].equals("I_HAVE") && !parts[1].equals(myIP)) {
                	System.out.println("in listenFileReturns in 1.if");
                	String fileName = parts[3];
                    long fileSize = Long.parseLong(parts[2]);
                    String senderIP = parts[1];

                    holdit.computeIfAbsent(fileName, k -> new HashSet<>()).add(senderIP);

                    long chunkSize = 512 * 1024; // 512KB in bytes
                    long numChunks = fileSize / chunkSize;
                    int lastChunkSize = (int)(fileSize % chunkSize);
                    file_chunk_count.put(fileName, new ChunkInfo(numChunks + (lastChunkSize > 0 ? 1 : 0), lastChunkSize));
                    send_for_getfile_boolean = true;
                    new Thread(this::send_for_getfile).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    public void populateSharedFilesList(String filem) {
    	System.out.println("In populateSharedFiles:");
    	sharedFiles.add(filem);
    }
    public void print_populate() {
    	for(String s:sharedFiles) {
    		System.out.println("Now:"+s);
    	}
    }
    
    public List<String> getSharedFiles() {
        return new ArrayList<>(sharedFiles); // Returns a defensive copy of the sharedFiles list
    }
   
    public interface FileNamesListener{
    void onFileNamesReceived(List<String> fileNames);
    }

    public void send_for_getfile() {//ALmış olduğu filesize bilgisiyle dosyayı bulunduran ip sayısına göre ne kadar chunk olacağını belirler.
        try {//ve de ber bir chunk için round robin döndürür.
        	if(send_for_getfile_boolean == true) {
        		System.out.println("in send_for_getfile");
                String myIP = NetworkUtils.getLocalAddress(); // Get local IP address

                for (String fileName : holdit.keySet()) {
                	System.out.println("in send_for_getfile in for");
                    if (!filePathMap.containsKey(fileName)) {
                    	System.out.println("in send_for_getfile in 1.if");
                        ChunkInfo chunkInfo = file_chunk_count.get(fileName);
                        Set<String> ips = holdit.get(fileName);

                        if (chunkInfo != null && ips != null) {
                        	System.out.println("in send_for_getfile in 2.if");
                            long totalChunks = chunkInfo.getChunkCount();
                            List<String> ipList = new ArrayList<>(ips); // Convert set to list for indexed access

                            int j = 0; // Index for IPs
                            for (int i = 0; i < totalChunks; i++) {
                            	System.out.println("in send_for_getfile in 2.for");
                                int whichChunk = i;
                                String messageToSend = "CHUNKS " + sharedSecret + " " + myIP + " " + String.valueOf(whichChunk) + " "+fileName;
                                byte[] message = messageToSend.getBytes();
                                DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(ipList.get(j)), 2091);
                                you_theseChunks.send(packet);

                                j++; // Move to next IP
                                if (j == ipList.size()-1) { // If at the end of the list, reset to start
                                    j = 0;
                                }
                            }
                        }
                    }
                }
        	}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void listen_ill_sendChunks() {// Send the chunks that wanted with filename and chunk index
        try {
        	//Thread.sleep(4000);
        	System.out.println("in listen_ill_sendChunks()");
            String myIP = NetworkUtils.getLocalAddress(); // Get local IP address

            while (true) {
            	System.out.println("in listen_ill_sendChunks it get in while");
                byte[] buf = new byte[1024];
                System.out.println("in listen_ill_sendChunks it created buf");
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                System.out.println("Gelen paketin adresi bu: "+packet.getAddress());
                i_getChunks.receive(packet);
                System.out.println("in listen_ill_sendChunks socket get packet");
                String received = new String(packet.getData(), 0, packet.getLength());
                String[] parts = received.split(" ");
                System.out.println("Message "+ Arrays.toString(parts));
                if(parts.length < 5) {
                	System.out.println("Not working"+parts.length);
                }//parts.length >= 5 && parts[0].equals("CHUNK") && parts[1].equals(sharedSecret) && !parts[2].equals(myIP))
                else {
                	System.out.println("in listen_ill_sendChunks it get in 1.if");
                	int whichChunk = Integer.parseInt(parts[3]);
                    String fileName = parts[4];
                    String filePath = filePathMap.get(fileName);

                    if (filePath != null) {
                    	System.out.println("in listen_ill_sendChunks it get in 2.if");
                    	// Assuming the function 'getChunkData' retrieves the specific chunk data
                        byte[] chunkData = getChunkData(filePath, whichChunk);

                        String checksum = generateChecksum(chunkData);
                        String base64ChunkData=Base64.getEncoder().encodeToString(chunkData);
                        String messageToSend = "THIS " + sharedSecret + " " + myIP + " " + base64ChunkData + " " + fileName +" "+ parts[3]+" "+checksum;
                        System.out.println("listen_ill_sendChunks'tan gönderilecek olan mesajın boyutu: "+messageToSend.length());
                        byte[] message = messageToSend.getBytes();
                        String yourip = parts[2];
                        DatagramPacket sendPacket = new DatagramPacket(message, message.length, InetAddress.getByName(yourip), 6789);
                        System.out.println("İNSİDE liste_ill myıp: "+myIP);
                        get_thisFile.send(sendPacket);
                    }
                }
             
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] getChunkData(String filePath, int chunkNumber) {
        File file = new File(filePath);
        byte[] chunkData = new byte[CHUNK_SIZE];

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long offset = (long) chunkNumber * CHUNK_SIZE;
            raf.seek(offset);

            int bytesRead;
            if (offset + CHUNK_SIZE > file.length()) {
                // If this is the last chunk and it's smaller than the standard chunk size
                int remainingSize = (int)(file.length() - offset);
                chunkData = new byte[remainingSize];
            }
            bytesRead = raf.read(chunkData);

            if (bytesRead < chunkData.length) {
                // Resize the array in case the read data is smaller than the allocated array
                chunkData = java.util.Arrays.copyOf(chunkData, bytesRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0]; // Return an empty array in case of an error
        }

        return chunkData;
    }
    public void take_chunks_Concatenate() {
        try {
        	System.out.println("in take_chunksConcatenate");
            while (true) {
            	System.out.println("in take_chunksConcatenate it get in while loop");
                byte[] buf = new byte[4096]; // Adjust buffer size as needed
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                finalSocket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                String[] parts = received.split(" ");

                if (parts.length >= 7 && parts[0].equals("THIS") && parts[1].equals(sharedSecret) && !downloaded.contains(parts[4])) {
                	System.out.println("in take_chunksConcatenate it get in 1.if");
                	String myIP = NetworkUtils.getLocalAddress(); // Get local IP address
                    if (!parts[2].equals(myIP)) { // Ensure it's not from the local IP
                    	System.out.println("in take_chunksConcatenate it get in 2.if");
                    	String fileName = parts[4];
                        int chunkIndex = Integer.parseInt(parts[5]);
                        String receivedChecksum = parts[6];

                        byte[] chunkData = Base64.getDecoder().decode(parts[3]);
                        if (isValidChecksum(chunkData, receivedChecksum)) {
                        	System.out.println("in take_chunksConcatenate it get in 3.if");
                        	byte[][] fileChunks = files_to_concatenate.computeIfAbsent(fileName, k -> new byte[holdit.get(fileName).size()][]);
                            fileChunks[chunkIndex] = chunkData; // Set the chunk data

                            // Check if all chunks have been received
                            if (areAllChunksReceived(fileChunks)) {
                            	System.out.println("in take_chunksConcatenate it get in 4.if");
                            	saveFile(fileName, fileChunks);
                                downloaded.add(fileName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean areAllChunksReceived(byte[][] fileChunks) {
        for (byte[] chunk : fileChunks) {
            if (chunk == null) {
                return false;
            }
        }
        return true;
    }

    private void saveFile(String fileName, byte[][] fileChunks) {
        try (FileOutputStream fos = new FileOutputStream(Node.downloadPath + File.separator + fileName)) {
            for (byte[] chunk : fileChunks) {
                fos.write(chunk);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(data);
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isValidChecksum(byte[] data, String expectedChecksum) {
        String actualChecksum = generateChecksum(data);
        return actualChecksum != null && actualChecksum.equals(expectedChecksum);
    }

}