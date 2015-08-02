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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.ServiceTicket;

import com.github.stephanarts.cas.ticket.registry.ZMQTicketRegistry;

import java.util.Collection;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit test for ZMQTicketRegistry.
 */
@RunWith(JUnit4.class)
public class ZMQTicketRegistryTest
{
    /**
     * Logging Class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void testWriteGetDelete() throws Exception {
        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket.getId()).thenReturn(ticketId);

        String[] addresses = {"tcp://localhost:5555"};
        ZMQTicketRegistry registry = new ZMQTicketRegistry(
                addresses,
                "localhost",
                5555,
                1500,
                500,
                2000);

        registry.addTicket(ticket);

        final ServiceTicket ticketFromRegistry = (ServiceTicket) registry.getTicket(ticketId);
        Assert.assertNotNull(ticketFromRegistry);
        Assert.assertEquals(ticketId, ticketFromRegistry.getId());
        registry.deleteTicket(ticketId);
        Assert.assertNull(registry.getTicket(ticketId));

        registry.destroy();

        registry = null;
    }

    @Test
    public void testGetTickets() throws Exception {
        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket.getId()).thenReturn(ticketId);

        String[] addresses = {"tcp://localhost:5556"};
        ZMQTicketRegistry registry = new ZMQTicketRegistry(
                addresses,
                "localhost",
                5556,
                1500,
                500,
                2000);

        registry.addTicket(ticket);

        Collection<Ticket> tickets = registry.getTickets();

        registry.destroy();

        Assert.assertNotNull(tickets);
        Assert.assertEquals(1, tickets.size());

        registry = null;
    }

    /**
     * testNeedsCallback().
     *
     * verify that ZMQTicketRegistry.needsCallback() returns true.
     */
    @Test
    public void testNeedsCallback() throws Exception {

        String[] addresses = {"tcp://localhost:5557"};
        ZMQTicketRegistry registry = new ZMQTicketRegistry(
                addresses,
                "localhost",
                5557,
                1500,
                500,
                2000);

        registry.destroy();

        Assert.assertTrue(registry.needsCallback());
    }

    /**
     * testGetProviderId.
     *
     * Test that getProviderId returns a string.
     */
    @Test
    public void testGetProviderId() throws Exception {

        String[] addresses = {"tcp://localhost:5558"};
        ZMQTicketRegistry registry = new ZMQTicketRegistry(
                addresses,
                "localhost",
                5558,
                1500,
                500,
                2000);

        String providerId = registry.getProviderId();

        registry.destroy();

        Assert.assertNotNull(providerId);
    }

    /**
     * w00t.
     */
    @Test
    public void testThreadCleanup() throws Exception {
        String[] addresses = {"tcp://localhost:5559"};
        ZMQTicketRegistry registry = new ZMQTicketRegistry(
                addresses,
                "localhost",
                5559,
                1500,
                500,
                2000);


        registry.destroy();

        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);

        logger.error("Threads: "+threadArray.length);
        for (int i = 0; i < threadArray.length; ++i) {
            logger.error(threadArray[i].getName()+" _ "+threadArray[i].getState().toString());
        }
    }

    @Test
    public void testUpdate() throws Exception {
        final String oldTicketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket oldTicket = mock(ServiceTicket.class, withSettings().serializable());
        when(oldTicket.getId()).thenReturn(oldTicketId);
        when(oldTicket.getCountOfUses()).thenReturn(0);

        final String newTicketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket newTicket = mock(ServiceTicket.class, withSettings().serializable());
        when(newTicket.getId()).thenReturn(newTicketId);
        when(newTicket.getCountOfUses()).thenReturn(42);

        String[] addresses = {"tcp://localhost:5555"};
        ZMQTicketRegistry registry = new ZMQTicketRegistry(
                addresses,
                "localhost",
                5555,
                1500,
                500,
                2000);

        registry.addTicket(oldTicket);

        ServiceTicket ticketFromRegistry = (ServiceTicket) registry.getTicket(oldTicketId);
        Assert.assertNotNull(ticketFromRegistry);
        Assert.assertEquals(0, ticketFromRegistry.getCountOfUses());

        registry.updateTicket(newTicket);

        ticketFromRegistry = (ServiceTicket) registry.getTicket(oldTicketId);
        Assert.assertNotNull(ticketFromRegistry);
        Assert.assertEquals(42, ticketFromRegistry.getCountOfUses());

        registry.destroy();

        registry = null;
    }
}
