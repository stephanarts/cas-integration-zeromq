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
            registerMethod("cas.addTicket", new AddMethod(this.ticketMap));
            registerMethod("cas.getTicket", new GetMethod(this.ticketMap));
            registerMethod("cas.updateTicket", new UpdateMethod(this.ticketMap));
            registerMethod("cas.deleteTicket", new DeleteMethod(this.ticketMap));
            registerMethod("cas.getTickets", new GetTicketsMethod(this.ticketMap));
        } catch(final JSONRPCException e) {
            logger.error(e.getMessage());
        }
    }
}
