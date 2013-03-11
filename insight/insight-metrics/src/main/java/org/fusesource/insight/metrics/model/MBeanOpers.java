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
package org.fusesource.insight.metrics.model;

import java.util.List;

public class MBeanOpers extends Request {

    protected final String obj;
    protected final String oper;
    protected final List<Object> args;
    protected final List<String> sig;

    public MBeanOpers(String name, String obj, String oper, List<Object> args, List<String> sig) {
        super(name);
        this.obj = obj;
        this.oper = oper;
        this.args = args;
        this.sig = sig;
    }

    @Override
    public String getType() {
        return "oper";
    }

    public String getObj() {
        return obj;
    }

    public String getOper() {
        return oper;
    }

    public List<Object> getArgs() {
        return args;
    }

    public List<String> getSig() {
        return sig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MBeanOpers that = (MBeanOpers) o;

        if (args != null ? !args.equals(that.args) : that.args != null) return false;
        if (obj != null ? !obj.equals(that.obj) : that.obj != null) return false;
        if (oper != null ? !oper.equals(that.oper) : that.oper != null) return false;
        if (sig != null ? !sig.equals(that.sig) : that.sig != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = obj != null ? obj.hashCode() : 0;
        result = 31 * result + (oper != null ? oper.hashCode() : 0);
        result = 31 * result + (args != null ? args.hashCode() : 0);
        result = 31 * result + (sig != null ? sig.hashCode() : 0);
        return result;
    }
}
