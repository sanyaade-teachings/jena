/*
 * (c) Copyright 2004, 2005, 2006, 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package dev;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.hp.hpl.jena.query.expr.E_Function;
import com.hp.hpl.jena.query.expr.NodeValue;
import com.hp.hpl.jena.query.junit.QueryTestSuiteFactory;

import engine3.QueryEngineX;

public class TestEngine3 extends TestCase
{
    public static TestSuite suite()
    {
        NodeValue.VerboseWarnings = false ;
        E_Function.WarnOnUnknownFunction = false ;
        QueryEngineX.register() ;
        
        TestSuite ts = QueryTestSuiteFactory.make("../ARQ/testing/ARQ/manifest-engine2.ttl") ;

        //TestSuite ts = QueryTestSuiteFactory.make("../ARQ/testing/ARQ/Basic/manifest.ttl") ;
        //TestSuite ts = QueryTestSuiteFactory.make("../ARQ/testing/ARQ/Algebra/manifest.ttl") ;
        
        // SPARQL test suite (does not test algebra)
        //TestSuite ts = QueryTestSuiteFactory.make("../ARQ/testing/ARQ/manifest-arq.ttl") ;
        return ts ;
    }
}

/*
 * (c) Copyright 2004, 2005, 2006, 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */