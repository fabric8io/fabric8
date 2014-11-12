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
package io.fabric8.gateway.fabric.support.vertx;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.TestControl;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;

@TestControl(startScopes = SessionScoped.class)
@RunWith(CdiTestRunner.class)
public class VertxServiceTest {

    
    @Inject VertxService vertxService;
    
    @Test
    public void VertxServiceExistsTest() {
        Assert.assertNotNull(vertxService);
        Assert.assertEquals(VertxServiceImpl.class.getName(), vertxService.getClass().getSuperclass().getName());
    }
    
    @Test
    public void VertxExistsTest() {
        final Vertx vertx = vertxService.getVertx();
        Assert.assertNotNull(vertx);
        System.out.println("test");
        
        

        vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
          public void handle(final HttpServerRequest req) {
        	final HttpClient client = vertx.createHttpClient().setHost("localhost").setPort(9002);
            client.setKeepAlive(false);
            client.setPipelining(false);
        	System.out.println("Proxying request: " + req.uri());
            final HttpClientRequest cReq = client.request(req.method(), req.uri(), new Handler<HttpClientResponse>() {
              public void handle(HttpClientResponse cRes) {
                System.out.println("Proxying response: " + cRes.statusCode());
                req.response().setStatusCode(cRes.statusCode());
                MultiMap headers=cRes.headers();
                headers.set("Connection", "close");
                req.response().headers().set(headers);
                req.response().setChunked(true);
                
                cRes.dataHandler(new Handler<Buffer>() {
                  public void handle(Buffer data) {
                    System.out.println("3. Proxying response body:" + data);
                    req.response().write(data);
                  }
                });
                cRes.endHandler(new VoidHandler() {
                  public void handle() {
                	  System.out.println("4 end");
                    req.response().end();
                  }
                });
              }
            });
            MultiMap headers=req.headers();
            headers.set("Connection", "close");
            cReq.headers().set(headers);
            cReq.setChunked(true);
            req.dataHandler(new Handler<Buffer>() {
              public void handle(Buffer data) {
                System.out.println("1. Proxying request body:" + data);
                cReq.write(data);
              }
            });
            req.endHandler(new VoidHandler() {
              public void handle() {
                System.out.println("2. end of the request");
                cReq.end();
              }
            });
          }
        }).listen(8080);
        System.out.println("end");
    }
    
    
}
