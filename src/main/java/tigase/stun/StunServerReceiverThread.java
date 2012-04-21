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

import de.javawi.jstun.attribute.MessageAttributeInterface.MessageAttributeType;
import de.javawi.jstun.attribute.*;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.header.MessageHeaderInterface.MessageHeaderType;
import de.javawi.jstun.header.MessageHeaderParsingException;
import de.javawi.jstun.util.Address;
import de.javawi.jstun.util.UtilityException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StunServerReceiverThread extends Thread {

        private static final Logger log = Logger.getLogger(StunServerReceiverThread.class.getCanonicalName());
        private DatagramSocket receiverSocket;
        private DatagramSocket changedPort;
        private DatagramSocket changedIP;
        private DatagramSocket changedPortIP;
        private boolean shutdown;

        public StunServerReceiverThread(DatagramSocket receiver, Vector<DatagramSocket> sockets) {
                receiverSocket = receiver;
                for (DatagramSocket socket : sockets) {
                        if ((socket.getLocalPort() != receiverSocket.getLocalPort()) && (socket.getLocalAddress() == receiverSocket.getLocalAddress())) {
                                changedPort = socket;
                        }
                        if ((socket.getLocalPort() != receiverSocket.getLocalPort()) && (socket.getLocalAddress() != receiverSocket.getLocalAddress())) {
                                changedPortIP = socket;
                        }
                        if ((socket.getLocalPort() == receiverSocket.getLocalPort()) && (socket.getLocalAddress() != receiverSocket.getLocalAddress())) {
                                changedIP = socket;
                        }
                }
        }

        @Override
        public void run() {
                shutdown = false;
                while (!shutdown) {
                        try {
                                DatagramPacket packet = new DatagramPacket(new byte[200], 200);
                                receiverSocket.receive(packet);
                                MessageHeader header = MessageHeader.parseHeader(packet.getData());
                                try {
                                        header.parseAttributes(packet.getData());
                                        if (header.getType() == MessageHeaderType.BindingRequest) {
                                                ChangeRequest cr = (ChangeRequest) header.getMessageAttribute(MessageAttributeType.ChangeRequest);
                                                if (cr == null) {
                                                        throw new MessageAttributeException("Message attribute change request is not set.");
                                                }
                                                ResponseAddress ra = (ResponseAddress) header.getMessageAttribute(MessageAttributeType.ResponseAddress);

                                                MessageHeader sendMH = new MessageHeader(MessageHeaderType.BindingResponse);
                                                sendMH.setTransactionID(header.getTransactionID());

                                                // Mapped address attribute
                                                MappedAddress ma = new MappedAddress();
                                                ma.setAddress(new Address(packet.getAddress().getAddress()));
                                                ma.setPort(packet.getPort());
                                                sendMH.addMessageAttribute(ma);
                                                // Changed address attribute
                                                ChangedAddress ca = new ChangedAddress();
                                                ca.setAddress(new Address(changedPortIP.getLocalAddress().getAddress()));
                                                ca.setPort(changedPortIP.getLocalPort());
                                                sendMH.addMessageAttribute(ca);
                                                if (cr.isChangePort() && (!cr.isChangeIP())) {
                                                        // Source address attribute
                                                        SourceAddress sa = new SourceAddress();
                                                        sa.setAddress(new Address(changedPort.getLocalAddress().getAddress()));
                                                        sa.setPort(changedPort.getLocalPort());
                                                        sendMH.addMessageAttribute(sa);
                                                        byte[] data = sendMH.getBytes();
                                                        DatagramPacket send = new DatagramPacket(data, data.length);
                                                        if (ra != null) {
                                                                send.setPort(ra.getPort());
                                                                send.setAddress(ra.getAddress().getInetAddress());
                                                        } else {
                                                                send.setPort(packet.getPort());
                                                                send.setAddress(packet.getAddress());
                                                        }
                                                        changedPort.send(send);
                                                } else if ((!cr.isChangePort()) && cr.isChangeIP()) {
                                                        // Source address attribute
                                                        SourceAddress sa = new SourceAddress();
                                                        sa.setAddress(new Address(changedIP.getLocalAddress().getAddress()));
                                                        sa.setPort(changedIP.getLocalPort());
                                                        sendMH.addMessageAttribute(sa);
                                                        byte[] data = sendMH.getBytes();
                                                        DatagramPacket send = new DatagramPacket(data, data.length);
                                                        if (ra != null) {
                                                                send.setPort(ra.getPort());
                                                                send.setAddress(ra.getAddress().getInetAddress());
                                                        } else {
                                                                send.setPort(packet.getPort());
                                                                send.setAddress(packet.getAddress());
                                                        }
                                                        changedIP.send(send);
                                                } else if ((!cr.isChangePort()) && (!cr.isChangeIP())) {
                                                        // Source address attribute
                                                        SourceAddress sa = new SourceAddress();
                                                        sa.setAddress(new Address(receiverSocket.getLocalAddress().getAddress()));
                                                        sa.setPort(receiverSocket.getLocalPort());
                                                        sendMH.addMessageAttribute(sa);
                                                        byte[] data = sendMH.getBytes();
                                                        DatagramPacket send = new DatagramPacket(data, data.length);
                                                        if (ra != null) {
                                                                send.setPort(ra.getPort());
                                                                send.setAddress(ra.getAddress().getInetAddress());
                                                        } else {
                                                                send.setPort(packet.getPort());
                                                                send.setAddress(packet.getAddress());
                                                        }
                                                        receiverSocket.send(send);
                                                } else if (cr.isChangePort() && cr.isChangeIP()) {
                                                        // Source address attribute
                                                        SourceAddress sa = new SourceAddress();
                                                        sa.setAddress(new Address(changedPortIP.getLocalAddress().getAddress()));
                                                        sa.setPort(changedPortIP.getLocalPort());
                                                        sendMH.addMessageAttribute(sa);
                                                        byte[] data = sendMH.getBytes();
                                                        DatagramPacket send = new DatagramPacket(data, data.length);
                                                        if (ra != null) {
                                                                send.setPort(ra.getPort());
                                                                send.setAddress(ra.getAddress().getInetAddress());
                                                        } else {
                                                                send.setPort(packet.getPort());
                                                                send.setAddress(packet.getAddress());
                                                        }
                                                        changedPortIP.send(send);
                                                }
                                        }
                                } catch (UnknownMessageAttributeException umae) {
                                        umae.printStackTrace();
                                        // Generate Binding error response
                                        MessageHeader sendMH = new MessageHeader(MessageHeaderType.BindingErrorResponse);
                                        sendMH.setTransactionID(header.getTransactionID());

                                        // Unknown attributes
                                        UnknownAttribute ua = new UnknownAttribute();
                                        ua.addAttribute(umae.getType());
                                        sendMH.addMessageAttribute(ua);

                                        byte[] data = sendMH.getBytes();
                                        DatagramPacket send = new DatagramPacket(data, data.length);
                                        send.setPort(packet.getPort());
                                        send.setAddress(packet.getAddress());
                                        receiverSocket.send(send);
                                }
                        } catch (Exception ex) {
                                log.log(Level.FINE, "error processing received socket data", ex);
//			} catch (IOException ioe) {
//				ioe.printStackTrace();
//			} catch (MessageAttributeParsingException mape) {
//				mape.printStackTrace();
//			} catch (MessageAttributeException mae) {
//				mae.printStackTrace();
//			} catch (MessageHeaderParsingException mhpe) {
//				mhpe.printStackTrace();
//			} catch (UtilityException ue) {
//				ue.printStackTrace();
//			} catch (ArrayIndexOutOfBoundsException aioobe) {
//				aioobe.printStackTrace();
                        }
                }
        }

        public void shutdown() {
                shutdown = true;
                interrupt();
        }
}
