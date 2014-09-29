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

package com.github.stephanarts.cas.ticket.registry.support;

/**
 * HeartbeatException Class.
 */
public class HeartbeatException extends Exception {

    /**
     * Verbose HeartbeatException Error-message.
     */
    private final String message;

    /**
     * Create a HeartbeatException object.
     *
     * @param  message  The error-message
     */
    public HeartbeatException(final String message) {
        super(message);

        this.message = message;
    }

    /**
     * Return the HeartbeatException error-message.
     *
     * @return         String containing HeartbeatException error-message.
     */
    public final String getMessage() {
        return this.message;
    }
}
