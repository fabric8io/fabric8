/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.rest.dto;

import java.util.List;

/**
 */
public class ValidationResult {
    private final List<UIMessageDTO> messages;
    private final boolean valid;
    private final boolean canExecute;
    private final String out;
    private final String err;
    private WizardResultsDTO wizardResults;

    public ValidationResult(List<UIMessageDTO> messages, boolean valid, boolean canExecute, String out, String err) {
        this.messages = messages;
        this.valid = valid;
        this.canExecute = canExecute;
        this.out = out;
        this.err = err;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "messages=" + messages +
                ", valid=" + valid +
                ", canExecute=" + canExecute +
                ", out='" + out + '\'' +
                ", err='" + err + '\'' +
                '}';
    }

    public boolean isCanExecute() {
        return canExecute;
    }

    public String getErr() {
        return err;
    }

    public List<UIMessageDTO> getMessages() {
        return messages;
    }

    public String getOut() {
        return out;
    }

    public boolean isValid() {
        return valid;
    }

    public void setWizardResults(WizardResultsDTO wizardResults) {
        this.wizardResults = wizardResults;
    }

    public WizardResultsDTO getWizardResults() {
        return wizardResults;
    }
}
