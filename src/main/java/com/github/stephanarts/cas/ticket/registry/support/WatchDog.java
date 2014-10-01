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

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
//import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;
//import org.zeromq.ZMsg;

/**
 * WatchDog class.
 */
public class WatchDog extends Thread {

    /**
     * Logging Class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * ZMQ Context.
     */
    private final Context context;

    /**
     * ZMQ Sockets.
     */
    private Socket[]  sockets = null;

    /**
     * HeartbeatTimeout.
     */
    private int heartbeatTimeout = 200;

    /**
     * HeartbeatInterval.
     */
    private int heartbeatInterval = 5000;

    /**
     * Create a WatchDog object.
     *
     */
    public WatchDog() {

        this.context = ZMQ.context(1);

    }

    /**
     * setClients.
     *
     * @param clients .. the Clients.
     */
    public final void setClients(final JSONRPCClient[] clients) {

        /* Clean up old sockets */
        for(int i = 0; i < this.sockets.length; ++i) {
            this.sockets[i].setLinger(0);
            this.sockets[i].close();
        }

        this.sockets = new Socket[clients.length];
        for(int i = 0; i < this.sockets.length; ++i) {
            this.sockets[i] = this.context.socket(ZMQ.REQ);
            this.sockets[i].connect(clients[i].getConnectURI());
        }
    }

    /**
     * Run the heartbeat process.
     */
    public final void run() {

        while(!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(this.heartbeatInterval);

                Poller items = new Poller(this.sockets.length);

                for(int i = 0; i < this.sockets.length; ++i) {
                    this.sockets[i].send(new byte[] {0x0}, 0);
                    items.register(this.sockets[i], Poller.POLLIN);
                }

                items.poll(this.heartbeatTimeout);

            } catch (final InterruptedException ex) {
                break;
            }
        }
    }
}
