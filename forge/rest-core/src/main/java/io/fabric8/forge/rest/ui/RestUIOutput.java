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
