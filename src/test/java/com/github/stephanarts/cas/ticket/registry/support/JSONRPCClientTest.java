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

import com.github.stephanarts.cas.ticket.registry.support.PaceMaker;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCClient;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;


/**
 * Unit test for JSONRPCClient.
 */
@RunWith(JUnit4.class)
public class JSONRPCClientTest
{

    //static String connectURI = new String("inproc://jsonrpcclient-test");
    static String connectURI = new String("tcp://localhost:2222");

    /**
     * Logging Class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static ResponseServer server;

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

                    msg = new String(body.getData());

                    req = new JSONObject(msg);

                    methodName = req.getString("method");
                    message.removeLast();
                    if (methodName.equals("valid")) {
                        message.addString("{\"json-rpc\": \"2.0\", \"result\": { \"OK\":\"...\"}}");
                        message.send(this.socket);
                    }
                    if (methodName.equals("invalid")) {
                        message.addString("{\"json-rpc\": \"2.0\", \"status\"... \"OK\"}");
                        message.send(this.socket);
                    }
                    if (methodName.equals("error")) {
                        message.addString("{\"json-rpc\": \"2.0\", \"error\": {\"code\": -32501, \"message\": \"Test\"}}");
                        message.send(this.socket);
                    }
                    if (methodName.equals("timeout")) {
                    }
                    logger.error("METHOD: "+methodName);
                }
            }

        }

        public final void start() {
            this.context = ZMQ.context(1);
            this.socket = context.socket(ZMQ.ROUTER);

            this.socket.bind(JSONRPCClientTest.connectURI);

            logger.debug("START");
            super.start();
        }

    }

    @BeforeClass
    public static void beforeTest() {
        server = new ResponseServer();
        server.start();        
    }

    @AfterClass
    public static void afterTest() {
        server.interrupt();
    }


    /**
     * testValidResponse
     *
     * Goal:
     * Test handling of a valid response.
     *
     */
    @Test
    public void testValidResponse() throws Exception {
        logger.info("testValidResponse");

        JSONRPCClient c = new JSONRPCClient(JSONRPCClientTest.connectURI);
        JSONObject params = new JSONObject();

        c.connect();

        c.call("valid", params);

        c.disconnect();
    }

    /**
     * testInvalidResponse
     *
     * Goal:
     * Test handling of an invalid response.
     *
     */
    @Test
    public void testInvalidResponse() throws Exception {
        logger.info("testInvalidResponse");

        JSONRPCClient c = new JSONRPCClient(JSONRPCClientTest.connectURI);
        JSONObject params = new JSONObject();
        JSONObject result = null;

        c.connect();

        try {
            result = c.call("invalid", params);
            Assert.assertNotNull(result);
        } catch (final JSONRPCException e) {
            Assert.assertEquals(-32700, e.getCode());
            return;
        }

        Assert.fail("No Exception Thrown");
    } 

    /**
     * testErrorResponse
     *
     * Goal:
     * Test handling of an individual error response.
     *
     */
    @Test
    public void testErrorResponse() throws Exception {
        logger.info("testErrorResponse");
        JSONRPCClient c = new JSONRPCClient(JSONRPCClientTest.connectURI);
        JSONObject params = new JSONObject();
        JSONObject result = null;

        c.connect();

        try {
            result = c.call("error", params);
            Assert.assertNotNull(result);
            
        } catch (final JSONRPCException e) {
            Assert.assertEquals(e.getCode(), -32501);
            c.disconnect();
            return;
        }

        c.disconnect();

        Assert.fail("No Exception Thrown");
    }

    /**
     * testTimeout
     *
     * Goal:
     * test that the proper exception is thrown when the server
     * takes too long to respond.
     *
     * Method:
     * no-response server object sends no reply.
     */
    @Test
    public void testTimeout() throws Exception {
        logger.info("testTimeout");
        JSONRPCClient c = new JSONRPCClient(JSONRPCClientTest.connectURI);
        JSONObject params = new JSONObject();
        JSONObject result = null;
        int a = 0;

        c.connect();

        try {
            result = c.call("timeout", params);
            Assert.assertNotNull(result);
        } catch (final JSONRPCException e) {
            Assert.assertEquals(-32300, e.getCode());
            c.disconnect();
            return;
        }

        c.disconnect();

        Assert.fail("No Exception Thrown");
    }

    /**
     * Test if getConnectURI returns the URI.
     */
    @Test
    public void testGetConnectURI() throws Exception {
        logger.info("testConnectURI");
        String uri = new String("tcp://localhost:1010");
        JSONRPCClient c = new JSONRPCClient(uri);

        Assert.assertTrue("getConnectURI does not return the correct URI", uri.equals(c.getConnectURI()));
    }

    /**
     * Test if sejreturns the URI.
     */
    @Test
    public void testAvailability() throws Exception {
        logger.info("testAvailability");

        String uri = new String("tcp://localhost:1010");
        JSONRPCClient c = new JSONRPCClient(uri);
        boolean available = false;

        /**
         * Set the available bit to true.
         * Don't assume a default.
         */
        c.setAvailable(true);

        available = c.getAvailable();

        /**
         * Check if the value equals true.
         */
        Assert.assertTrue(available);

        /**
         * Set the available bit to false.
         */
        c.setAvailable(false);

        available = c.getAvailable();

        /**
         * Check if the value equals false.
         */
        Assert.assertFalse(available);
    }

    @Test
    public void testPaceMaker() throws Exception {
        String uri = new String("tcp://localhost:1010");
        PaceMaker p = new PaceMaker();
        JSONRPCClient c = new JSONRPCClient(uri, p);

        Assert.assertEquals(p.getClientCount(), 0);

        c.connect();

        Assert.assertEquals(p.getClientCount(), 1);

        c.disconnect();

        Assert.assertEquals(p.getClientCount(), 0);

    }

}
