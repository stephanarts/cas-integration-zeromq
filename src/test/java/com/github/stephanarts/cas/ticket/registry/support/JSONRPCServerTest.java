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

import org.junit.After;
import org.junit.Before;
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

import com.github.stephanarts.cas.ticket.registry.support.JSONRPCServer;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

/*
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
*/

/**
 * Unit test for JSONRPCServer.
 */
@RunWith(JUnit4.class)
public class JSONRPCServerTest
{

    /*
    private Context context;

    private Socket socket;

    private int port = 5000;
    */

    private class TestMethod implements IMethod {
        public JSONObject execute(JSONObject params) {
            JSONObject result = new JSONObject();
            result.put("c", "5");
            return result;
        }
    }

    private class TestJSONRPCExceptionMethod implements IMethod {
        public JSONObject execute(JSONObject params) throws JSONRPCException {
            throw new JSONRPCException(-35400, "Application error");
        }
    }

    private class TestExceptionMethod implements IMethod {
        public JSONObject execute(JSONObject params) throws Exception {
            throw new Exception("Unexpected Error");
        }
    }

    @Test
    public void testRegisterMethod() throws Exception {
        JSONRPCServer server = new JSONRPCServer("tcp://localhost:7890");
        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);

        server.start();
        socket.connect("tcp://localhost:7890");

        boolean testcase_1 = false;
        boolean testcase_2 = false;
        boolean testcase_3 = false;

        try {
            server.registerMethod("test", new TestMethod());
            testcase_1 = true;
        } catch (JSONRPCException e) {
            testcase_1 = false;
        }

        Assert.assertTrue(testcase_1);

        try {
            server.registerMethod("test", new TestMethod());
            testcase_2 = true;
        } catch (JSONRPCException e) {
            testcase_2 = false;
        }

        Assert.assertFalse(testcase_2);

        try {
            server.registerMethod("test-a", new TestMethod());
            testcase_3 = true;
        } catch (JSONRPCException e) {
            testcase_3 = false;
        }

        Assert.assertTrue(testcase_3);

        socket.close();
        context.close();
        server.interrupt();
    }

    @Test
    public void testCallMethod() throws Exception {
        JSONRPCServer server = new JSONRPCServer("tcp://localhost:7891");
        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);

        server.start();
        socket.connect("tcp://localhost:7891");

        try {
            server.registerMethod("test-a", new TestMethod());
        } catch (JSONRPCException e) {
            throw new Exception(e);
        }

        socket.send("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"test-a\",\"params\":{\"a\":\"b\"}}", ZMQ.DONTWAIT);

        PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
        int rc = ZMQ.poll(items, 5000);
        if(rc == -1) {
            throw new Exception("ZMQ.poll failed");
        }

        if(items[0].isReadable()) {
            // We got a reply from the server, must match sequence
            ZMsg message = ZMsg.recvMsg(socket);
            JSONObject response = new JSONObject(new String(message.getLast().getData()));
            JSONObject result = response.getJSONObject("result");
            String     res = result.getString("c");

            Assert.assertTrue(res.equals("5"));
        } else {
            throw new Exception("Failed to get reply from server");
        }

        socket.close();
        context.close();
        server.interrupt();
    }

    @Test
    public void testMissingMethod() throws Exception {
        JSONRPCServer server = new JSONRPCServer("tcp://localhost:7892");
        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);

        String requests[] = {
            "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"test-b\",\"params\":{\"a\":\"b\"}}",
            "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"test-c\",\"params\":{\"a\":\"b\"}}"
        };

        server.start();
        socket.connect("tcp://localhost:7892");

        try {
            server.registerMethod("test-a", new TestMethod());
            server.registerMethod("test-b", null);
        } catch (JSONRPCException e) {
            throw new Exception(e);
        }

        for(int i = 0; i < requests.length ; ++i) {
            socket.send(requests[i],ZMQ.DONTWAIT);

            PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
            int rc = ZMQ.poll(items, 5000);
            if(rc == -1) {
                throw new Exception("ZMQ.poll failed");
            }

            if(items[0].isReadable()) {
                // We got a reply from the server, must match sequence
                ZMsg message = ZMsg.recvMsg(socket);
                JSONObject response = new JSONObject(new String(message.getLast().getData()));
                JSONObject result = response.getJSONObject("error");
                int code = result.getInt("code");
                String msg = result.getString("message");

                Assert.assertTrue(code==-32601);
                Assert.assertTrue(msg.equals("Method not Found"));
            } else {
                throw new Exception("Failed to get reply from server");
            }
        }

        socket.close();
        context.close();
        server.interrupt();
    }

    @Test
    public void testInvalidRequest() throws Exception {
        JSONRPCServer server = new JSONRPCServer("tcp://localhost:7893");
        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);

        String requests[] = {
            "{\"jsonrpc\":\"3.0\",\"id\":\"1\",\"method\":\"test-b\",\"params\":{\"a\":\"b\"}}",
            "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"params\":{\"a\":\"b\"}}",
            "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"test-a\"}",
            "{\"id\":\"1\",\"method\":\"test-b\",\"params\":{\"a\":\"b\"}}",
            /* Only named params are supported */
            "{\"id\":\"1\",\"method\":\"test-b\",\"params\":[]}",
        };

        server.start();
        socket.connect("tcp://localhost:7893");

        try {
            server.registerMethod("test-a", new TestMethod());
        } catch (JSONRPCException e) {
            throw new Exception(e);
        }

        for(int i = 0; i < requests.length ; ++i) {
            socket.send(requests[i], ZMQ.DONTWAIT);

            PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
            int rc = ZMQ.poll(items, 5000);
            if(rc == -1) {
                throw new Exception("ZMQ.poll failed");
            }

            if(items[0].isReadable()) {
                // We got a reply from the server, must match sequence
                ZMsg message = ZMsg.recvMsg(socket);
                JSONObject response = new JSONObject(new String(message.getLast().getData()));
                JSONObject result = response.getJSONObject("error");

                int code = result.getInt("code");
                String msg = result.getString("message");

                Assert.assertEquals(-32600,code);
                Assert.assertTrue(msg.equals("Invalid Request"));
            } else {
                throw new Exception("Failed to get reply from server");
            }
        }

        socket.close();
        context.close();
        server.interrupt();
    }

    @Test
    public void testParseError() throws Exception {
        JSONRPCServer server = new JSONRPCServer("tcp://localhost:7894");
        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);

        String requests[] = {
            "INVALID"
        };

        server.start();
        socket.connect("tcp://localhost:7894");

        try {
            server.registerMethod("test-a", new TestMethod());
        } catch (JSONRPCException e) {
            throw new Exception(e);
        }

        for(int i = 0; i < requests.length ; ++i) {
            socket.send(requests[i], ZMQ.DONTWAIT);

            PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
            int rc = ZMQ.poll(items, 5000);
            if(rc == -1) {
                throw new Exception("ZMQ.poll failed");
            }

            if(items[0].isReadable()) {
                // We got a reply from the server, must match sequence
                ZMsg message = ZMsg.recvMsg(socket);
                JSONObject response = new JSONObject(new String(message.getLast().getData()));
                JSONObject result = response.getJSONObject("error");

                int code = result.getInt("code");
                String msg = result.getString("message");

                Assert.assertEquals(-32700,code);
                Assert.assertTrue(msg.equals("Parse error"));
            } else {
                throw new Exception("Failed to get reply from server");
            }
        }

        socket.close();
        context.close();
        server.interrupt();
    }

    @Test
    public void testCallMethodException() throws Exception {
        JSONRPCServer server = new JSONRPCServer("tcp://localhost:7895");
        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);

        server.start();
        socket.connect("tcp://localhost:7895");

        try {
            server.registerMethod("test-a", new TestExceptionMethod());
        } catch (JSONRPCException e) {
            throw new Exception(e);
        }

        socket.send("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"test-a\",\"params\":{\"a\":\"b\"}}", ZMQ.DONTWAIT);

        PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
        int rc = ZMQ.poll(items, 5000);
        if(rc == -1) {
            throw new Exception("ZMQ.poll failed");
        }

        if(items[0].isReadable()) {
            ZMsg message = ZMsg.recvMsg(socket);
            JSONObject response = new JSONObject(new String(message.getLast().getData()));
            JSONObject result = response.getJSONObject("error");

            int code = result.getInt("code");
            String msg = result.getString("message");

            Assert.assertEquals(-32603,code);
            Assert.assertTrue(msg.equals("Internal error"));
        } else {
            throw new Exception("Failed to get reply from server");
        }

        socket.close();
        context.close();
        server.interrupt();
    }

    @Test
    public void testCallMethodJSONRPCException() throws Exception {
        JSONRPCServer server = new JSONRPCServer("tcp://localhost:7896");
        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);

        server.start();
        socket.connect("tcp://localhost:7896");

        try {
            server.registerMethod("test-a", new TestJSONRPCExceptionMethod());
        } catch (JSONRPCException e) {
            throw new Exception(e);
        }

        socket.send("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"test-a\",\"params\":{\"a\":\"b\"}}", ZMQ.DONTWAIT);

        PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
        int rc = ZMQ.poll(items, 5000);
        if(rc == -1) {
            throw new Exception("ZMQ.poll failed");
        }

        if(items[0].isReadable()) {
            ZMsg message = ZMsg.recvMsg(socket);
            JSONObject response = new JSONObject(new String(message.getLast().getData()));
            JSONObject result = response.getJSONObject("error");

            int code = result.getInt("code");
            String msg = result.getString("message");

            Assert.assertEquals(-35400,code);
            Assert.assertTrue(msg.equals("Application error"));
        } else {
            throw new Exception("Failed to get reply from server");
        }

        socket.close();
        context.close();
        server.interrupt();
    }

    @Test
    public void testHeartbeat() throws Exception {
        JSONRPCServer server = new JSONRPCServer("tcp://localhost:7897");

        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);
        PollItem[] items = {new PollItem(socket, Poller.POLLIN)};

        server.start();
        socket.connect("tcp://localhost:7897");

        socket.send(new byte[] {0x0}, 0);

        int rc = ZMQ.poll(items, 200);
        if(rc == -1) {
            Assert.fail("Poll failed");
        }
        if(items[0].isReadable()) {
            ZMsg message = ZMsg.recvMsg(socket);
            ZFrame body = message.getLast();
            byte[] d = body.getData();

            socket.close();
            context.close();
            server.interrupt();

            Assert.assertEquals(1, d.length);
            Assert.assertEquals(0x0, d[0]);
            return;
        }

        socket.close();
        context.close();
        server.interrupt();

        Assert.fail("No heartbeat received.");
    }
}
