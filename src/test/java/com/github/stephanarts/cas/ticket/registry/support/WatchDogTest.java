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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.json.JSONObject;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMsg;
import org.zeromq.ZFrame;

import com.github.stephanarts.cas.ticket.registry.support.WatchDog;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Matchers.*;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for WatchDog.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(WatchDog.class)
public class WatchDogTest 
{

    /**
     * Logging Class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    static String connectURI = new String("tcp://localhost:6666");

    private static class ResponseServer extends Thread {
        /**
         * Logging Class.
         */
        protected final Logger logger = LoggerFactory.getLogger(getClass());

        Context context;
        Socket  socket;

        public final void run() {

            ZMsg   message;
            ZFrame body;
            String msg;
            String methodName;
            JSONObject req;

            logger.debug("RUN");
            Poller items = new Poller(1);

            items.register(this.socket, Poller.POLLIN);

            logger.debug("poll");
            while(!Thread.currentThread().isInterrupted()) {
                items.poll();
                logger.trace("poll-res");

                if(items.pollin(0)) {
                    message = ZMsg.recvMsg(this.socket);
                    body = message.getLast();

                    byte[] d = body.getData();
                    if (d.length == 1 && d[0] == 0x0) {
                        /* Send pong */
                        message.removeLast();
                        message.addLast(new byte[] {0x0});
                        message.send(this.socket);
                    } else {
                        msg = new String(body.getData());

                        req = new JSONObject(msg);

                        methodName = req.getString("method");
                        message.removeLast();
                        if (methodName.equals("valid")) {
                            message.addString("{\"json-rpc\": \"2.0\", \"result\": { \"OK\":\"...\"}}");
                            message.send(this.socket);
                        }

                        logger.error("METHOD: "+methodName);
                    }
                }
            }

        }

        public final void start() {
            this.context = ZMQ.context(1);
            this.socket = context.socket(ZMQ.ROUTER);

            this.socket.bind(WatchDogTest.connectURI);

            logger.debug("START");
            super.start();
        }

    }

    @BeforeClass
    public static void beforeTest() {

    }

    @AfterClass
    public static void afterTest() {

    }


    @Test
    public void testConstructor() throws Exception {
        WatchDog w = new WatchDog();
    }

    @Test
    public void testLifeCycle() throws Exception {
        WatchDog w = new WatchDog();
        w.start();
        Assert.assertTrue(w.isAlive());
        w.cleanup();
        Assert.assertFalse(w.isAlive());
    }

    @Test
    public void testDefaultHeartbeatInterval() throws Exception {
        WatchDog w = new WatchDog();
        w.start();

        Assert.assertEquals(5000, w.getHeartbeatInterval());

        w.cleanup();
    }

    @Test
    public void testDefaultHeartbeatTimeout() throws Exception {
        WatchDog w = new WatchDog();
        w.start();

        Assert.assertEquals(200, w.getHeartbeatTimeout());

        w.cleanup();
    }

    @Test
    public void testHeartbeatInterval() throws Exception {
        WatchDog w = new WatchDog();
        w.start();

        Assert.assertEquals(5000, w.getHeartbeatInterval());

        w.setHeartbeatInterval(2000);

        Assert.assertEquals(2000, w.getHeartbeatInterval());

        w.cleanup();
    }

    @Test
    public void testHeartbeatTimeout() throws Exception {
        WatchDog w = new WatchDog();
        w.start();

        Assert.assertEquals(200, w.getHeartbeatTimeout());

        w.setHeartbeatTimeout(400);

        Assert.assertEquals(400, w.getHeartbeatTimeout());

        w.cleanup();
    }

    @Test
    public void testFailedHeartbeat() throws Exception {
        boolean available;
        ResponseServer server = new ResponseServer();
        WatchDog w = new WatchDog();
        JSONRPCClient[] c = { new JSONRPCClient(connectURI, null)};
        long responseTime = 0;

        responseTime = c[0].getResponseTime();
        Assert.assertEquals(0, responseTime);

        w.setClients(c);
        

        w.setHeartbeatTimeout(100);
        w.setHeartbeatInterval(100);

        w.start();

        Thread.sleep(1000);
        available = c[0].getAvailable();
        if (available == true) {
            w.cleanup();
            Assert.fail("Watchdog did not set availability to false");
        }

        server.start();        

        c[0].connect();

        Thread.sleep(1000);
        available = c[0].getAvailable();
        if (available == false) {
            c[0].disconnect();
            w.cleanup();
            server.interrupt();
            Assert.fail("Watchdog did not set availability to true");
        }

        responseTime = c[0].getResponseTime();
        Assert.assertNotEquals(0, responseTime);

        c[0].disconnect();
        w.cleanup();
        server.interrupt();
    }

}
