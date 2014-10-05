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
import org.zeromq.ZMsg;

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
     * ZMQ Control Socket.
     */
    private final Socket  controlSocket;

    /**
     * ZMQ Sockets.
     */
    private Socket[]  sockets = {};
    private JSONRPCClient[]  clients = {};

    /**
     * HeartbeatTimeout.
     */
    private int heartbeatTimeout = 200;

    /**
     * HeartbeatInterval.
     */
    private int heartbeatInterval = 5000;

    private static int NR = 0;

    private static Object NRLOCK = new Object();

    private final int nr;

    /**
     * Create a WatchDog object.
     *
     */
    public WatchDog() {

        this.context = ZMQ.context(1);

        this.controlSocket = this.context.socket(ZMQ.PULL);

        synchronized(this.NRLOCK) {
            this.NR++;
            this.nr = this.NR;
        }
    }

    /**
     * setClients.
     *
     * @param clients .. the Clients.
     */
    public final void setClients(final JSONRPCClient[] clients) {

        /* Clean up old sockets */
        synchronized(this) {

            this.clients = clients;

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
    }

    /**
     * Start the WatchDog thread.
     *
     * Binds the sockets before calling Thread.start(), this way
     * they are already bound before the first run() is executed.
     */
    public final void start() {
        /** Bind Socket */
        this.controlSocket.bind("inproc://watchdog-"+this.nr);
        logger.debug("Starting WatchDog ["+this.nr+"]");
        super.start();
    }

    /**
     * Run the heartbeat process.
     */
    public final void run() {

        int controlSocketIndex = 0;

        ZMsg   message;

        Poller items = new Poller(2);

        controlSocketIndex = items.register(this.controlSocket, Poller.POLLIN);

        while(!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(this.heartbeatInterval);

                synchronized(this) {
                    if (this.sockets.length > 0) {
                        for(int i = 0; i < this.sockets.length; ++i) {

                            int index = items.register(this.sockets[i], Poller.POLLIN);
                            this.sockets[i].send(new byte[] {0x0}, 0);

                            items.poll(this.heartbeatTimeout);

                            if(items.pollin(controlSocketIndex)) {
                                message = ZMsg.recvMsg(controlSocket);
                                logger.debug("Received STOP message [" + this.nr + "]");
                                break;
                            }

                            if(items.pollin(index)) {
                                message = ZMsg.recvMsg(this.sockets[i]);
                            } else {
                                logger.debug("Missed Heartbeat");

                                this.sockets[i].setLinger(0);
                                this.sockets[i].close();

                                this.sockets[i] = this.context.socket(ZMQ.REQ);
                                this.sockets[i].connect(this.clients[i].getConnectURI());
                            }

                            items.unregister(this.sockets[i]);

                        }
                    } else {
                        items.poll(this.heartbeatTimeout);

                        if(items.pollin(controlSocketIndex)) {
                            message = ZMsg.recvMsg(controlSocket);
                            logger.debug("Received STOP message [" + this.nr + "]");
                            break;
                        }
                    }
                }
            } catch (final InterruptedException ex) {
                    break;
            }
        }

        this.controlSocket.close();
    }

    /**
     * Send a 'stop' message to the control socket.
     *
     */
    public final void interrupt() {
        byte[] msg = new byte[1];
        Socket s = this.context.socket(ZMQ.PUSH);
        s.connect("inproc://watchdog-"+this.nr);
        s.send(msg, ZMQ.NOBLOCK);
        s.close();
        logger.debug("Sent a STOP Message to inproc://watchdog-"+this.nr);
        //super.interrupt();
    }
}
