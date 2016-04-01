package org.hisp.dhis.common;

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

import java.util.List;
import java.util.function.BiConsumer;

import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.deletion.DeletionHandler;

/**
 * @author Lars Helge Overland
 */
public abstract class GenericAnalyticalObjectDeletionHandler<T extends AnalyticalObject>
    extends DeletionHandler
{
    protected abstract AnalyticalObjectService<T> getAnalyticalObjectService();
    
    @Override
    public void deleteIndicator( Indicator indicator )
    {
        removeItem( getAnalyticalObjectService().getAnalyticalObjects( indicator ), indicator, ( ao, di ) -> ao.removeDataDimensionItem( di ) );
    }
    
    @Override
    public void deleteDataElement( DataElement dataElement )
    {
        removeItem( getAnalyticalObjectService().getAnalyticalObjects( dataElement ), dataElement, ( ao, di ) -> ao.removeDataDimensionItem( di ) );
    }

    @Override
    public void deleteDataSet( DataSet dataSet )
    {
        removeItem( getAnalyticalObjectService().getAnalyticalObjects( dataSet ), dataSet, ( ao, di ) -> ao.removeDataDimensionItem( di ) );
    }

    @Override
    public void deletePeriod( Period period )
    {
        removeItem( getAnalyticalObjectService().getAnalyticalObjects( period ), period, ( ao, di ) -> ao.getPeriods().remove( di ) );
    }
    
    @Override
    public void deleteOrganisationUnit( OrganisationUnit organisationUnit )
    {
        removeItem( getAnalyticalObjectService().getAnalyticalObjects( organisationUnit ), organisationUnit, ( ao, di ) -> ao.getOrganisationUnits().remove( di ) );
    }
    
    @Override
    public void deleteCategoryOptionGroup( CategoryOptionGroup categoryOptionGroup )
    {
        removeItem( getAnalyticalObjectService().getAnalyticalObjects( categoryOptionGroup ), categoryOptionGroup, ( ao, di ) -> ao.getCategoryOptionGroups().remove( di ) );
    }
    
    protected void removeItem( List<T> analyticalObjects, DimensionalItemObject itemObject, BiConsumer<AnalyticalObject, DimensionalItemObject> updateOperation )
    {
        for ( T analyticalObject : analyticalObjects )
        {
            updateOperation.accept( analyticalObject, itemObject );
            
            getAnalyticalObjectService().update( analyticalObject );
        }
    }
}
