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

import org.json.JSONObject;
import org.json.JSONException;

import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import org.zeromq.ZFrame;
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
            throw new JSONRPCException(-1, "Method already registered");
        }

        this.methodMap.put(name, method);
    }

    /**
     * Run the server.
     */
    public void run() {

        ZMsg   message;
        ZFrame body;
        String msg;

        JSONObject request;

        String methodName;
        String methodId = null;

        JSONObject params;

        IMethod    method;

        JSONObject response = new JSONObject();
        JSONObject result;
        JSONObject error;

        Poller items = new Poller(1);

        response.put("jsonrpc", "2.0");

        /** Bind Socket */
        this.socket.bind(this.bindUri);

        items.register(this.socket, Poller.POLLIN);

        /** Enter the main event-loop */
        while(!Thread.currentThread().isInterrupted()) {
            items.poll();

            if(items.pollin(0)) {
                message = ZMsg.recvMsg(socket);
                body = message.getLast();
                msg = new String(body.getData());

                response.remove("id");
                response.remove("error");
                response.remove("result");

                try {
                    request = new JSONObject(msg);
                    if(!request.has("json-rpc")) {
                        /**
                         * code = -32600
                         * msg = Invalid Request
                         */
                        throw new JSONRPCException(
                                -32600,
                                "Invalid Request");
                    } else {
                        if(!request.getString("json-rpc").equals("2.0")) {
                            /**
                             * code = -32600
                             * msg = Invalid Request
                             */
                            throw new JSONRPCException(
                                    -32600,
                                    "Invalid Request");
                        }

                    }

                    /**
                     * Get the methodId, required for sending a response.
                     */
                    methodId = request.getString("id");

                    if(!request.has("params")) {
                        /**
                         * code = -32600
                         * msg = Invalid Request
                         */
                        throw new JSONRPCException(
                                -32600,
                                "Invalid Request");
                    }

                    /**
                     * We only support named params at the moment.
                     */
                    params = request.getJSONObject("params");
                    if(params == null) {
                        /**
                         * code = -32600
                         * msg = Invalid Request
                         */
                        throw new JSONRPCException(
                                -32600,
                                "Invalid Request");
                    }

                    if(!request.has("method")) {
                        /**
                         * code = -32600
                         * msg = Invalid Request
                         */
                        throw new JSONRPCException(
                                -32600,
                                "Invalid Request");
                    } else {
                        methodName = request.getString("method");
                        if (!this.methodMap.containsKey(methodName)) {
                            /**
                             * code = -32601
                             * msg = Method not Found
                             */
                            throw new JSONRPCException(
                                    -32601,
                                    "Method not Found");
                        }

                        method = this.methodMap.get(methodName);
                        if (method == null) {
                            /**
                             * code = -32601
                             * msg = Method not Found
                             */
                            throw new JSONRPCException(
                                    -32601,
                                    "Method not Found");
                        }

                        result = method.execute(params);

                        if(methodId != null) {
                            response.put("id", methodId);
                            response.put("result", result);
                        }
                    }
                } catch (final JSONException e) {
                    if(methodId != null) {
                        response.put("id", methodId);

                        error = new JSONObject();
                        response.put("error", error);
                        error.put("code", -32700);
                        error.put("message", "Parse Error");
                    }
                } catch (final JSONRPCException e) {
                    if(methodId != null) {
                        response.put("id", methodId);

                        error = new JSONObject();
                        response.put("error", error);
                        error.put("code", e.getCode());
                        error.put("message", e.getMessage());
                    }
                } catch (final Exception e) {
                    if(methodId != null) {
                        response.put("id", methodId);

                        error = new JSONObject();
                        response.put("error", error);
                        error.put("code", -32603);
                        error.put("message", "Internal error");
                    }
                }
            }
        }
    }
}
