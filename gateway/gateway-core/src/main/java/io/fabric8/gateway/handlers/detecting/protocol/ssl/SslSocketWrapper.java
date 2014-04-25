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
package io.fabric8.gateway.handlers.detecting.protocol.ssl;

import io.fabric8.gateway.SocketWrapper;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;
import org.vertx.java.core.streams.WriteStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

/**
 */
public class SslSocketWrapper extends SocketWrapper implements ReadStream<SslSocketWrapper>, WriteStream<SslSocketWrapper> {

    private Handler<Void> plainDrainHandler;

    public enum ClientAuth {
        WANT, NEED, NONE
    };

    final private SocketWrapper next;

    private SSLEngine engine;
    private Handler<Throwable> plainExceptionHandler;
    private boolean failed = false;

    //////////////////////////////////////////////////////////////////////////
    //
    // ReadStream<SslSocketWrapper> interface impl.
    //
    //////////////////////////////////////////////////////////////////////////
    private Buffer encryptedReadBuffer;
    private boolean encryptedReadBufferUnderflow;
    private boolean encryptedReadEOF = false;
    private Buffer plainReadBuffer;
    private Handler<Void> plainEndHandler;
    private Handler<Buffer> plainDataHandler;
    private int readPaused = 0;

    public void putBackHeader(Buffer buffer) {
        if( engine!=null ) {
            throw new IllegalStateException("putBackHeader must be called before init");
        }
        encryptedReadBuffer = buffer;
    }

    private void pumpReads() {
        pumpReads(true);

    }
    private void pumpReads(boolean allowHandshake) {
        boolean pump = true;
        while( pump ) {
            pump = false;

            if( readPaused > 0 || failed ) {
                return;
            }

            if( encryptedReadBuffer!=null && plainReadBuffer==null && !encryptedReadBufferUnderflow ) {
                ByteBuffer input = ByteBuffer.wrap(encryptedReadBuffer.getBytes());
                ByteBuffer output = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());

                try {
                    boolean done = false;
                    while( !done ) {
                        done = true;

                        SSLEngineResult result = engine.unwrap(input, output);
                        switch( result.getStatus() ) {
                            case CLOSED:
                                engine.closeInbound();
                                break;
                            case BUFFER_UNDERFLOW:
                                encryptedReadBufferUnderflow = true;
                                break;
                            case OK:
                                switch(engine.getHandshakeStatus()) {
                                    case NEED_TASK:
                                    case NEED_WRAP:
                                        break;
                                    default:
                                        done = !input.hasRemaining();
                                }
                                break;
                            case BUFFER_OVERFLOW:
                                throw new SSLException("BUFFER_OVERFLOW");
                        }

                        // Lets fill the plain buffer..
                        output.flip();
                        if( output.remaining() > 0 ) {
                            pump = true;
                            int len = output.remaining();
                            if( plainReadBuffer == null ) {
                                plainReadBuffer = new Buffer(len);
                            }
                            plainReadBuffer.appendBytes(output.array(), output.arrayOffset()+ output.position(), len);
                        }
                        output.clear();

                    }
                } catch (SSLException e) {
                    onFailure(e);
                    return;
                } finally {
                    int len = input.remaining();
                    if( len > 0 ) {
                        // we need to compact the encryptedReadBuffer
                        if( input.position()!=0  ) {
                            encryptedReadBuffer = new Buffer(len);
                            encryptedReadBuffer.appendBytes(input.array(), input.arrayOffset()+ input.position(), len);
                        }
                    } else {
                        // everything was consumed.
                        encryptedReadBuffer = null;
                    }
                }
            }

            // Send the plain buffer to the the data handler...
            if( plainReadBuffer !=null && readPaused==0 ) {
                pump = true;
                Buffer data = plainReadBuffer;
                plainReadBuffer = null;
                Handler<Buffer> handler = plainDataHandler;
                if( handler !=null ) {
                    handler.handle(data);
                }
            }

            if( encryptedReadBuffer==null && plainReadBuffer==null && encryptedReadEOF ) {
                encryptedReadEOF = false;
                Handler<Void> handler = plainEndHandler;
                if( handler !=null ) {
                    handler.handle(null);
                }
            }

            if (engine.getHandshakeStatus()!=NOT_HANDSHAKING ) {
                if( allowHandshake ) {
                    handshake();
                }
                return;
            }
        }
    }

    @Override
    public SslSocketWrapper dataHandler(Handler<Buffer> handler) {
        if( plainDataHandler!=null && handler == null ) {
            pause();
        }
        boolean needsResume = plainDataHandler==null && handler!=null;
        plainDataHandler = handler;
        if(needsResume) {
            resume();
        }
        return this;
    }

    @Override
    public SslSocketWrapper endHandler(Handler<Void> voidHandler) {
        plainEndHandler = voidHandler;
        return this;
    }

    @Override
    public SslSocketWrapper pause() {
        readPaused ++;
        if( readPaused == 1 ) {
            next.readStream().pause();
        }
        return this;
    }

    @Override
    public SslSocketWrapper resume() {
        readPaused --;
        if( readPaused ==0 ) {
            next.readStream().resume();
        }
        pumpReads();
        return this;
    }

    @Override
    public SslSocketWrapper exceptionHandler(Handler<Throwable> throwableHandler) {
        plainExceptionHandler = throwableHandler;
        return this;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // WriteStream<SslSocketWrapper> interface impl.
    //
    //////////////////////////////////////////////////////////////////////////

    private boolean writeOverflow;
    private Buffer plainWriteBuffer;
    private Buffer encryptedWriteBuffer;

    @Override
    public SslSocketWrapper drainHandler(Handler<Void> voidHandler) {
        plainDrainHandler = voidHandler;
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return plainWriteBuffer != null;
    }

    @Override
    public SslSocketWrapper write(Buffer buffer) {
        if( plainWriteBuffer==null ) {
            plainWriteBuffer = buffer;
        } else {
            plainWriteBuffer.appendBuffer(buffer);
        }
        pumpWrites();
        return this;
    }

    @Override
    public SslSocketWrapper setWriteQueueMaxSize(int i) {
        return this;
    }

    private final Handler<Void> drainHandler = new Handler<Void>() {
        @Override
        public void handle(Void aVoid) {
            writeOverflow = false;
            pumpWrites();
        }
    };

    private void pumpWrites() {
        pumpWrites(true);
    }

    private void pumpWrites(boolean allowHandshake) {
        boolean pump = true;
        while (pump) {
            pump= false;

            if( failed ) {
                return;
            }

            if( plainWriteBuffer!=null ) {
                ByteBuffer input = ByteBuffer.wrap(plainWriteBuffer.getBytes());
                ByteBuffer output = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());

                try {
                    boolean done = false;
                    while( !done ) {
                        done = true;
                        SSLEngineResult result = engine.wrap(input, output);
                        switch( result.getStatus() ) {
                            case OK:
                                switch(engine.getHandshakeStatus()) {
                                    case NEED_TASK:
                                    case NEED_UNWRAP:
                                        break;
                                    default:
                                        done = !input.hasRemaining();
                                }
                                break;
                            case CLOSED:
                                throw new SSLException("CLOSED");
                            case BUFFER_UNDERFLOW:
                                break;
                            case BUFFER_OVERFLOW:
                                done = false;
                        }

                        // Lets fill the plain buffer..
                        output.flip();
                        int len = output.remaining();
                        if( len > 0 ) {
                            pump = true;
                            if( encryptedWriteBuffer == null ) {
                                encryptedWriteBuffer = new Buffer(len);
                            }
                            encryptedWriteBuffer.appendBytes(output.array(), output.arrayOffset()+ output.position(), len);
                        }
                        output.clear();
                    }
                } catch (SSLException e) {
                   onFailure(e);
                   return;
                } finally {
                    int len = input.remaining();
                    if( len > 0 ) {
                        // we need to compact the plainWriteBuffer
                        if( input.position()!=0  ) {
                            plainWriteBuffer = new Buffer(len);
                            plainWriteBuffer.appendBytes(input.array(), input.arrayOffset()+ input.position(), len);
                        }
                    } else {
                        // everything was consumed.
                        plainWriteBuffer = null;
                    }
                }
            }

            if( encryptedWriteBuffer !=null && !writeOverflow ) {
                if( next.writeStream().writeQueueFull() ) {
                    writeOverflow = true;
                    next.writeStream().drainHandler(drainHandler);
                } else {
                    pump = true;
                    Buffer data = encryptedWriteBuffer;
                    encryptedWriteBuffer = null;
                    next.writeStream().write(data);
                }
            }

            if (engine.getHandshakeStatus()!=NOT_HANDSHAKING ) {
                if( allowHandshake ) {
                    handshake();
                }
                return;
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // SocketWrapper interface impl.
    //
    //////////////////////////////////////////////////////////////////////////

    @Override
    public ReadStream readStream() {
        return this;
    }
    @Override
    public WriteStream writeStream() {
        return this;
    }

    @Override
    public void close() {
        next.close();
    }

    @Override
    public InetSocketAddress localAddress() {
        return next.localAddress();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return next.remoteAddress();
    }

    @Override
    public Object stream() {
        return this;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // SslSocketWrapper public interface impl:
    //
    //////////////////////////////////////////////////////////////////////////

    public SslSocketWrapper(SocketWrapper plainWrapper) {
        this.next = plainWrapper;
        pause();
    }

    public void initClient(SSLContext sslContext, String host, int port, String disabledCypherSuites, String enabledCipherSuites) {
        assert engine == null;
        engine = sslContext.createSSLEngine(host, port);
        engine.setUseClientMode(true);
        initCipherSuites(disabledCypherSuites, enabledCipherSuites);
        init();
    }

    public void initServer(SSLContext sslContext, ClientAuth clientAuth, String disabledCypherSuites, String enabledCipherSuites) {
        assert engine == null;
        engine = sslContext.createSSLEngine();
        engine.setUseClientMode(false);
        switch (clientAuth) {
            case WANT: engine.setWantClientAuth(true); break;
            case NEED: engine.setNeedClientAuth(true); break;
            case NONE: engine.setWantClientAuth(false); break;
        }
        initCipherSuites(disabledCypherSuites, enabledCipherSuites);
        init();
    }

    private void initCipherSuites(String disabledCypherSuites, String enabledCipherSuites) {
        if (enabledCipherSuites != null) {
            engine.setEnabledCipherSuites(splitOnCommas(enabledCipherSuites));
        } else {
            engine.setEnabledCipherSuites(engine.getSupportedCipherSuites());
        }

        if( disabledCypherSuites!=null ) {
            String[] disabledList = splitOnCommas(disabledCypherSuites);
            ArrayList<String> enabled = new ArrayList<String>();
            for (String suite : engine.getEnabledCipherSuites()) {
                boolean add = true;
                for (String disabled : disabledList) {
                    if( suite.contains(disabled) ) {
                        add = false;
                        break;
                    }
                }
                if( add ) {
                    enabled.add(suite);
                }
            }
            engine.setEnabledCipherSuites(enabled.toArray(new String[enabled.size()]));
        }
    }

    private void init() {
        this.next.readStream().dataHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                if( encryptedReadBuffer == null ) {
                    encryptedReadBuffer = buffer;
                } else {
                    encryptedReadBuffer.appendBuffer(buffer);
                }
                encryptedReadBufferUnderflow = false;
                pumpReads();
            }
        });
        this.next.readStream().endHandler(new Handler<Void>() {
            @Override
            public void handle(Void x) {
                encryptedReadEOF = true;
            }
        });
        this.next.readStream().exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable error) {
                onFailure(error);
            }
        });
    }

    static private String[] splitOnCommas(String value) {
        ArrayList<String> rc = new ArrayList<String>();
        for( String x : value.split(",") ) {
            rc.add(x.trim());
        }
        return rc.toArray(new String[rc.size()]);
    }

    public void handshake() {
        if( failed )
            return;
        try {
            while( true ) {
                SSLEngineResult.HandshakeStatus status = engine.getHandshakeStatus();
                switch (status) {
                    case FINISHED:
                    case NOT_HANDSHAKING:
                        return;

                    case NEED_TASK:
                        final Runnable task = engine.getDelegatedTask();
                        if( task!=null ) {
                            task.run();
                        }
                        break;

                    case NEED_WRAP:
                        if( plainWriteBuffer==null ) {
                            plainWriteBuffer = new Buffer();
                        }
                        pumpWrites(false);
                        break;

                    case NEED_UNWRAP:
                        if( encryptedReadBuffer!=null ) {
                            pumpReads(false);
                            break;
                        } else {
                            return;
                        }

                    default:
                        System.err.println("Unexpected ssl engine handshake status: "+ status);
                        break;
                }
            }
        } finally {
            SSLEngineResult.HandshakeStatus status = engine.getHandshakeStatus();
            if( status == NOT_HANDSHAKING ) {
                pumpWrites(false);
                pumpReads(false);
            }
        }
    }

    private void onFailure(Throwable error) {
        failed = true;
        Handler<Throwable> handler = plainExceptionHandler;
        if( handler!=null ) {
            handler.handle(error);
        }
    }

}
