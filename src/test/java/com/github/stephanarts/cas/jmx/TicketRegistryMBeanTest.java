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

package com.github.stephanarts.cas.jmx;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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

import java.lang.reflect.UndeclaredThrowableException;

import com.github.stephanarts.cas.ticket.registry.ZMQTicketRegistry;

/**
 * Unit test for ZMQProviderMBeanTest.
 */
@RunWith(JUnit4.class)
public class TicketRegistryMBeanTest 
{

    static JMXConnectorServer sc = null;
    static JMXConnector cc = null;
    static MBeanServerConnection mbsc = null;
    static TicketRegistryMBean mbeanProxy = null;
    static ZMQTicketRegistry reg = null;

    @BeforeClass
    public static void beforeTest() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("CAS:type=TicketRegistry,port='9898'");

        String []addresses = {"tcp://localhost:9898"};

        TicketRegistryMBeanTest.reg = new ZMQTicketRegistry(
                addresses,
                "localhost",
                9898,
                500,
                700,
                5000);

        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
        TicketRegistryMBeanTest.sc = 
            JMXConnectorServerFactory.newJMXConnectorServer(
                url,
                null,
                mbs);
        TicketRegistryMBeanTest.sc.start();

        //try {
            JMXServiceURL addr = TicketRegistryMBeanTest.sc.getAddress();

            TicketRegistryMBeanTest.cc = 
                    JMXConnectorFactory.connect(addr);

            TicketRegistryMBeanTest.mbsc = TicketRegistryMBeanTest.cc.getMBeanServerConnection();

            TicketRegistryMBeanTest.mbeanProxy = JMX.newMBeanProxy(
                    TicketRegistryMBeanTest.mbsc,
                    name,
                    TicketRegistryMBean.class, true);
        //} catch(Exception e){ }
    }

    @AfterClass
    public static void afterTest() throws Exception {

        TicketRegistryMBeanTest.reg = null;

        if (TicketRegistryMBeanTest.cc != null) {
            TicketRegistryMBeanTest.cc.close();
            TicketRegistryMBeanTest.cc = null;
        }
        TicketRegistryMBeanTest.sc.stop();
        TicketRegistryMBeanTest.sc = null;
    }

    @Test
    public void testGetProviderId() throws Exception {
        try {
            String proxyId = TicketRegistryMBeanTest.mbeanProxy.getProviderId();
            String regId = reg.getProviderId();

            Assert.assertTrue(
                    proxyId.equals(regId));
        } catch (UndeclaredThrowableException e) {
            Assert.fail(e.getCause().getMessage());
        }
    }
}
