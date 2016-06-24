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
package io.fabric8.cxf.endpoint;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

public class FourSpacePrettyPrinter extends DefaultPrettyPrinter {
    
    public FourSpacePrettyPrinter() {
        _objectIndenter = Lf4SpacesIndenter.INSTANCE;
    }
    
    public static class Lf4SpacesIndenter extends NopIndenter {
        public static final Lf4SpacesIndenter INSTANCE = new Lf4SpacesIndenter();

        private static final String SYS_LF;
        
        private static int spacecount = 64;
        private static char[] spaces = new char[spacecount];
        
        static {
            String lf = null;
            try {
                lf = System.getProperty("line.separator");
            } catch (Throwable t) {
                //
            } 
            SYS_LF = (lf == null) ? "\n" : lf;
        }

        
        static {
            Arrays.fill(spaces, ' ');
        }

        
        
        @Override
        public boolean isInline() {
            return false;
        }

        @Override
        public void writeIndentation(JsonGenerator jg, int level) throws IOException, JsonGenerationException {
            jg.writeRaw(SYS_LF);
            if (level > 0) { // should we err on negative values (as there's some flaw?)
                level = level * 4; // 4 spaces per level
                while (level > spacecount) { // should never happen but...
                    jg.writeRaw(spaces, 0, spacecount);
                    level -= spaces.length;
                }
                jg.writeRaw(spaces, 0, level);
            }
        }
    }

}
