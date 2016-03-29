package org.hisp.dhis.analytics;

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

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;

import com.google.common.collect.Lists;

import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElement;
import org.hisp.dhis.program.ProgramIndicator;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lars Helge Overland
 */
public class AnalyticsUtilsTest extends DhisSpringTest
{
    @Test
    public void testGetByDataDimensionType()
    {
        Program prA = createProgram( 'A' );
        
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        ProgramDataElement pdeA = new ProgramDataElement( prA, deA );
        ProgramDataElement pdeB = new ProgramDataElement( prA, deB );
        ProgramIndicator piA = createProgramIndicator( 'A', prA, null, null );
        
        List<DimensionalItemObject> list = Lists.newArrayList( deA, deB, pdeA, pdeB, piA );
        
        assertEquals( Lists.newArrayList( deA, deB ), AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.AGGREGATE_DATA_ELEMENT, list ) );
        assertEquals( Lists.newArrayList( pdeA, pdeB ), AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.PROGRAM_DATA_ELEMENT, list ) );
        assertEquals( Lists.newArrayList( piA ), AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.PROGRAM_INDICATOR, list ) );
        assertEquals( Lists.newArrayList(), AnalyticsUtils.getByDataDimensionType( DataDimensionItemType.PROGRAM_ATTRIBUTE, list ) );
    }
    
    @Test
    public void testConvertDxToOperand()
    {
        Map<String, Double> map = new HashMap<>();
        map.put( "GauDLAiXPKT-kC1OT9Q1n1j-R9U8q7X1aJG", 10d );
        map.put( "YkRvCLedQa4-h1dJ9W4dWor-Zrd4DAf8M99", 11d );
        map.put( "PcfRp1HETO8-zqXKIEycBck-KBJBZopYMPV", 12d );
        
        Map<String, Double> convertedMap = AnalyticsUtils.convertDxToOperand( map );
        
        assertTrue( convertedMap.containsKey( "GauDLAiXPKT.kC1OT9Q1n1j-R9U8q7X1aJG" ) );
        assertTrue( convertedMap.containsKey( "YkRvCLedQa4.h1dJ9W4dWor-Zrd4DAf8M99" ) );
        assertTrue( convertedMap.containsKey( "PcfRp1HETO8.zqXKIEycBck-KBJBZopYMPV" ) );
    }
    
    @Test
    public void testGetRoundedValueObject()
    {
        DataQueryParams paramsA = new DataQueryParams();
        DataQueryParams paramsB = new DataQueryParams();
        paramsB.setSkipRounding( true );
        
        assertEquals( null, AnalyticsUtils.getRoundedValueObject( paramsA, null ) );
        assertEquals( "Car", AnalyticsUtils.getRoundedValueObject( paramsA, "Car" ) );
        assertEquals( 3d, AnalyticsUtils.getRoundedValueObject( paramsA, 3d ) );
        assertEquals( 3.1, (Double) AnalyticsUtils.getRoundedValueObject( paramsA, 3.123 ), 0.01 );
        assertEquals( 3.123, (Double) AnalyticsUtils.getRoundedValueObject( paramsB, 3.123 ), 0.01 );
    }

    @Test
    public void testGetRoundedValueDouble()
    {
        DataQueryParams paramsA = new DataQueryParams();
        DataQueryParams paramsB = new DataQueryParams();
        paramsB.setSkipRounding( true );
        
        assertEquals( null, AnalyticsUtils.getRoundedValue( paramsA, null, null ) );
        assertEquals( 3d, AnalyticsUtils.getRoundedValue( paramsA, null, 3d ), 0.01 );
        assertEquals( 3.1, AnalyticsUtils.getRoundedValue( paramsA, null, 3.123 ), 0.01 );
        assertEquals( 3.1, AnalyticsUtils.getRoundedValue( paramsA, 1, 3.123 ), 0.01 );
        assertEquals( 3.12, AnalyticsUtils.getRoundedValue( paramsA, 2, 3.123 ), 0.01 );
        assertEquals( 3.123, AnalyticsUtils.getRoundedValue( paramsB, 2, 3.123 ), 0.01 );
    }
}
