/*
 * #%L
 * Wildfly Gravia Subsystem
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package org.wildfly.extension.fabric.parser;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.fabric.service.FabricBootstrapService;
import org.wildfly.extension.fabric.service.FabricRuntimeService;
import org.wildfly.extension.gravia.parser.GraviaSubsystemBootstrap;
import org.jboss.gravia.runtime.Runtime;

/**
 * The fabric subsystem add update handler.
 *
 * @since 13-Nov-2013
 */
final class FabricSubsystemAdd extends AbstractBoottimeAddStepHandler {

    public FabricSubsystemAdd(SubsystemState subsystemState) {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {

        final FabricSubsystemBootstrap bootstrap = new FabricSubsystemBootstrap();

        // Register subsystem services
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                newControllers.addAll(bootstrap.getSubsystemServices(context, verificationHandler));
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        // Register deployment unit processors
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                bootstrap.addDeploymentUnitProcessors(processorTarget);
            }
        }, OperationContext.Stage.RUNTIME);
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }

    static class FabricSubsystemBootstrap extends GraviaSubsystemBootstrap {

        @Override
        protected ServiceController<?> getBoostrapService(OperationContext context, ServiceVerificationHandler verificationHandler) {
            return new FabricBootstrapService().install(context.getServiceTarget(), verificationHandler);
        }

        @Override
        protected ServiceController<Runtime> getRuntimeService(OperationContext context, ServiceVerificationHandler verificationHandler) {
            return new FabricRuntimeService().install(context.getServiceTarget(), verificationHandler);
        }
    }
}
