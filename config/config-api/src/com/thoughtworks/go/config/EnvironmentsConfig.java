/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.domain.EnvironmentPipelineMatchers;
import com.thoughtworks.go.util.comparator.AlphaAsciiComparator;

/**
 * @understands the current persistent information related to multiple logical groupings of machines
 */
@ConfigTag("environments")
@ConfigCollection(EnvironmentConfig.class)
public class EnvironmentsConfig extends BaseCollection<EnvironmentConfig> implements Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public EnvironmentsConfig() { }

    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public void validateContainOnlyUuids(Set<String> uuids) {
        for (EnvironmentConfig environmentConfig : this) {
            environmentConfig.validateContainsOnlyUuids(uuids);
        }
    }

    public void validateContainOnlyPiplines(List<CaseInsensitiveString> pipelineNames) {
        for (EnvironmentConfig environmentConfig : this) {
            environmentConfig.validateContainsOnlyPipelines(pipelineNames);
        }
    }

    public void addAgentsToEnvironment(String environmentName, String... uuids) {
        EnvironmentConfig environment = getOrCreateEnvironment(environmentName);
        for (String uuid : uuids) {
            environment.addAgent(uuid);
        }
    }

    private EnvironmentConfig getOrCreateEnvironment(String environmentName) {
        for (EnvironmentConfig config : this) {
            if (config.hasName(new CaseInsensitiveString(environmentName))) {
                return config;
            }
        }
        EnvironmentConfig config = new EnvironmentConfig(new CaseInsensitiveString(environmentName));
        add(config);
        return config;
    }

    public EnvironmentPipelineMatcher matchersForPipeline(String pipelineName) {
        for (EnvironmentConfig config : this) {
            if (config.containsPipeline(new CaseInsensitiveString(pipelineName))) {
                return config.createMatcher();
            }
        }
        return null;
    }

    @Override
    public boolean add(EnvironmentConfig environment) {
        validateNotADuplicate(environment);
        return super.add(environment);
    }

    private void validateNotADuplicate(EnvironmentConfig environmentConfig) {
        if (hasEnvironmentNamed(environmentConfig.name())) {
            throw new RuntimeException(String.format("Environment with name '%s' already exists.", environmentConfig.name()));
        }
        for (EnvironmentConfig config : this) {
            if (config.hasSamePipelinesAs(environmentConfig)) {
                throw new RuntimeException("One of the environments contains duplicate pipelines");
            }
        }
    }

    public void addPipelinesToEnvironment(String environmentName, String ... pipelineNames) {
        EnvironmentConfig environment = getOrCreateEnvironment(environmentName);
        for (String pipelineName : pipelineNames) {
            environment.addPipeline(new CaseInsensitiveString(pipelineName));
        }
    }

    public EnvironmentPipelineMatchers matchers() {
        EnvironmentPipelineMatchers environmentPipelineMatchers = new EnvironmentPipelineMatchers();
        for (EnvironmentConfig environment : this) {
            environmentPipelineMatchers.add(environment.createMatcher());
        }
        return environmentPipelineMatchers;
    }

    public CaseInsensitiveString findEnvironmentNameForPipeline(final CaseInsensitiveString pipelineName) {
        EnvironmentConfig environment = findEnvironmentForPipeline(pipelineName);
        return environment == null ? null : environment.name();
    }

    public EnvironmentConfig findEnvironmentForPipeline(final CaseInsensitiveString pipelineName) {
        for (EnvironmentConfig config : this) {
            if (config.containsPipeline(pipelineName)) {
                return config;
            }
        }
        return null;
    }

    public boolean isPipelineAssociatedWithAnyEnvironment(final CaseInsensitiveString pipelineName) {
        for (EnvironmentConfig environment : this) {
            if (environment.containsPipeline(pipelineName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAgentUnderEnvironment(String agentUuid) {
        for (EnvironmentConfig environment : this) {
            if (environment.hasAgent(agentUuid)) {
                return true;
            }
        }
        return false;
    }

    public EnvironmentConfig named(final CaseInsensitiveString envName) throws NoSuchEnvironmentException {
        for (EnvironmentConfig environmentConfig : this) {
            if(environmentConfig.name().equals(envName)) {
                return environmentConfig;
            }
        }
        throw new NoSuchEnvironmentException(envName);
    }

    public List<CaseInsensitiveString> names() {
        ArrayList<CaseInsensitiveString> names = new ArrayList<CaseInsensitiveString>();
        for (EnvironmentConfig environment : this) {
            names.add(environment.name());
        }
        return names;
    }

    public TreeSet<String> environmentsForAgent(String agentUuid) {
        TreeSet<String> environmentNames = new TreeSet<String>(new AlphaAsciiComparator());
        for (EnvironmentConfig config : this) {
            if (config.hasAgent(agentUuid)) {
                environmentNames.add(CaseInsensitiveString.str(config.name()));
            }
        }
        return environmentNames;
    }

    public boolean hasEnvironmentNamed(CaseInsensitiveString environmentName) {
        for (EnvironmentConfig environmentConfig : this) {
            if(environmentConfig.name().equals(environmentName)) {
                return true;
            }
        }
        return false;
    }


    public void removeAgentFromAllEnvironments(String uuid) {
        for (EnvironmentConfig environmentConfig : this) {
            environmentConfig.removeAgent(uuid);
        }
    }
}