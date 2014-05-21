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
    public JSONObject execute(final JSONObject params) throws JSONRPCException {
        JSONObject result = new JSONObject();

        logger.debug("Add Ticket");
        try {
            String ticketId = params.getString("ticket-id");
            String serializedTicket = params.getString("ticket");

            ByteArrayInputStream bi = new ByteArrayInputStream(
            DatatypeConverter.parseBase64Binary(serializedTicket));
            ObjectInputStream si = new ObjectInputStream(bi);

            Ticket ticket =(Ticket) si.readObject();
            if(this.map.containsKey(ticket.hashCode())) {
                logger.error("Duplicate Key {}", ticketId);
                /**
                 * TODO
                 *
                 * Raise JSONRPCException.
                 */
            } else {
                this.map.put(ticketId.hashCode(), ticket);
            }

            logger.debug("Ticket-ID '{}'", ticketId);
        } catch(final Exception e) {
            logger.debug(e.getMessage());
        }

        return result;
    }
}
