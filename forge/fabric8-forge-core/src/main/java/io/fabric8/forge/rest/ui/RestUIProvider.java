/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.forge.rest.ui;

import org.jboss.forge.addon.ui.DefaultUIDesktop;
import org.jboss.forge.addon.ui.UIDesktop;
import org.jboss.forge.addon.ui.UIProvider;
import org.jboss.forge.addon.ui.output.UIOutput;

import java.io.ByteArrayOutputStream;

public class RestUIProvider implements UIProvider {

	private final UIOutput output;
	private ByteArrayOutputStream out = new ByteArrayOutputStream();
	private ByteArrayOutputStream err = new ByteArrayOutputStream();
	private final String uiName = "UiProvider";

	public RestUIProvider() {
		super();
		this.output = new RestUIOutput(out, err);
	}

	@Override
	public boolean isGUI() {
		return true;
	}

	@Override
	public UIOutput getOutput() {
		return output;
	}

	@Override
	public UIDesktop getDesktop() {
		return new DefaultUIDesktop();
	}

	public String getOut() {
		return out.toString();
	}

	public String getErr() {
		return err.toString();
	}

        @Override
        public String getName() {
                return uiName;
        }

        @Override
        public boolean isEmbedded() {
                return false;
        }
}
