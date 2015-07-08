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
package io.fabric8.forge.rest.dto;

import org.jboss.forge.addon.ui.output.UIMessage;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class UIMessageDTO {
    private final String description;
    private final String inputName;
    private final UIMessage.Severity severity;

    public static List<UIMessageDTO> toDtoList(Iterable<UIMessage> messages) {
        List<UIMessageDTO> answer = new ArrayList<>();
        if (messages != null) {
            for (UIMessage message : messages) {
                answer.add(new UIMessageDTO(message));
            }
        }
        return answer;
    }

    public UIMessageDTO(UIMessage message) {
        this.description = message.getDescription();
        this.inputName = message.getSource().getName();
        this.severity = message.getSeverity();
    }

    @Override
    public String toString() {
        return "UIMessageDTO{" +
                "description='" + description + '\'' +
                ", inputName='" + inputName + '\'' +
                ", severity=" + severity +
                '}';
    }

    public String getDescription() {
        return description;
    }

    public String getInputName() {
        return inputName;
    }

    public UIMessage.Severity getSeverity() {
        return severity;
    }
}
