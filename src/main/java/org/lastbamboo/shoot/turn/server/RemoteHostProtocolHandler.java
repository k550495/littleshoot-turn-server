package org.lastbamboo.shoot.turn.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.shoot.protocol.ProtocolHandler;
import org.lastbamboo.shoot.turn.message.TurnMessage;
import org.lastbamboo.shoot.turn.message.TurnMessageFactory;

/**
 * This "protocol" handler simply reads data from the connecting client and
 * passes it to the TURN client.  This happens slightly differently depending
 * on whether or not the client has set the "active connection."  If the
 * client has set the active connection, any incoming data is simply forwarded
 * along without modification.  If the active connection has not been set,
 * the data is encapsulated in a TURN "data indication" message that also
 * includes the address and port the data came from.
 */
public final class RemoteHostProtocolHandler implements ProtocolHandler
    {
    
    /**
     * Logger for this class.
     */
    private static final Log LOG = 
        LogFactory.getLog(RemoteHostProtocolHandler.class);
    private final TurnClient m_turnClient;
    private final TurnMessageFactory m_turnMessageFactory;

    /**
     * Creates a new protocol handler for handling incoming data from remote 
     * clients sneding data to the TURN client.
     * @param client The TURN client to forward data to.
     * @param messageFactory The factory for creating messages.
     */
    public RemoteHostProtocolHandler(final TurnClient client, 
        final TurnMessageFactory messageFactory)
        {
        this.m_turnClient = client;
        this.m_turnMessageFactory = messageFactory;
        }

    public void handleMessages(final ByteBuffer messageBuffer, 
        final InetSocketAddress remoteAddress) throws IOException
        {
        LOG.trace("Received data from remote host: "+remoteAddress);
        
        // TODO: How can we avoid making this copy while not writing the
        // entire TCP buffer??
        messageBuffer.flip();
        final ByteBuffer dataBuffer = 
            ByteBuffer.allocate(messageBuffer.limit());
        dataBuffer.put(messageBuffer);
        dataBuffer.rewind();
        
        // If the client has set the active destination, just forward the data
        // along.
        if (this.m_turnClient.hasActiveDestination())
            {
            this.m_turnClient.getReaderWriter().write(dataBuffer);
            }
        
        // Otherwise, encapsulate the data in a "Data Indication" message.
        else
            {
            final TurnMessage dataIndication = 
                this.m_turnMessageFactory.createDataIndication(dataBuffer, 
                    remoteAddress);
            
            LOG.trace("Wraping data in Data Indication: "+dataIndication);
            this.m_turnClient.getReaderWriter().write(
                dataIndication.toByteBuffers());
            LOG.trace("Wrote data indication message...");
            }
        }
    }
