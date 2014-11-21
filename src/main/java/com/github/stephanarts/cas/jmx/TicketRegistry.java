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

package com.github.stephanarts.cas.jmx;

import com.github.stephanarts.cas.ticket.registry.ZMQTicketRegistry;

/**
 * TicketRegistry for monitoring the ZMQTicketRegistry.
 *
 */
public class TicketRegistry implements TicketRegistryMBean {

    private ZMQTicketRegistry registry = null;

    /**
     * Create an MBean. 
     *
     * @param reg ZMQTicketRegistry
     */
    public TicketRegistry(final ZMQTicketRegistry reg) {
        this.registry = reg;
    }

    /**
     * Return providerId.
     *
     * @return local Provider-ID.
     */
    public final String getProviderId() {
        return this.registry.getProviderId();
    }
}
