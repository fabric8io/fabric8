/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
