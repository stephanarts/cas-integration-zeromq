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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jasig.cas.ticket.Ticket;

import com.github.stephanarts.cas.ticket.registry.support.JSONRPCServer;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

/**
 * ZMQProvider Class.
 */
public class ZMQProvider extends JSONRPCServer {

    /**
     * Logging Class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final HashMap<Integer, Ticket> ticketMap =
            new HashMap<Integer, Ticket>();

    /**
     * Counter for Thread-name.
     */
    private static int NR = 0;

    /**
     *  Create a ZMQProvider.
     *
     *  @param bindUri    BindURI
     *  @param uniqueId   UniqueId used for identification.
     *  @param heartbeatInterval interval (in ms) to send a heartbeat message to clients.
     */
    public ZMQProvider(
            final String bindUri,
            final String uniqueId,
            final int heartbeatInterval) {
        super(bindUri);

        NR++;

        this.setName("ZMQProvider-"+NR);

        try {
            registerMethod("cas.addTicket", new AddMethod(this.ticketMap));
            registerMethod("cas.getTicket", new GetMethod(this.ticketMap));
            registerMethod("cas.updateTicket", new UpdateMethod(this.ticketMap));
            registerMethod("cas.deleteTicket", new DeleteMethod(this.ticketMap));
            registerMethod("cas.getTickets", new GetTicketsMethod(this.ticketMap));
            registerMethod("cas.getProviderId", new GetProviderIdMethod(uniqueId));
        } catch(final JSONRPCException e) {
            logger.error(e.getMessage());
        }
    }
}
