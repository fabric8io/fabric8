/*
 * Copyright 2005-2014 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.apmagent;

import io.fabric8.apmagent.metrics.ApmAgentContext;
import io.fabric8.apmagent.utils.PropertyUtils;
import org.jolokia.jvmagent.JvmAgent;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.MalformedURLException;
import java.security.ProtectionDomain;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApmAgent implements ClassFileTransformer {
  public static final ApmAgent INSTANCE = new ApmAgent();
  private static final Logger LOG = LoggerFactory.getLogger(ApmAgent.class);

  final ApmConfiguration configuration = new ApmConfiguration();
  private Instrumentation instrumentation;
  private AtomicBoolean cleanUp = new AtomicBoolean();
  private AtomicBoolean initialized = new AtomicBoolean();
  private BlockingQueue<Class<?>> blockingQueue = new LinkedBlockingDeque<Class<?>>();
  private Thread transformThread;

  // The following is the entry point when loaded dynamically to inject
  // recorders from the target process.

  private ApmAgent() {
  }

  // The following is the entry point when loaded as a java agent along with
  // the target process on the java command line.

  public static void agentmain(final String args,
                               final Instrumentation instrumentation) throws Exception {
    try {

      ApmAgent agent = ApmAgent.INSTANCE;
      if (agent.initialize(instrumentation, args)) {
        if (agent.getConfiguration().isStartJolokiaAgent()) {
          JvmAgent.agentmain(args);
        }
        agent.start();
        agent.instrument(args);
      }
    } catch (Exception e) {
      LOG.error("Failed in agentmain", e);
      throw e;

    }

  }

  public static void premain(String args, Instrumentation instrumentation) throws Exception {
    try {
      ApmAgent agent = ApmAgent.INSTANCE;
      if (agent.initialize(instrumentation, args)) {
        if (agent.getConfiguration().isStartJolokiaAgent()) {
          JvmAgent.premain(args);
        }
        agent.start();
        agent.instrument(args);
      }
    } catch (Exception e) {
      LOG.error("Failed in premain", e);
      throw e;
    }
  }

  @Override
  public synchronized byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
    byte[] buffer = null;
    if (!cleanUp.get()) {
      if (configuration.isAudit(className) && !ApmAgentContext.INSTANCE.isAlreadyTransformed(className)) {
        ClassReader cr = new ClassReader(classfileBuffer);

        ClassWriter cw = new ClassWriter(cr,
            ClassWriter.COMPUTE_MAXS);

        ApmClassVisitor visitor = new ApmClassVisitor(cw, className);
        cr.accept(visitor, ClassReader.SKIP_FRAMES);
        buffer = cw.toByteArray();
        ApmAgentContext.INSTANCE.addTransformedClass(className);
        LOG.trace("TRANSFORMED " + className);
      }
    }

    return buffer;
  }

  // The following is the implementation for instrumenting the target code.

  public void instrument(String args) throws FileNotFoundException, UnmodifiableClassException {
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
            LOG.error("Could not retransform " + c.getName(), e);
          }
        }
      }
    }
    if (configuration.isAsyncTransformation() && !blockingQueue.isEmpty()) {
      startTransformThread();
    }
  }

  public boolean isInitialized() {
    return initialized.get();
  }

  public ApmConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * @param instrumentation
   * @param args
   * @return false if already initialized, else true if is actually initialized
   */
  public boolean initialize(final Instrumentation instrumentation, String args) throws ClassNotFoundException, FileNotFoundException, MalformedURLException, InstantiationException, IllegalAccessException {
    boolean result = false;
    if ((result = initialized.compareAndSet(false, true))) {
      this.instrumentation = instrumentation;
      cleanUp.set(false);

      this.instrumentation = instrumentation;
      PropertyUtils.setProperties(configuration, args);

      ApmAgentContext apmAgentContext = ApmAgentContext.INSTANCE;
      apmAgentContext.initialize();

      //add shutdown hook
      Thread cleanup = new Thread() {

        @Override
        public void run() {

          try {

            ApmAgent apmAgent = ApmAgent.INSTANCE;
            apmAgent.shutdowm("");

          } catch (Exception e) {
            LOG.error("Failed to run shutdown hook", e);
          }

        }

      };
      Runtime.getRuntime().addShutdownHook(cleanup);
    }
    return result;
  }

  public void start() {
    if (isInitialized()) {
      ApmAgentContext apmAgentContext = ApmAgentContext.INSTANCE;
      apmAgentContext.start();
    }
  }

  // The following is the implementation for resetting the instrumentation.

  public void shutdowm(String args) {
    if (initialized.compareAndSet(true, false)) {
      instrumentation.removeTransformer(this);
      Thread t = transformThread;
      transformThread = null;
      if (t != null && !t.isInterrupted()) {
        t.interrupt();
      }
      cleanUp.set(true);
      try {
        instrument(args);
      } catch (Throwable e) {
        LOG.error("Failed to shutdown", e);
      }
    }
  }

  private boolean isInstrumentClass(Class c) {

    if (!instrumentation.isModifiableClass(c)) {
      LOG.trace("NO INSTRUMENT: Class " + c.getName() + " is not modifiable");
      return false;
    }
    if (!configuration.isAudit(c.getName())) {
      LOG.trace("NO INSTRUMENT: Class " + c.getName() + " is blacklisted");
      return false;
    }
    if (c.isArray() || c.isAnnotation() || c.isInterface() || c.isPrimitive() || c.isSynthetic() || c.isEnum()) {
      LOG.trace("NO INSTRUMENT: Class " + c.getName() + " is an array, primitive, annotation or enum etc");
      return false;
    }
    return true;
  }

  private synchronized void startTransformThread() {
    if (configuration.isAsyncTransformation() && transformThread == null) {
      transformThread = new Thread(new Runnable() {
        @Override
        public void run() {
          while (isInitialized() && !blockingQueue.isEmpty()) {
            try {
              Class<?> aClass = blockingQueue.take();
              if (aClass != null) {
                if (isInstrumentClass(aClass)) {
                  try {
                    instrumentation.retransformClasses(new Class[]{aClass});
                  } catch (Throwable e) {
                    LOG.error("Could not re transform " + aClass.getName(), e);
                  }
                }
              }
            } catch (InterruptedException e) {
              shutdowm("");
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
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    CheckClassAdapter.verify(new ClassReader(transformed), false, pw);
    if (sw.toString().length() != 0) {
      result = false;
      LOG.error(" Failed to transform class: " + className);
      LOG.error(sw.toString());
    }
    return result;
  }
}
