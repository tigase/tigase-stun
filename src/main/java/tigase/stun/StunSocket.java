/*
 * Tigase STUN Component - STUN server component for Tigase XMPP Server
 * Copyright (C) 2012 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.stun;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * @author andrzej
 */
public class StunSocket
		extends DatagramSocket {

	private final InetAddress externalAddress;
	private final int externalPort;

	public StunSocket(int localPort, InetAddress localAddress, int externalPort, InetAddress externalAddress)
			throws SocketException {
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
