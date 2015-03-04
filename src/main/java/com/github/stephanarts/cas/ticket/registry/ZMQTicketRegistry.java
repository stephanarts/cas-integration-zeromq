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

import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import javax.management.MBeanServer;

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

    private PaceMaker pacemaker;

    /**
     * Creates a new TicketRegistry Backend.
     *
     * An instance of the ZMQTicketRegistry stores
     * CAS Tickets in a cluster of Registry-Providers.
     *
     * @param providers         Array of providers to connect to
     * @param address           Address to bind the RegistryProvider on
     * @param port              TCP port to bind the RegistryProvider on
     * @param requestTimeout    Timeout
     * @param heartbeatTimeout  Timeout
     * @param heartbeatInterval Interval
     *
     * @throws Exception if localProvider could not be found
     */
    public ZMQTicketRegistry(
                final String[] providers,
                final String address,
                final int port,
                final int requestTimeout,
                final int heartbeatTimeout,
                final int heartbeatInterval)
            throws Exception {

        this.provider = new ZMQProvider(
                "tcp://"+address+":"+port,
                this.providerId);

        this.provider.start();

        this.pacemaker = new PaceMaker();

        pacemaker.setHeartbeatInterval(heartbeatInterval);
        pacemaker.setHeartbeatTimeout(heartbeatTimeout);

        this.registryBroker = new RegistryBroker(
                providers,
                requestTimeout,
                this.pacemaker,
                this.providerId);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("CAS:type=TicketRegistry,provider='"+port+"'");
        mbs.registerMBean(this.provider, name);

        try {
            this.registryBroker.bootstrap();
            int nTickets = this.registryBroker.getTickets().size();
            logger.info("Bootstrapping complete, got "+nTickets+" tickets.");
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
        this.provider.cleanup();
        this.registryBroker.cleanup();
        this.pacemaker.destroy();
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

    /**
     * Get local Provider ID.
     *
     * @return providerId.
     */
    public String getProviderId() {
        return this.providerId;
    }
}
