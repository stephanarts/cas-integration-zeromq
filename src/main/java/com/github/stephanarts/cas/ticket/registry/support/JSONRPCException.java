/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.github.stephanarts.cas.ticket.registry.support;

/**
 * JSONRPCException Class.
 */
class JSONRPCException extends Exception {

    /**
     * Numeric JSON-RPC Error-code.
     */
    private int code;

    /**
     * Verbose JSON-RPC Error-message.
     */
    private String message;

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
    public int getCode() {
        return this.code;
    }

    /**
     * Return the JSON-RPC error-message.
     *
     * @return         String containing JSON-RPC error-message.
     */
    public String getMessage() {
        return this.message;
    }
}
