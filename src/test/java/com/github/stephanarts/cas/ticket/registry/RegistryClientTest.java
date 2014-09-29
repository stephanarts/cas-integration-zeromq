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

import com.github.stephanarts.cas.ticket.registry.RegistryClient;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;
import com.github.stephanarts.cas.ticket.registry.provider.ZMQProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit test for RegistryClient.
 */
@RunWith(JUnit4.class)
public class RegistryClientTest
{

    @Test
    public void testConstructor() throws Exception {
        RegistryClient client = new RegistryClient("tcp://localhost:4444");
    }

   
    @Test
    public void testAddTicket() throws Exception {
        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket.getId()).thenReturn(ticketId);

        String[] addresses = {"tcp://localhost:4440"};
        ZMQProvider provider = new ZMQProvider(addresses[0], "add", 200);
        RegistryClient client = new RegistryClient(addresses[0]);

        provider.start();

        try {
            client.addTicket(ticket);
        } catch (final JSONRPCException e) {
            provider.interrupt();
            Assert.fail("Adding ticket Failed");
        }

        provider.interrupt();
    }

    @Test
    public void testAddNullTicket() throws Exception {
        String[] addresses = {"tcp://localhost:4441"};
        ZMQProvider provider = new ZMQProvider(addresses[0], "add", 200);
        RegistryClient client = new RegistryClient(addresses[0]);

        provider.start();

        try {
            client.addTicket(null);
        } catch (final JSONRPCException e) {
            provider.interrupt();
            return;
        } catch (final Exception e) {
            provider.interrupt();
            Assert.fail("Wrong Exception thrown");
        }

        provider.interrupt();
        Assert.fail("No exception thrown");
    }

    @Test
    public void testUpdateTicket() throws Exception {
        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket.getId()).thenReturn(ticketId);

        String[] addresses = {"tcp://localhost:4442"};
        ZMQProvider provider = new ZMQProvider(addresses[0], "add", 200);
        RegistryClient client = new RegistryClient(addresses[0]);

        provider.start();

        try {
            client.addTicket(ticket);
        } catch (final JSONRPCException e) {
            provider.interrupt();
            Assert.fail("Adding ticket Failed");
        } catch (final Exception e) {
            provider.interrupt();
            Assert.fail("Adding ticket Failed");
        }

        try {
            client.updateTicket(ticket);
        } catch (final JSONRPCException e) {
            provider.interrupt();
            Assert.fail("Update ticket Failed");
        }

        provider.interrupt();
    }
}
