/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.stun;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 *
 * @author andrzej
 */
public class StunSocket extends DatagramSocket {
        
        private final InetAddress externalAddress;
        private final int externalPort;
        
        public StunSocket(int localPort, InetAddress localAddress, int externalPort, InetAddress externalAddress) throws SocketException {
                super(localPort, localAddress);
                this.externalPort = externalPort;
                this.externalAddress = externalAddress;
        }
        
        public InetAddress getExternalAddress() {
                return externalAddress;
        }
        
        public int getExternalPort() {
                return externalPort;
        }
        
}
