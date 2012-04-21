/*
 * XTigase XMPP Server
 * Copyright (C) 2009 "Andrzej Wójcik" <andrzej@hi-low.eu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * Last modified by $Author: andrzej $
 * $Date: 2009-05-09 19:11:43 +0200 (So 09.05.2009) $
 */
package tigase.stun;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.conf.Configurable;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;

public class StunComponent extends AbstractMessageReceiver implements Configurable {

        private static final Logger log = Logger.getLogger(StunComponent.class.getCanonicalName());
        private static final String STUN_DISCO_DESCRIPTION = "STUN Component";
        private static final String PRIMARY_IP_KEY = "stun-primary-ip";
        private static final String PRIMARY_PORT_KEY = "stun-primary-port";
        private static final String SECONDARY_IP_KEY = "stun-secondary-ip";
        private static final String SECONDARY_PORT_KEY = "stun-secondary-port";
        private Vector<DatagramSocket> sockets = null;
        private List<StunServerReceiverThread> threads = null;

        @Override
        public void setProperties(Map<String, Object> props) {
                super.setProperties(props);

                if (props.size() == 1) {
                        return;
                }

                deinit();

                try {
                        init(props);
                } catch (UnknownHostException e) {
                        log.warning(e.getMessage());
                } catch (SocketException e) {
                        log.warning(e.getMessage());
                }

        }

        public void deinit() {
                if (threads != null) {
                        for (StunServerReceiverThread thread : threads) {
                                thread.shutdown();
                        }
                        threads.clear();
                }
                if (sockets != null) {
                        for (DatagramSocket socket : sockets) {
                                socket.close();
                        }
                        sockets.clear();
                }
        }

        public void init(Map<String, Object> props) throws UnknownHostException, SocketException {
                if (threads == null) {
                        threads = new LinkedList<StunServerReceiverThread>();
                }
                if (sockets == null) {
                        sockets = new Vector<DatagramSocket>();
                }

                InetAddress primaryAddress = InetAddress.getByName((String) props.get(PRIMARY_IP_KEY));
                int primaryPort = (Integer) props.get(PRIMARY_PORT_KEY);
                InetAddress secondaryAddress = InetAddress.getByName((String) props.get(SECONDARY_IP_KEY));
                int secondaryPort = (Integer) props.get(SECONDARY_PORT_KEY);

                sockets.add(new DatagramSocket(primaryPort, primaryAddress));
                sockets.add(new DatagramSocket(secondaryPort, primaryAddress));
                sockets.add(new DatagramSocket(primaryPort, secondaryAddress));
                sockets.add(new DatagramSocket(secondaryPort, secondaryAddress));

                for (DatagramSocket socket : sockets) {
                        socket.setReceiveBufferSize(2000);
                        StunServerReceiverThread ssrt = new StunServerReceiverThread(socket, sockets);
                        threads.add(ssrt);
                        ssrt.start();
                }
        }

        @Override
        public String getDiscoDescription() {
                return STUN_DISCO_DESCRIPTION;
        }
        
        @Override
        public void processPacket(Packet packet) {
                try {
                        addOutPacket(Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet, null, false));
                } catch (PacketErrorTypeException ex) {
                        log.log(Level.WARNING, "bad packet type to return error = {0}", packet);
                }
        }
}
