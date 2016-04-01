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
package io.fabric8.apmagent.strategy.trace;

import io.fabric8.apmagent.ApmConfiguration;
import io.fabric8.apmagent.ClassInfo;
import io.fabric8.apmagent.Strategy;
import io.fabric8.apmagent.metrics.ApmAgentContext;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class TraceStrategy implements Strategy, ClassFileTransformer {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(TraceStrategy.class);

    private ApmAgentContext context;
    private ApmConfiguration configuration;
    private Instrumentation instrumentation;
    private BlockingQueue<Class<?>> blockingQueue = new LinkedBlockingDeque<>();
    private AtomicBoolean initialized = new AtomicBoolean();
    private AtomicBoolean started = new AtomicBoolean();
    private AtomicBoolean cleanUp = new AtomicBoolean();
    private Thread transformThread;

    public TraceStrategy(ApmAgentContext context, Instrumentation instrumentation) {
        this.context = context;
        this.configuration = context.getConfiguration();
        this.instrumentation = instrumentation;
    }

    @Override
    public void initialize() throws Exception {
        if (initialized.compareAndSet(false, true)) {
            configuration.addChangeListener(this);
        }
    }

    @Override
    public void start() throws Exception {
        if (started.compareAndSet(false, true)) {
            initialize();
            instrumentApplication();
        }
    }

    @Override
    public void stop() {
        if (started.compareAndSet(true, false)) {
            // noop
        }
    }

    @Override
    public void shutDown() {
        if (initialized.compareAndSet(true, false)) {
            stop();
            configuration.removeChangeListener(this);
            instrumentation.removeTransformer(this);
            Thread t = transformThread;
            transformThread = null;
            if (t != null && !t.isInterrupted()) {
                t.interrupt();
            }
            cleanUp.set(true);
            try {
                //clean up
                instrumentApplication();
            } catch (Throwable e) {
                LOG.warn("Failed to shutdown due " + e.getMessage() + ". This exception is ignored.", e);
            }
        }
    }

    public boolean isAudit(String className) {
        return configuration.isAudit(className);
    }

    public boolean isAudit(String className, String methodName) {
        return configuration.isAudit(className, methodName);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] buffer = null;
        ClassInfo classInfo = context.getClassInfo(className);
        classInfo.setOriginalClass(classBeingRedefined);
        if (classInfo.getTransformed() == null) {
            //we haven't been transformed before
            classInfo.setOriginal(classfileBuffer);
        }
        if (!cleanUp.get()) {
            byte[] classBufferToRedefine = classInfo.getOriginal();

            if (configuration.isAudit(className)) {
                if (classInfo.isTransformed()) {
                    //remove metrics from methods no longer defined
                    context.resetMethods(classInfo);
                }
                ClassReader cr = new ClassReader(classBufferToRedefine);

                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

                ApmClassVisitor visitor = new ApmClassVisitor(this, cw, classInfo);
                cr.accept(visitor, ClassReader.SKIP_FRAMES);
                buffer = cw.toByteArray();
                if (!verifyClass(className, buffer)) {
                    classInfo.setCanTransform(false);
                    buffer = null;
                }
                classInfo.setTransformed(buffer);
            }
        } else {
            if (classInfo.getOriginal() != null) {
                buffer = classInfo.getOriginal();
                context.resetAll(classInfo);
            }
        }
        return buffer;
    }

    @Override
    public void configurationChanged() {
        if (started.get()) {
            if (configuration.isFilterChanged()) {
                List<ClassInfo> deltas = context.buildDeltaList();
                if (deltas != null && !deltas.isEmpty()) {
                    for (ClassInfo classInfo : deltas) {
                        if (configuration.isAsyncTransformation()) {
                            try {
                                blockingQueue.put(classInfo.getOriginalClass());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        } else {
                            try {
                                instrumentation.retransformClasses(new Class[]{classInfo.getOriginalClass()});
                            } catch (Throwable e) {
                                LOG.warn("Could not transform " + classInfo.getClassName() + " due " + e.getMessage(), e);
                            }
                        }
                    }
                    if (configuration.isAsyncTransformation() && !blockingQueue.isEmpty()) {
                        startTransformThread();
                    }
                }
            }
        }
    }

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public void setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public ApmAgentContext getContext() {
        return context;
    }

    public void setContext(ApmAgentContext context) {
        this.context = context;
        this.configuration = context.getConfiguration();
    }

    private void instrumentApplication() throws FileNotFoundException, UnmodifiableClassException {
        if (!instrumentation.isRetransformClassesSupported()) {
            throw new UnmodifiableClassException();
        }

        instrumentation.addTransformer(this, true);

        for (Class<?> c : instrumentation.getAllLoadedClasses()) {
            if (isInstrumentClass(c)) {
                if (configuration.isAsyncTransformation()) {
                    try {
                        blockingQueue.put(c);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    try {
                        instrumentation.retransformClasses(new Class[]{c});
                    } catch (Throwable e) {
                        LOG.error("Could not transform " + c.getName(), e);
                    }
                }
            }
        }
        if (configuration.isAsyncTransformation() && !blockingQueue.isEmpty()) {
            startTransformThread();
        }
    }

    private boolean isInstrumentClass(Class c) {

        if (!instrumentation.isModifiableClass(c)) {
            LOG.trace("NO INSTRUMENT: Class {} is not modifiable", c.getName());
            return false;
        }
        if (!configuration.isAudit(c.getName())) {
            LOG.trace("NO INSTRUMENT: Class {} is blacklisted", c.getName());
            return false;
        }
        if (c.isArray() || c.isAnnotation() || c.isInterface() || c.isPrimitive() || c.isSynthetic() || c.isEnum()) {
            LOG.trace("NO INSTRUMENT: Class {} is an array, primitive, annotation or enum etc.", c.getName());
            return false;
        }
        return true;
    }

    private synchronized void startTransformThread() {
        if (configuration.isAsyncTransformation() && transformThread == null) {
            transformThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (initialized.get() && !blockingQueue.isEmpty()) {
                        try {
                            Class<?> aClass = blockingQueue.take();
                            if (aClass != null) {
                                if (isInstrumentClass(aClass)) {
                                    try {
                                        instrumentation.retransformClasses(new Class[]{aClass});
                                    } catch (Throwable e) {
                                        LOG.error("Could not transform " + aClass.getName(), e);
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                            shutDown();
                        }
                    }
                }
            });
            transformThread.setDaemon(true);
            transformThread.start();
        }
    }

    private boolean verifyClass(String className, byte[] transformed) {
        boolean result = true;
        if (configuration.isVerifyClasses()) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            CheckClassAdapter.verify(new ClassReader(transformed), false, pw);
            if (sw.toString().length() != 0) {
                result = false;
                LOG.error("Failed to transform class: " + className);
                LOG.error(sw.toString());
            }
        }
        return result;
    }
}
