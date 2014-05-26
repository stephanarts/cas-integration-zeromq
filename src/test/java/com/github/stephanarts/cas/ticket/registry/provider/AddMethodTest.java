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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import javax.xml.bind.DatatypeConverter;


import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.json.JSONObject;

import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.ServiceTicket;

import com.github.stephanarts.cas.ticket.registry.provider.AddMethod;
import com.github.stephanarts.cas.ticket.registry.support.IMethod;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit test for AddMethod.
 */
@RunWith(JUnit4.class)
public class AddMethodTest
{
    @Test
    public void testValidInput() throws Exception {
        final byte[] serializedTicket;
        final HashMap<Integer, Ticket> map = new HashMap<Integer, Ticket>();
        final JSONObject params = new JSONObject();
        final IMethod method = new AddMethod(map);

        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket.getId()).thenReturn(ticketId);

        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(ticket);
            so.flush();
            serializedTicket = bo.toByteArray();

            params.put("ticket-id", ticketId);
            params.put("ticket", DatatypeConverter.printBase64Binary(serializedTicket));
            method.execute(params);

        } catch (final JSONRPCException e) {
            Assert.fail(e.getMessage());
        } catch (final Exception e) {
            throw new Exception(e);
        }
    }

    @Test
    public void testInvalidParameters() throws Exception {
        final HashMap<Integer, Ticket> map = new HashMap<Integer, Ticket>();
        final JSONObject params = new JSONObject();
        final IMethod method = new AddMethod(map);

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
    public void testDuplicateTicket() throws Exception {
    }

    @Test
    public void testDeserializationError() throws Exception {
        final HashMap<Integer, Ticket> map = new HashMap<Integer, Ticket>();
        final JSONObject params = new JSONObject();
        final IMethod method = new AddMethod(map);

        final String ticketId = "ST-1234567890ABCDEFGHIJKL-crud";
        /*
        final ServiceTicket ticket = mock(ServiceTicket.class, withSettings().serializable());
        when(ticket.getId()).thenReturn(ticketId);
        */

        params.put("ticket-id", ticketId);
        params.put("ticket", "FAIL"); 

        try {
            method.execute(params);
        } catch (final JSONRPCException e) {
            Assert.assertEquals(-32501, e.getCode());
            Assert.assertTrue(e.getMessage().equals("Could not decode Ticket"));
            return;
        }

        Assert.fail("No Exception Thrown");
    }
}
