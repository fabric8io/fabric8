/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.jaxb.dynamic;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.jaxb.JAXBUtils.JCodeModel;
import org.apache.cxf.common.jaxb.JAXBUtils.JDefinedClass;
import org.apache.cxf.common.jaxb.JAXBUtils.JPackage;
import org.apache.cxf.common.jaxb.JAXBUtils.S2JJAXBModel;
import org.apache.cxf.common.jaxb.JAXBUtils.SchemaCompiler;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.helpers.FileUtils;

/**
 * A helper class which allows the dynamic invocation of XJC so that we can load new bytecode and JAXB contexts
 * for XML schema documents at runtime
 */
public class DynamicXJC {
    private static final Logger LOG = LogUtils.getL7dLogger(DynamicXJC.class);

    private String[] schemaCompilerOptions;
    private String tmpdir = SystemPropertyAction.getProperty("java.io.tmpdir");
    private final ClassLoader classLoader;
    private Map<String, Object> jaxbContextProperties;
    private List<String> schemaUrls = new ArrayList<String>();

    public DynamicXJC(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }


    public CompileResults compileSchemas() {
        SchemaCompiler compiler = createSchemaCompiler();

        // our hashcode + timestamp ought to be enough.
        String stem = toString() + "-" + System.currentTimeMillis();
        File src = new File(tmpdir, stem + "-src");
        if (!src.mkdir()) {
            throw new IllegalStateException("Unable to create working directory " + src.getPath());
        }

        boolean first = true;
        StringBuilder sb = new StringBuilder();

        for (String rawUrl : getSchemaUrls()) {
            String schemaUrl = resolveUrl(rawUrl);
            InnerErrorListener listener = new InnerErrorListener(schemaUrl);
            Object elForRun = ReflectionInvokationHandler
                    .createProxyWrapper(listener,
                            JAXBUtils.getParamClass(compiler, "setErrorListener"));

            compiler.setErrorListener(elForRun);

            compiler.parseSchema(new InputSource(schemaUrl));

            S2JJAXBModel intermediateModel = compiler.bind();
            listener.throwException();

            JCodeModel codeModel = intermediateModel.generateCode(null, elForRun);

            for (Iterator<JPackage> packages = codeModel.packages(); packages.hasNext(); ) {
                JPackage jpackage = packages.next();
                if (!isValidPackage(jpackage)) {
                    continue;
                }
                if (first) {
                    first = false;
                } else {
                    sb.append(':');
                }
                sb.append(jpackage.name());
            }
            JAXBUtils.logGeneratedClassNames(LOG, codeModel);

            try {
                Object writer = JAXBUtils.createFileCodeWriter(src);
                codeModel.build(writer);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to write generated Java files for schemas: "
                        + e.getMessage(), e);
            }
        }
        String packageList = sb.toString();
        File classes = new File(tmpdir, stem + "-classes");
        if (!classes.mkdir()) {
            throw new IllegalStateException("Unable to create working directory " + classes.getPath());
        }
        StringBuilder classPath = new StringBuilder();
        try {
            setupClasspath(classPath, classLoader);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        List<File> srcFiles = FileUtils.getFilesRecurse(src, ".+\\.java$");
        if (!compileJavaSrc(classPath.toString(), srcFiles, classes.toString())) {
            LOG.log(Level.SEVERE,
                    new Message("COULD_NOT_COMPILE_SRC", LOG, getSchemaUrls().toString()).toString());
        }
        FileUtils.removeDir(src);
        URL[] urls;
        try {
            urls = new URL[] {classes.toURI().toURL()};
        } catch (MalformedURLException mue) {
            throw new IllegalStateException("Internal error; a directory returns a malformed URL: "
                    + mue.getMessage(), mue);
        }
        ClassLoader cl = ClassLoaderUtils.getURLClassLoader(urls, classLoader);

        JAXBContext context;
        Map<String, Object> contextProperties = jaxbContextProperties;

        if (contextProperties == null) {
            contextProperties = Collections.emptyMap();
        }

        try {
            if (StringUtils.isEmpty(packageList)) {
                context = JAXBContext.newInstance(new Class[0], contextProperties);
            } else {
                context = JAXBContext.newInstance(packageList, cl, contextProperties);
            }
        } catch (JAXBException jbe) {
            throw new IllegalStateException("Unable to create JAXBContext for generated packages: "
                    + jbe.getMessage(), jbe);
        }

        // keep around for class loader discovery later
        classes.deleteOnExit();
        //FileUtils.removeDir(classes);
        return new CompileResults(cl, context);
    }

    public void addSchemaUrl(String url) {
        getSchemaUrls().add(url);
    }

    public List<String> getSchemaUrls() {
        return schemaUrls;
    }

    public void setSchemaUrls(List<String> schemaUrls) {
        this.schemaUrls = schemaUrls;
    }

    public Map<String, Object> getJaxbContextProperties() {
        return jaxbContextProperties;
    }

    public void setJaxbContextProperties(Map<String, Object> jaxbContextProperties) {
        this.jaxbContextProperties = jaxbContextProperties;
    }

    public String[] getSchemaCompilerOptions() {
        return schemaCompilerOptions;
    }

    public void setSchemaCompilerOptions(String[] schemaCompilerOptions) {
        this.schemaCompilerOptions = schemaCompilerOptions;
    }

    protected SchemaCompiler createSchemaCompiler() {
        SchemaCompiler compiler =
                JAXBUtils.createSchemaCompilerWithDefaultAllocator(new HashSet<String>());
        if (schemaCompilerOptions != null && schemaCompilerOptions.length > 0) {
            compiler.getOptions().parseArguments(schemaCompilerOptions);
        }
        return compiler;
    }

    /**
     * Lets try deal with URLs which start with classpath:
     */
    protected String resolveUrl(String rawUrl) {
        String classpathPrefix = "classpath:";
        if (rawUrl.startsWith(classpathPrefix)) {
            String remaining = rawUrl.substring(classpathPrefix.length());
            // lets see if we can find the URL on the class loader
            try {
                URL resource = ClassLoaderUtils.getResource(remaining, getClass());
                if (resource != null) {
                    return resource.toString();
                }
            } catch (Throwable e) {
                LOG.fine("Ignored error trying to resolve '" + remaining + "' on the classpath: " + e);
            }
        }
        return rawUrl;
    }

    private boolean isValidPackage(JPackage jpackage) {
        if (jpackage == null) {
            return false;
        }
        String name = jpackage.name();
        if ("org.w3._2001.xmlschema".equals(name)
                || "java.lang".equals(name)
                || "java.io".equals(name)
                || "generated".equals(name)) {
            return false;
        }
        Iterator<JDefinedClass> i = jpackage.classes();
        while (i.hasNext()) {
            JDefinedClass current = i.next();
            if ("ObjectFactory".equals(current.name())) {
                return true;
            }
        }
        return false;
    }


    class InnerErrorListener {

        private String url;
        private StringBuilder errors = new StringBuilder();
        private Exception ex;

        InnerErrorListener(String url) {
            this.url = url;
        }

        public void throwException() {
            if (errors.length() > 0) {
                throw new RuntimeException(errors.toString(), ex);
            }
        }

        public void error(SAXParseException arg0) {
            if (ex == null) {
                ex = arg0;
            }
            if (errors.length() == 0) {
                errors.append("Error compiling schema from WSDL at {").append(url).append("}: \n");
            } else {
                errors.append("\n");
            }
            if (arg0.getLineNumber() > 0) {
                errors.append(arg0.getLocalizedMessage() + "\n"
                        + " at line " + arg0.getLineNumber()
                        + " column " + arg0.getColumnNumber()
                        + " of schema " + arg0.getSystemId()
                        + "\n");
            } else {
                errors.append(arg0.getMessage());
                errors.append("\n");
            }
        }

        public void fatalError(SAXParseException arg0) {
            throw new RuntimeException("Fatal error compiling schema from WSDL at {" + url + "}: "
                    + arg0.getMessage(), arg0);
        }

        public void info(SAXParseException arg0) {
            // ignore
        }

        public void warning(SAXParseException arg0) {
            // ignore
        }

    }

    static void addClasspathFromManifest(StringBuilder classPath, File file)
            throws URISyntaxException, IOException {

        JarFile jar = null;
        try {
            jar = new JarFile(file);
            Attributes attr = null;
            if (jar.getManifest() != null) {
                attr = jar.getManifest().getMainAttributes();
            }
            if (attr != null) {
                String cp = attr.getValue("Class-Path");
                while (cp != null) {
                    String fileName = cp;
                    int idx = fileName.indexOf(' ');
                    if (idx != -1) {
                        fileName = fileName.substring(0, idx);
                        cp = cp.substring(idx + 1).trim();
                    } else {
                        cp = null;
                    }
                    URI uri = new URI(fileName);
                    File f2;
                    if (uri.isAbsolute()) {
                        f2 = new File(uri);
                    } else {
                        f2 = new File(file, fileName);
                    }
                    if (f2.exists()) {
                        classPath.append(f2.getAbsolutePath());
                        classPath.append(File.pathSeparator);
                    }
                }
            }
        } finally {
            if (jar != null) {
                jar.close();
            }
        }
    }

    static void setupClasspath(StringBuilder classPath, ClassLoader classLoader)
            throws URISyntaxException, IOException {

        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ClassLoader tcl = classLoader;
        do {
            if (tcl instanceof URLClassLoader) {
                URL[] urls = ((URLClassLoader)tcl).getURLs();
                if (urls == null) {
                    urls = new URL[0];
                }
                for (URL url : urls) {
                    if (url.getProtocol().startsWith("file")) {
                        File file = null;
                        // CXF-3884 use url-decoder to get the decoded file path from the url
                        try {
                            if (url.getPath() == null) {
                                continue;
                            }
                            file = new File(URLDecoder.decode(url.getPath(), "utf-8"));
                        } catch (UnsupportedEncodingException uee) {
                            // ignored as utf-8 is supported
                        }

                        if (null != file && file.exists()) {
                            classPath.append(file.getAbsolutePath())
                                    .append(System
                                            .getProperty("path.separator"));

                            if (file.getName().endsWith(".jar")) {
                                addClasspathFromManifest(classPath, file);
                            }
                        }
                    }
                }
            } else if (tcl.getClass().getName().contains("weblogic")) {
                // CXF-2549: Wrong classpath for dynamic client compilation in Weblogic
                try {
                    Method method = tcl.getClass().getMethod("getClassPath");
                    Object weblogicClassPath = method.invoke(tcl);
                    classPath.append(weblogicClassPath)
                            .append(File.pathSeparator);
                } catch (Exception e) {
                    LOG.log(Level.FINE, "unsuccessfully tried getClassPath method", e);
                }
            }
            tcl = tcl.getParent();
            if (null == tcl) {
                break;
            }
        } while (!tcl.equals(scl.getParent()));
    }

    protected boolean compileJavaSrc(String classPath, List<File> srcList, String dest) {
        org.apache.cxf.common.util.Compiler javaCompiler
                = new org.apache.cxf.common.util.Compiler();

        javaCompiler.setClassPath(classPath);
        javaCompiler.setOutputDir(dest);
        javaCompiler.setTarget("1.6");

        return javaCompiler.compileFiles(srcList);
    }


}
