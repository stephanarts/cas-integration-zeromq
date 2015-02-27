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

/**
 * ZMQProviderMBean for monitoring the ticketRegistry.
 *
 */
public interface ZMQProviderMBean {

    /**
     * Return ticketMap size.
     *
     * @return number of tickets stored.
     */
    int getSize();

    /**
     * Return providerId.
     *
     * @return local Provider-ID.
     */
    String getProviderId();

    /**
     * Return statistics.
     *
     * @param method Method used.
     *
     * @return number of calls.
     */
    int getStats(final String method);
}
