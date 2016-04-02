/**
 *  Copyright 2005-2016 Red Hat, Inc.
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

package io.fabric8.cxf.endpoint.jaxrs;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;


@XmlRootElement(name = "Book", namespace = "http://www.example.org/books")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Book", propOrder = {"name", "id" })
public class Book2 {
    @XmlElement(name = "name", namespace = "http://www.example.org/books")
    private String name;
    @XmlElement(name = "id", namespace = "http://www.example.org/books")
    private long id;
    
    @Context
    @XmlTransient
    private UriInfo uriInfo; 
    
    @BeanParam
    @XmlTransient
    private QueryBean2 queryBean;
    
    public Book2() {
    }
    
    public Book2(String name, long id) {
        this.name = name;
        this.id = id;
    }
    
    public void setName(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }
    
    public void setId(long i) {
        id = i;
    }
    public long getId() {
        return id;
    }
    
    @GET
    @Path("rc")
    @Produces("application/xml")
    public Book2 initFromContext() {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        id = Long.valueOf(params.getFirst("bookid"));
        name = params.getFirst("bookname");
        return this;
    }
    
    @GET
    @Path("rc/bean")
    @Produces("application/xml")
    public Book2 initFromQueryBean(@BeanParam QueryBean bean) {
        id = bean.getBookid();
        name = bean.getBookname();
        return this;
    }
    
    @GET
    @Path("rc/bean2")
    @Produces("application/xml")
    public Book2 initFromQueryBean2() {
        id = queryBean.getBookid();
        name = queryBean.getBookname();
        return this;
    }

    public static class QueryBean {
        private long id;
        private String name;

        public long getBookid() {
            return id;
        }

        @QueryParam("bookid")
        public void setBookid(long i) {
            this.id = i;
        }
        
        public String getBookname() {
            return name;
        }

        @QueryParam("bookname")
        public void setBookname(String bookname) {
            this.name = bookname;
        }
        
    }
    
    public static class QueryBean2 extends QueryBean {
        
    }
}
