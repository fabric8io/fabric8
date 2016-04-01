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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.annotations.GZIP;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.Nullable;
import org.apache.cxf.jaxrs.ext.Oneway;
import org.apache.cxf.jaxrs.ext.xml.XMLInstruction;
import org.apache.cxf.jaxrs.ext.xml.XSISchemaLocation;
import org.apache.cxf.jaxrs.impl.ResourceContextImpl;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;

@Path("/bookstore")
@GZIP(threshold = 1)
public class BookStore {

    private Map<Long, Book> books = new HashMap<Long, Book>();
    private long bookId = 123;
    private String defaultName;
    private long defaultId;

    @PathParam("CDId")
    private String currentCdId;
    @Context
    private HttpHeaders httpHeaders;
    @Context 
    private SecurityContext securityContext;
    @Context 
    private UriInfo ui;
    @Context 
    private ResourceContext resourceContext;
    
    @BeanParam
    private BookBean theBookBean;
    
    private Book2 book2Sub = new Book2();
    
    public BookStore() {
    }
    
    @PostConstruct
    public void postConstruct() {
    }
    
    @PreDestroy
    public void preDestroy() {
    }

    @GET
    @Path("/bookarray")
    public String[] getBookStringArray() {
        return new String[]{"Good book"};
    }
    
    @GET
    @Path("/bookindexintarray")
    @Produces("text/plain")
    public int[] getBookIndexAsIntArray() {
        return new int[]{1, 2, 3};
    }
    
    @GET
    @Path("/bookindexdoublearray")
    @Produces("text/plain")
    public double[] getBookIndexAsDoubleArray() {
        return new double[]{1, 2, 3};
    }
        
    @GET
    @Path("/redirect")
    public Response getBookRedirect(@QueryParam("redirect") Boolean done,
                                    @QueryParam("sameuri") Boolean sameuri) {
        if (done == null) {
            String uri = sameuri.equals(Boolean.TRUE) 
                ? ui.getAbsolutePathBuilder().queryParam("redirect", "true").build().toString()
                : "http://otherhost/redirect";
            return Response.status(303).header("Location", uri).build();
        } else {
            return Response.ok(new Book("CXF", 123L), "application/xml").build();
        }
    }
    
    @GET
    @Path("/redirect/relative")
    public Response getBookRedirectRel(@QueryParam("redirect") Boolean done,
                                       @QueryParam("loop") boolean loop) {
        if (done == null) {
            if (loop) {
                return Response.status(303).header("Location", "relative?loop=true").build();                
            } else {
                return Response.status(303).header("Location", "relative?redirect=true").build();    
            }
        } else {
            return Response.ok(new Book("CXF", 124L), "application/xml").build();
        }
    }
    
    @GET
    @Path("/booklist")
    public List<String> getBookListArray() {
        return Collections.singletonList("Good book");
    }
    
    @GET
    @Path("/customtext")
    @Produces("text/custom")
    public String getCustomBookTest() {
        return "Good book";
    }

    @GET
    @Path("/httpresponse")
    public void getBookDesciptionHttpResponse(@Context HttpServletResponse response) {
        response.setContentType("text/plain");
        try {
            response.getOutputStream().write("Good Book".getBytes());
        } catch (IOException ex) {
            throw new WebApplicationException(ex);
        }
    }

    @DELETE
    @Path("/deletebody")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book deleteBodyBook(Book book) {
        return book;
    }
    
    @POST
    @Path("/emptyform")
    @Produces("text/plain")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String postEmptyForm(javax.ws.rs.core.Form form) {
        if (!form.asMap().isEmpty()) {
            throw new WebApplicationException(400);
        }
        return "empty form";
    }
    
    @GET
    @Path("/booknames/123")
    @Produces("application/bar")
    public byte[] getBookName123() {
        Long l = Long.parseLong("123");
        return books.get(l).getName().getBytes();
    }
    
    @GET
    @Path("/beanparam/{id}")
    @Produces("application/xml")
    public Book getBeanParamBook(@BeanParam BookBean bean) {
        long id = bean.getId() + bean.getId2() + bean.getId3();
        return books.get(id);
    }

    @GET
    @Path("/beanparam2/{id}")
    @Produces("application/xml")
    public Book getBeanParamBook2() {
        return getBeanParamBook(theBookBean);
    }
    
    @GET
    @Path("emptybook")
    @Produces({"application/xml", "application/json" })
    public Book getEmptyBook() {
        return null;
    }
    
    @GET
    @Path("emptybook/nillable")
    @Produces({"application/xml", "application/json" })
    @Nullable
    public Book getEmptyBookNullable() {
        return null;
    }
    
    @GET
    @Path("allCharsButA-B/:@!$&'()*+,;=-._~")
    public Book getWithComplexPath() {
        return new Book("Encoded Path", 125L);
    }
    
    @GET
    @Path("object")
    public Object getBookAsObject() {
        return new Book("Book as Object", 125L);
    }
    
    @GET
    @Path("/default")
    @Produces("application/xml")
    public Book getDefaultBook() {
        return new Book(defaultName, defaultId);
    }

    @POST
    @Path("emptypost")
    public void emptypost() {
        String uri = ui.getAbsolutePath().toString();
        if (uri.endsWith("/")) {
            throw new WebApplicationException(400);
        }
    }
    
    @PUT
    @Path("emptyput")
    public void emptyput() {
    }
    
    @POST
    public void emptypostNoPath() {
        emptypost();
    }
    
    @GET
    @Path("webappexception")
    public Book throwException() {
        Response response = Response.serverError().entity("This is a WebApplicationException").build();
        throw new WebApplicationException(response);
    }
    
    @GET
    @Path("webappexceptionXML")
    public Book throwExceptionXML() {
        Response response = Response.status(406).type("application/xml")
                            .entity("<Book><name>Exception</name><id>999</id></Book>")
                            .build();
        throw new WebApplicationException(response);
    }
    
    @GET
    @Path("tempredirect")
    public Response tempRedirectAndSetCookies() {
        URI uri = UriBuilder.fromPath("whatever/redirection")
            .queryParam("css1", "http://bar").build();
        return Response.temporaryRedirect(uri)
                       .header("Set-Cookie", "a=b").header("Set-Cookie", "c=d")
                       .build();
    }
    
    @GET
    @Path("setcookies")
    public Response setComplexCookies() {
        return Response.ok().header("Set-Cookie", 
                                    "bar.com.anoncart=107894933471602436; Domain=.bar.com;"
                                    + " Expires=Thu, 01-Oct-2020 23:44:22 GMT; Path=/")
                                    .build();
    }
    

    @GET
    @Path("setmanycookies")
    public Response setTwoCookies() {
        return Response.ok().header("Set-Cookie", "JSESSIONID=0475F7F30A26E5B0C15D69; Path=/")
            .header("Set-Cookie", "COOKIETWO=dummy; Expires=Sat, 20-Nov-2010 19:11:32 GMT; Path=/")
            .header("Set-Cookie", "COOKIETWO=dummy2; expires=Sat, 20-Nov-2010 19:11:32 GMT; Path=/")
            .build();
    }

    @GET
    @Path("name-in-query")
    @Produces("application/xml")
    @XMLInstruction("<!DOCTYPE Something SYSTEM 'my.dtd'><?xmlstylesheet href='common.css' ?>")
    @XSISchemaLocation("book.xsd")
    public Book getBookFromQuery(@QueryParam("name") String name) {
        return new Book(name, 321L);
    }

    @GET
    @Path("books/check/{id}")
    @Produces("text/plain")
    public boolean checkBook(@PathParam("id") Long id) {
        return books.containsKey(id);
    }
    
    @GET
    @Path("books/check/malformedmt/{id}")
    @Produces("text/plain")
    public Response checkBookMalformedMT(@PathParam("id") Long id,
                                         @Context MessageContext mc) {
        mc.put("org.apache.cxf.jaxrs.mediaTypeCheck.strict", false);
        return Response.ok(books.containsKey(id)).type("text").build();
    }
    
    @POST
    @Path("books/check2")
    @Produces("text/plain")
    @Consumes("text/plain")
    public Boolean checkBook2(Long id) {
        return books.containsKey(id);
    }

    @GET
    @Path("timetable")
    public Calendar getTimetable() {
        return new GregorianCalendar();
    }
    
    @GET
    @Path("wrongparametertype")
    public void wrongParameterType(@QueryParam("p") Map<?, ?> p) {
        throw new IllegalStateException("This op is not expected to be invoked");
    }
    
    @GET
    @Path("exceptionduringconstruction")
    public void wrongParameterType(@QueryParam("p") BadBook p) {
        throw new IllegalStateException("This op is not expected to be invoked");
    }
    
    @POST
    @Path("/unsupportedcontenttype")
    @Consumes("application/xml")
    public String unsupportedContentType() {
        throw new IllegalStateException("This op is not expected to be invoked");
    }

    @OPTIONS
    @Path("/options")
    public Response getOptions() throws Exception {
        return Response.ok().header("Allow", "POST")
                            .header("Allow", "PUT")
                            .header("Allow", "GET")
                            .header("Allow", "DELETE")
                            .build();
    }
    
    @POST
    @Path("post401")
    public Response get401WithText() throws Exception {
        return Response.status(401).entity("This is 401").build();
    }
    
    @GET
    @Path("infault")
    public Response infault() {
        throw new RuntimeException();
    }
    
    @GET
    @Path("infault2")
    public Response infault2() {
        throw new RuntimeException();
    }

    @GET
    @Path("outfault")
    public Response outfault() {
        return Response.ok().build();
    }
    
    @POST
    @Path("/collections")
    @Produces({"application/xml", "application/json" })
    @Consumes({"application/xml", "application/json" })
    public List<Book> getBookCollection(List<Book> bs) throws Exception {
        if (bs == null || bs.size() != 2) {
            throw new RuntimeException();
        }
        return bs;
    }
    
    @POST
    @Path("/collectionBook")
    @Produces({"application/xml", "application/json" })
    @Consumes({"application/xml", "application/json" })
    public List<Book> postBookGetCollection(Book book) throws Exception {
        List<Book> list = new ArrayList<Book>();
        list.add(book);
        return list;
    }
    
    @POST
    @Path("/collections3")
    @Produces({"application/xml", "application/json" })
    @Consumes({"application/xml", "application/json" })
    public Book postCollectionGetBook(List<Book> bs) throws Exception {
        if (bs == null || bs.size() != 2) {
            throw new RuntimeException();
        }
        return bs.get(0);
    }

    @GET
    @Path("/collections")
    @Produces({"application/xml", "application/json" })
    public List<Book> getBookCollection() throws Exception {
        return new ArrayList<Book>(books.values());
    }
    
    @POST
    @Path("/array")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book[] getBookArray(Book[] bs) throws Exception {
        if (bs == null || bs.length != 2) {
            throw new RuntimeException();
        }
        return bs;
    }

    @GET
    @Path("/books/buffer")
    @Produces("application/bar")
    public InputStream getBufferedBook() {
        return getClass().getResourceAsStream("resources/expected_get_book123.txt");
    }
    
    @GET
    @Path("/books/fail-early")
    @Produces("application/bar")
    public StreamingOutput failEarlyInWrite() {
        return new StreamingOutputImpl(true);
    }
    
    @GET
    @Path("/books/fail-late")
    @Produces("application/bar")
    public StreamingOutput writeToStreamAndFail() {
        return new StreamingOutputImpl(false);
    }

    @Path("/booksubresource/context")
    public Book2 getBookSubResourceRC() {
        return resourceContext.getResource(Book2.class);
    }
    
    @Path("/booksubresource/instance/context")
    public Book2 getBookSubResourceInstanceRC(@Context ResourceContext rc) {
        // This cast is temporarily
        return ((ResourceContextImpl)rc).initResource(book2Sub);
    }

    @POST
    @Path("/books/null")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book handleNullBook(@Nullable Book book) {
        if (book != null) {
            throw new WebApplicationException(400);
        }
        return new Book("Default Book", 222L);
    }
    
    @POST
    @Path("/books")
    @Produces("text/xml")
    @Consumes("application/xml")
    public Response addBook(Book book) {
        String ct1 = httpHeaders.getMediaType().toString();
        String ct2 = httpHeaders.getRequestHeader("Content-Type").get(0);
        String ct3 = httpHeaders.getRequestHeaders().getFirst("Content-Type");
        if (!("application/xml".equals(ct1) && ct1.equals(ct2) && ct1.equals(ct3))) {
            throw new RuntimeException("Unexpected content type");
        }
        
        book.setId(bookId + 1);
        return Response.ok(book).build();
    }
    
    @POST
    @Path("/books2")
    @Produces("text/xml")
    @Consumes("application/xml")
    public Book addBook2(Book book) {
        return new Book("Book echo", book.getId() + 1);
    }
    
    @POST
    @Path("/oneway")
    @Oneway
    public void onewayRequest() {
        if (!PhaseInterceptorChain.getCurrentMessage().getExchange().isOneWay()) {
            throw new WebApplicationException();
        }
    }
    
    @POST
    @Path("/books/customstatus")
    @Produces("application/xml")
    @Consumes("text/xml")
    public Book addBookCustomFailure(Book book, @Context HttpServletResponse response) {
        response.setStatus(233);
        response.addHeader("CustomHeader", "CustomValue");
        book.setId(888);
        return book;
    }
    
    @POST
    @Path("/booksinfo")
    @Produces("text/xml")
    @Consumes("application/xml")
    public Response addBook(@XmlJavaTypeAdapter(BookInfoAdapter.class) 
                            BookInfo bookInfo) {
        return Response.ok(bookInfo.asBook()).build();
    }

    @POST
    @Path("/binarybooks")
    @Produces("text/xml")
    @Consumes("application/octet-stream")
    public Response addBinaryBook(long[] book) {
        return Response.ok(book).build();
    }
    
    @PUT
    @Path("/books/")
    public Response updateBook(Book book) {
        Book b = books.get(book.getId());

        Response r;
        if (b != null) {
            r = Response.ok(book).build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }
    
    @PUT
    @Path("/books/{id}")
    public Response createBook(@PathParam("id") Long id) {
        Book b = books.get(id);

        Response r;
        if (b == null) {
            Book newBook = new Book();
            newBook.setId(id);
            r = Response.ok(newBook).build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }
    
    @PUT
    @Path("/bookswithdom/")
    public DOMSource updateBook(DOMSource ds) {
        return ds;
    }
    
    @PUT
    @Path("/bookswithjson/")
    @Consumes("application/json")
    public Response updateBookJSON(Book book) {
        Book b = books.get(book.getId());

        Response r;
        if (b != null) {
            r = Response.ok(book).build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    @DELETE
    @Path("/books/{bookId}/")
    public Response deleteBook(@PathParam("bookId") String id) {
        Book b = books.get(Long.parseLong(id));

        Response r;
        if (b != null) {
            r = Response.ok().build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }
    
    @DELETE
    @Path("/books/id")
    public Response deleteWithQuery(@QueryParam("value") @DefaultValue("-1") int id) {
        if (id != 123) {
            throw new WebApplicationException();
        }
        Book b = books.get(new Long(id));

        Response r;
        if (b != null) {
            r = Response.ok().build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    @POST
    @Path("/booksplain")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Long echoBookId(long theBookId) {
        return new Long(theBookId);
    }
    
    @POST
    @Path("/booksecho")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response echoBookNameAndHeader(@HeaderParam("CustomHeader") String headerValue, String name) {
        return Response.ok().entity(name).header("CustomHeader", headerValue).build();
    }
    
    @Path("/bookstoresub")
    public BookStore echoThroughBookStoreSub() {
        return this;
    }

    @POST
    @Path("/booksecho2")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response echoBookNameAndHeader2(String name) {
        return echoBookNameAndHeader(httpHeaders.getRequestHeader("CustomHeader").get(0), name);
    }
    
    @POST
    @Path("/booksecho3")
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response echoBookNameAndHeader3(String name) {
        return echoBookNameAndHeader(httpHeaders.getRequestHeader("customheader").get(0), name);
    }

    @GET
    @Path("quotedheaders")
    public Response getQuotedHeader() {
        return Response.
                ok().
                header("SomeHeader1", "\"some text, some more text\"").
                header("SomeHeader2", "\"some text\"").
                header("SomeHeader2", "\"quoted,text\"").
                header("SomeHeader2", "\"even more text\"").
                header("SomeHeader3", "\"some text, some more text with inlined \\\"\"").
                header("SomeHeader4", "\"\"").
                build();
    }

    @GET
    @Path("badlyquotedheaders")
    public Response getBadlyQuotedHeader(@QueryParam("type")int t) {
        Response.ResponseBuilder rb = Response.ok();
        switch(t) {
        case 0:
            // problem: no trailing quote - doesn't trigger AbstractClient.parseQuotedHeaderValue
            rb.header("SomeHeader0", "\"some text");
            break;
        case 1:
            // problem: text doesn't end with " - triggers AbstractClient.parseQuotedHeaderValue
            rb.header("SomeHeader1", "\"some text, some more text with inlined \\\"");
            break;
        case 2:
            // problem: no character after \ - doesn't trigger AbstractClient.parseQuotedHeaderValue
            rb.header("SomeHeader2", "\"some te\\");
            break;
        case 3:
            // problem: mix of plain text and quoted text in same line - doesn't trigger
            // AbstractClient.parseQuotedHeaderValue
            rb.header("SomeHeader3", "some text").header("SomeHeader3", "\"other quoted\", text").
                header("SomeHeader3", "blah");
            break;
        default:
            throw new RuntimeException("Don't know how to handle type: " + t);
        }
        return rb.build();
    }

    @XmlJavaTypeAdapter(BookInfoAdapter2.class)
    interface BookInfoInterface {
        String getName();
        
        long getId();
    }
    
    static class BookInfo {
        private String name;
        private long id;
        
        public BookInfo() {
            
        }
        
        public BookInfo(Book b) {
            this.name = b.getName();
            this.id = b.getId();
            if (id == 0) {
                id = 124;
            }
        }
        
        public String getName() {
            return name;
        }
        
        public long getId() {
            return id;
        }
       
        public Book asBook() {
            Book b = new Book();
            b.setId(id);
            b.setName(name);
            return b;
        }
    }
    
    static class BookInfo2 extends BookInfo implements BookInfoInterface {
        public BookInfo2() {
            
        }
        
        public BookInfo2(Book b) {
            super(b);
        }
    }
        
    public static class BookInfoAdapter2 extends XmlAdapter<Book, BookInfo2> {
        @Override
        public Book marshal(BookInfo2 v) throws Exception {
            return new Book(v.getName(), v.getId());
        }

        @Override
        public BookInfo2 unmarshal(Book b) throws Exception {
            return new BookInfo2(b);
        }
    }
    
    public static class BookInfoAdapter extends XmlAdapter<Book, BookInfo> {

        @Override
        public Book marshal(BookInfo v) throws Exception {
            return new Book(v.getName(), v.getId());
        }

        @Override
        public BookInfo unmarshal(Book b) throws Exception {
            return new BookInfo(b);
        }
        
    }
    
    static class BadBook {
        public BadBook(String s) {
            throw new RuntimeException("The bad book");
        }
    }
    
    private static class StreamingOutputImpl implements StreamingOutput {

        private boolean failEarly;
        
        public StreamingOutputImpl(boolean failEarly) {
            this.failEarly = failEarly;
        }
        
        public void write(OutputStream output) throws IOException, WebApplicationException {
            if (failEarly) {
                throw new WebApplicationException(
                     Response.status(410).header("content-type", "text/plain")
                     .entity("This is supposed to go on the wire").build());
            } else {
                output.write("This is not supposed to go on the wire".getBytes());
                throw new WebApplicationException(410);
            }
        } 
        
    }

    public static class BookBean {
        private long id;
        private long id2;
        private long id3;

        public long getId() {
            return id;
        }

        @PathParam("id")
        public void setId(long id) {
            this.id = id;
        }
        
        public long getId2() {
            return id2;
        }

        @QueryParam("id2")
        public void setId2(long id2) {
            this.id2 = id2;
        }
        
        @Context
        public void setUriInfo(UriInfo ui) {
            String id3Value = ui.getQueryParameters().getFirst("id3");
            if (id3Value != null) {
                this.id3 = Long.valueOf(id3Value);
            }
        }

        public long getId3() {
            return id3;
        }

    }
    
    public static class BookNotReturnedException extends RuntimeException {

        private static final long serialVersionUID = 4935423670510083220L;

        public BookNotReturnedException(String errorMessage) {
            super(errorMessage);
        }
        
    }

    public static class StringArrayBodyReaderWriter 
        implements MessageBodyReader<String[]>, MessageBodyWriter<String[]> {
        public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return String[].class.isAssignableFrom(arg0);
        }

        public String[] readFrom(Class<String[]> arg0, Type arg1,
            Annotation[] arg2, MediaType arg3, MultivaluedMap<String, String> arg4, InputStream arg5)
            throws IOException, WebApplicationException {
            return new String[] {IOUtils.readStringFromStream(arg5)};
        }

        public long getSize(String[] arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
            return -1;
        }

        public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return String[].class.isAssignableFrom(arg0);
        }

        public void writeTo(String[] arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
                            MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException,
            WebApplicationException {
            arg6.write(arg0[0].getBytes());
        }

    }
        
    public static class StringListBodyReaderWriter 
        implements MessageBodyReader<List<String>>, MessageBodyWriter<List<String>> {
        public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return List.class.isAssignableFrom(arg0) 
                && String.class == InjectionUtils.getActualType(arg1);
        }

        public List<String> readFrom(Class<List<String>> arg0, Type arg1,
            Annotation[] arg2, MediaType arg3, MultivaluedMap<String, String> arg4, InputStream arg5)
            throws IOException, WebApplicationException {
            return Collections.singletonList(IOUtils.readStringFromStream(arg5));
        }

        public long getSize(List<String> arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
            return -1;
        }

        public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return List.class.isAssignableFrom(arg0) 
                && String.class == InjectionUtils.getActualType(arg1);
        }

        public void writeTo(List<String> arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
                            MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException,
            WebApplicationException {
            arg6.write(arg0.get(0).getBytes());
        }

    }
    
    public static class PrimitiveIntArrayReaderWriter 
        implements MessageBodyReader<int[]>, MessageBodyWriter<int[]> {
        public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return int[].class.isAssignableFrom(arg0);
        }
    
        public int[] readFrom(Class<int[]> arg0, Type arg1,
            Annotation[] arg2, MediaType arg3, MultivaluedMap<String, String> arg4, InputStream arg5)
            throws IOException, WebApplicationException {
            String[] stringArr = IOUtils.readStringFromStream(arg5).split(",");
            int[] intArr = new int[stringArr.length];
            for (int i = 0; i < stringArr.length; i++) {
                intArr[i] = Integer.valueOf(stringArr[i]);
            }
            return intArr;
            
        }
    
        public long getSize(int[] arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
            return -1;
        }
    
        public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return int[].class.isAssignableFrom(arg0);
        }
    
        public void writeTo(int[] arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
                            MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException,
            WebApplicationException {
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arg0.length; i++) {
                sb.append(Integer.toString(arg0[i]));
                if (i + 1 < arg0.length) {
                    sb.append(",");
                }
            }
            arg6.write(sb.toString().getBytes());
        }
    
    }
    public static class PrimitiveDoubleArrayReaderWriter 
        implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
        public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return double[].class.isAssignableFrom(arg0);
        }
    
        public Object readFrom(Class<Object> arg0, Type arg1,
            Annotation[] arg2, MediaType arg3, MultivaluedMap<String, String> arg4, InputStream arg5)
            throws IOException, WebApplicationException {
            String[] stringArr = IOUtils.readStringFromStream(arg5).split(",");
            double[] intArr = new double[stringArr.length];
            for (int i = 0; i < stringArr.length; i++) {
                intArr[i] = Double.valueOf(stringArr[i]);
            }
            return intArr;
            
        }
    
        public long getSize(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
            return -1;
        }
    
        public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
            return double[].class.isAssignableFrom(arg0);
        }
    
        public void writeTo(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
                            MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException,
            WebApplicationException {
            
            double[] arr = (double[])arg0;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length; i++) {
                sb.append(Double.toString(arr[i]));
                if (i + 1 < arr.length) {
                    sb.append(",");
                }
            }
            arg6.write(sb.toString().getBytes());
        }
    
    }    
}


