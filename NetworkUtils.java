package project471;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkUtils {

    public static String getLocalAddress() throws SocketException {
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface iface = ifaces.nextElement();
            Enumeration<InetAddress> addresses = iface.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                // IPv4 adreslerini kontrol et
                if (addr.getHostAddress().matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && !addr.isLoopbackAddress()) {
                    return addr.getHostAddress();
                }
            }
        }
        return null; // Uygun bir adres bulunamadı
    }

    public static void main(String[] args) {
        try {
            String localAddress = getLocalAddress();
            if(localAddress != null) {
                System.out.println("Local IP Address: " + localAddress);
            } else {
                System.out.println("Local IP Address not found.");
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
