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
import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.ServiceTicket;

import com.github.stephanarts.cas.ticket.registry.ZMQTicketRegistry;

import java.util.Collection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit test for ZMQTicketRegistry.
 */
@RunWith(JUnit4.class)
public class ZMQTicketRegistryTest
{
    @Test
    public void testWriteGetDelete() throws Exception {
        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket.getId()).thenReturn(ticketId);

        String[] addresses = {"tcp://localhost:5555"};
        ZMQTicketRegistry registry = new ZMQTicketRegistry(
                addresses,
                addresses[0],
                1500,
                500,
                2000);

        registry.addTicket(ticket);

        final ServiceTicket ticketFromRegistry = (ServiceTicket) registry.getTicket(ticketId);
        Assert.assertNotNull(ticketFromRegistry);
        Assert.assertEquals(ticketId, ticketFromRegistry.getId());
        registry.deleteTicket(ticketId);
        Assert.assertNull(registry.getTicket(ticketId));

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
                addresses[0],
                1500,
                500,
                2000);

        registry.addTicket(ticket);

        Collection<Ticket> tickets = registry.getTickets();
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
                addresses[0],
                1500,
                500,
                2000);

        Assert.assertTrue(registry.needsCallback());
    }
}
