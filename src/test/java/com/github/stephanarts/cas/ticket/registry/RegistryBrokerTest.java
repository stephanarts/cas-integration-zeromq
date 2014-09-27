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

        RegistryBroker broker = new RegistryBroker(
            addresses,
            addresses[0]);

        broker.close();
    }

    @Test
    public void testBootstrapSuccess() throws Exception {
        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket.getId()).thenReturn(ticketId);

        String[] addresses = {"tcp://localhost:4440","tcp://localhost:4441"};

        ZMQProvider provider = new ZMQProvider(addresses[1], "primary");
        RegistryClient populator = new RegistryClient(addresses[1]);
        RegistryClient checker = new RegistryClient(addresses[0]);
        RegistryBroker broker = null;

        provider.start();

        populator.addTicket(ticket);


        /*
         * If bootstrapping is successfull,
         * checker returns the ticket we've added
         */
        broker = new RegistryBroker(
            addresses,
            addresses[0]);

        try {
            broker.bootstrap();
        } catch (final BootstrapException e) {
            provider.interrupt();
            broker.close();
            Assert.fail("Bootstrapping Failed"); 
        }

        final ServiceTicket ticketFromRegistry = (ServiceTicket) checker.getTicket(ticketId);

        provider.interrupt();

        broker.close();

        Assert.assertNotNull(ticketFromRegistry);
        Assert.assertEquals(ticketId, ticketFromRegistry.getId());
    }

    @Test
    public void testBootstrapFailure() throws Exception {
        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket.getId()).thenReturn(ticketId);

        String[] addresses = {"tcp://localhost:4440","tcp://localhost:4441"};

        RegistryBroker broker = null;

        /*
         * If bootstrapping is successfull,
         * checker returns the ticket we've added
         */
        broker = new RegistryBroker(
            addresses,
            addresses[0]);

        try {
            broker.bootstrap();
        } catch (final BootstrapException e) {
            broker.close();
            return;
        }

        broker.close();

        Assert.fail ("BootstrapException not thrown");
    }
}
