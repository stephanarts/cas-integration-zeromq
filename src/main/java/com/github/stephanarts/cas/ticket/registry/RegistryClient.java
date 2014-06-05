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

import java.util.ArrayList;
import java.util.Collection;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import javax.xml.bind.DatatypeConverter;


import org.json.JSONObject;
import org.json.JSONArray;
//import org.json.JSONException;

import org.jasig.cas.ticket.Ticket;


import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCClient;

/**
 * RegistryClient Class.
 */
public class RegistryClient extends JSONRPCClient {

    /**
     * Logging Class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Create RegistryClient object.
     *
     * @param connectUri   URI to connect the client to.
     */
    public RegistryClient(final String connectUri) {
        super(connectUri);
    }

    /**
     * addTicket Method.
     *
     * @param ticket CAS Ticket object
     *
     * @throws JSONRPCException Throws JSONRPCException containing any error.
     */
    public final void addTicket(final Ticket ticket)
            throws JSONRPCException {

        byte[] serializedTicket = {};
        JSONObject params = new JSONObject();

        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(ticket);
            so.flush();
            serializedTicket = bo.toByteArray();
        } catch (final Exception e) {
            throw new JSONRPCException(-32501, "Could not decode Ticket");
        }

        params.put("ticket-id", ticket.getId());
        params.put("ticket", DatatypeConverter.printBase64Binary(serializedTicket));
        
        this.call("cas.addTicket", params);
    }

    /**
     * Delete a ticket from the ticketregistry.
     *
     * @param ticketId       Ticket-object to delete from Registry
     *
     * @throws JSONRPCException Throws JSONRPCException containing any error.
     */
    public final void deleteTicket(final String ticketId)
            throws JSONRPCException {

        JSONObject params = new JSONObject();

        params.put("ticket-id", ticketId);

        this.call("cas.deleteTicket", params);
    }


    /**
     * Get a ticket from the ticketregistry.
     *
     * @param ticketId       id of ticket-object to get from Registry
     *
     * @return Ticket Object
     *
     * @throws JSONRPCException Throws JSONRPCException containing any error.
     */
    public final Ticket getTicket(final String ticketId)
            throws JSONRPCException {

        JSONObject params = new JSONObject();
        JSONObject result;

        Ticket ticket = null;
        String serializedTicket;

        params.put("ticket-id", ticketId);

        result = this.call("cas.getTicket", params);

        if (result.has("ticket")) {
            serializedTicket = result.getString("ticket");

            try {
                ByteArrayInputStream bi = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(serializedTicket));
                ObjectInputStream si = new ObjectInputStream(bi);

                ticket = (Ticket) si.readObject();
            } catch (final Exception e) {
                throw new JSONRPCException(-32501, "Could not decode Ticket");
            }
        }


        if (ticket == null) {
            logger.debug("Failed to get Ticket: {}", ticketId);
        }

        return ticket;
    }

    /**
     * updateTicket Method.
     *
     * @param ticket CAS Ticket object
     *
     * @return Response Object
     *
     * @throws JSONRPCException Throws JSONRPCException containing any error.
     */
    public final JSONObject updateTicket(final Ticket ticket)
            throws JSONRPCException {

        byte[] serializedTicket = {};
        JSONObject params = new JSONObject();

        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(ticket);
            so.flush();
            serializedTicket = bo.toByteArray();
        } catch (final Exception e) {
            throw new JSONRPCException(-32501, "Could not decode Ticket");
        }

        params.put("ticket-id", ticket.getId());
        params.put("ticket", DatatypeConverter.printBase64Binary(serializedTicket));
        
        return this.call("cas.updateTicket", params);
    }

    /**
     * Get a ticket from the ticketregistry.
     *
     * @return Ticket Objects
     *
     * @throws JSONRPCException Throws JSONRPCException containing any error.
     */
    public final Collection<Ticket> getTickets()
            throws JSONRPCException {

        JSONObject params = new JSONObject();
        JSONObject result;
        JSONArray  resultTickets;

        Ticket ticket;
        ArrayList<Ticket> tickets = new ArrayList<Ticket>();

        result = this.call("cas.getTickets", params);

        if (result.has("tickets")) {
            resultTickets = result.getJSONArray("tickets");
            for(int i = 0; i < result.length(); ++i) {
                try {
                    String serializedTicket = resultTickets.getString(i);
                    ByteArrayInputStream bi = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(serializedTicket));
                    ObjectInputStream si = new ObjectInputStream(bi);

                    ticket = (Ticket) si.readObject();

                    tickets.add(ticket);
                } catch (final Exception e) {
                    throw new JSONRPCException(-32501, "Could not decode Ticket");
                }
            }
        }


        return tickets;
    }


}
