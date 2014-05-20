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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Assert;

import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.ServiceTicket;

import com.github.stephanarts.cas.ticket.registry.ZMQTicketRegistry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit test for ZMQTicketRegistry.
 */
public class ZMQTicketRegistryTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ZMQTicketRegistryTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ZMQTicketRegistryTest.class );
    }

    public void testWriteGetDelete() throws Exception {
        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket.getId()).thenReturn(ticketId);
        String[] addresses = {"tcp://localhost:5555"};
        ZMQTicketRegistry registry = new ZMQTicketRegistry(addresses,"tcp://localhost:5555", 30, 30);

        registry.addTicket(ticket);

        final ServiceTicket ticketFromRegistry = (ServiceTicket) registry.getTicket(ticketId);
        Assert.assertNotNull(ticketFromRegistry);
        Assert.assertEquals(ticketId, ticketFromRegistry.getId());
        registry.deleteTicket(ticketId);
        Assert.assertNull(registry.getTicket(ticketId));
    }
}
