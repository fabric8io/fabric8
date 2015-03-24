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
}
