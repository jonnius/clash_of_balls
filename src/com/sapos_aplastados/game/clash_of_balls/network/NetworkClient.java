/*
 * Copyright (C) 2012-2013 Hans Hardmeier <hanshardmeier@gmail.com>
 * Copyright (C) 2012-2013 Andrin Jenal
 * Copyright (C) 2012-2013 Beat Küng <beat-kueng@gmx.net>
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */
package com.sapos_aplastados.game.clash_of_balls.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.alljoyn.bus.BusException;

import android.util.Log;

import com.sapos_aplastados.game.clash_of_balls.game.Vector;
import com.sapos_aplastados.game.clash_of_balls.game.event.Event;
import com.sapos_aplastados.game.clash_of_balls.game.event.EventPool;
import com.sapos_aplastados.game.clash_of_balls.network.Networking.AllJoynError;
import com.sapos_aplastados.game.clash_of_balls.network.Networking.AllJoynErrorData;
import com.sapos_aplastados.game.clash_of_balls.network.Networking.ConnectedClient;
import com.sapos_aplastados.game.clash_of_balls.network.Networking.NetworkData;

/**
 * this class handles all client-side networking stuff
 * - list of available servers
 * - start/stop discovery of servers
 * - connect to a server (join game)
 * - receive events from server
 * - send sensor updates
 * - networking errors
 *
 */
public class NetworkClient {
	private static final String TAG = "NetworkClient";
	
	private final Networking m_networking;
	private boolean m_bHas_sensor_update=false;
	private Vector m_sensor_update=new Vector();
	
	private List<String> m_available_servers=new ArrayList<String>();
	
	private Queue<Event> m_available_events = new LinkedList<Event>();
	
	public EventPool m_event_pool = new EventPool(20);
	
	public NetworkClient(Networking networking) {
		m_networking = networking;
	}
	
	public ConnectedClient getConnectedClient(int idx) 
		{ return m_networking.connectedClient(idx); }
	public int getConnectedClientCount() { return m_networking.connectedClientCount(); }
	
	public String getOwnUniqueName() {
		return m_networking.getUniqueName();
	}
	
	//this will return the server_id
	//to get the (displayable) name, call: Networking.getNameFromServerId()
	public String serverId(int idx) {
		return m_available_servers.get(idx);
	}
	public int serverIdCount() { return m_available_servers.size(); }
	
	//look for open games
	public void startDiscovery() {
		m_networking.startDiscovery();
	}
	public void stopDiscovery() {
		m_networking.stopDiscovery();
		//clear the server list
		m_available_servers=new ArrayList<String>();
	}
	
	public void setOwnName(String name) {
		m_networking.setServerName(name);
	}
	//join a game
	public void connectToServer(String server_id) {
		m_networking.joinSession(server_id);
	}
	
	
	public boolean hasEvents() { return !m_available_events.isEmpty(); }
	public Event getNextEvent() { return m_available_events.poll(); }
		//does not remove the element from the queue
	public Event peekNextEvent() { return m_available_events.peek(); }
	
	
	//call this every frame, or in a regular time period
	public void handleReceive() {
		try {
			//available servers
			String server_id;
			//joined servers
			while((server_id=m_networking.receiveServerFound()) != null) {
				//check if already added
				boolean exists=false;
				for(String server : m_available_servers) {
					if(server.equals(server_id)) exists=true;
				}
				if(!exists) {
					m_available_servers.add(server_id);
					Log.i(TAG, "New Server found: "+server_id);
				}
			}
			//lost servers
			while((server_id=m_networking.receiveServerLost()) != null) {
				//find where
				for(int i=0; i<m_available_servers.size(); ++i) {
					if(m_available_servers.get(i).equals(server_id)) {
						m_available_servers.remove(i);
						Log.i(TAG, "Server lost: "+server_id);
					}
				}
			}
			
			//server updates
			NetworkData d;
			while((d=m_networking.receiveGameCommand()) != null) {
				
				ByteArrayInputStream bais = new ByteArrayInputStream(d.data);
				DataInputStream di = new DataInputStream(bais);
				Event e;
				while((e=Event.read(di, m_event_pool)) != null) {
					m_available_events.add(e);
				}
			}
			
			if(m_bHas_sensor_update) {
				m_bHas_sensor_update=false;
				m_networking.sendSensorUpdate(m_sensor_update);
			}
		} catch(BusException e) {
			Log.e(TAG, "BusException");
			e.printStackTrace();
			m_network_error = new AllJoynErrorData();
			m_network_error.error_string = "";
			m_network_error.error = AllJoynError.BUS_EXCEPTION;
		}
	}
	
	private AllJoynErrorData m_network_error=null;
	
	//will return null if no error
	public AllJoynErrorData getNetworkError() {
		if(m_network_error != null) {
			AllJoynErrorData d = m_network_error;
			m_network_error = null;
			return d;
		}
		return m_networking.getError();
	}
	
	public void sensorUpdate(Vector new_data) {
		m_sensor_update.set(new_data);
		m_bHas_sensor_update=true;
	}
	
}
