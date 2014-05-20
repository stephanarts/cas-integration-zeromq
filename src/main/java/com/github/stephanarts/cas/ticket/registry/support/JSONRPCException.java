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
 * JSONRPCException Class.
 */
public class JSONRPCException extends Exception {

    /**
     * Numeric JSON-RPC Error-code.
     */
    private int code;

    /**
     * Verbose JSON-RPC Error-message.
     */
    private final String message;

    /**
     * Create a JSONRPCException object.
     *
     * @param  code     The numeric JSONRPC error-code
     * @param  message  The error-message
     */
    public JSONRPCException(final int code, final String message) {
        super(message);

        this.code = code;
        this.message = message;
    }

    /**
     * Return the numeric JSON-RPC error-code.
     *
     * @return         int containing numeric JSON-RPC error-code.
     */
    public final int getCode() {
        return this.code;
    }

    /**
     * Return the JSON-RPC error-message.
     *
     * @return         String containing JSON-RPC error-message.
     */
    public final String getMessage() {
        return this.message;
    }
}
