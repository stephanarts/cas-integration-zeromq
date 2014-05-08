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

package com.github.stephanarts.cas.ticket.registry;

import java.util.HashMap;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import javax.xml.bind.DatatypeConverter;

import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jasig.cas.ticket.Ticket;

import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

//import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;

/**
 * Provider Class.
 */
class Provider extends Thread {

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

    private final HashMap<Integer, Ticket> map;

    /**
     *  Create a RegistryProvider.
     *
     *  @param context    ZeroMQ Context
     *  @param bindUri    BindURI
     */
    public Provider(final String bindUri) {

        this.context = ZMQ.context(1);

        this.socket = this.context.socket(ZMQ.ROUTER);
        this.socket.bind(bindUri);
        this.setName("ZMQProviderThread-1");

        this.map = new HashMap<Integer, Ticket>();

    }

    /**
     * The main provider loop.
     */
    public void run() {
        ZMsg message;
        Poller items = new Poller(1);
        items.register(socket, Poller.POLLIN);

        JSONObject request;
        JSONObject requestParams;
        String     requestId;
        String     requestMethod;
        String     ticketId;
        String     serializedTicket;

        JSONObject response;
        JSONObject responseResult;

        while(!Thread.currentThread().isInterrupted()) {
            // poll and memorize multipart detection
            items.poll();

            if(items.pollin(0)) {
                message = ZMsg.recvMsg(socket);
                logger.debug(new String(message.getLast().getData()));
                try {
                    request = new JSONObject(new String(message.getLast().getData()));
                    requestId     = request.getString("id");
                    requestParams = request.getJSONObject("params");
                    requestMethod = request.getString("method");

                    response = new JSONObject();
                    responseResult = new JSONObject();
                    response.put("json-rpc", "2.0");
                    response.put("id", requestId);

                    try {

                        if(requestMethod.equals("delete")) {
                            logger.debug("Delete Ticket");

                            ticketId = requestParams.getString("ticket-id");

                            if(!this.map.containsKey(ticketId.hashCode())) {
                                logger.warn("Missing Key {}", ticketId);
                            }

                            this.map.remove(ticketId.hashCode());
                        }
                        if(requestMethod.equals("update")) {
                            logger.debug("Update Ticket");

                            ticketId = requestParams.getString("ticket-id");
                            serializedTicket = requestParams.getString("ticket");

                            ByteArrayInputStream bi = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(serializedTicket));
                            ObjectInputStream si = new ObjectInputStream(bi);

                            Ticket ticket =(Ticket) si.readObject();

                            if(!this.map.containsKey(ticket.hashCode())) {
                                logger.warn("Missing Key {}", ticketId);
                            }

                            this.map.put(ticketId.hashCode(), ticket);

                            logger.debug("Ticket-ID '{}'", ticketId);

                        }
                        if(requestMethod.equals("add")) {
                            logger.debug("Add Ticket");
                            ticketId = requestParams.getString("ticket-id");
                            serializedTicket = requestParams.getString("ticket");

                            ByteArrayInputStream bi = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(serializedTicket));
                            ObjectInputStream si = new ObjectInputStream(bi);

                            Ticket ticket =(Ticket) si.readObject();
                            if(this.map.containsKey(ticket.hashCode())) {
                                logger.error("Duplicate Key {}", ticketId);
                            } else {
                                this.map.put(ticketId.hashCode(), ticket);
                            }

                            logger.debug("Ticket-ID '{}'", ticketId);


                        }
                        if(requestMethod.equals("get")) {
                            logger.debug("GET");

                            ticketId = requestParams.getString("ticket-id");
                           
                            Ticket t = this.map.get(ticketId.hashCode());
                            byte[] serializedTicketArray;

                            ByteArrayOutputStream bo = new ByteArrayOutputStream();
                            ObjectOutputStream so = new ObjectOutputStream(bo);
                            so.writeObject(t);
                            so.flush();
                            serializedTicketArray = bo.toByteArray();

                            responseResult.put("ticket-id", ticketId);
                            responseResult.put("ticket", DatatypeConverter.printBase64Binary(serializedTicketArray));
                        }

                        response.put("result", responseResult);
                    } catch(final Exception e) {
                        response.put("error", responseResult);
                    }

                    message.removeLast();
                    message.addString(response.toString());
                    message.send(socket);

                } catch(final Exception e) {
                    logger.error(e.getMessage());
                }
            }
            /*
            try {
                logger.debug("Provider Run");
                Thread.sleep(1000);
            } catch(InterruptedException e) {
            }
            */
        }
        this.socket.close();
        this.context.term();
    }
}
