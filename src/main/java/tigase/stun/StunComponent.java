/**
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


package tigase.stun;

//~--- non-JDK imports --------------------------------------------------------


//~--- JDK imports ------------------------------------------------------------

import java.io.File;

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
import tigase.conf.ConfigurationException;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;
import tigase.stats.StatisticsList;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;

/**
 * Class description
 *
 *
 * @version        Enter version here..., 13/02/16
 * @author         Enter your name here...
 */
public class StunComponent
				extends AbstractMessageReceiver
				implements Configurable, StatisticsCollector {
	private static final Logger log =
		Logger.getLogger(StunComponent.class.getCanonicalName());
	private static final String PRIMARY_EXTERNAL_IP_KEY     = "stun-primary-external-ip";
	private static final String PRIMARY_EXTERNAL_PORT_KEY   = "stun-primary-external-port";
	private static final String PRIMARY_IP_KEY              = "stun-primary-ip";
	private static final String PRIMARY_PORT_KEY            = "stun-primary-port";
	private static final String SECONDARY_EXTERNAL_IP_KEY   = "stun-secondary-external-ip";
	private static final String SECONDARY_EXTERNAL_PORT_KEY =
		"stun-secondary-external-port";
	private static final String SECONDARY_IP_KEY       = "stun-secondary-ip";
	private static final String SECONDARY_PORT_KEY     = "stun-secondary-port";
	private static final String STUN_DISCO_DESCRIPTION = "STUN Component";

	//~--- fields ---------------------------------------------------------------

	private long last_hour_packets                 = 0;
	private long last_minute_packets               = 0;
	private long last_second_packets               = 0;
	private long packets_per_hour                  = 0;
	private long packets_per_minute                = 0;
	private long packets_per_second                = 0;
	private long packets_received                  = 0;
	private Vector<StunSocket> sockets             = null;
	private List<StunServerReceiverThread> threads = null;

	//~--- constructors ---------------------------------------------------------

//private Licence lic;

	/**
	 * Constructs ...
	 *
	 */
	public StunComponent() {

	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param props
	 * @throws ConfigurationException
	 */
	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
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

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
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

	/**
	 * Method description
	 *
	 *
	 * @param props
	 *
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public void init(Map<String, Object> props)
					throws UnknownHostException, SocketException {
		if (threads == null) {
			threads = new LinkedList<StunServerReceiverThread>();
		}
		if (sockets == null) {
			sockets = new Vector<StunSocket>();
		}

		InetAddress primaryAddress =
			InetAddress.getByName((String) props.get(PRIMARY_IP_KEY));
		int primaryPort              = (Integer) props.get(PRIMARY_PORT_KEY);
		InetAddress secondaryAddress =
			InetAddress.getByName((String) props.get(SECONDARY_IP_KEY));
		int secondaryPort                    = (Integer) props.get(SECONDARY_PORT_KEY);
		InetAddress primaryExternalAddress   = primaryAddress;
		int primaryExternalPort              = primaryPort;
		InetAddress secondaryExternalAddress = secondaryAddress;
		int secondaryExternalPort            = secondaryPort;

		if (props.containsKey(PRIMARY_EXTERNAL_IP_KEY)) {
			primaryExternalAddress =
				InetAddress.getByName((String) props.get(PRIMARY_EXTERNAL_IP_KEY));
		}
		if (props.containsKey(PRIMARY_EXTERNAL_PORT_KEY)) {
			primaryExternalPort = (Integer) props.get(PRIMARY_EXTERNAL_PORT_KEY);
		}
		if (props.containsKey(SECONDARY_EXTERNAL_IP_KEY)) {
			secondaryExternalAddress =
				InetAddress.getByName((String) props.get(SECONDARY_EXTERNAL_IP_KEY));
		}
		if (props.containsKey(SECONDARY_EXTERNAL_PORT_KEY)) {
			secondaryExternalPort = (Integer) props.get(SECONDARY_EXTERNAL_PORT_KEY);
		}
		sockets.add(new StunSocket(primaryPort, primaryAddress, primaryExternalPort,
															 primaryExternalAddress));
		sockets.add(new StunSocket(secondaryPort, primaryAddress, secondaryExternalPort,
															 primaryExternalAddress));
		sockets.add(new StunSocket(primaryPort, secondaryAddress, primaryExternalPort,
															 secondaryExternalAddress));
		sockets.add(new StunSocket(secondaryPort, secondaryAddress, secondaryExternalPort,
															 secondaryExternalAddress));
		for (StunSocket socket : sockets) {
			socket.setReceiveBufferSize(2000);

			StunServerReceiverThread ssrt = new StunServerReceiverThread(socket, sockets, this);

			threads.add(ssrt);
			ssrt.start();
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getDiscoDescription() {
		return STUN_DISCO_DESCRIPTION;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	@Override
	public void processPacket(Packet packet) {
		try {
			addOutPacket(Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet, null,
							false));
		} catch (PacketErrorTypeException ex) {
			log.log(Level.WARNING, "bad packet type to return error = {0}", packet);
		}
	}

	/**
	 * Utility method executed precisely every hour. A component can overwrite the
	 * method to put own code to be executed at the regular intervals of time.
	 * <p/>
	 * Note, no extensive calculations should happen in this method nor long
	 * lasting operations. It is essential that the method processing does not
	 * exceed 1 hour. The overriding method must call the the super method first
	 * and only then run own code.
	 */
	public synchronized void everyHour() {

		packets_per_hour  = packets_received - last_hour_packets;
		last_hour_packets = packets_received;
		super.everyHour();
	}

	/**
	 * Utility method executed precisely every minute. A component can overwrite
	 * the method to put own code to be executed at the regular intervals of time.
	 * <p/>
	 * Note, no extensive calculations should happen in this method nor long
	 * lasting operations. It is essential that the method processing does not
	 * exceed 1 minute. The overriding method must call the the super method first
	 * and only then run own code.
	 */
	public synchronized void everyMinute() {
		packets_per_minute  = packets_received - last_minute_packets;
		last_minute_packets = packets_received;
		super.everyMinute();
	}

	/**
	 * Utility method executed precisely every second. A component can overwrite
	 * the method to put own code to be executed at the regular intervals of time.
	 * <p/>
	 * Note, no extensive calculations should happen in this method nor long
	 * lasting operations. It is essential that the method processing does not
	 * exceed 1 second. The overriding method must call the the super method first
	 * and only then run own code.
	 */
	public synchronized void everySecond() {
		packets_per_second  = packets_received - last_second_packets;
		last_second_packets = packets_received;
		super.everySecond();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		list.add(getName(), "Total STUN packets", packets_received, Level.FINE);
		list.add(getName(), "Last second STUN packets", packets_per_second, Level.FINE);
		list.add(getName(), "Last minute STUNB packets", packets_per_minute, Level.FINE);
		list.add(getName(), "Last hour STUN packets", packets_per_hour, Level.FINE);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	@Override
	public void packetReceived() {
		packets_received++;
	}
}


//~ Formatted in Tigase Code Convention on 13/02/16
