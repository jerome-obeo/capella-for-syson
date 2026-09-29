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
package org.eclipse.capella.application.configuration.details.view.semanticbrowser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.capella.model.transverse.services.CommonQueryService.ARCADIA_ELEMENT;
import static org.eclipse.capella.model.transverse.services.CommonQueryService.ARCADIA_IS_REALIZED_BY;
import static org.eclipse.capella.model.transverse.services.CommonQueryService.ARCADIA_PREFIX;
import static org.eclipse.capella.model.transverse.services.CommonQueryService.ARCADIA_REALIZES;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.eclipse.capella.model.transverse.services.CommonCreationService;
import org.eclipse.capella.model.transverse.services.CommonUpdateService;
import org.eclipse.capella.tests.fixtures.CapellaModel;
import org.eclipse.capella.tests.fixtures.SemanticDataTestFixture;
import org.eclipse.sirius.components.collaborative.api.IRepresentationSearchService;
import org.eclipse.sirius.components.core.api.IIdentityService;
import org.eclipse.sirius.web.domain.boundedcontexts.representationdata.services.api.IRepresentationMetadataSearchService;
import org.eclipse.syson.sysml.FeatureDirectionKind;
import org.eclipse.syson.sysml.SysmlPackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests traceability categories in the semantic browser.
 */
public class SemanticBrowserServiceTests {

    private static final SemanticDataTestFixture FIXTURE = new SemanticDataTestFixture();

    private final SemanticBrowserService service = new SemanticBrowserService(
            mock(IRepresentationMetadataSearchService.class), mock(IIdentityService.class), mock(IRepresentationSearchService.class));

    private final CommonUpdateService updateService = new CommonUpdateService();

    private CapellaModel capellaModel;

    @BeforeEach
    public void beforeEach() {
        this.capellaModel = FIXTURE.createCapellaModel();
    }

    @Test
    public void traceabilityCategoriesShouldReflectAddedAndRemovedFunctions() {
        var firstRealizer = this.capellaModel.getLogicalArchitecturePerspective().getFunctionsPackage().getRootFunction().getElement();
        var secondRealizer = new CommonCreationService().createFunction(firstRealizer);
        var realized = this.capellaModel.getSystemAnalysisPerspective().getFunctionsPackage().getRootFunction().getElement();

        assertThat(this.service.getReferencedElementsCategories(firstRealizer)).doesNotContain("Realizes");
        assertThat(this.service.getReferencingElementsCategories(realized)).doesNotContain("Is Realized By");

        this.updateService.setRealizes(firstRealizer, realized);
        this.updateService.setRealizes(secondRealizer, realized);

        assertThat(this.service.getReferencedElementsCategories(firstRealizer)).contains("Realizes");
        assertThat(this.service.getReferencedCategoryElements(firstRealizer, "Realizes")).containsExactly(realized);
        assertThat(this.service.getReferencingElementsCategories(realized)).contains("Is Realized By");
        assertThat(this.service.getReferencingCategoryElements(realized, "Is Realized By")).containsExactlyInAnyOrder(firstRealizer, secondRealizer);

        this.updateService.removeRealizes(firstRealizer, realized);

        assertThat(this.service.getReferencedElementsCategories(firstRealizer)).doesNotContain("Realizes");
        assertThat(this.service.getReferencingCategoryElements(realized, "Is Realized By")).containsExactly(secondRealizer);
    }

    @Test
    public void traceabilityCategoriesShouldIncludeFunctionPorts() {
        var logicalFunction = this.capellaModel.getLogicalArchitecturePerspective().getFunctionsPackage().getRootFunction().getElement();
        var systemFunction = this.capellaModel.getSystemAnalysisPerspective().getFunctionsPackage().getRootFunction().getElement();
        var creationService = new CommonCreationService();
        var realizerPort = creationService.createFunctionPort(logicalFunction, FeatureDirectionKind.OUT);
        var realizedPort = creationService.createFunctionPort(systemFunction, FeatureDirectionKind.OUT);

        this.updateService.setFeatureReferenceValues(realizerPort, ARCADIA_PREFIX + ARCADIA_ELEMENT, ARCADIA_REALIZES,
                List.of(realizedPort), SysmlPackage.eINSTANCE.getOccurrenceUsage());
        this.updateService.setFeatureReferenceValues(realizedPort, ARCADIA_PREFIX + ARCADIA_ELEMENT, ARCADIA_IS_REALIZED_BY,
                List.of(realizerPort), SysmlPackage.eINSTANCE.getOccurrenceUsage());

        assertThat(this.service.getReferencedElementsCategories(realizerPort)).contains("Realizes");
        assertThat(this.service.getReferencedCategoryElements(realizerPort, "Realizes")).containsExactly(realizedPort);
        assertThat(this.service.getReferencingElementsCategories(realizedPort)).contains("Is Realized By");
        assertThat(this.service.getReferencingCategoryElements(realizedPort, "Is Realized By")).containsExactly(realizerPort);
    }
}
