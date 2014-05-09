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

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.json.JSONObject;

import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
//import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;

/**
 * JSONRPCServer Class.
 */
class JSONRPCServer extends Thread {

    /**
     * Logging Class.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * ZMQ Context.
     */
    private final Context context;

    /**
     * ZMQ Socket.
     */
    private final Socket  socket;

    /**
     * BindURI.
     */
    private final String  bindUri;


    /**
     * Hashmap of Methods.
     */
    private final HashMap<String, IMethod> methodMap;


    /**
     * Create a JSONRPCServer object.
     *
     * @param bindUri   The URI to listen on
     */
    public JSONRPCServer(final String bindUri) {

        this.context = ZMQ.context(1);

        this.socket = this.context.socket(ZMQ.ROUTER);

        this.bindUri = bindUri;

        this.setName("JSONRPCServer");

        this.methodMap = new HashMap<String, IMethod>();

    }

    /**
     * Register a JSONRPCFunction.
     *
     * @param name                    Method Name.
     * @param method                  Method Class
     * @throws JSONRPCException       Exception
     */
    public void registerMethod(
            final String  name,
            final IMethod method) throws JSONRPCException {

        /**
         * Check if a method with this name is already
         * registered.
         */
        if(this.methodMap.containsKey(name)) {
            throw new JSONRPCException();
        }

        this.methodMap.put(name, method);
    }

    /**
     * Run the server.
     */
    public void run() {

        ZMsg message;

        Poller items = new Poller(1);

        /** Bind Socket */
        this.socket.bind(this.bindUri);

        items.register(this.socket, Poller.POLLIN);

        /** Enter the main event-loop */
        while(!Thread.currentThread().isInterrupted()) {
            items.poll();

            if(items.pollin(0)) {
                message = ZMsg.recvMsg(socket);
            }
        }
    }
}
