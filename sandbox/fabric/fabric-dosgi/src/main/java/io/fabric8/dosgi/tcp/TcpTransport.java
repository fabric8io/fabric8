/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.fabric8.dosgi.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.fabric8.dosgi.io.ProtocolCodec;
import io.fabric8.dosgi.io.Transport;
import io.fabric8.dosgi.io.TransportListener;
import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.DispatchSource;
import org.fusesource.hawtdispatch.Retained;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpTransport implements Transport {

    private static final Logger LOG = LoggerFactory.getLogger(TcpTransport.class);

    protected State _serviceState = CREATED;

    protected Map<String, Object> socketOptions;

    public static class State {
        public String toString() {
            return getClass().getSimpleName();
        }
        public boolean isStarted() {
            return false;
        }
    }

    static class CallbackSupport extends State {
        LinkedList<Runnable> callbacks = new LinkedList<Runnable>();

        void add(Runnable r) {
            if (r != null) {
                callbacks.add(r);
            }
        }

        void done() {
            for (Runnable callback : callbacks) {
                callback.run();
            }
        }
    }

    abstract static class SocketState {
        void onStop(Runnable onCompleted) {
        }
        void onCanceled() {
        }
        boolean is(Class<? extends SocketState> clazz) {
            return getClass()==clazz;
        }
    }

    public static final State CREATED = new State();

    public static class STARTING extends CallbackSupport {
    }

    public static final State STARTED = new State() {
        public boolean isStarted() {
            return true;
        }
    };
    public static class STOPPING extends CallbackSupport {
    }

    public static final State STOPPED = new State();


    final public void start() {
        start(null);
    }

    final public void stop() {
        stop(null);
    }

    final public void start(final Runnable onCompleted) {
        queue().execute(new Runnable() {
            public void run() {
                if (_serviceState == CREATED ||
                        _serviceState == STOPPED) {
                    final STARTING state = new STARTING();
                    state.add(onCompleted);
                    _serviceState = state;
                    _start(new Runnable() {
                        public void run() {
                            _serviceState = STARTED;
                            state.done();
                        }
                    });
                } else if (_serviceState instanceof STARTING) {
                    ((STARTING) _serviceState).add(onCompleted);
                } else if (_serviceState == STARTED) {
                    if (onCompleted != null) {
                        onCompleted.run();
                    }
                } else {
                    if (onCompleted != null) {
                        onCompleted.run();
                    }
                    LOG.error("start should not be called from state: " + _serviceState);
                }
            }
        });
    }

    final public void stop(final Runnable onCompleted) {
        queue().execute(new Runnable() {
            public void run() {
                if (_serviceState == STARTED) {
                    final STOPPING state = new STOPPING();
                    state.add(onCompleted);
                    _serviceState = state;
                    _stop(new Runnable() {
                        public void run() {
                            _serviceState = STOPPED;
                            state.done();
                        }
                    });
                } else if (_serviceState instanceof STOPPING) {
                    ((STOPPING) _serviceState).add(onCompleted);
                } else if (_serviceState == STOPPED) {
                    if (onCompleted != null) {
                        onCompleted.run();
                    }
                } else {
                    if (onCompleted != null) {
                        onCompleted.run();
                    }
                    LOG.error("stop should not be called from state: " + _serviceState);
                }
            }
        });
    }

    protected State getServiceState() {
        return _serviceState;
    }

    static class DISCONNECTED extends SocketState{}

    class CONNECTING extends SocketState{
        void onStop(Runnable onCompleted) {
            trace("CONNECTING.onStop");
            CANCELING state = new CANCELING();
            socketState = state;
            state.onStop(onCompleted);
        }
        void onCanceled() {
            trace("CONNECTING.onCanceled");
            CANCELING state = new CANCELING();
            socketState = state;
            state.onCanceled();
        }
    }

    class CONNECTED extends SocketState {
        void onStop(Runnable onCompleted) {
            trace("CONNECTED.onStop");
            CANCELING state = new CANCELING();
            socketState = state;
            state.add(createDisconnectTask());
            state.onStop(onCompleted);
        }
        void onCanceled() {
            trace("CONNECTED.onCanceled");
            CANCELING state = new CANCELING();
            socketState = state;
            state.add(createDisconnectTask());
            state.onCanceled();
        }
        Runnable createDisconnectTask() {
            return new Runnable(){
                public void run() {
                    listener.onTransportDisconnected(TcpTransport.this);
                }
            };
        }
    }

    class CANCELING extends SocketState {
        private LinkedList<Runnable> runnables =  new LinkedList<Runnable>();
        private int remaining;
        private boolean dispose;

        public CANCELING() {
            if( readSource!=null ) {
                remaining++;
                readSource.cancel();
            }
            if( writeSource!=null ) {
                remaining++;
                writeSource.cancel();
            }
        }
        void onStop(Runnable onCompleted) {
            trace("CANCELING.onCompleted");
            add(onCompleted);
            dispose = true;
        }
        void add(Runnable onCompleted) {
            if( onCompleted!=null ) {
                runnables.add(onCompleted);
            }
        }
        void onCanceled() {
            trace("CANCELING.onCanceled");
            remaining--;
            if( remaining!=0 ) {
                return;
            }
            try {
                channel.close();
            } catch (IOException ignore) {
            }
            socketState = new CANCELED(dispose);
            for (Runnable runnable : runnables) {
                runnable.run();
            }
            if (dispose) {
                dispose();
            }
        }
    }

    class CANCELED extends SocketState {
        private boolean disposed;

        public CANCELED(boolean disposed) {
            this.disposed=disposed;
        }

        void onStop(Runnable onCompleted) {
            trace("CANCELED.onStop");
            if( !disposed ) {
                disposed = true;
                dispose();
            }
            onCompleted.run();
        }
    }

    protected URI remoteLocation;
    protected URI localLocation;
    protected TransportListener listener;
    protected String remoteAddress;
    protected ProtocolCodec codec;

    protected SocketChannel channel;

    protected SocketState socketState = new DISCONNECTED();

    protected DispatchQueue dispatchQueue;
    private DispatchSource readSource;
    private DispatchSource writeSource;

    protected boolean useLocalHost = true;

    int max_read_rate;
    int max_write_rate;
    protected RateLimitingChannel rateLimitingChannel;

    class RateLimitingChannel implements ReadableByteChannel, WritableByteChannel {

        int read_allowance = max_read_rate;
        boolean read_suspended = false;
        int read_resume_counter = 0;
        int write_allowance = max_write_rate;
        boolean write_suspended = false;

        public void resetAllowance() {
            if( read_allowance != max_read_rate || write_allowance != max_write_rate) {
                read_allowance = max_read_rate;
                write_allowance = max_write_rate;
                if( write_suspended ) {
                    write_suspended = false;
                    resumeWrite();
                }
                if( read_suspended ) {
                    read_suspended = false;
                    resumeRead();
                    for( int i=0; i < read_resume_counter ; i++ ) {
                        resumeRead();
                    }
                }
            }
        }

        public int read(ByteBuffer dst) throws IOException {
            if( max_read_rate==0 ) {
                return channel.read(dst);
            } else {
                int remaining = dst.remaining();
                if( read_allowance ==0 || remaining ==0 ) {
                    return 0;
                }

                int reduction = 0;
                if( remaining > read_allowance) {
                    reduction = remaining - read_allowance;
                    dst.limit(dst.limit() - reduction);
                }
                int rc=0;
                try {
                    rc = channel.read(dst);
                    read_allowance -= rc;
                } finally {
                    if( reduction!=0 ) {
                        if( dst.remaining() == 0 ) {
                            // we need to suspend the read now until we get
                            // a new allowance..
                            readSource.suspend();
                            read_suspended = true;
                        }
                        dst.limit(dst.limit() + reduction);
                    }
                }
                return rc;
            }
        }

        public int write(ByteBuffer src) throws IOException {
            if( max_write_rate==0 ) {
                return channel.write(src);
            } else {
                int remaining = src.remaining();
                if( write_allowance ==0 || remaining ==0 ) {
                    return 0;
                }

                int reduction = 0;
                if( remaining > write_allowance) {
                    reduction = remaining - write_allowance;
                    src.limit(src.limit() - reduction);
                }
                int rc = 0;
                try {
                    rc = channel.write(src);
                    write_allowance -= rc;
                } finally {
                    if( reduction!=0 ) {
                        if( src.remaining() == 0 ) {
                            // we need to suspend the read now until we get
                            // a new allowance..
                            write_suspended = true;
                            suspendWrite();
                        }
                        src.limit(src.limit() + reduction);
                    }
                }
                return rc;
            }
        }

        public boolean isOpen() {
            return channel.isOpen();
        }

        public void close() throws IOException {
            channel.close();
        }

        public void resumeRead() {
            if( read_suspended ) {
                read_resume_counter += 1;
            } else {
                _resumeRead();
            }
        }

    }

    private final Runnable CANCEL_HANDLER = new Runnable() {
        public void run() {
            socketState.onCanceled();
        }
    };

    static final class OneWay {
        final Object command;
        final Retained retained;

        public OneWay(Object command, Retained retained) {
            this.command = command;
            this.retained = retained;
        }
    }

    public void connected(SocketChannel channel) throws IOException, Exception {
        this.channel = channel;

        if( codec !=null ) {
            initializeCodec();
        }

        this.channel.configureBlocking(false);
        this.remoteAddress = channel.socket().getRemoteSocketAddress().toString();
        channel.socket().setSoLinger(true, 0);
        channel.socket().setTcpNoDelay(true);

        this.socketState = new CONNECTED();
    }

    protected void initializeCodec() {
        codec.setReadableByteChannel(readChannel());
        codec.setWritableByteChannel(writeChannel());
    }

    public void connecting(URI remoteLocation, URI localLocation) throws IOException, Exception {
        this.channel = SocketChannel.open();
        this.channel.configureBlocking(false);
        this.remoteLocation = remoteLocation;
        this.localLocation = localLocation;

        if (localLocation != null) {
            InetSocketAddress localAddress = new InetSocketAddress(InetAddress.getByName(localLocation.getHost()), localLocation.getPort());
            channel.socket().bind(localAddress);
        }

        String host = resolveHostName(remoteLocation.getHost());
        InetSocketAddress remoteAddress = new InetSocketAddress(host, remoteLocation.getPort());
        channel.connect(remoteAddress);
        this.socketState = new CONNECTING();
    }


    public DispatchQueue queue() {
        return dispatchQueue;
    }

    public void setDispatchQueue(DispatchQueue queue) {
        this.dispatchQueue = queue;
    }

    public void _start(Runnable onCompleted) {
        try {
            if (socketState.is(CONNECTING.class) ) {
                trace("connecting...");
                // this allows the connect to complete..
                readSource = Dispatch.createSource(channel, SelectionKey.OP_CONNECT, dispatchQueue);
                readSource.setEventHandler(new Runnable() {
                    public void run() {
                        if (getServiceState() != STARTED) {
                            return;
                        }
                        try {
                            trace("connected.");
                            channel.finishConnect();
                            readSource.setCancelHandler(null);
                            readSource.cancel();
                            readSource=null;
                            socketState = new CONNECTED();
                            onConnected();
                        } catch (IOException e) {
                            onTransportFailure(e);
                        }
                    }
                });
                readSource.setCancelHandler(CANCEL_HANDLER);
                readSource.resume();

            } else if (socketState.is(CONNECTED.class) ) {
                dispatchQueue.execute(new Runnable() {
                    public void run() {
                        try {
                            trace("was connected.");
                            onConnected();
                        } catch (IOException e) {
                             onTransportFailure(e);
                        }
                    }
                });
            } else {
                System.err.println("cannot be started.  socket state is: "+socketState);
            }
        } finally {
            if( onCompleted!=null ) {
                onCompleted.run();
            }
        }
    }

    public void _stop(final Runnable onCompleted) {
        trace("stopping.. at state: "+socketState);
        socketState.onStop(onCompleted);
    }

    protected String resolveHostName(String host) throws UnknownHostException {
        String localName = InetAddress.getLocalHost().getHostName();
        if (localName != null && isUseLocalHost()) {
            if (localName.equals(host)) {
                return "localhost";
            }
        }
        return host;
    }

    protected void onConnected() throws IOException {

        readSource = Dispatch.createSource(channel, SelectionKey.OP_READ, dispatchQueue);
        writeSource = Dispatch.createSource(channel, SelectionKey.OP_WRITE, dispatchQueue);

        readSource.setCancelHandler(CANCEL_HANDLER);
        writeSource.setCancelHandler(CANCEL_HANDLER);

        readSource.setEventHandler(new Runnable() {
            public void run() {
                drainInbound();
            }
        });
        writeSource.setEventHandler(new Runnable() {
            public void run() {
                drainOutbound();
            }
        });

        if( max_read_rate!=0 || max_write_rate!=0 ) {
            rateLimitingChannel = new RateLimitingChannel();
            schedualRateAllowanceReset();
        }

        remoteAddress = channel.socket().getRemoteSocketAddress().toString();
        listener.onTransportConnected(this);
    }

    private void schedualRateAllowanceReset() {
        dispatchQueue.executeAfter(1, TimeUnit.SECONDS, new Runnable(){
            public void run() {
                if( !socketState.is(CONNECTED.class) ) {
                    return;
                }
                rateLimitingChannel.resetAllowance();
                schedualRateAllowanceReset();
            }
        });
    }

    private void dispose() {
        if( readSource!=null ) {
            readSource.cancel();
            readSource=null;
        }

        if( writeSource!=null ) {
            writeSource.cancel();
            writeSource=null;
        }
        this.codec = null;
    }

    public void onTransportFailure(IOException error) {
        listener.onTransportFailure(this, error);
        socketState.onCanceled();
    }


    public boolean full() {
        return codec.full();
    }

    public boolean offer(Object command) {
        assert Dispatch.getCurrentQueue() == dispatchQueue;
        try {
            if (!socketState.is(CONNECTED.class)) {
                throw new IOException("Not connected.");
            }
            if (getServiceState() != STARTED) {
                throw new IOException("Not running.");
            }

            ProtocolCodec.BufferState rc = codec.write(command);
            switch (rc ) {
                case FULL:
                    return false;
                default:
                    if( drained ) {
                        drained = false;
                        resumeWrite();
                    }
                    return true;
            }
        } catch (IOException e) {
            onTransportFailure(e);
            return false;
        }

    }


    boolean drained = true;
    /**
     *
     */
    protected void drainOutbound() {
        assert Dispatch.getCurrentQueue() == dispatchQueue;
        if (getServiceState() != STARTED || !socketState.is(CONNECTED.class)) {
            return;
        }
        try {
            if( codec.flush() == ProtocolCodec.BufferState.WAS_EMPTY && flush() ) {
                if( !drained ) {
                    drained = true;
                    suspendWrite();
                    listener.onRefill(this);
                }
            }
        } catch (IOException e) {
            onTransportFailure(e);
        }
    }

    protected boolean flush() throws IOException {
        return true;
    }

    protected void drainInbound() {
        if (!getServiceState().isStarted() || readSource.isSuspended()) {
            return;
        }
        try {
            long initial = codec.getReadCounter();
            // Only process upto 64k worth of data at a time so we can give
            // other connections a chance to process their requests.
            while( codec.getReadCounter()-initial < 1024*64 ) {
                Object command = codec.read();
                if ( command!=null ) {
                    try {
                        listener.onTransportCommand(this, command);
                    } catch (Throwable e) {
                        onTransportFailure(new IOException("Transport listener failure."));
                    }

                    // the transport may be suspended after processing a command.
                    if (getServiceState() == STOPPED || readSource.isSuspended()) {
                        return;
                    }
                } else {
                    return;
                }
            }
        } catch (IOException e) {
            onTransportFailure(e);
        }
    }


    public String getRemoteAddress() {
        return remoteAddress;
    }

    private boolean assertConnected() {
        try {
            if ( !isConnected() ) {
                throw new IOException("Not connected.");
            }
            return true;
        } catch (IOException e) {
            onTransportFailure(e);
        }
        return false;
    }

    public void suspendRead() {
        if( isConnected() && readSource!=null ) {
            readSource.suspend();
        }
    }


    public void resumeRead() {
        if( isConnected() && readSource!=null ) {
            if( rateLimitingChannel!=null ) {
                rateLimitingChannel.resumeRead();
            } else {
                _resumeRead();
            }
        }
    }
    private void _resumeRead() {
        readSource.resume();
        dispatchQueue.execute(new Runnable(){
            public void run() {
                drainInbound();
            }
        });
    }

    protected void suspendWrite() {
        if( isConnected() && writeSource!=null ) {
            writeSource.suspend();
        }
    }
    protected void resumeWrite() {
        if( isConnected() && writeSource!=null ) {
            writeSource.resume();
            dispatchQueue.execute(new Runnable(){
                public void run() {
                    drainOutbound();
                }
            });
        }
    }

    public TransportListener getTransportListener() {
        return listener;
    }

    public void setTransportListener(TransportListener listener) {
        this.listener = listener;
    }

    public ProtocolCodec getProtocolCodec() {
        return codec;
    }

    public void setProtocolCodec(ProtocolCodec protocolCodec) {
        this.codec = protocolCodec;
        if( channel!=null && codec!=null ) {
            initializeCodec();
        }
    }

    public boolean isConnected() {
        return socketState.is(CONNECTED.class);
    }

    public boolean isDisposed() {
        return getServiceState() == STOPPED;
    }

    public void setSocketOptions(Map<String, Object> socketOptions) {
        this.socketOptions = socketOptions;
    }

    public boolean isUseLocalHost() {
        return useLocalHost;
    }

    /**
     * Sets whether 'localhost' or the actual local host name should be used to
     * make local connections. On some operating systems such as Macs its not
     * possible to connect as the local host name so localhost is better.
     */
    public void setUseLocalHost(boolean useLocalHost) {
        this.useLocalHost = useLocalHost;
    }


    private void trace(String message) {
        if( LOG.isTraceEnabled() ) {
            final String label = dispatchQueue.getLabel();
            if( label !=null ) {
                LOG.trace(label +" | "+message);
            } else {
                LOG.trace(message);
            }
        }
    }

    public SocketChannel getSocketChannel() {
        return channel;
    }

    public ReadableByteChannel readChannel() {
        if(rateLimitingChannel!=null) {
            return rateLimitingChannel;
        } else {
            return channel;
        }
    }

    public WritableByteChannel writeChannel() {
        if(rateLimitingChannel!=null) {
            return rateLimitingChannel;
        } else {
            return channel;
        }
    }

    public int getMax_read_rate() {
        return max_read_rate;
    }

    public void setMax_read_rate(int max_read_rate) {
        this.max_read_rate = max_read_rate;
    }

    public int getMax_write_rate() {
        return max_write_rate;
    }

    public void setMax_write_rate(int max_write_rate) {
        this.max_write_rate = max_write_rate;
    }

}
