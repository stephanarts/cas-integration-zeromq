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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONObject;
import org.json.JSONException;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMsg;

/**
 * JSONRPCClient Class.
 */
public class JSONRPCClient {

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
    private Socket  socket;

    /**
     * ConnectUri.
     */
    private final String  connectUri;

    /**
     * Request ID nr.
     */
    private int           id;

    /**
     * RequestTimeout.
     */
    private final int requestTimeout;

    /**
     * PaceMaker.
     */
    private final PaceMaker pacemaker;

    /**
     * Available, indicates if the server that
     * this client connects to is available.
     *
     * It can be set by a watchdog process.
     */
    private boolean available = true;

    /**
     * Create a JSONRPCClient object.
     *
     * @param connectUri   The URI to connect to
     * @param pacemaker    Pacemaker instance
     */
    public JSONRPCClient(
            final String connectUri,
            final PaceMaker pacemaker) {

        this.context = ZMQ.context(1);

        this.connectUri = connectUri;

        this.requestTimeout = 1500;

        this.socket = this.context.socket(ZMQ.REQ);

        this.pacemaker = pacemaker;
    }

    /**
     * Create a JSONRPCClient object.
     *
     * @param connectUri   The URI to connect to
     */
    public JSONRPCClient(
            final String connectUri) {
        this(connectUri, null);
    }

    /**
     * Connect.
     */
    public final void connect() {

        this.socket.connect(this.connectUri);

        if (this.pacemaker != null) {
            this.pacemaker.addClient(this);
        }
    }

    /**
     * Disconnect.
     */
    public final void disconnect() {
        this.socket.close();

        if (this.pacemaker != null) {
            this.pacemaker.removeClient(this);
        }
    }

    /**
     * Call a JSON-RPC method.
     *
     * @param method      String containing method-name
     * @param params      JSONObject containing call-parameters.
     *
     * @return            JSONObject containing call-result
     *
     * @throws JSONRPCException Throws JSONRPCException if an error occurs
     *                          these can be related to JSONRPC, or the application.
     */
    public final JSONObject call(
            final String method,
            final JSONObject params)
            throws JSONRPCException {

        JSONObject request = new JSONObject();
        JSONObject error;
        JSONObject result;
        JSONObject response;
        this.id++;

        request.put("jsonrpc", "2.0");
        request.put("id", this.id);
        request.put("method", method);
        request.put("params", params);

        logger.trace("Sending data...");

        synchronized(this.socket) {
            this.socket.send(request.toString(), 0);

            PollItem[] items = {new PollItem(this.socket, Poller.POLLIN)};

            logger.trace("Waiting for response data...");
            int rc = ZMQ.poll(items, this.requestTimeout);
            if(rc == -1) {
                throw new JSONRPCException(-32603, "Internal error");
            }

            if(items[0].isReadable()) {
                // We got a reply from the server, must match sequence
                ZMsg message = ZMsg.recvMsg(socket);

                try {
                    response = new JSONObject(new String(message.getLast().getData()));
                } catch(final JSONException e) {
                    throw new JSONRPCException(-32700, "Parse error");
                }
                if (response.has("result")) {
                    result = response.getJSONObject("result");
                    return result;
                }
                if (response.has("error")) {
                    error = response.getJSONObject("error");
                    throw new JSONRPCException(error.getInt("code"), error.getString("message"));
                }
                throw new JSONRPCException(-32603, "Internal error");
            } else {
                logger.debug("Failed to get reply from {}", this.connectUri);

                cleanup();
                connect();
            }
        }

        throw new JSONRPCException(-32300, "Request Timeout");
    }

    /**
     * Cleanup.
     */
    private void cleanup() {
        this.socket.setLinger(0);
        this.socket.close();
        this.socket = this.context.socket(ZMQ.REQ);
    }

    /**
     * Return the connectURI.
     *
     * @return ConnectURI of configured end-point.
     */
    public final String getConnectURI() {
        return this.connectUri;
    }

    /**
     * Return if the server is available.
     *
     * @return availability of peer
     */
    public final boolean getAvailable() {
        return this.available;
    }

    /**
     * Set if the server is available.
     *
     * @param available availability of peer,
     *                  should be used by watchdog thread.
     */
    public final void setAvailable(final boolean available) {
        if (this.available != available) {
            logger.warn(
                    "Server: '"
                    +this.connectUri
                    +"' - available: "
                    +available);
        }
        this.available = available;
    }

    /**
     * destroy.
     */
    public final void destroy() {
        this.disconnect();
        this.socket = null;
        this.context.close();
    }

}
