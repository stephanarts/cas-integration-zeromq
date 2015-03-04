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

package com.github.stephanarts.cas.ticket.registry.provider;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMsg;
import org.zeromq.ZFrame;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.management.*;
import javax.management.*;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

/**
 * Unit test for ZMQProviderMBeanTest.
 */
@RunWith(JUnit4.class)
public class ZMQProviderMBeanTest
{
    private static MBeanServer mbs;
    private static ObjectName  mbeanName;
    private static ZMQProvider provider;

    static JMXConnectorServer sc = null;
    static JMXConnector cc = null;
    static MBeanServerConnection mbsc = null;
    static ZMQProviderMBean mbeanProxy = null;

    @BeforeClass
    public static void beforeTest() throws Exception {
        ZMQProviderMBeanTest.mbs =
            ManagementFactory.getPlatformMBeanServer();
        try {
            ZMQProviderMBeanTest.mbeanName = new ObjectName(
                    "TEST:type=UnitTest,name=ZMQProviderMBeanTest");
            ZMQProviderMBeanTest.provider = new ZMQProvider(
                    "tcp://*:9898",
                    "A");

            ZMQProviderMBeanTest.mbs.registerMBean(
                    ZMQProviderMBeanTest.provider,
                    ZMQProviderMBeanTest.mbeanName);

            ZMQProviderMBeanTest.provider.start();

            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
            ZMQProviderMBeanTest.sc = 
                JMXConnectorServerFactory.newJMXConnectorServer(
                    url,
                    null,
                    ZMQProviderMBeanTest.mbs);
            ZMQProviderMBeanTest.sc.start();

            JMXServiceURL addr = ZMQProviderMBeanTest.sc.getAddress();

            ZMQProviderMBeanTest.cc = 
                    JMXConnectorFactory.connect(addr);

            ZMQProviderMBeanTest.mbsc =
                ZMQProviderMBeanTest.cc.getMBeanServerConnection();

            ZMQProviderMBeanTest.mbeanProxy = JMX.newMBeanProxy(
                    ZMQProviderMBeanTest.mbsc,
                    ZMQProviderMBeanTest.mbeanName,
                    ZMQProviderMBean.class, true);

        } catch (NotCompliantMBeanException e) {
        } catch (MBeanRegistrationException e) {
        } catch (InstanceAlreadyExistsException e) {
        } catch (MalformedObjectNameException e) {
        }
    }

    @AfterClass
    public static void afterTest() {
        try {
            ZMQProviderMBeanTest.provider.cleanup();
        } catch (Exception e) { }
    }

    @Test
    public void testGetSize() throws Exception {
        Assert.assertEquals(0, mbeanProxy.getSize());

    }

    @Test
    public void testGetId() throws Exception {
        Assert.assertEquals("A", mbeanProxy.getProviderId());
    }

    @Test
    public void testGetMissingMethodStats() throws Exception {
        Assert.assertEquals(-1, mbeanProxy.getStats("missing"));
    }

    @Test
    public void testGetStatsAddTicketsMethod() throws Exception {
        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);

        socket.connect("tcp://localhost:9898");

        Assert.assertEquals(0, mbeanProxy.getStats("cas.addTicket"));

        for (int i = 1; i < 10; ++i) {

            socket.send("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"cas.addTicket\",\"params\":{\"a\":\"b\"}}", ZMQ.DONTWAIT);
            PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
            int rc = ZMQ.poll(items, 5000);
            if(rc == -1) {
                throw new Exception("ZMQ.poll failed");
            }

            if(items[0].isReadable()) {
                ZMsg message = ZMsg.recvMsg(socket);
            } else {
                throw new Exception("Failed to get reply from server");
            }

            Assert.assertEquals(i, mbeanProxy.getStats("cas.addTicket"));

        }

        socket.close();
        context.close();
    }

    @Test
    public void testGetStatsGetTicketMethod() throws Exception {
        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);

        socket.connect("tcp://localhost:9898");

        Assert.assertEquals(0, mbeanProxy.getStats("cas.getTicket"));

        for (int i = 1; i < 10; ++i) {

            socket.send("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"cas.getTicket\",\"params\":{\"a\":\"b\"}}", ZMQ.DONTWAIT);
            PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
            int rc = ZMQ.poll(items, 5000);
            if(rc == -1) {
                throw new Exception("ZMQ.poll failed");
            }

            if(items[0].isReadable()) {
                ZMsg message = ZMsg.recvMsg(socket);
            } else {
                throw new Exception("Failed to get reply from server");
            }

            Assert.assertEquals(i, mbeanProxy.getStats("cas.getTicket"));
        }

        socket.close();
        context.close();
    }

    @Test
    public void testGetStatsGetTicketsMethod() throws Exception {
        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);

        socket.connect("tcp://localhost:9898");

        Assert.assertEquals(0, mbeanProxy.getStats("cas.getTickets"));

        for (int i = 1; i < 10; ++i) {

            socket.send("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"cas.getTickets\",\"params\":{\"a\":\"b\"}}", ZMQ.DONTWAIT);
            PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
            int rc = ZMQ.poll(items, 5000);
            if(rc == -1) {
                throw new Exception("ZMQ.poll failed");
            }

            if(items[0].isReadable()) {
                ZMsg message = ZMsg.recvMsg(socket);
            } else {
                throw new Exception("Failed to get reply from server");
            }

            Assert.assertEquals(i, mbeanProxy.getStats("cas.getTickets"));

        }

        socket.close();
        context.close();
    }

    @Test
    public void testGetStatsDeleteTicketMethod() throws Exception {
        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);

        socket.connect("tcp://localhost:9898");

        Assert.assertEquals(0, mbeanProxy.getStats("cas.deleteTicket"));

        for (int i = 1; i < 10; ++i) {

            socket.send("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"cas.deleteTicket\",\"params\":{\"a\":\"b\"}}", ZMQ.DONTWAIT);
            PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
            int rc = ZMQ.poll(items, 5000);
            if(rc == -1) {
                throw new Exception("ZMQ.poll failed");
            }

            if(items[0].isReadable()) {
                ZMsg message = ZMsg.recvMsg(socket);
            } else {
                throw new Exception("Failed to get reply from server");
            }

            Assert.assertEquals(i, mbeanProxy.getStats("cas.deleteTicket"));

        }

        socket.close();
        context.close();
    }

    @Test
    public void testGetStatsUpdateTicketMethod() throws Exception {
        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);

        socket.connect("tcp://localhost:9898");

        Assert.assertEquals(0, mbeanProxy.getStats("cas.updateTicket"));

        for (int i = 1; i < 10; ++i) {

            socket.send("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"cas.updateTicket\",\"params\":{\"a\":\"b\"}}", ZMQ.DONTWAIT);
            PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
            int rc = ZMQ.poll(items, 5000);
            if(rc == -1) {
                throw new Exception("ZMQ.poll failed");
            }

            if(items[0].isReadable()) {
                ZMsg message = ZMsg.recvMsg(socket);
            } else {
                throw new Exception("Failed to get reply from server");
            }

            Assert.assertEquals(i, mbeanProxy.getStats("cas.updateTicket"));

        }

        socket.close();
        context.close();
    }

    @Test
    public void testGetStatsGetProviderIdMethod() throws Exception {
        Context context = ZMQ.context(1);
        Socket socket = context.socket(ZMQ.REQ);

        socket.connect("tcp://localhost:9898");

        Assert.assertEquals(0, mbeanProxy.getStats("cas.getProviderId"));

        for (int i = 1; i < 10; ++i) {

            socket.send("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"cas.getProviderId\",\"params\":{\"a\":\"b\"}}", ZMQ.DONTWAIT);
            PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
            int rc = ZMQ.poll(items, 5000);
            if(rc == -1) {
                throw new Exception("ZMQ.poll failed");
            }

            if(items[0].isReadable()) {
                ZMsg message = ZMsg.recvMsg(socket);
            } else {
                throw new Exception("Failed to get reply from server");
            }

            Assert.assertEquals(i, mbeanProxy.getStats("cas.getProviderId"));

        }

        socket.close();
        context.close();
    }
}
