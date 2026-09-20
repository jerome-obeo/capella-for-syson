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
package org.eclipse.capella.diagram.customization.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.capella.diagram.customization.services.api.IDiagramFilter;
import org.eclipse.sirius.components.collaborative.diagrams.DiagramContext;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IRepresentationDescriptionSearchService;
import org.eclipse.sirius.components.diagrams.Diagram;
import org.eclipse.sirius.components.diagrams.DiagramStyle;
import org.eclipse.sirius.web.domain.boundedcontexts.representationdata.services.api.IRepresentationMetadataSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DiagramFilterService}.
 *
 * @author Jerome Gout
 */
public class DiagramFilterServiceTests {

    private static final String REPRESENTATION_ID = "representation-id";

    private static final String INITIALLY_ACTIVE_FILTER_ID = "initially-active-filter";

    private static final String INITIALLY_INACTIVE_FILTER_ID = "initially-inactive-filter";

    @Test
    @DisplayName("GIVEN an initially active filter, WHEN it is deactivated, THEN its explicit state overrides its initial state")
    public void deactivateInitiallyActiveFilter() {
        var service = this.createService();

        assertThat(service.isDiagramFilterActive(INITIALLY_ACTIVE_FILTER_ID, REPRESENTATION_ID)).isTrue();

        service.setDiagramFilterState(new IEditingContext.NoOp(), this.createDiagramContext(), INITIALLY_ACTIVE_FILTER_ID, false);

        assertThat(service.isDiagramFilterActive(INITIALLY_ACTIVE_FILTER_ID, REPRESENTATION_ID)).isFalse();
    }

    @Test
    @DisplayName("GIVEN an active filter, WHEN it is activated multiple times and deactivated once, THEN it is inactive")
    public void repeatedActivationIsIdempotent() {
        var service = this.createService();
        var diagramContext = this.createDiagramContext();

        service.setDiagramFilterState(new IEditingContext.NoOp(), diagramContext, INITIALLY_ACTIVE_FILTER_ID, true);
        service.setDiagramFilterState(new IEditingContext.NoOp(), diagramContext, INITIALLY_ACTIVE_FILTER_ID, true);
        service.setDiagramFilterState(new IEditingContext.NoOp(), diagramContext, INITIALLY_ACTIVE_FILTER_ID, false);

        assertThat(service.isDiagramFilterActive(INITIALLY_ACTIVE_FILTER_ID, REPRESENTATION_ID)).isFalse();
    }

    @Test
    @DisplayName("GIVEN filters with different initial states, WHEN one state changes, THEN the other filter keeps its state")
    public void filterStatesAreIndependent() {
        var service = this.createService();

        service.setDiagramFilterState(new IEditingContext.NoOp(), this.createDiagramContext(), INITIALLY_INACTIVE_FILTER_ID, true);

        assertThat(service.isDiagramFilterActive(INITIALLY_ACTIVE_FILTER_ID, REPRESENTATION_ID)).isTrue();
        assertThat(service.isDiagramFilterActive(INITIALLY_INACTIVE_FILTER_ID, REPRESENTATION_ID)).isTrue();
    }

    private DiagramFilterService createService() {
        return new DiagramFilterService(
                mock(IRepresentationDescriptionSearchService.class),
                mock(IRepresentationMetadataSearchService.class),
                List.of(this.createFilter(INITIALLY_ACTIVE_FILTER_ID, true), this.createFilter(INITIALLY_INACTIVE_FILTER_ID, false)),
                List.of(),
                List.of());
    }

    private IDiagramFilter createFilter(String id, boolean initialState) {
        IDiagramFilter filter = mock(IDiagramFilter.class);
        when(filter.getId()).thenReturn(id);
        when(filter.getInitialState()).thenReturn(initialState);
        return filter;
    }

    private DiagramContext createDiagramContext() {
        Diagram diagram = Diagram.newDiagram(REPRESENTATION_ID)
                .descriptionId("diagram-description-id")
                .targetObjectId("diagram-target-id")
                .nodes(List.of())
                .edges(List.of())
                .style(DiagramStyle.newDiagramStyle().build())
                .build();
        return new DiagramContext(diagram, List.of(), List.of(), List.of());
    }
}
