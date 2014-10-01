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

import com.github.stephanarts.cas.ticket.registry.support.JSONRPCClient;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

/**
 * Unit test for JSONRPCClient.
 */
@RunWith(JUnit4.class)
public class JSONRPCClientTest
{

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
        Socket  validResponseSocket;
        Socket  invalidResponseSocket;
        Socket  errorResponseSocket;
        Socket  noResponseSocket;

        public final void run() {

            ZMsg   message;
            ZFrame body;
            String msg;

            logger.debug("RUN");
            Poller items = new Poller(3);

            items.register(this.validResponseSocket, Poller.POLLIN);
            items.register(this.invalidResponseSocket, Poller.POLLIN);
            items.register(this.errorResponseSocket, Poller.POLLIN);
            items.register(this.noResponseSocket, Poller.POLLIN);

            logger.debug("poll");
            while(!Thread.currentThread().isInterrupted()) {
                items.poll();
                logger.trace("poll-res");

                if(items.pollin(0)) {
                    message = ZMsg.recvMsg(this.validResponseSocket);
                    message.removeLast();
                    message.addString("{\"json-rpc\": \"2.0\", \"result\": { \"OK\":\"...\"}}");
                    message.send(this.validResponseSocket);
                }
                if(items.pollin(1)) {
                    message = ZMsg.recvMsg(this.invalidResponseSocket);
                    message.removeLast();
                    message.addString("{\"json-rpc\": \"2.0\", \"status\"... \"OK\"}");
                    message.send(this.invalidResponseSocket);
                }
                if(items.pollin(2)) {
                    message = ZMsg.recvMsg(this.errorResponseSocket);
                    message.removeLast();
                    message.addString("{\"json-rpc\": \"2.0\", \"error\": {\"code\": -32501, \"message\": \"Test\"}}");
                    message.send(this.errorResponseSocket);
                }
                if(items.pollin(3)) {
                    message = ZMsg.recvMsg(this.noResponseSocket);
                    message.removeLast();
                }
            }

        }

        public final void start() {
            this.context = ZMQ.context(1);
            this.validResponseSocket = context.socket(ZMQ.ROUTER);
            this.invalidResponseSocket = context.socket(ZMQ.ROUTER);
            this.errorResponseSocket = context.socket(ZMQ.ROUTER);
            this.noResponseSocket = context.socket(ZMQ.ROUTER);

            this.validResponseSocket.bind("tcp://localhost:2222");
            this.invalidResponseSocket.bind("tcp://localhost:2223");
            this.errorResponseSocket.bind("tcp://localhost:2224");
            this.noResponseSocket.bind("tcp://localhost:2225");

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
        JSONRPCClient c = new JSONRPCClient("tcp://localhost:2222");
        JSONObject params = new JSONObject();

        c.connect();

        logger.debug("testValidResponse.call");
        c.call("t", params);

        try {
            Thread.sleep(30000);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        c.call("t", params);

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
        JSONRPCClient c = new JSONRPCClient("tcp://localhost:2223");
        JSONObject params = new JSONObject();
        JSONObject result = null;

        c.connect();

        try {
            logger.debug("testInvalidResponse.call");
            result = c.call("t", params);
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
        JSONRPCClient c = new JSONRPCClient("tcp://localhost:2224");
        JSONObject params = new JSONObject();
        JSONObject result = null;

        c.connect();

        try {
            result = c.call("t", params);
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
     * testValidEndurance
     *
     * Goal:
     * Test that the client still responds properly after
     * Handing 5000 valid Responses.
     *
     */
    @Ignore
    @Test
    public void testValidEndurance() throws Exception {
        JSONRPCClient c = new JSONRPCClient("tcp://localhost:2222");
        JSONObject params = new JSONObject();

        c.connect();

        logger.debug("testValidEndurance.call");

        for(int i = 0; i < 5000; ++i) {
            c.call("t", params);
        }

        c.disconnect();

    }

    /**
     * testErrorEndurance
     *
     * Goal:
     * Test that the client still responds properly after
     * Handing 5000 application errors.
     *
     */
    @Ignore
    @Test
    public void testErrorEndurance() throws Exception {
        JSONRPCClient c = new JSONRPCClient("tcp://localhost:2224");
        JSONObject params = new JSONObject();
        JSONObject result = null;
        int a = 0;

        c.connect();

        for(int i = 0; i < 5000; ++i) {
            try {
                result = c.call("t", params);
                Assert.assertNotNull(result);
            } catch (final JSONRPCException e) {
                Assert.assertEquals(-32501, e.getCode());
                a++;
            }
        }

        c.disconnect();

        if (a == 5000) {
            return;
        }

        Assert.fail("No Exception Thrown");
    }

    /**
     * testInvalidEndurance
     *
     * Goal:
     * Test that the client still responds properly after
     * Handing 5000 invalid responses.
     *
     */
    @Test
    public void testInvalidEndurance() throws Exception {
        JSONRPCClient c = new JSONRPCClient("tcp://localhost:2223");
        JSONObject params = new JSONObject();
        JSONObject result = null;
        int a = 0;

        c.connect();

        for(int i = 0; i < 5000; ++i) {
            try {
                result = c.call("t", params);
                Assert.assertNotNull(result);
            } catch (final JSONRPCException e) {
                Assert.assertEquals(-32700, e.getCode());
                a++;
            }
        }

        c.disconnect();

        if (a == 5000) {
            return;
        }

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
        JSONRPCClient c = new JSONRPCClient("tcp://localhost:2225");
        JSONObject params = new JSONObject();
        JSONObject result = null;
        int a = 0;

        c.connect();

        try {
            result = c.call("t", params);
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
        String uri = new String("tcp://localhost:1010");
        JSONRPCClient c = new JSONRPCClient(uri);

        Assert.assertTrue("getConnectURI does not return the correct URI", uri.equals(c.getConnectURI()));
    }

}
