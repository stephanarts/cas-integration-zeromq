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
 * Unit test for ZMQProviderMBeanTest.
 */
@RunWith(JUnit4.class)
public class ZMQProviderMBeanTest
{
    @Test
    public void testGetSize() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("com.github.stephanarts.cas.ticket.registry.provider:type=ZMQProviderMBean");
        ZMQProvider mbean = new ZMQProvider("tcp://*:9898", "A");
        mbs.registerMBean(mbean, name);

        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
        JMXConnectorServer sc = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        sc.start();
        JMXConnector cc = null;
        try {
            JMXServiceURL addr = sc.getAddress();

            cc = JMXConnectorFactory.connect(addr);

            MBeanServerConnection mbsc = cc.getMBeanServerConnection();

            ZMQProviderMBean mbeanProxy = JMX.newMBeanProxy(
                    mbsc,
                    name,
                    ZMQProviderMBean.class, true);

            Assert.assertEquals(0, mbeanProxy.getSize());
        } finally {
            if (cc != null) {
                cc.close();
            }
            sc.stop();
        }
    }
}
