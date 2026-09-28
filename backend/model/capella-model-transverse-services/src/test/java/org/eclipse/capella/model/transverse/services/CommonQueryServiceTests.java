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
package org.eclipse.capella.model.transverse.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.capella.tests.semantic.AbstractSemanticTests;
import org.eclipse.syson.sysml.FeatureDirectionKind;
import org.junit.jupiter.api.Test;

/**
 * Tests the common semantic element query service.
 *
 * @author Jerome Gout
 */
public class CommonQueryServiceTests extends AbstractSemanticTests {

    private final CommonQueryService commonQueryService = new CommonQueryService();

    @Test
    public void getArcadiaPerspectivePackageShouldReturnTheRequestedPerspectivePackage() {
        var context = this.capellaModel.getLogicalArchitecturePerspective().getStructurePackage().getElement();
        var expected = this.capellaModel.getSystemAnalysisPerspective().getElement();

        var result = this.commonQueryService.getArcadiaPerspectivePackage(context, ArcadiaEngineeringPerspective.SystemAnalysis);

        assertThat(result).contains(expected);
    }

    @Test
    public void getRealizableElementsShouldOnlyReturnMatchingFunctionsFromPreviousPerspective() {
        var source = this.capellaModel.getLogicalArchitecturePerspective().getFunctionsPackage().getRootFunction().getElement();
        var expected = this.capellaModel.getSystemAnalysisPerspective().getFunctionsPackage().getRootFunction().getElement();
        var earlier = this.capellaModel.getOperationalAnalysisPerspective().getFunctionsPackage().getRootFunction().getElement();
        var later = this.capellaModel.getPhysicalArchitecturePerspective().getFunctionsPackage().getRootFunction().getElement();

        assertThat(this.commonQueryService.getRealizableElements(source))
                .contains(expected)
                .doesNotContain(earlier, source, later);
    }

    @Test
    public void getRealizableElementsShouldBeEmptyForOperationalAnalysis() {
        var source = this.capellaModel.getOperationalAnalysisPerspective().getFunctionsPackage().getRootFunction().getElement();

        assertThat(this.commonQueryService.getRealizableElements(source)).isEmpty();
    }

    @Test
    public void getRealizableElementsWhenSourceIsActorShouldOnlyReturnActors() {
        var creationService = new CommonCreationService();
        var source = creationService.createActor(this.capellaModel.getLogicalArchitecturePerspective().getStructurePackage().getElement());
        var targetPackage = this.capellaModel.getSystemAnalysisPerspective().getStructurePackage().getElement();
        var actor = creationService.createActor(targetPackage);
        var component = creationService.createComponent(targetPackage);

        assertThat(this.commonQueryService.getRealizableElements(source)).contains(actor).doesNotContain(component);
    }

    @Test
    public void getRealizableElementsWhenSourceIsComponentShouldExcludeActors() {
        var creationService = new CommonCreationService();
        var source = creationService.createComponent(this.capellaModel.getLogicalArchitecturePerspective().getStructurePackage().getElement());
        var targetPackage = this.capellaModel.getSystemAnalysisPerspective().getStructurePackage().getElement();
        var actor = creationService.createActor(targetPackage);
        var component = creationService.createComponent(targetPackage);

        assertThat(this.commonQueryService.getRealizableElements(source)).contains(component).doesNotContain(actor);
    }

    @Test
    public void getRealizesWidgetLabelShouldMatchFunctionPerspective() {
        var systemFunction = this.capellaModel.getSystemAnalysisPerspective().getFunctionsPackage().getRootFunction().getElement();
        var logicalFunction = this.capellaModel.getLogicalArchitecturePerspective().getFunctionsPackage().getRootFunction().getElement();
        var physicalFunction = this.capellaModel.getPhysicalArchitecturePerspective().getFunctionsPackage().getRootFunction().getElement();

        assertThat(this.commonQueryService.getRealizesWidgetLabel(systemFunction)).isEqualTo("Realized Operational Activities");
        assertThat(this.commonQueryService.getRealizesWidgetLabel(logicalFunction)).isEqualTo("Realized System Functions");
        assertThat(this.commonQueryService.getRealizesWidgetLabel(physicalFunction)).isEqualTo("Realized Logical Functions");
    }

    @Test
    public void getRealizesWidgetLabelShouldMatchFunctionPortDirection() {
        var function = this.capellaModel.getSystemAnalysisPerspective().getFunctionsPackage().getRootFunction().getElement();
        var creationService = new CommonCreationService();
        var input = creationService.createFunctionPort(function, FeatureDirectionKind.IN);
        var output = creationService.createFunctionPort(function, FeatureDirectionKind.OUT);

        assertThat(this.commonQueryService.getRealizesWidgetLabel(input)).isEqualTo("Realized Function Input Ports");
        assertThat(this.commonQueryService.getRealizesWidgetLabel(output)).isEqualTo("Realized Function Output Ports");
    }

    @Test
    public void getRealizedByElementsShouldReflectRealizesReferences() {
        var realizer = this.capellaModel.getLogicalArchitecturePerspective().getFunctionsPackage().getRootFunction().getElement();
        var realized = this.capellaModel.getSystemAnalysisPerspective().getFunctionsPackage().getRootFunction().getElement();
        var updateService = new CommonUpdateService();

        updateService.setRealizes(realizer, realized);

        assertThat(this.commonQueryService.getRealizedByElements(realized)).contains(realizer);
        assertThat(this.commonQueryService.getRealizedByElements(realizer)).doesNotContain(realized);

        updateService.clearRealizes(realizer);

        assertThat(this.commonQueryService.getRealizedByElements(realized)).isEmpty();
    }

    @Test
    public void removeRealizesShouldPreserveOtherRealizers() {
        var firstRealizer = this.capellaModel.getLogicalArchitecturePerspective().getFunctionsPackage().getRootFunction().getElement();
        var secondRealizer = new CommonCreationService().createFunction(firstRealizer);
        var realized = this.capellaModel.getSystemAnalysisPerspective().getFunctionsPackage().getRootFunction().getElement();
        var updateService = new CommonUpdateService();

        updateService.setRealizes(firstRealizer, realized);
        updateService.setRealizes(secondRealizer, realized);
        assertThat(this.commonQueryService.getRealizedByElements(realized)).containsExactlyInAnyOrder(firstRealizer, secondRealizer);

        updateService.removeRealizes(firstRealizer, realized);

        assertThat(this.commonQueryService.getRealizedByElements(realized)).containsExactly(secondRealizer);
    }
}
