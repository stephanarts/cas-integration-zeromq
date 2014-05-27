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

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import javax.xml.bind.DatatypeConverter;

import org.json.JSONObject;
//import org.json.JSONException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jasig.cas.ticket.Ticket;

import com.github.stephanarts.cas.ticket.registry.support.IMethod;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

/**
 * AddMethod Class.
 */
public final class AddMethod implements IMethod {

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
    public AddMethod(final HashMap<Integer, Ticket> map) {
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
    public JSONObject execute(final JSONObject params)
            throws JSONRPCException {

        JSONObject result = new JSONObject();

        String ticketId = null;
        String serializedTicket = null;

        Ticket ticket = null;

        logger.debug("Add Ticket");
        if (params.length() != 2) {
            throw new JSONRPCException(-32602, "Invalid Params");
        }
        if (!(params.has("ticket-id") && params.has("ticket"))) {
            throw new JSONRPCException(-32602, "Invalid Params");
        }
        try {
            ticketId = params.getString("ticket-id");
            serializedTicket = params.getString("ticket");

            ByteArrayInputStream bi = new ByteArrayInputStream(
            DatatypeConverter.parseBase64Binary(serializedTicket));
            ObjectInputStream si = new ObjectInputStream(bi);

            ticket =(Ticket) si.readObject();
        } catch(final Exception e) {
            throw new JSONRPCException(-32501, "Could not decode Ticket");
        }

        if(this.map.containsKey(ticketId.hashCode())) {
            logger.error("Duplicate Key {}", ticketId);
            throw new JSONRPCException(-32502, "Duplicate Ticket");
        } else {
            this.map.put(ticketId.hashCode(), ticket);
        }

        logger.debug("Ticket-ID '{}'", ticketId);

        return result;
    }
}
