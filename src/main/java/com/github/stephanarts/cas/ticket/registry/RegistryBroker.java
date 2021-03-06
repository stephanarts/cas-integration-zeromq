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

import org.jasig.cas.ticket.Ticket;

import com.github.stephanarts.cas.ticket.registry.support.PaceMaker;
import com.github.stephanarts.cas.ticket.registry.support.JSONRPCException;

import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import javax.management.MBeanServer;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;


/**
* Ticket registry implementation that stores tickets via JSON-RPC
* over a ZeroMQ transport layer.
*
* @author Stephan Arts
*/
public final class RegistryBroker
        implements RegistryBrokerMBean {

    /**
     * Logging Class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private RegistryClient[] providers;

    private RegistryClient   localProvider;

    private final int requestTimeout;

    private boolean bootstrapped = false;

    private ObjectName[] mbeans;


    /**
     * Creates a new RegistryBroker.
     *
     * The RegistryBroker stores tickets in the given
     * ZMQ Registry-Providers.
     *
     * @param providers         Array of providers to connect to
     * @param requestTimeout    timeout for client requests.
     * @param pacemaker         PaceMaker instance.
     * @param localProviderId   id that matches the 'local' provider
     *                          used for quick lookups.
     *
     * @throws Exception if localProvider could not be found
     */
    public RegistryBroker(
                final String[] providers,
                final int requestTimeout,
                final PaceMaker pacemaker,
                final String localProviderId)
            throws Exception {

        RegistryClient client;
        String id;

        this.requestTimeout = requestTimeout;

        this.providers = new RegistryClient[providers.length];
        this.mbeans    = new ObjectName[providers.length];

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        for(int i = 0; i < this.providers.length; ++i) {
            client = new RegistryClient(providers[i], pacemaker);

            this.mbeans[i] = new ObjectName(
                    "CAS:type=TicketRegistry,client='"+i+"'");
            mbs.registerMBean(client, this.mbeans[i]);
            try {
                id = client.getProviderId();
                if (localProviderId.equals(id)) {
                    this.localProvider = client;
                }
            } catch (final JSONRPCException e) {
                logger.error(e.getMessage());
            }

            this.providers[i] = client;
        }

        if (this.localProvider == null) {
            for(int i = 0; i < this.providers.length; ++i) {
                this.providers[i].destroy();
                try {
                    mbs.unregisterMBean(this.mbeans[i]);
                } catch (final InstanceNotFoundException e) {
                    logger.warn(e.toString());
                } catch (final MBeanRegistrationException e) {
                    logger.warn(e.toString());
                }
            }
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
                if (this.providers[i].getAvailable()) {
                    try {
                        tickets = this.providers[i].getTickets();
                    } catch (final JSONRPCException e) {
                        logger.error("Provider '"+this.providers[i].getConnectURI()+"': "+e.getMessage());
                        continue;
                    }
                } else {
                    logger.debug("Provider "+this.providers[i].getConnectURI()+" is not available");
                    continue;
                }

                this.bootstrapped = true;

                for(Ticket ticket: tickets) {
                    try {
                        this.localProvider.addTicket(ticket);
                    } catch (final JSONRPCException e) {
                        this.bootstrapped = false;
                        throw new BootstrapException("ehm...");
                    }
                } /* Bootstrap success */
                return;
            }
        }

        /* Tried all providers, no success... */
        throw new BootstrapException("Tried all providers, no success...");
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
                if (this.providers[i].getAvailable()) {
                    this.providers[i].updateTicket(ticket);
                }
            } catch (final JSONRPCException e) {
                logger.error("updateTicket error: " + e.getMessage());
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
                if (this.providers[i].getAvailable()) {
                    this.providers[i].addTicket(ticket);
                }
            } catch (final JSONRPCException e) {
                logger.error("addTicket error: " + e.getMessage());
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
                if (this.providers[i].getAvailable()) {
                    this.providers[i].deleteTicket(ticketId);
                }
            } catch (final JSONRPCException e) {
                logger.error("deleteTicket error: " + e.getMessage());
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
            if (e.getCode() == -32503) {
                logger.debug("Missing Ticket: " + ticketId);
            } else {
                logger.error("getTicket error: " + e.getMessage());
            }
        }

        return null;
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
            logger.error("getTickets error: " + e.getMessage());
        }

        return tickets;
    }

    /**
     * cleanup.
     */
    public void cleanup() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        for(int i = 0; i < this.providers.length; ++i) {
            this.providers[i].destroy();
            try {
                mbs.unregisterMBean(this.mbeans[i]);
            } catch (final InstanceNotFoundException e) {
                logger.warn(e.toString());
            } catch (final MBeanRegistrationException e) {
                logger.warn(e.toString());
            }
        }
    }

    /**
     * Return number of providers.
     *
     * @return number of provideers.
     */
    public int getProviders() {
        return this.providers.length;
    }
}
