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

import org.jboss.forge.addon.ui.output.UIOutput;

import java.io.OutputStream;
import java.io.PrintStream;

public class RestUIOutput implements UIOutput {

	private final PrintStream out;
	private final PrintStream err;

	public RestUIOutput(OutputStream out, OutputStream err) {
		this.out = new PrintStream(out);
		this.err = new PrintStream(err);
	}

	@Override
	public PrintStream out() {
		return out;
	}

	@Override
	public PrintStream err() {
		return err;
	}

	@Override
	public void error(PrintStream writer, String message) {
		writer.print("[ERROR] ");
		writer.println(message);
	}

	@Override
	public void success(PrintStream writer, String message) {
		writer.print("[SUCCESS] ");
		writer.println(message);
	}

	@Override
	public void info(PrintStream writer, String message) {
		writer.print("[INFO] ");
		writer.println(message);
	}

	@Override
	public void warn(PrintStream writer, String message) {
		writer.print("[WARNING] ");
		writer.println(message);
	}

}
