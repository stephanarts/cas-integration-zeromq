/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.stephanarts.cas.ticket.registry.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

/**
 * PaceMaker Class.
 *
 * The pacemaker class, a singleton.
 */
public final class PaceMaker {

    /**
     * Logging Class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private int nWorkers = 1;

    private LinkedList<JSONRPCClient> clients = new LinkedList<JSONRPCClient>();

    private WatchDog[] workers = {};


    /**
     * Pacemaker constructor.
     */
    public PaceMaker() {
        this.workers = new WatchDog[1];
        this.workers[0] = new WatchDog();

        this.workers[0].start();
    }

    /**
     * Set heartbeatInterval.
     *
     * @param interval
     */
    public void setHeartbeatInterval(final int interval) {
        for(int i = 0; i < this.workers.length; ++i) {
            this.workers[i].setHeartbeatInterval(interval);
        }
    }

    /**
     * Set heartbeatTimeout.
     *
     * @param interval
     */
    public void setHeartbeatTimeout(final int timeout) {
        for(int i = 0; i < this.workers.length; ++i) {
            this.workers[i].setHeartbeatTimeout(timeout);
        }
    }

    /**
     * setWorkers(nr).
     *
     * Set the number of pacemaker worker threads
     * responsible for sending heartbeat messages.
     *
     * @param workers Number of Watchdog worker threads.
     */
    public void setWorkers(final int workers) {
        nWorkers = workers;
    }

    /**
     * addClient.
     *
     * Add a JSONRPCClient to retrieve.
     * 
     * @param client JSONRPCClient.
     */
    public void addClient(final JSONRPCClient client) {

        int b = 0;

        if(!this.clients.contains(client)) {
            this.clients.add(client);
        }

        for(int i = 0; i < this.workers.length; ++i) {

            JSONRPCClient[] aClients = new JSONRPCClient[this.clients.size()/this.workers.length];

            for(int c = 0; c < aClients.length; ++c) {
                if (b < this.clients.size()) {
                    aClients[c] = this.clients.get(b);
                } else {
                    aClients[c] = null;
                }
                b++;
            }

            this.workers[i].setClients(aClients);
        }
    }

    /**
     * removeClient.
     *
     * @param client JSONRPCClient.
     */
    public void removeClient(final JSONRPCClient client) {

        int b = 0;

        this.clients.remove(client);

        for(int i = 0; i < this.workers.length; ++i) {

            JSONRPCClient[] aClients = new JSONRPCClient[this.clients.size()/this.workers.length];

            for(int c = 0; c < aClients.length; ++c) {
                if (b < this.clients.size()) {
                    aClients[c] = this.clients.get(b);
                } else {
                    aClients[c] = null;
                }
                b++;
            }

            this.workers[i].setClients(aClients);
        }
 
    }

    public int getClientCount() {
        return this.clients.size();
    }
}
