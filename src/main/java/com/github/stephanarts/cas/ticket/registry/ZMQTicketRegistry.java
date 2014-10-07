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
import java.util.UUID;

import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.registry.AbstractDistributedTicketRegistry;
import org.springframework.beans.factory.DisposableBean;

import com.github.stephanarts.cas.ticket.registry.provider.ZMQProvider;
import com.github.stephanarts.cas.ticket.registry.support.PaceMaker;

/**
 * Ticket registry implementation that stores tickets via JSON-RPC
 * over a ZeroMQ transport layer.
 *
 * @author Stephan Arts
 */
public final class ZMQTicketRegistry
        extends AbstractDistributedTicketRegistry
        implements DisposableBean {

    /**
     * Logging Class.
     */
    //protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final String providerId = UUID.randomUUID().toString();

    private final ZMQProvider provider;


private RegistryBroker   registryBroker;

    /**
     * Creates a new TicketRegistry Backend.
     *
     * An instance of the ZMQTicketRegistry stores
     * CAS Tickets in a cluster of Registry-Providers.
     *
     * @param providers         Array of providers to connect to
     * @param bindUri           URI to bind the RegistryProvider on
     * @param requestTimeout    Timeout
     * @param heartbeatTimeout  Timeout
     * @param heartbeatInterval Interval
     *
     * @throws Exception if localProvider could not be found
     */
    public ZMQTicketRegistry(
                final String[] providers,
                final String bindUri,
                final int requestTimeout,
                final int heartbeatTimeout,
                final int heartbeatInterval)
            throws Exception {

        this.provider = new ZMQProvider(
                bindUri,
                this.providerId);

        this.provider.start();

        this.registryBroker = new RegistryBroker(
                providers,
                requestTimeout,
                PaceMaker.getInstance(),
                this.providerId);

        try {
            this.registryBroker.bootstrap();
        } catch (final BootstrapException e) {
            logger.warn(e.getMessage());
        }
    }

    /**
     * Update a ticket in the ticketregistry.
     *
     * @param ticket       Ticket-object to update in Registry
     */
    protected void updateTicket(final Ticket ticket) {
        this.registryBroker.updateTicket(ticket);
    }

    /**
     * Add a ticket to the ticketregistry.
     *
     * @param ticket       Ticket-object to add to Registry
     */
    public void addTicket(final Ticket ticket) {
        this.registryBroker.addTicket(ticket);
    }

    /**
     * Delete a ticket from the ticketregistry.
     *
     * @param ticketId       Ticket-object to delete from Registry
     *
     * @return     true/false ???
     */
    public boolean deleteTicket(final String ticketId) {
        this.registryBroker.deleteTicket(ticketId);
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
        return registryBroker.getTicket(ticketId);
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void destroy() throws Exception {
        this.provider.interrupt();
        return;
    }


    /**
     * {@inheritDoc}
     *
     */
    @Override
    public Collection<Ticket> getTickets() {
        return this.registryBroker.getTickets();
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
