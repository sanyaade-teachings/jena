/*
 * (c) Copyright 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.engine.iterator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.Binding0;
import com.hp.hpl.jena.sparql.engine.binding.BindingKey;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.expr.E_Aggregator;

public class QueryIterGroup extends QueryIterPlainWrapper
{
    public QueryIterGroup(QueryIterator qIter, 
                          List groupVars,
                          Map groupExprs,
                          List aggregators,
                          ExecutionContext execCxt)
    {
        super(null, execCxt) ;
        Iterator iter = calc(qIter, groupVars, aggregators) ;
        setIterator(iter) ;
    }

    // Phase 1 : Consume the input iterator, assigning groups (keys) 
    //           and push rows through the aggregator function. 
    
    // Phase 2 : Go over the group bindings and assign the value of each aggregation.
    
    private static Iterator calc(QueryIterator iter, List groupVars, List aggregators)
    {
        // Stage 1 : assign bindings to buckets by key and pump through the aggregrators.
        
        // Could also be a Set key=>binding because BindingKey has the necessary entry-like quality. 
        Map buckets = new HashMap() ;    
        
        for ( ; iter.hasNext() ; )
        {
            Binding b = iter.nextBinding() ;
            BindingKey key = genKey(groupVars, b) ;
            
            // Assumes key binding has value based .equals/.hashCode. 
            if ( ! buckets.containsKey(key) )
                buckets.put(key, key.getBinding()) ;
            
            // Assumes an aggregator is a per-execution mutable thingy
            if ( aggregators != null )
            {
                for ( Iterator aggIter = aggregators.iterator() ; aggIter.hasNext() ; )
                {
                    E_Aggregator agg = (E_Aggregator)aggIter.next();
                    agg.getAggregator().accumulate(key, b) ;
                }
            }
        }
        
        // Stage 2 : for each bucket, get binding, add aggregator values
        // (Key is the first binding we saw for the group (projected to the group vars)).
        
        // If it is null, nothing to do.
        if ( aggregators != null )
        {
            for ( Iterator bIter = buckets.keySet().iterator() ; bIter.hasNext(); )
            {
                BindingKey key = (BindingKey)bIter.next();
                Binding binding = (Binding)buckets.get(key) ; // == key.getBinding() ;
                for ( Iterator aggIter = aggregators.iterator() ; aggIter.hasNext() ; )
                {
                    E_Aggregator agg = (E_Aggregator)aggIter.next();
                    Var v = agg.asVar() ;
                    Node value =  agg.getAggregator().getValue(key) ;
                    // Extend with the aggregations.
                    binding.add(v, value) ;
                }
            }
        }

        // Results - the binding modified by the aggregations.
        
        return buckets.values().iterator() ;
    }
    
    static private BindingKey genKey(List vars, Binding binding) 
    {
        return new BindingKey(copyProject(vars, binding)) ;
    }
    
    static private Binding copyProject(List vars, Binding binding)
    {
        // No group vars (implicit or explicit) => working on whole result set. 
        if ( vars.size() == 0 )
        { return new Binding0() ; }
        
        Binding x = new BindingMap() ;
        for ( Iterator iter = vars.listIterator() ; iter.hasNext() ; )
        {
            Var var = (Var)iter.next() ;
            Node node = binding.get(var) ;
            if ( node != null )
                x.add(var, node) ;
        }
        return x ;
    }
}



/*
 * (c) Copyright 2007 Hewlett-Packard Development Company, LP
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


/*
 * (c) Copyright 2007 Hewlett-Packard Development Company, LP
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