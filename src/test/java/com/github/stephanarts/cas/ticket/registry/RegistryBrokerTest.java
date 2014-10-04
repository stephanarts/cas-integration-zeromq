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

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.ServiceTicket;

import com.github.stephanarts.cas.ticket.registry.provider.ZMQProvider;
import com.github.stephanarts.cas.ticket.registry.BootstrapException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit test for RegistryBroker.
 */
@RunWith(JUnit4.class)
public class RegistryBrokerTest
{

    @Test
    public void testConstructor() throws Exception {
        String[] addresses = {"tcp://localhost:4444"};

        ZMQProvider provider = new ZMQProvider(
                addresses[0],
                "testConstructor");

        provider.start();

        RegistryBroker broker = new RegistryBroker(
            addresses,
            1500,
            200,
            "testConstructor");

        provider.interrupt();
    }

    @Test
    public void testBootstrapSuccess() throws Exception {
        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket.getId()).thenReturn(ticketId);

        String[] addresses = {"tcp://localhost:4440","tcp://localhost:4441"};

        ZMQProvider provider0 = new ZMQProvider(addresses[0], "primary");
        ZMQProvider provider1 = new ZMQProvider(addresses[1], "secondary");
        RegistryClient populator = new RegistryClient(addresses[1]);
        RegistryClient checker = new RegistryClient(addresses[0]);
        RegistryBroker broker = null;

        provider0.start();
        provider1.start();

        populator.addTicket(ticket);


        /*
         * If bootstrapping is successfull,
         * checker returns the ticket we've added
         */
        broker = new RegistryBroker(
            addresses,
            1500,
            200,
            "primary");

        try {
            broker.bootstrap();
        } catch (final BootstrapException e) {
            provider0.interrupt();
            provider1.interrupt();
        }

        final ServiceTicket ticketFromRegistry = (ServiceTicket) checker.getTicket(ticketId);

        provider0.interrupt();
        provider1.interrupt();

        Assert.assertNotNull(ticketFromRegistry);
        Assert.assertEquals(ticketId, ticketFromRegistry.getId());
    }

    @Test
    public void testBootstrapFailure() throws Exception {
        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket.getId()).thenReturn(ticketId);

        String[] addresses = {"tcp://localhost:4440","tcp://localhost:4441"};

        ZMQProvider provider = new ZMQProvider(
                addresses[0],
                "bootstrapFailure");

        provider.start();

        RegistryBroker broker = null;

        /*
         * If bootstrapping is successfull,
         * checker returns the ticket we've added
         */
        broker = new RegistryBroker(
            addresses,
            1500,
            200,
            "bootstrapFailure");

        try {
            broker.bootstrap();
        } catch (final BootstrapException e) {
            provider.interrupt();
            return;
        }

        provider.interrupt();

        Assert.fail ("BootstrapException not thrown");
    }

    @Test
    public void testMissingLocalProvider() throws Exception {
        String[] addresses = {"tcp://localhost:4444"};
        RegistryBroker broker = null;
        try {
            broker = new RegistryBroker(
                addresses,
                1500,
                200,
                "missing");
        } catch (Exception e) {
            return;
        }

        Assert.fail("Exception not thrown");
    }
}
