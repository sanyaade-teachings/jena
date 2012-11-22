/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.riot.stream;

import java.io.InputStream ;

import org.apache.jena.riot.RDFLanguages ;
import org.apache.jena.riot.TypedInputStream2 ;
import org.openjena.atlas.web.ContentType ;
import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;

public class LocatorClassLoader  implements Locator
{
    static Logger log = LoggerFactory.getLogger(LocatorClassLoader.class) ;

    private final ClassLoader classLoader ;
    public LocatorClassLoader(ClassLoader _classLoader)
    {
        classLoader =_classLoader ;
    }
    
    @Override
    public boolean equals( Object other )
    {
        return 
            other instanceof LocatorClassLoader 
            && classLoader == ((LocatorClassLoader) other).classLoader;
    }
    
    @Override
    public int hashCode()
        { return classLoader.hashCode(); }
    
    @Override
    public TypedInputStream2 open(String resourceName)
    {
        if ( classLoader == null )
            return null ;
            
        InputStream in = classLoader.getResourceAsStream(resourceName) ;
        if ( in == null )
        {
            if ( StreamManager.logAllLookups && log.isTraceEnabled() )
                log.trace("Failed to open: "+resourceName) ;
            return null ;
        }
        
        if ( StreamManager.logAllLookups  && log.isTraceEnabled() )
            log.trace("Found: "+resourceName) ;
        
        ContentType ct = RDFLanguages.guessContentType(resourceName) ;
        // No sensible base URI.
        return new TypedInputStream2(in, ct, null) ;
    }
    
    public ClassLoader getClassLoader()
    {
        return classLoader ;
    }

    @Override
    public String getName() { return "ClassLoaderLocator" ; }
    
}
