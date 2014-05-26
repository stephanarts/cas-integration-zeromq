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

        socket.send("{\"json-rpc\":\"2.0\",\"id\":\"1\",\"method\":\"test-a\",\"params\":{\"a\":\"b\"}}", ZMQ.DONTWAIT);

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

        server.start();
        socket.connect("tcp://localhost:7892");

        try {
            server.registerMethod("test-a", new TestMethod());
        } catch (JSONRPCException e) {
            throw new Exception(e);
        }

        socket.send("{\"json-rpc\":\"2.0\",\"id\":\"1\",\"method\":\"test-b\",\"params\":{\"a\":\"b\"}}",ZMQ.DONTWAIT);

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

        socket.close();
        context.close();
        server.interrupt();
    }

    @Test
    public void testInvalidRequest() throws Exception {
        JSONRPCServer server = new JSONRPCServer("tcp://localhost:7893");
        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);

        server.start();
        socket.connect("tcp://localhost:7893");

        try {
            server.registerMethod("test-a", new TestMethod());
        } catch (JSONRPCException e) {
            throw new Exception(e);
        }

        socket.send("{\"json-rpc\":\"2.0\",\"id\":\"1\",\"method\":\"test-b\",\"params\":{\"a\":\"b\"}}", ZMQ.DONTWAIT);

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
        } else {
            throw new Exception("Failed to get reply from server");
        }

        socket.close();
        context.close();
        server.interrupt();
    }
}
