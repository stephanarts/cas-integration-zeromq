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

import java.util.Collection;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import javax.xml.bind.DatatypeConverter;

import javax.validation.constraints.Min;

import org.jasig.cas.ticket.Ticket;
//import org.jasig.cas.ticket.ServiceTicket;
//import org.jasig.cas.ticket.TicketGrantingTicket;
import org.jasig.cas.ticket.registry.AbstractDistributedTicketRegistry;
import org.springframework.beans.factory.DisposableBean;

import org.json.JSONObject;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;



/**
* Key-value ticket registry implementation that stores tickets in memcached keyed on the ticket ID.
*
* @author Stephan Arts
*/
public final class ZMQTicketRegistry extends AbstractDistributedTicketRegistry implements DisposableBean {

    /**
     * TGT cache entry timeout in seconds.
     */
    @Min(0)
    private final int tgtTimeout;

    /**
     * ST cache entry timeout in seconds.
     */
    @Min(0)
    private final int stTimeout;


    /**
     * ZMQ Context.
     */
    private final Context context;

    private final Provider provider;

    private final String[] hostnames;


    private final int requestTimeout = 1500; // msecs, (> 1000!)


    /**
     * Creates a new instance that stores tickets in the given ZMQ Registry-Providers.
     *
     * @param providers                         Array of providers to connect to
     * @param bindUri                           URI to bind the RegistryProvider on
     * @param ticketGrantingTicketTimeOut       Timeout
     * @param serviceTicketTimeOut              Timeout
     */
    public ZMQTicketRegistry(
                final String[] providers,
                final String bindUri,
                final int ticketGrantingTicketTimeOut,
                final int serviceTicketTimeOut) {
        this.tgtTimeout = ticketGrantingTicketTimeOut;
        this.stTimeout = serviceTicketTimeOut;

        this.context = ZMQ.context(1);

        this.provider = new Provider(bindUri);

        this.provider.start();

        this.hostnames = providers;
    }

    /**
     * Update a ticket in the ticketregistry.
     *
     * @param ticket       Ticket-object to update in Registry
     */
    protected void updateTicket(final Ticket ticket) {
        logger.debug("Updating ticket {}", ticket);

        String hostname;
        byte[] serializedTicket;
        JSONObject obj = new JSONObject();
        JSONObject params = new JSONObject();

        Socket socket;

        for(int i = 0; i < this.hostnames.length; ++i) {

            hostname = this.hostnames[i];
            logger.debug("Updating to {}", hostname);
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                ObjectOutputStream so = new ObjectOutputStream(bo);
                so.writeObject(ticket);
                so.flush();
                serializedTicket = bo.toByteArray();

                obj.put("json-rpc", "2.0");
                obj.put("method", "update");
                obj.put("params", params);

                params.put("ticket-id", ticket.getId());
                params.put("ticket", DatatypeConverter.printBase64Binary(serializedTicket));

                socket = this.context.socket(ZMQ.REQ);
                socket.connect(hostname);
                socket.send(obj.toString(), 0);
                socket.close();
            } catch (final Exception e) {
                logger.error(e.getMessage());
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

        String hostname;
        byte[] serializedTicket;
        JSONObject request = new JSONObject();
        JSONObject requestParams = new JSONObject();
        JSONObject response;
        JSONObject result;

        Socket socket;

        request.put("json-rpc", "2.0");
        request.put("id", "41");
        request.put("method", "add");
        request.put("params", requestParams);

        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(ticket);
            so.flush();
            serializedTicket = bo.toByteArray();
        } catch (final Exception e) {
            return;
        }

        requestParams.put("ticket-id", ticket.getId());
        requestParams.put("ticket", DatatypeConverter.printBase64Binary(serializedTicket));

        for(int i = 0; i < this.hostnames.length; ++i) {
            hostname = this.hostnames[i];
            logger.debug("Adding to {}", hostname);
            try {
                socket = this.context.socket(ZMQ.REQ);
                socket.connect(hostname);
                socket.send(request.toString(), 0);

                PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
                int rc = ZMQ.poll(items, this.requestTimeout);
                if(rc == -1) {
                    break;
                }

                if(items[0].isReadable()) {
                    // We got a reply from the server, must match sequence
                    ZMsg message = ZMsg.recvMsg(socket);
                    response = new JSONObject(new String(message.getLast().getData()));
                    result = response.getJSONObject("result");
                    socket.close();
                    return;
                } else {
                    logger.debug("Failed to get reply from {}", hostname);
                    socket.close();
                }
            } catch (final Exception e) {
                logger.error(e.getMessage());
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

        String hostname;

        JSONObject request = new JSONObject();
        JSONObject requestParams = new JSONObject();
        JSONObject response;
        JSONObject result;

        Socket socket;

        request.put("json-rpc", "2.0");
        request.put("id", "41");
        request.put("method", "delete");
        request.put("params", requestParams);
        requestParams.put("ticket-id", ticketId);

        for(int i = 0; i < this.hostnames.length; ++i) {

            hostname = this.hostnames[i];
            logger.debug("Deleting from {}", hostname);
            try {
                socket = this.context.socket(ZMQ.REQ);
                socket.connect(hostname);
                socket.send(request.toString(), 0);

                PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
                int rc = ZMQ.poll(items, this.requestTimeout);
                if(rc == -1) {
                    break;
                }

                if(items[0].isReadable()) {
                    // We got a reply from the server, must match sequence
                    ZMsg message = ZMsg.recvMsg(socket);
                    response = new JSONObject(new String(message.getLast().getData()));
                    result = response.getJSONObject("result");
                } else {
                    logger.debug("Failed to get reply from {}", hostname);
                    socket.close();
                }
            } catch (final Exception e) {
                logger.debug(e.getMessage());
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

        String hostname;
        JSONObject request = new JSONObject();
        JSONObject requestParams = new JSONObject();
        JSONObject response;
        JSONObject result;

        Socket socket;

        request.put("json-rpc", "2.0");
        request.put("id", "41");
        request.put("method", "get");
        request.put("params", requestParams);
        requestParams.put("ticket-id", ticketId);

        for(int i = 0; i < this.hostnames.length; ++i) {

            hostname = this.hostnames[i];
            logger.debug("Getting from {}", hostname);
            try {
                socket = this.context.socket(ZMQ.REQ);
                socket.connect(hostname);
                socket.send(request.toString(), 0);

                PollItem[] items = {new PollItem(socket, Poller.POLLIN)};
                int rc = ZMQ.poll(items, this.requestTimeout);
                if(rc == -1) {
                    break;
                }

                if(items[0].isReadable()) {
                    // We got a reply from the server, must match sequence
                    ZMsg message = ZMsg.recvMsg(socket);
                    response = new JSONObject(new String(message.getLast().getData()));
                    result = response.getJSONObject("result");

                    String serializedTicket = result.getString("ticket");
                    ByteArrayInputStream bi = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(serializedTicket));
                    ObjectInputStream si = new ObjectInputStream(bi);

                    Ticket ticket = (Ticket) si.readObject();
                    socket.close();
                    if(ticket != null) {
                      logger.debug("Get Ticket: {}", ticketId);
                    } else {
                      logger.debug("Failed to get Ticket: {}", ticketId);
                    }
                    if(ticket != null) {
                        return ticket;
                    }
                } else {
                    logger.debug("Failed to get reply from {}", hostname);
                    socket.close();
                }
            } catch (final Exception e) {
                logger.debug(e.getMessage());
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void destroy() throws Exception {
        this.provider.stop();
        return;
    }


    /**
     * {@inheritDoc}
     * This operation is not supported.
     *
     * @throws UnsupportedOperationException if you try and call this operation.
     */
    @Override
    public Collection<Ticket> getTickets() {
        throw new UnsupportedOperationException("GetTickets not supported.");
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    protected boolean needsCallback() {
        return true;
    }

}
