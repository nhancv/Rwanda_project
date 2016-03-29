package org.hisp.dhis.dataintegrity;

/*
 * Copyright (c) 2004-2016, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.SectionService;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.ExpressionValidationOutcome;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleService;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.hisp.dhis.commons.collection.ListUtils.getDuplicates;

/**
 * @author Lars Helge Overland
 */
@Transactional
public class DefaultDataIntegrityService
    implements DataIntegrityService
{
    private static final Log log = LogFactory.getLog( DefaultDataIntegrityService.class );

    private static final String FORMULA_SEPARATOR = "#";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private IndicatorService indicatorService;

    public void setIndicatorService( IndicatorService indicatorService )
    {
        this.indicatorService = indicatorService;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private SectionService sectionService;

    public void setSectionService( SectionService sectionService )
    {
        this.sectionService = sectionService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private OrganisationUnitGroupService organisationUnitGroupService;

    public void setOrganisationUnitGroupService( OrganisationUnitGroupService organisationUnitGroupService )
    {
        this.organisationUnitGroupService = organisationUnitGroupService;
    }

    private ValidationRuleService validationRuleService;

    public void setValidationRuleService( ValidationRuleService validationRuleService )
    {
        this.validationRuleService = validationRuleService;
    }

    private ExpressionService expressionService;

    public void setExpressionService( ExpressionService expressionService )
    {
        this.expressionService = expressionService;
    }

    private DataEntryFormService dataEntryFormService;

    public void setDataEntryFormService( DataEntryFormService dataEntryFormService )
    {
        this.dataEntryFormService = dataEntryFormService;
    }

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    // -------------------------------------------------------------------------
    // DataIntegrityService implementation
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // DataElement
    // -------------------------------------------------------------------------

    @Override
    public List<DataElement> getDataElementsWithoutDataSet()
    {
        return dataElementService.getDataElementsWithoutDataSets();
    }

    @Override
    public List<DataElement> getDataElementsWithoutGroups()
    {
        return dataElementService.getDataElementsWithoutGroups();
    }

    @Override
    public SortedMap<DataElement, Collection<DataSet>> getDataElementsAssignedToDataSetsWithDifferentPeriodTypes()
    {
        Collection<DataElement> dataElements = dataElementService.getAllDataElements();

        Collection<DataSet> dataSets = dataSetService.getAllDataSets();

        SortedMap<DataElement, Collection<DataSet>> targets = new TreeMap<>( IdentifiableObjectNameComparator.INSTANCE );

        for ( DataElement element : dataElements )
        {
            final Set<PeriodType> targetPeriodTypes = new HashSet<>();
            final Collection<DataSet> targetDataSets = new HashSet<>();

            for ( DataSet dataSet : dataSets )
            {
                if ( dataSet.getDataElements().contains( element ) )
                {
                    targetPeriodTypes.add( dataSet.getPeriodType() );
                    targetDataSets.add( dataSet );
                }
            }

            if ( targetPeriodTypes.size() > 1 )
            {
                targets.put( element, targetDataSets );
            }
        }

        return targets;
    }

    @Override
    public SortedMap<DataElement, Collection<DataElementGroup>> getDataElementsViolatingExclusiveGroupSets()
    {
        Collection<DataElementGroupSet> groupSets = dataElementService.getAllDataElementGroupSets();

        SortedMap<DataElement, Collection<DataElementGroup>> targets = new TreeMap<>( IdentifiableObjectNameComparator.INSTANCE );

        for ( DataElementGroupSet groupSet : groupSets )
        {
            Collection<DataElement> duplicates = getDuplicates(
                new ArrayList<>( groupSet.getDataElements() ), IdentifiableObjectNameComparator.INSTANCE );

            for ( DataElement duplicate : duplicates )
            {
                targets.put( duplicate, duplicate.getGroups() );
            }
        }

        return targets;
    }

    @Override
    public SortedMap<DataSet, Collection<DataElement>> getDataElementsInDataSetNotInForm()
    {
        SortedMap<DataSet, Collection<DataElement>> map = new TreeMap<>( IdentifiableObjectNameComparator.INSTANCE );

        Collection<DataSet> dataSets = dataSetService.getAllDataSets();

        for ( DataSet dataSet : dataSets )
        {
            if ( !dataSet.getFormType().isDefault() )
            {
                Set<DataElement> formElements = new HashSet<>();

                if ( dataSet.hasDataEntryForm() )
                {
                    formElements.addAll( dataEntryFormService.getDataElementsInDataEntryForm( dataSet ) );
                }
                else if ( dataSet.hasSections() )
                {
                    formElements.addAll( dataSet.getDataElementsInSections() );
                }

                Set<DataElement> dataSetElements = new HashSet<>( dataSet.getDataElements() );

                dataSetElements.removeAll( formElements );

                if ( dataSetElements.size() > 0 )
                {
                    map.put( dataSet, dataSetElements );
                }
            }
        }

        return map;
    }
    
    @Override
    public List<DataElementCategoryCombo> getInvalidCategoryCombos()
    {
        List<DataElementCategoryCombo> categoryCombos = categoryService.getAllDataElementCategoryCombos();
        
        return categoryCombos.stream().filter( c -> !c.isValid() ).collect( Collectors.toList() );
    }

    // -------------------------------------------------------------------------
    // DataSet
    // -------------------------------------------------------------------------

    @Override
    public SetMap<DataSet, DataElementOperand> getCategoryOptionCombosNotInDataElementCategoryCombo()
    {
        SetMap<DataSet, DataElementOperand> map = new SetMap<>();

        Collection<DataSet> dataSets = dataSetService.getAllDataSets();

        for ( DataSet dataSet : dataSets )
        {
            if ( dataSet.hasDataEntryForm() )
            {
                Set<DataElementOperand> operands = dataEntryFormService.getOperandsInDataEntryForm( dataSet );

                if ( operands != null )
                {
                    if ( operands.size() > 2000 )
                    {
                        log.warn( "Skipped integrity check for data set: " + dataSet.getName() + ", too many operands: " + operands.size() );
                        continue;
                    }

                    for ( DataElementOperand operand : operands )
                    {
                        DataElement dataElement = dataElementService.getDataElement( operand.getDataElementId() );
                        DataElementCategoryOptionCombo optionCombo = categoryService.getDataElementCategoryOptionCombo( operand.getOptionComboId() );
                        Set<DataElementCategoryOptionCombo> optionCombos = dataElement.getCategoryCombo() != null ? dataElement.getCategoryCombo().getOptionCombos() : null;

                        if ( optionCombos == null || !optionCombos.contains( optionCombo ) )
                        {
                            DataElementOperand persistedOperand = new DataElementOperand( dataElement, optionCombo );
                            map.putValue( dataSet, persistedOperand );
                        }
                    }
                }
            }
        }

        return map;
    }

    @Override
    public List<DataSet> getDataSetsNotAssignedToOrganisationUnits()
    {
        Collection<DataSet> dataSets = dataSetService.getAllDataSets();

        return dataSets.stream().filter( ds -> ds.getSources() == null || ds.getSources().isEmpty() ).collect( Collectors.toList() );
    }

    // -------------------------------------------------------------------------
    // Section
    // -------------------------------------------------------------------------

    @Override
    public List<Section> getSectionsWithInvalidCategoryCombinations()
    {
        List<Section> sections = new ArrayList<>();

        for ( Section section : sectionService.getAllSections() )
        {
            if ( section != null && section.categorComboIsInvalid() )
            {
                sections.add( section );
            }
        }

        return sections;
    }

    // -------------------------------------------------------------------------
    // Indicator
    // -------------------------------------------------------------------------

    @Override
    public Set<Set<Indicator>> getIndicatorsWithIdenticalFormulas()
    {
        Map<String, Indicator> formulas = new HashMap<>();

        Map<String, Set<Indicator>> targets = new HashMap<>();

        List<Indicator> indicators = indicatorService.getAllIndicators();

        for ( Indicator indicator : indicators )
        {
            final String formula = indicator.getNumerator() + FORMULA_SEPARATOR + indicator.getDenominator();

            if ( formulas.containsKey( formula ) )
            {
                if ( targets.containsKey( formula ) )
                {
                    targets.get( formula ).add( indicator );
                }
                else
                {
                    Set<Indicator> elements = new HashSet<>();

                    elements.add( indicator );
                    elements.add( formulas.get( formula ) );

                    targets.put( formula, elements );
                    targets.get( formula ).add( indicator );
                }
            }
            else
            {
                formulas.put( formula, indicator );
            }
        }

        return Sets.newHashSet( targets.values() );
    }

    @Override
    public List<Indicator> getIndicatorsWithoutGroups()
    {
        return indicatorService.getIndicatorsWithoutGroups();
    }

    @Override
    public SortedMap<Indicator, String> getInvalidIndicatorNumerators()
    {
        SortedMap<Indicator, String> invalids = new TreeMap<>( IdentifiableObjectNameComparator.INSTANCE );

        for ( Indicator indicator : indicatorService.getAllIndicators() )
        {
            ExpressionValidationOutcome result = expressionService.expressionIsValid( indicator.getNumerator() );

            if ( !result.isValid() )
            {
                invalids.put( indicator, result.getKey() );
            }
        }

        return invalids;
    }

    @Override
    public SortedMap<Indicator, String> getInvalidIndicatorDenominators()
    {
        SortedMap<Indicator, String> invalids = new TreeMap<>( IdentifiableObjectNameComparator.INSTANCE );

        for ( Indicator indicator : indicatorService.getAllIndicators() )
        {
            ExpressionValidationOutcome result = expressionService.expressionIsValid( indicator.getDenominator() );

            if ( !result.isValid() )
            {
                invalids.put( indicator, result.getKey() );
            }
        }

        return invalids;
    }

    @Override
    public SortedMap<Indicator, Collection<IndicatorGroup>> getIndicatorsViolatingExclusiveGroupSets()
    {
        Collection<IndicatorGroupSet> groupSets = indicatorService.getAllIndicatorGroupSets();

        SortedMap<Indicator, Collection<IndicatorGroup>> targets = new TreeMap<>( IdentifiableObjectNameComparator.INSTANCE );

        for ( IndicatorGroupSet groupSet : groupSets )
        {
            Collection<Indicator> duplicates = getDuplicates(
                new ArrayList<>( groupSet.getIndicators() ), IdentifiableObjectNameComparator.INSTANCE );

            for ( Indicator duplicate : duplicates )
            {
                targets.put( duplicate, duplicate.getGroups() );
            }
        }

        return targets;
    }

    // -------------------------------------------------------------------------
    // Period
    // -------------------------------------------------------------------------

    @Override
    public List<Period> getDuplicatePeriods()
    {
        Collection<Period> periods = periodService.getAllPeriods();

        List<Period> duplicates = new ArrayList<>();

        ListMap<String, Period> map = new ListMap<>();

        for ( Period period : periods )
        {
            String key = period.getPeriodType().getName() + period.getStartDate().toString();

            period.setName( period.toString() );

            map.putValue( key, period );
        }

        for ( String key : map.keySet() )
        {
            List<Period> values = map.get( key );

            if ( values != null && values.size() > 1 )
            {
                duplicates.addAll( values );
            }
        }

        return duplicates;
    }

    // -------------------------------------------------------------------------
    // OrganisationUnit
    // -------------------------------------------------------------------------

    @Override
    public Set<OrganisationUnit> getOrganisationUnitsWithCyclicReferences()
    {
        List<OrganisationUnit> organisationUnits = organisationUnitService.getAllOrganisationUnits();

        Set<OrganisationUnit> cyclic = new HashSet<>();

        Set<OrganisationUnit> visited = new HashSet<>();

        OrganisationUnit parent = null;

        for ( OrganisationUnit unit : organisationUnits )
        {
            parent = unit;

            while ( (parent = parent.getParent()) != null )
            {
                if ( parent.equals( unit ) ) // Cyclic reference
                {
                    cyclic.add( unit );

                    break;
                }
                else if ( visited.contains( parent ) ) // Ends in cyclic ref
                {
                    break;
                }
                else
                {
                    visited.add( parent ); // Remember visited
                }
            }

            visited.clear();
        }

        return cyclic;
    }

    @Override
    public List<OrganisationUnit> getOrphanedOrganisationUnits()
    {
        List<OrganisationUnit> units = organisationUnitService.getAllOrganisationUnits();
        
        return units.stream().filter( ou -> ou.getParent() == null && ( ou.getChildren() == null || ou.getChildren().size() == 0 ) ).collect( Collectors.toList() );
    }

    @Override
    public List<OrganisationUnit> getOrganisationUnitsWithoutGroups()
    {
        return organisationUnitService.getOrganisationUnitsWithoutGroups();
    }

    @Override
    public SortedMap<OrganisationUnit, Collection<OrganisationUnitGroup>> getOrganisationUnitsViolatingExclusiveGroupSets()
    {
        Collection<OrganisationUnitGroupSet> groupSets = organisationUnitGroupService.getAllOrganisationUnitGroupSets();

        TreeMap<OrganisationUnit, Collection<OrganisationUnitGroup>> targets =
            new TreeMap<>( IdentifiableObjectNameComparator.INSTANCE );

        for ( OrganisationUnitGroupSet groupSet : groupSets )
        {
            Collection<OrganisationUnit> duplicates = getDuplicates(
                new ArrayList<>( groupSet.getOrganisationUnits() ), IdentifiableObjectNameComparator.INSTANCE );

            for ( OrganisationUnit duplicate : duplicates )
            {
                targets.put( duplicate, new HashSet<>( duplicate.getGroups() ) );
            }
        }

        return targets;
    }

    @Override
    public List<OrganisationUnitGroup> getOrganisationUnitGroupsWithoutGroupSets()
    {
        Collection<OrganisationUnitGroup> groups = organisationUnitGroupService.getAllOrganisationUnitGroups();
        
        return groups.stream().filter( g -> g == null || g.getGroupSet() == null ).collect( Collectors.toList() );
    }

    // -------------------------------------------------------------------------
    // ValidationRule
    // -------------------------------------------------------------------------

    @Override
    public List<ValidationRule> getValidationRulesWithoutGroups()
    {
        Collection<ValidationRule> validationRules = validationRuleService.getAllValidationRules();
        
        return validationRules.stream().filter( r -> r.getGroups() == null || r.getGroups().isEmpty() ).collect( Collectors.toList() );
    }

    @Override
    public SortedMap<ValidationRule, String> getInvalidValidationRuleLeftSideExpressions()
    {
        SortedMap<ValidationRule, String> invalids = new TreeMap<>(
            IdentifiableObjectNameComparator.INSTANCE );

        for ( ValidationRule rule : validationRuleService.getAllValidationRules() )
        {
            ExpressionValidationOutcome result = expressionService.expressionIsValid( rule.getLeftSide().getExpression() );

            if ( !result.isValid() )
            {
                invalids.put( rule, result.getKey() );
            }
        }

        return invalids;
    }

    @Override
    public SortedMap<ValidationRule, String> getInvalidValidationRuleRightSideExpressions()
    {
        SortedMap<ValidationRule, String> invalids = new TreeMap<>(
            IdentifiableObjectNameComparator.INSTANCE );

        for ( ValidationRule rule : validationRuleService.getAllValidationRules() )
        {
            ExpressionValidationOutcome result = expressionService.expressionIsValid( rule.getRightSide().getExpression() );

            if ( !result.isValid() )
            {
                invalids.put( rule, result.getKey() );
            }
        }

        return invalids;
    }

    @Override
    public DataIntegrityReport getDataIntegrityReport()
    {
        DataIntegrityReport report = new DataIntegrityReport();
        
        report.setDataElementsWithoutDataSet( new ArrayList<>( getDataElementsWithoutDataSet() ) );
        report.setDataElementsWithoutGroups( new ArrayList<>( getDataElementsWithoutGroups() ) );
        report.setDataElementsAssignedToDataSetsWithDifferentPeriodTypes( getDataElementsAssignedToDataSetsWithDifferentPeriodTypes() );
        report.setDataElementsViolatingExclusiveGroupSets( getDataElementsViolatingExclusiveGroupSets() );
        report.setDataElementsInDataSetNotInForm( getDataElementsInDataSetNotInForm() );
        report.setInvalidCategoryCombos( getInvalidCategoryCombos() );

        log.info( "Checked data elements" );

        report.setCategoryOptionCombosNotInDataElementCategoryCombo( getCategoryOptionCombosNotInDataElementCategoryCombo() );
        report.setDataSetsNotAssignedToOrganisationUnits( new ArrayList<>( getDataSetsNotAssignedToOrganisationUnits() ) );
        report.setSectionsWithInvalidCategoryCombinations( new ArrayList<>( getSectionsWithInvalidCategoryCombinations() ) );

        log.info( "Checked data sets" );

        report.setIndicatorsWithIdenticalFormulas( getIndicatorsWithIdenticalFormulas() );
        report.setIndicatorsWithoutGroups( new ArrayList<>( getIndicatorsWithoutGroups() ) );
        report.setInvalidIndicatorNumerators( getInvalidIndicatorNumerators() );
        report.setInvalidIndicatorDenominators( getInvalidIndicatorDenominators() );
        report.setIndicatorsViolatingExclusiveGroupSets( getIndicatorsViolatingExclusiveGroupSets() );

        log.info( "Checked indicators" );

        report.setDuplicatePeriods( getDuplicatePeriods() );

        log.info( "Checked periods" );

        report.setOrganisationUnitsWithCyclicReferences( new ArrayList<>( getOrganisationUnitsWithCyclicReferences() ) );
        report.setOrphanedOrganisationUnits( new ArrayList<>( getOrphanedOrganisationUnits() ) );
        report.setOrganisationUnitsWithoutGroups( new ArrayList<>( getOrganisationUnitsWithoutGroups() ) );
        report.setOrganisationUnitsViolatingExclusiveGroupSets( getOrganisationUnitsViolatingExclusiveGroupSets() );
        report.setOrganisationUnitGroupsWithoutGroupSets( new ArrayList<>( getOrganisationUnitGroupsWithoutGroupSets() ) );
        report.setValidationRulesWithoutGroups( new ArrayList<>( getValidationRulesWithoutGroups() ) );

        log.info( "Checked organisation units" );

        report.setInvalidValidationRuleLeftSideExpressions( getInvalidValidationRuleLeftSideExpressions() );
        report.setInvalidValidationRuleRightSideExpressions( getInvalidValidationRuleRightSideExpressions() );

        log.info( "Checked validation rules" );

        Collections.sort( report.getDataElementsWithoutDataSet(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( report.getDataElementsWithoutGroups(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( report.getDataSetsNotAssignedToOrganisationUnits(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( report.getSectionsWithInvalidCategoryCombinations(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( report.getIndicatorsWithoutGroups(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( report.getOrganisationUnitsWithCyclicReferences(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( report.getOrphanedOrganisationUnits(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( report.getOrganisationUnitsWithoutGroups(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( report.getOrganisationUnitGroupsWithoutGroupSets(), IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( report.getValidationRulesWithoutGroups(), IdentifiableObjectNameComparator.INSTANCE );

        return report;
    }

    @Override
    public FlattenedDataIntegrityReport getFlattenedDataIntegrityReport()
    {
        return new FlattenedDataIntegrityReport( getDataIntegrityReport() );
    }
}
