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

package org.fusesource.insight.jetty;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.jmx.ObjectMBean;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.util.component.Container.Relationship;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.ShutdownThread;

import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Container class for the MBean instances
 */
public class MBeanContainerWrapper extends MBeanContainer
{

    private final static Logger LOG = Log.getLogger(MBeanContainer.class.getName());

    private static Class[] OBJ_ARG = new Class[]{Object.class};

    public MBeanContainerWrapper(MBeanServer server) {
        super(null);
        this._server = server;
    }

    private final MBeanServer _server;
    private final WeakHashMap<Object, ObjectName> _beans = new WeakHashMap<Object, ObjectName>();
    private final HashMap<String, Integer> _unique = new HashMap<String, Integer>();
    private final WeakHashMap<ObjectName,List<Relationship>> _relations = new WeakHashMap<ObjectName,List<Container.Relationship>>();
    private String _domain = null;

    /**
     * Lookup an object name by instance
     *
     * @param object instance for which object name is looked up
     * @return object name associated with specified instance, or null if not found
     */
    public synchronized ObjectName findMBean(Object object)
    {
        ObjectName bean = _beans.get(object);
        return bean == null ? null : bean;
    }

    /**
     * Lookup an instance by object name
     *
     * @param oname object name of instance
     * @return instance associated with specified object name, or null if not found
     */
    public synchronized Object findBean(ObjectName oname)
    {
        for (Map.Entry<Object, ObjectName> entry : _beans.entrySet())
        {
            ObjectName bean = entry.getValue();
            if (bean.equals(oname))
                return entry.getKey();
        }
        return null;
    }

    /**
     * Retrieve instance of MBeanServer used by container
     *
     * @return instance of MBeanServer
     */
    public MBeanServer getMBeanServer()
    {
        return _server;
    }

    /**
     * Set domain to be used to add MBeans
     *
     * @param domain domain name
     */
    public void setDomain(String domain)
    {
        _domain = domain;
    }

    /**
     * Retrieve domain name used to add MBeans
     *
     * @return domain name
     */
    public String getDomain()
    {
        return _domain;
    }

    /**
     * Implementation of Container.Listener interface
     *
     * @see org.eclipse.jetty.util.component.Container.Listener#add(org.eclipse.jetty.util.component.Container.Relationship)
     */
    public synchronized void add(Relationship relationship)
    {
        LOG.debug("add {}",relationship);
        ObjectName parent = _beans.get(relationship.getParent());
        if (parent == null)
        {
            addBean(relationship.getParent());
            parent = _beans.get(relationship.getParent());
        }

        ObjectName child = _beans.get(relationship.getChild());
        if (child == null)
        {
            addBean(relationship.getChild());
            child = _beans.get(relationship.getChild());
        }

        if (parent != null && child != null)
        {
            List<Container.Relationship> rels = _relations.get(parent);
            if (rels==null)
            {
                rels=new ArrayList<Relationship>();
                _relations.put(parent,rels);
            }
            rels.add(relationship);
        }
    }

    /**
     * Implementation of Container.Listener interface
     *
     * @see org.eclipse.jetty.util.component.Container.Listener#remove(org.eclipse.jetty.util.component.Container.Relationship)
     */
    public synchronized void remove(Relationship relationship)
    {
        LOG.debug("remove {}",relationship);
        ObjectName parent = _beans.get(relationship.getParent());
        ObjectName child = _beans.get(relationship.getChild());

        if (parent != null && child != null)
        {
            List<Container.Relationship> rels = _relations.get(parent);
            if (rels!=null)
            {
                for (Iterator<Relationship> i=rels.iterator();i.hasNext();)
                {
                    Container.Relationship r = i.next();
                    if (relationship.equals(r) || r.getChild()==null)
                        i.remove();
                }
            }
        }
    }

    /**
     * Implementation of Container.Listener interface
     *
     * @see org.eclipse.jetty.util.component.Container.Listener#removeBean(java.lang.Object)
     */
    public synchronized void removeBean(Object obj)
    {
        LOG.debug("removeBean {}",obj);
        ObjectName bean = _beans.remove(obj);

        if (bean != null)
        {
            List<Container.Relationship> beanRelations= _relations.remove(bean);
            if (beanRelations != null)
            {
                LOG.debug("Unregister {}", beanRelations);
                List<?> removeList = new ArrayList<Object>(beanRelations);
                for (Object r : removeList)
                {
                    Container.Relationship relation = (Relationship)r;
                    relation.getContainer().update(relation.getParent(), relation.getChild(), null, relation.getRelationship(), true);
                }
            }

            try
            {
                _server.unregisterMBean(bean);
                LOG.debug("Unregistered {}", bean);
            }
            catch (javax.management.InstanceNotFoundException e)
            {
                LOG.ignore(e);
            }
            catch (Exception e)
            {
                LOG.warn(e);
            }
        }
    }

    /**
     * Implementation of Container.Listener interface
     *
     * @see org.eclipse.jetty.util.component.Container.Listener#addBean(java.lang.Object)
     */
    public synchronized void addBean(Object obj)
    {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Class clazz = obj.getClass();
            if (obj.getClass().getName().startsWith("org.ops4j.pax.web.")) {
                clazz = obj.getClass().getSuperclass();
            }
            Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
            addBean(obj, clazz);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    protected void addBean(Object obj, Class clazz)
    {
        LOG.debug("addBean {}",obj);
        try
        {
            if (obj == null || _beans.containsKey(obj))
                return;

            Object mbean = mbeanFor(obj, clazz);
            if (mbean == null)
                return;

            ObjectName oname = null;
            if (mbean instanceof ObjectMBean)
            {
                try {
                    Method method = ObjectMBean.class.getDeclaredMethod("setMBeanContainer", MBeanContainer.class);
                    method.setAccessible(true);
                    method.invoke(mbean, this);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
//                ((ObjectMBean)mbean).setMBeanContainer(this);
                oname = ((ObjectMBean)mbean).getObjectName();
            }

            //no override mbean object name, so make a generic one
            if (oname == null)
            {
                String type = clazz.getName().toLowerCase();
                int dot = type.lastIndexOf('.');
                if (dot >= 0)
                    type = type.substring(dot + 1);

                String context = null;
                if (mbean instanceof ObjectMBean)
                {
                    context = makeName(((ObjectMBean)mbean).getObjectContextBasis());
                }

                String name = null;
                if (mbean instanceof ObjectMBean)
                {
                    name = makeName(((ObjectMBean)mbean).getObjectNameBasis());
                }

                StringBuffer buf = new StringBuffer();
                buf.append("type=").append(type);
                if (context != null && context.length()>1)
                {
                    buf.append(buf.length()>0 ? ",":"");
                    buf.append("context=").append(context);
                }
                if (name != null && name.length()>1)
                {
                    buf.append(buf.length()>0 ? ",":"");
                    buf.append("name=").append(name);
                }

                String basis = buf.toString();
                Integer count = _unique.get(basis);
                count = count == null ? 0 : 1 + count;
                _unique.put(basis, count);

                //if no explicit domain, create one
                String domain = _domain;
                if (domain == null)
                    domain = clazz.getPackage().getName();

                oname = ObjectName.getInstance(domain + ":" + basis + ",id=" + count);
            }

            ObjectInstance oinstance = _server.registerMBean(mbean, oname);
            LOG.debug("Registered {}", oinstance.getObjectName());
            _beans.put(obj, oinstance.getObjectName());

        }
        catch (Exception e)
        {
            LOG.warn("bean: " + obj, e);
        }
    }

    /**
     * @param basis name to strip of special characters.
     * @return normalized name
     */
    public String makeName(String basis)
    {
        if (basis==null)
            return basis;
        return basis.replace(':', '_').replace('*', '_').replace('?', '_').replace('=', '_').replace(',', '_').replace(' ', '_');
    }

    /**
     * Perform actions needed to start lifecycle
     *
     * @see org.eclipse.jetty.util.component.AbstractLifeCycle#doStart()
     */
    public void doStart()
    {
        ShutdownThread.register(this);
    }

    /**
     * Perform actions needed to stop lifecycle
     *
     * @see org.eclipse.jetty.util.component.AbstractLifeCycle#doStop()
     */
    public void doStop()
    {
        Set<Object> removeSet = new HashSet<Object>(_beans.keySet());
        for (Object removeObj : removeSet)
        {
            removeBean(removeObj);
        }
    }

    static Object mbeanFor(Object o, Class clazz)
    {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try
        {
            Class oClass = clazz;
            Object mbean = null;

            while (mbean == null && oClass != null)
            {
                Thread.currentThread().setContextClassLoader(oClass.getClassLoader());

                String pName = oClass.getPackage().getName();
                String cName = oClass.getName().substring(pName.length() + 1);
                String mName = pName + ".jmx." + cName + "MBean";

                try
                {
                    Class mClass = (Object.class.equals(oClass))?oClass=ObjectMBean.class:Loader.loadClass(oClass,mName,true);
                    if (LOG.isDebugEnabled())
                        LOG.debug("mbeanFor " + o + " mClass=" + mClass);

                    try
                    {
                        Constructor constructor = mClass.getConstructor(OBJ_ARG);
                        mbean=constructor.newInstance(new Object[]{o});
                    }
                    catch(Exception e)
                    {
                        LOG.ignore(e);
                        if (ModelMBean.class.isAssignableFrom(mClass))
                        {
                            mbean=mClass.newInstance();
                            ((ModelMBean)mbean).setManagedResource(o, "objectReference");
                        }
                    }

                    if (mbean instanceof DynamicMBean)
                    {
                        ((DynamicMBean) mbean).getMBeanInfo();
                    }

                    if (LOG.isDebugEnabled())
                        LOG.debug("mbeanFor " + o + " is " + mbean);
                    return mbean;
                }
                catch (ClassNotFoundException e)
                {
                    // The code below was modified to fix bugs 332200 and JETTY-1416
                    // The issue was caused by additional information added to the
                    // message after the class name when running in Apache Felix,
                    // as well as before the class name when running in JBoss.
                    if (e.getMessage().contains(mName))
                        LOG.ignore(e);
                    else
                        LOG.warn(e);
                }
                catch (Error e)
                {
                    LOG.warn(e);
                    mbean = null;
                }
                catch (Exception e)
                {
                    LOG.warn(e);
                    mbean = null;
                }

                oClass = oClass.getSuperclass();
            }
        }
        catch (Exception e)
        {
            LOG.ignore(e);
        }
        return null;
    }

}
