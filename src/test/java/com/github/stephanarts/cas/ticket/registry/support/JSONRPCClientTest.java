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

        public final void run() {

            ZMsg   message;
            ZFrame body;
            String msg;

            logger.debug("RUN");
            Poller items = new Poller(3);

            items.register(this.validResponseSocket, Poller.POLLIN);
            items.register(this.invalidResponseSocket, Poller.POLLIN);
            items.register(this.errorResponseSocket, Poller.POLLIN);

            logger.debug("poll");
            while(!Thread.currentThread().isInterrupted()) {
                items.poll();
                logger.debug("poll-res");

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
            }

        }

        public final void start() {
            this.context = ZMQ.context(1);
            this.validResponseSocket = context.socket(ZMQ.ROUTER);
            this.invalidResponseSocket = context.socket(ZMQ.ROUTER);
            this.errorResponseSocket = context.socket(ZMQ.ROUTER);

            this.validResponseSocket.bind("tcp://localhost:2222");
            this.invalidResponseSocket.bind("tcp://localhost:2223");
            this.errorResponseSocket.bind("tcp://localhost:2224");

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
        server.stop();
    }


    @Test
    public void testValidResponse() throws Exception {
        JSONRPCClient c = new JSONRPCClient("tcp://localhost:2222");
        JSONObject params = new JSONObject();

        logger.debug("testValidResponse.call");
        c.call("t", params);
    }

    @Test
    public void testInvalidResponse() throws Exception {
        JSONRPCClient c = new JSONRPCClient("tcp://localhost:2223");
        JSONObject params = new JSONObject();
        JSONObject result = null;

        try {
            logger.debug("testInvalidResponse.call");
            result = c.call("t", params);
            Assert.assertNotNull(result);
        } catch (final JSONRPCException e) {
            Assert.assertEquals(e.getCode(), -32500);
            return;
        }

        Assert.fail("No Exception Thrown");
    } 

    @Test
    public void testErrorResponse() throws Exception {
        JSONRPCClient c = new JSONRPCClient("tcp://localhost:2224");
        JSONObject params = new JSONObject();
        JSONObject result = null;

        try {
            result = c.call("t", params);
            Assert.assertNotNull(result);
            
        } catch (final JSONRPCException e) {
            Assert.assertEquals(e.getCode(), -32501);
            return;
        }

        Assert.fail("No Exception Thrown");
    }
}