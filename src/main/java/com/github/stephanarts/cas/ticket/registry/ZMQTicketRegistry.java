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

import java.util.Collection;

import javax.validation.constraints.Min;

import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.registry.AbstractDistributedTicketRegistry;
import org.springframework.beans.factory.DisposableBean;

import com.github.stephanarts.cas.ticket.registry.provider.ZMQProvider;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

/**
* Ticket registry implementation that stores tickets via JSON-RPC
* over a ZeroMQ transport layer.
*
* @author Stephan Arts
*/
public final class ZMQTicketRegistry extends AbstractDistributedTicketRegistry implements DisposableBean {

    /**
     * TGT cache entry timeout in seconds.
     */
    @Min(0)
    private final int tgtTimeout;

    /**
     * ST cache entry timeout in seconds.
     */
    @Min(0)
    private final int stTimeout;


    private final ZMQProvider provider;

    private final RegistryClient[] providers;


    private final int requestTimeout = 1500; // msecs, (> 1000!)


    /**
     * Creates a new instance that stores tickets in the given ZMQ Registry-Providers.
     *
     * @param providers                         Array of providers to connect to
     * @param bindUri                           URI to bind the RegistryProvider on
     * @param ticketGrantingTicketTimeOut       Timeout
     * @param serviceTicketTimeOut              Timeout
     */
    public ZMQTicketRegistry(
                final String[] providers,
                final String bindUri,
                final int ticketGrantingTicketTimeOut,
                final int serviceTicketTimeOut) {

        this.tgtTimeout = ticketGrantingTicketTimeOut;
        this.stTimeout = serviceTicketTimeOut;

        this.provider = new ZMQProvider(bindUri);

        this.provider.start();

        this.providers = new RegistryClient[providers.length];

        for(int i = 0; i < this.providers.length; ++i) {
            this.providers[i] = new RegistryClient(providers[i]);
        }
    }

    /**
     * Update a ticket in the ticketregistry.
     *
     * @param ticket       Ticket-object to update in Registry
     */
    protected void updateTicket(final Ticket ticket) {
        logger.debug("Updating ticket {}", ticket);

        for(int i = 0; i < this.providers.length; ++i) {
            try {
                this.providers[i].updateTicket(ticket);
            } catch (final JSONRPCException e) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * Add a ticket to the ticketregistry.
     *
     * @param ticket       Ticket-object to add to Registry
     */
    public void addTicket(final Ticket ticket) {
        logger.debug("Adding ticket {}", ticket);

        for(int i = 0; i < this.providers.length; ++i) {
            try {
                this.providers[i].addTicket(ticket);
            } catch (final JSONRPCException e) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * Delete a ticket from the ticketregistry.
     *
     * @param ticketId       Ticket-object to delete from Registry
     *
     * @return     true/false ???
     */
    public boolean deleteTicket(final String ticketId) {
        logger.debug("Deleting ticket {}", ticketId);

        for(int i = 0; i < this.providers.length; ++i) {
            try {
                this.providers[i].deleteTicket(ticketId);
            } catch (final JSONRPCException e) {
                logger.error(e.getMessage());
            }
        }

        return false;
    }

    /**
     * Get a ticket from the ticketregistry.
     *
     * @param ticketId       id of ticket-object to get from Registry
     *
     * @return               Ticket object
     */
    public Ticket getTicket(final String ticketId) {
        logger.debug("Get Ticket {}", ticketId);

        Ticket ticket = null;

        for(int i = 0; i < this.providers.length; ++i) {
            try {
                ticket = this.providers[i].getTicket(ticketId);
                return ticket;
            } catch (final JSONRPCException e) {
                logger.error(e.getMessage());
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void destroy() throws Exception {
        this.provider.stop();
        return;
    }


    /**
     * {@inheritDoc}
     * This operation is not supported.
     *
     * @throws UnsupportedOperationException if you try and call this operation.
     */
    @Override
    public Collection<Ticket> getTickets() {
        throw new UnsupportedOperationException("GetTickets not supported.");
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    protected boolean needsCallback() {
        return true;
    }

}
