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

/**
 * Unit test for RegistryClientMBeanTest.
 */
@RunWith(JUnit4.class)
public class RegistryClientMBeanTest
{
    private static MBeanServer mbs;
    private static ObjectName  mbeanName;
    private static RegistryClient client;

    static JMXConnectorServer sc = null;
    static JMXConnector cc = null;
    static MBeanServerConnection mbsc = null;
    static RegistryClientMBean mbeanProxy = null;

    @BeforeClass
    public static void beforeTest() throws Exception {
        RegistryClientMBeanTest.mbs =
            ManagementFactory.getPlatformMBeanServer();
        try {
            RegistryClientMBeanTest.mbeanName = new ObjectName(
                    "TEST:type=UnitTest,name=RegistryClientMBeanTest");
            RegistryClientMBeanTest.client = new RegistryClient (
                    "tcp://localhost:9898");

            RegistryClientMBeanTest.mbs.registerMBean(
                    RegistryClientMBeanTest.client,
                    RegistryClientMBeanTest.mbeanName);

            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
            RegistryClientMBeanTest.sc = 
                JMXConnectorServerFactory.newJMXConnectorServer(
                    url,
                    null,
                    RegistryClientMBeanTest.mbs);
            RegistryClientMBeanTest.sc.start();

            JMXServiceURL addr = RegistryClientMBeanTest.sc.getAddress();

            RegistryClientMBeanTest.cc = 
                    JMXConnectorFactory.connect(addr);

            RegistryClientMBeanTest.mbsc =
                RegistryClientMBeanTest.cc.getMBeanServerConnection();

            RegistryClientMBeanTest.mbeanProxy = JMX.newMBeanProxy(
                    RegistryClientMBeanTest.mbsc,
                    RegistryClientMBeanTest.mbeanName,
                    RegistryClientMBean.class, true);

        } catch (NotCompliantMBeanException e) {
        } catch (MBeanRegistrationException e) {
        } catch (InstanceAlreadyExistsException e) {
        } catch (MalformedObjectNameException e) {
        }
    }

    @AfterClass
    public static void afterTest() {
        try {
            RegistryClientMBeanTest.cc.close();
        } catch (Exception e) { }

        try {
            RegistryClientMBeanTest.sc.stop();
        } catch (Exception e) { }
    }

    /**
     * Test if the availability-flag can be read via the MBean.
     */
    @Test
    public void testGetAvailable() throws Exception {
        RegistryClientMBeanTest.client.setAvailable(false);
        Assert.assertEquals(false, mbeanProxy.getProviderAvailable());

        RegistryClientMBeanTest.client.setAvailable(true);
        Assert.assertEquals(true, mbeanProxy.getProviderAvailable());
    }

    /**
     * Test if the URI can be read via the MBean
     */
    @Test
    public void testGetProviderURI() throws Exception {
        Assert.assertTrue("tcp://localhost:9898".equals(mbeanProxy.getProviderURI()));
    }
}
