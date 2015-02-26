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

import org.json.JSONObject;
import org.json.JSONArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jasig.cas.ticket.Ticket;

import com.github.stephanarts.cas.ticket.registry.support.IMethod;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

/**
 * GetTicketsMethod Class.
 */
final class GetTicketsMethod implements IMethod {

    /**
     * Logging Class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private HashMap<Integer, Ticket> map; 

    /**
     * Constructor.
     *
     * @param map ticket-map.
     */
    public GetTicketsMethod(final HashMap<Integer, Ticket> map) {
        this.map = map;
    }

    /**
     * Execute the JSONRPCFunction.
     *
     * @param params    JSONRPC Method Parameters.
     *
     * @return          JSONRPC result object
     *
     * @throws JSONRPCException implementors can throw JSONRPCExceptions containing the error.
     */
    public JSONObject execute(final JSONObject params) throws JSONRPCException {
        JSONObject result = new JSONObject();
        JSONArray tickets = new JSONArray();

        String ticketId = null;

        if (params.length() != 0) {
            throw new JSONRPCException(-32602, "Invalid Params");
        }

        for(Ticket ticket: this.map.values()) {

            byte[] serializedTicketArray = {0};

            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                ObjectOutputStream so = new ObjectOutputStream(bo);
                so.writeObject(ticket);
                so.flush();
                serializedTicketArray = bo.toByteArray();
            } catch(final Exception e) {
                logger.debug(e.getMessage());
                throw new JSONRPCException(-32500, "Error extracting Ticket");
            }

            tickets.put(JSONObject.escape(DatatypeConverter.printBase64Binary(serializedTicketArray)));
        }

        logger.debug("GetTickets: "+tickets.length());

        result.put("tickets", tickets);

        return result;
    }
}
