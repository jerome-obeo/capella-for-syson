/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.capella.application.controllers.diagram;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.capella.AbstractIntegrationTests;
import org.eclipse.capella.CapellaProjectData;
import org.eclipse.capella.GivenCapellaServer;
import org.eclipse.capella.diagram.customization.dto.SetDiagramFilterStateInput;
import org.eclipse.capella.diagram.customization.dto.SetDiagramFilterStateSuccessPayload;
import org.eclipse.capella.diagram.lab.view.filters.LABShowFunctionsDiagramFilter;
import org.eclipse.capella.tests.graphql.RepresentationMetadataAvailableFiltersQueryRunner;
import org.eclipse.capella.tests.graphql.SetDiagramFilterStateMutationRunner;
import org.eclipse.sirius.components.collaborative.diagrams.dto.DiagramEventInput;
import org.eclipse.sirius.components.collaborative.diagrams.dto.DiagramRefreshedEventPayload;
import org.eclipse.sirius.components.diagrams.tests.graphql.DiagramEventSubscriptionRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import reactor.test.StepVerifier;

/**
 * Integration tests for the diagram filter state mutation.
 *
 * @author Jerome Gout
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@GivenCapellaServer
public class SetDiagramFilterStateIntegrationTest extends AbstractIntegrationTests {

    @Autowired
    private DiagramEventSubscriptionRunner diagramEventSubscriptionRunner;

    @Autowired
    private SetDiagramFilterStateMutationRunner setDiagramFilterStateMutationRunner;

    @Autowired
    private RepresentationMetadataAvailableFiltersQueryRunner representationMetadataAvailableFiltersQueryRunner;

    @Test
    @DisplayName("GIVEN an active diagram filter, WHEN it is deactivated, THEN its new state is returned and exposed")
    public void deactivateDiagramFilter() {
        String representationId = CapellaProjectData.GraphicalIds.LAB_LOGICAL_ARCHITECTURE_BLANK_DIAGRAM_ID;
        var diagramEventInput = new DiagramEventInput(UUID.randomUUID(), CapellaProjectData.EDITING_CONTEXT_ID, representationId);
        var diagramEvents = this.diagramEventSubscriptionRunner.run(diagramEventInput).flux();

        Runnable deactivateFilter = () -> {
            assertThat(this.getDiagramFilterState(representationId)).isTrue();

            var input = new SetDiagramFilterStateInput(UUID.randomUUID(), CapellaProjectData.EDITING_CONTEXT_ID, representationId, LABShowFunctionsDiagramFilter.ID, false);
            var result = this.setDiagramFilterStateMutationRunner.run(input).data();

            String typename = JsonPath.read(result, "$.data.setDiagramFilterState.__typename");
            Boolean active = JsonPath.read(result, "$.data.setDiagramFilterState.active");
            assertThat(typename).withFailMessage(result).isEqualTo(SetDiagramFilterStateSuccessPayload.class.getSimpleName());
            assertThat(active).isFalse();
            assertThat(this.getDiagramFilterState(representationId)).isFalse();
        };

        StepVerifier.create(diagramEvents)
                .expectNextMatches(DiagramRefreshedEventPayload.class::isInstance)
                .then(deactivateFilter)
                .expectNextMatches(DiagramRefreshedEventPayload.class::isInstance)
                .thenCancel()
                .verify(Duration.ofSeconds(10));
    }

    private boolean getDiagramFilterState(String representationId) {
        Map<String, Object> variables = Map.of(
                "editingContextId", CapellaProjectData.EDITING_CONTEXT_ID,
                "representationId", representationId
        );
        var result = this.representationMetadataAvailableFiltersQueryRunner.run(variables).data();
        List<Boolean> states = JsonPath.read(result, "$.data.viewer.editingContext.representation.availableDiagramFilters[?(@.id == '" + LABShowFunctionsDiagramFilter.ID + "')].state");
        assertThat(states).hasSize(1);
        return states.get(0);
    }
}
