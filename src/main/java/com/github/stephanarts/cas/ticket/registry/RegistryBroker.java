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


import java.util.Collection;
import java.util.ArrayList;
import java.util.UUID;

import org.jasig.cas.ticket.Ticket;

import com.github.stephanarts.cas.ticket.registry.provider.ZMQProvider;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

/**
* Ticket registry implementation that stores tickets via JSON-RPC
* over a ZeroMQ transport layer.
*
* @author Stephan Arts
*/
public final class RegistryBroker {

    /**
     * Logging Class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ZMQProvider provider;

    private final String providerId = UUID.randomUUID().toString();

    private RegistryClient[] providers;

    private RegistryClient   localProvider;

    private final int requestTimeout = 1500; // msecs, (> 1000!)

    private boolean bootstrapped = false;


    /**
     * Creates a new RegistryBroker.
     *
     * The RegistryBroker stores tickets in the given
     * ZMQ Registry-Providers.
     *
     * @param providers  Array of providers to connect to
     * @param bindUri    URI to bind the RegistryProvider on
     *
     * @throws Exception if localProvider could not be found
     */
    public RegistryBroker(
                final String[] providers,
                final String bindUri)
            throws Exception {

        RegistryClient client;
        String id;

        this.provider = new ZMQProvider(bindUri, this.providerId);

        this.provider.start();

        this.providers = new RegistryClient[providers.length];

        for(int i = 0; i < this.providers.length; ++i) {
            client = new RegistryClient(providers[i]);
            try {
                id = client.getProviderId();
                if (this.providerId.equals(id)) {
                    this.localProvider = client;
                }
            } catch (final JSONRPCException e) {
                logger.error(e.getMessage());
            }

            this.providers[i] = client;
        }

        if (this.localProvider == null) {
            throw new Exception("Local Provider not found");
        }

    }

    /**
     * bootstrap
     *
     * Bootstrap Local Provider.
     *
     * @throws BootstrapException when bootstrapping fails.
     */
    public void bootstrap() throws BootstrapException {

        Collection<Ticket> tickets = new ArrayList<Ticket>();

        for (int i = 0; i < this.providers.length; ++i) {
            /* Bootstrap the localProvider */
            if (this.providers[i] != this.localProvider) {
                try {
                    tickets = this.providers[i].getTickets();
                } catch (final JSONRPCException e) {
                    continue;
                }

                this.bootstrapped = true;

                for(Ticket ticket: tickets) {
                    try {
                        this.localProvider.addTicket(ticket);
                    } catch (final JSONRPCException e) {
                        this.bootstrapped = false;
                        throw new BootstrapException();
                    }
                }
                /* Bootstrap success */
                return;
            }
        }

        /* Tried all providers, no success... */
        throw new BootstrapException();
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

        /* Try local provider first */
        try {
            ticket = this.localProvider.getTicket(ticketId);
            return ticket;
        } catch (final JSONRPCException e) {
            logger.error(e.getMessage());
        }

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
     * Destroy the Broker.
     *
     * @throws Exception when destruction fails.
     */
    public void destroy() throws Exception {
        this.close();
        return;
    }


    /**
     * getTickets.
     *
     * Get all tickets from the registry,
     *
     * @return Collection of tickets.
     */
    public Collection<Ticket> getTickets() {
        Collection<Ticket> tickets = new ArrayList<Ticket>();

        try {
            tickets = this.localProvider.getTickets();
        } catch (final JSONRPCException e) {
            logger.error(e.getMessage());
        }

        return tickets;
    }

    /**
     * Close the RegistryBroker.
     *
     */
    public void close() {
        this.provider.interrupt();
    }
}
