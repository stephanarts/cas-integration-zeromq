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

package com.github.stephanarts.cas.ticket.registry;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;
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

import com.github.stephanarts.cas.ticket.registry.provider.ZMQProvider;

/**
 * Unit test for RegistryBrokerMBeanTest.
 */
@RunWith(JUnit4.class)
public class RegistryBrokerMBeanTest
{
    private static MBeanServer mbs;
    private static ObjectName  mbeanName;
    private static RegistryBroker broker;

    static JMXConnectorServer sc = null;
    static JMXConnector cc = null;
    static MBeanServerConnection mbsc = null;
    static RegistryBrokerMBean mbeanProxy = null;
    static String[] addresses = {"tcp://localhost:9898","tcp://localhost:9899"};
    static ZMQProvider provider;

    @BeforeClass
    public static void beforeTest() throws Exception {

        RegistryBrokerMBeanTest.provider = new ZMQProvider(
                addresses[0],
                "MBeanTest");

        RegistryBrokerMBeanTest.provider.start();

        RegistryBrokerMBeanTest.mbs =
            ManagementFactory.getPlatformMBeanServer();
        try {
            RegistryBrokerMBeanTest.mbeanName = new ObjectName(
                    "TEST:type=UnitTest,name=RegistryBrokerMBeanTest");
            RegistryBrokerMBeanTest.broker = new RegistryBroker (
                    addresses,
                    500,
                    null,
                    "MBeanTest");

            RegistryBrokerMBeanTest.mbs.registerMBean(
                    RegistryBrokerMBeanTest.broker,
                    RegistryBrokerMBeanTest.mbeanName);

            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
            RegistryBrokerMBeanTest.sc = 
                JMXConnectorServerFactory.newJMXConnectorServer(
                    url,
                    null,
                    RegistryBrokerMBeanTest.mbs);
            RegistryBrokerMBeanTest.sc.start();

            JMXServiceURL addr = RegistryBrokerMBeanTest.sc.getAddress();

            RegistryBrokerMBeanTest.cc = 
                    JMXConnectorFactory.connect(addr);

            RegistryBrokerMBeanTest.mbsc =
                RegistryBrokerMBeanTest.cc.getMBeanServerConnection();

            RegistryBrokerMBeanTest.mbeanProxy = JMX.newMBeanProxy(
                    RegistryBrokerMBeanTest.mbsc,
                    RegistryBrokerMBeanTest.mbeanName,
                    RegistryBrokerMBean.class, true);

        } catch (NotCompliantMBeanException e) {
        } catch (MBeanRegistrationException e) {
        } catch (InstanceAlreadyExistsException e) {
        } catch (MalformedObjectNameException e) {
        }
    }

    @AfterClass
    public static void afterTest() {
        try {
            RegistryBrokerMBeanTest.cc.close();
        } catch (Exception e) { }

        try {
            RegistryBrokerMBeanTest.sc.stop();
        } catch (Exception e) { }

        RegistryBrokerMBeanTest.provider.cleanup();
        RegistryBrokerMBeanTest.broker.cleanup();
    }

    /**
     * Test if the availability-flag can be read via the MBean.
     */
    @Test
    public void testGetAvailable() throws Exception {
        Assert.assertEquals(2, mbeanProxy.getProviders());
    }
}
