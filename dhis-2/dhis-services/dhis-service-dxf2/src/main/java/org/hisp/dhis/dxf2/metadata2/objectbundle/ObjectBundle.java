package org.hisp.dhis.dxf2.metadata2.objectbundle;

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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dxf2.metadata2.FlushMode;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ObjectBundle
{
    private final User user;

    private final ObjectBundleMode objectBundleMode;

    private final PreheatIdentifier preheatIdentifier;

    private final PreheatMode preheatMode;

    private final ImportStrategy importMode;

    private final MergeMode mergeMode;

    private final FlushMode flushMode;

    private final Preheat preheat;

    private ObjectBundleStatus objectBundleStatus = ObjectBundleStatus.CREATED;

    private Map<Boolean, Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>>> objects = new HashMap<>();

    private Map<Class<?>, Map<String, Map<String, Object>>> objectReferences = new HashMap<>();

    public ObjectBundle( ObjectBundleParams params, Preheat preheat, Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objectMap )
    {
        if ( !objects.containsKey( Boolean.TRUE ) ) objects.put( Boolean.TRUE, new HashMap<>() );
        if ( !objects.containsKey( Boolean.FALSE ) ) objects.put( Boolean.FALSE, new HashMap<>() );

        this.user = params.getUser();
        this.objectBundleMode = params.getObjectBundleMode();
        this.preheatIdentifier = params.getPreheatIdentifier();
        this.importMode = params.getImportMode();
        this.preheatMode = params.getPreheatMode();
        this.mergeMode = params.getMergeMode();
        this.flushMode = params.getFlushMode();
        this.preheat = preheat;

        addObject( objectMap );
    }

    public User getUser()
    {
        return user;
    }

    public ObjectBundleMode getObjectBundleMode()
    {
        return objectBundleMode;
    }

    public PreheatIdentifier getPreheatIdentifier()
    {
        return preheatIdentifier;
    }

    public PreheatMode getPreheatMode()
    {
        return preheatMode;
    }

    public ImportStrategy getImportMode()
    {
        return importMode;
    }

    public MergeMode getMergeMode()
    {
        return mergeMode;
    }

    public FlushMode getFlushMode()
    {
        return flushMode;
    }

    public ObjectBundleStatus getObjectBundleStatus()
    {
        return objectBundleStatus;
    }

    public void setObjectBundleStatus( ObjectBundleStatus objectBundleStatus )
    {
        this.objectBundleStatus = objectBundleStatus;
    }

    public Preheat getPreheat()
    {
        return preheat;
    }

    private void addObject( IdentifiableObject object )
    {
        if ( object == null )
        {
            return;
        }

        if ( !objects.get( Boolean.TRUE ).containsKey( object.getClass() ) )
        {
            objects.get( Boolean.TRUE ).put( object.getClass(), new ArrayList<>() );
        }

        if ( !objects.get( Boolean.FALSE ).containsKey( object.getClass() ) )
        {
            objects.get( Boolean.FALSE ).put( object.getClass(), new ArrayList<>() );
        }

        if ( isPersisted( object ) )
        {
            objects.get( Boolean.TRUE ).get( object.getClass() ).add( object );
        }
        else
        {
            objects.get( Boolean.FALSE ).get( object.getClass() ).add( object );

        }
    }

    private void addObject( List<IdentifiableObject> objects )
    {
        objects.forEach( this::addObject );
    }

    private void addObject( Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects )
    {
        objects.keySet().forEach( klass -> addObject( objects.get( klass ) ) );
    }

    public Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> getObjectMap()
    {
        Set<Class<? extends IdentifiableObject>> klasses = new HashSet<>();
        klasses.addAll( objects.get( Boolean.TRUE ).keySet() );
        klasses.addAll( objects.get( Boolean.FALSE ).keySet() );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objectMap = new HashMap<>();

        klasses.forEach( klass -> {
            objectMap.put( klass, new ArrayList<>() );
            objectMap.get( klass ).addAll( objects.get( Boolean.TRUE ).get( klass ) );
            objectMap.get( klass ).addAll( objects.get( Boolean.FALSE ).get( klass ) );
        } );

        return objectMap;
    }

    public List<IdentifiableObject> getObjects( Class<? extends IdentifiableObject> klass, boolean persisted )
    {
        List<IdentifiableObject> identifiableObjects = null;

        if ( persisted )
        {
            if ( objects.get( Boolean.TRUE ).containsKey( klass ) )
            {
                identifiableObjects = objects.get( Boolean.TRUE ).get( klass );
            }
        }
        else
        {
            if ( objects.get( Boolean.FALSE ).containsKey( klass ) )
            {
                identifiableObjects = objects.get( Boolean.FALSE ).get( klass );
            }
        }

        return identifiableObjects != null ? identifiableObjects : new ArrayList<>();
    }

    public Map<String, Map<String, Object>> getObjectReferences( Class<?> klass )
    {
        return objectReferences.get( klass );
    }

    public Map<Class<?>, Map<String, Map<String, Object>>> getObjectReferences()
    {
        return objectReferences;
    }

    public void setObjectReferences( Map<Class<?>, Map<String, Map<String, Object>>> objectReferences )
    {
        this.objectReferences = objectReferences;
    }

    public boolean isPersisted( IdentifiableObject object )
    {
        IdentifiableObject cachedObject = preheat.get( preheatIdentifier, object );
        return !(cachedObject == null || cachedObject.getId() == 0);
    }
}
