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

import java.util.HashMap;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import javax.xml.bind.DatatypeConverter;

import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jasig.cas.ticket.Ticket;

import com.github.stephanarts.cas.ticket.registry.support.IMethod;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCServer;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

/**
 * ZMQProvider Class.
 */
class ZMQProvider extends JSONRPCServer {

    /**
     * Logging Class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    
    private final HashMap<Integer, Ticket> ticketMap;

    /**
     *  Create a ZMQProvider.
     *
     *  @param bindUri    BindURI
     */
    public ZMQProvider(final String bindUri) {
        super(bindUri);

        this.ticketMap = new HashMap<Integer, Ticket>();

        try {
            registerMethod("add", new AddMethod(this.ticketMap));
            registerMethod("get", new GetMethod(this.ticketMap));
            registerMethod("update", new UpdateMethod(this.ticketMap));
            registerMethod("delete", new DeleteMethod(this.ticketMap));
        } catch(final JSONRPCException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * DeleteMethod Class.
     */
    private class DeleteMethod implements IMethod {

        private HashMap<Integer, Ticket> map; 

        /**
         * Constructor.
         *
         * @param map ticket-map.
         */
        public DeleteMethod(final HashMap<Integer, Ticket> map) {
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

            String ticketId = params.getString("ticket-id");

            if(!this.map.containsKey(ticketId.hashCode())) {
                logger.warn("Missing Key {}", ticketId);
            }

            this.map.remove(ticketId.hashCode());

            result.put("ticket-id", ticketId);
            result.put("status", "REMOVED");

            return result;
        }
    }

    /**
     * AddMethod Class.
     */
    private class AddMethod implements IMethod {

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

    /**
     * GetMethod Class.
     */
    private class GetMethod implements IMethod {

        private HashMap<Integer, Ticket> map; 

        /**
         * Constructor.
         *
         * @param map ticket-map.
         */
        public GetMethod(final HashMap<Integer, Ticket> map) {
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

            logger.debug("GET");

            String ticketId = params.getString("ticket-id");
           
            Ticket t = this.map.get(ticketId.hashCode());
            byte[] serializedTicketArray = {0};

            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                ObjectOutputStream so = new ObjectOutputStream(bo);
                so.writeObject(t);
                so.flush();
                serializedTicketArray = bo.toByteArray();
            } catch(final Exception e) {
                logger.debug(e.getMessage());
            }

            result.put("ticket-id", ticketId);
            result.put("ticket", DatatypeConverter.printBase64Binary(serializedTicketArray));
            return result;
        }
    }

    /**
     * UpdateMethod Class.
     */
    private class UpdateMethod implements IMethod {

        private HashMap<Integer, Ticket> map; 

        /**
         * Constructor.
         *
         * @param map ticket-map.
         */
        public UpdateMethod(final HashMap<Integer, Ticket> map) {
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
            logger.debug("Update Ticket");

            String ticketId = params.getString("ticket-id");
            String serializedTicket = params.getString("ticket");

            try {
                ByteArrayInputStream bi = new ByteArrayInputStream(
                        DatatypeConverter.parseBase64Binary(serializedTicket));
                ObjectInputStream si = new ObjectInputStream(bi);

                Ticket ticket =(Ticket) si.readObject();

                if(!this.map.containsKey(ticket.hashCode())) {
                    logger.warn("Missing Key {}", ticketId);
                }

                this.map.put(ticketId.hashCode(), ticket);
            } catch(final Exception e) {
                logger.debug(e.getMessage());
            }

            logger.debug("Ticket-ID '{}'", ticketId);

            return result;
        }
    }
}
