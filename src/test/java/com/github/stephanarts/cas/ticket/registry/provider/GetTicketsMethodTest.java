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

import java.util.HashMap;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ObjectOutputStream;

import org.json.JSONObject;
import org.json.JSONArray;

import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.ServiceTicket;

import com.github.stephanarts.cas.ticket.registry.provider.GetTicketsMethod;
import com.github.stephanarts.cas.ticket.registry.support.IMethod;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Matchers.*;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for GetTicketsMethod.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(GetTicketsMethod.class)
public class GetTicketsMethodTest
{
    @Test
    public void testValidInput() throws Exception {
        final HashMap<Integer, Ticket> map = new HashMap<Integer, Ticket>();
        final JSONObject params = new JSONObject();
        final IMethod method = new GetTicketsMethod(map);
        final JSONObject result;

        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket.getId()).thenReturn(ticketId);

        map.put(ticketId.hashCode(), ticket);

        try {
            result = method.execute(params);
        } catch (final JSONRPCException e) {
            Assert.fail(e.getMessage());
        } catch (final Exception e) {
            throw new Exception(e);
        }
    }

    @Test
    public void testInvalidParameters1() throws Exception {
        final HashMap<Integer, Ticket> map = new HashMap<Integer, Ticket>();
        final JSONObject params = new JSONObject();
        final IMethod method = new GetTicketsMethod(map);

        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";

        params.put("invalid-param", "MUST_FAIL"); 

        try {
            method.execute(params);
        } catch (final JSONRPCException e) {
            Assert.assertEquals(-32602, e.getCode());
            Assert.assertTrue(e.getMessage().equals("Invalid Params"));
            return;
        }

        Assert.fail("No Exception Thrown");
    }

    @Test
    public void testSerializationError() throws Exception {
        final HashMap<Integer, Ticket> map = new HashMap<Integer, Ticket>();
        final JSONObject params = new JSONObject();
        final IMethod method = new GetTicketsMethod(map);
        final JSONObject result;

        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());

        PowerMockito.whenNew(ObjectOutputStream.class).withAnyArguments().thenThrow(new Exception("broken"));

        map.put(ticketId.hashCode(), ticket);

        try {
            result = method.execute(params);
        } catch (final JSONRPCException e) {
            Assert.assertEquals(-32500, e.getCode());
            Assert.assertTrue(e.getMessage().equals("Error extracting Ticket"));
            return;
        } catch (final Exception e) {
            throw new Exception(e);
        }

        Assert.fail("No Exception Thrown");
    }

    @Test
    public void testGetMultipleTickets() throws Exception {
        final HashMap<Integer, Ticket> map = new HashMap<Integer, Ticket>();
        final JSONObject params = new JSONObject();
        final IMethod method = new GetTicketsMethod(map);
        final JSONObject result;
        final JSONArray  tickets;

        final String ticketId1 = "ST-1234567890ABCDEFGHIJKL-crud1";
        final ServiceTicket ticket1 = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket1.getId()).thenReturn(ticketId1);

        final String ticketId2 = "ST-1234567890ABCDEFGHIJKL-crud2";
        final ServiceTicket ticket2 = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket2.getId()).thenReturn(ticketId2);

        map.put(ticketId1.hashCode(), ticket1);
        map.put(ticketId2.hashCode(), ticket2);

        try {
            result = method.execute(params);
            if (!result.has("tickets")) {
                Assert.fail("key: tickets missing from result-set");
            }
            tickets = result.getJSONArray("tickets");
            Assert.assertEquals(2, tickets.length());
        } catch (final JSONRPCException e) {
            Assert.fail(e.getMessage());
        } catch (final Exception e) {
            throw new Exception(e);
        }
    }
}
