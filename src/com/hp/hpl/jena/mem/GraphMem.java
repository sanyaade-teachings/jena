/*
  (c) Copyright 2002, Hewlett-Packard Development Company, LP
  [See end of file]
  $Id: GraphMem.java,v 1.30 2004-06-30 12:58:00 chris-dollin Exp $
*/

package com.hp.hpl.jena.mem;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.impl.*;
import com.hp.hpl.jena.graph.query.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.util.HashUtils;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.WrappedIterator;

import java.util.*;

/**
    A memory-backed graph with S/P/O indexes. A GraphMem maintains a 
    reference count, set to one when it is created, and incremented by the method
    <code>openAgain()</code>. When the graph is closed, the count is decrememented,
    and when it reaches 0, the tables are trashed and GraphBase.close() called.
    Thus in normal use one close is enough, but GraphMakers using GraphMems
    can arrange to re-use the same named graph.
    
    @author  bwm, kers
*/
public class GraphMem extends GraphBase implements Graph 
    {
    /** the set storing all the triples in this GraphMem */
    Set triples = HashUtils.createSet();

    NodeToTriplesMap subjects = new NodeToTriplesMap();
    NodeToTriplesMap predicates = new NodeToTriplesMap();
    NodeToTriplesMap objects = new NodeToTriplesMap();

    protected int count;
    
    /**
        Initialises a GraphMem with the Minimal reification style
    */
    public GraphMem() 
        { this( ReificationStyle.Minimal ); }
    
    /**
        Initialises a GraphMem with the given reification style.
    */
    public GraphMem( ReificationStyle style )
        { 
        super( style );
        count = 1; 
        }

    public void close()
        {
        if (--count == 0)
            {
            triples = null;
            subjects = predicates = objects = null;
            super.close();
            }
        }
        
    public GraphMem openAgain()
        { 
        count += 1; 
        return this;
        }
        
    public void performAdd( Triple t )
        {
        if (getReifier().handledAdd( t ) || triples.contains( t ))
            return;
        else if (triples.add( t ))
            { subjects.add( t.getSubject(), t );
            predicates.add( t.getPredicate(), t );
            objects.add( t.getObject(), t ); }
        }

    public void performDelete( Triple t )
        {
        if (getReifier().handledRemove( t ))
            return;
        else if (triples.remove( t ))
            { subjects.remove( t.getSubject(), t );
            predicates.remove( t.getPredicate(), t );
            objects.remove( t.getObject(), t ); }
        }

    public int size()  
        {
        checkOpen();
        return triples.size();
        }

    public boolean isEmpty()
        {
        checkOpen();
        return triples.isEmpty();
        }
        
    private QueryHandler q;
    
    public QueryHandler queryHandler()
        {
        if (q == null) q = new GraphMemQueryHandler( this );
        return q;
        }
        
    private static class GraphMemQueryHandler extends SimpleQueryHandler
        {
        GraphMemQueryHandler( GraphMem graph ) 
            { 
            super( graph );
            }
        
        public ExtendedIterator objectsFor( Node p, Node o )
            {
            return p == null && o == null ? findObjects() : super.objectsFor( p, o );
            }
        
        public ExtendedIterator subjectsFor( Node p, Node o )
            {
            return p == null && o == null ? findSubjects() : super.subjectsFor( p, o );
            }   
        
        public ExtendedIterator findObjects()
            {
            return WrappedIterator.create( ((GraphMem) graph).objects.domain() );
            }
        
        public ExtendedIterator findSubjects()
            {
            return WrappedIterator.create( ((GraphMem) graph).subjects.domain() );
            }
        }
        
    /**
        Answer true iff t matches some triple in the graph. If t is concrete, we
        can use a simple membership test; otherwise we resort to the generic
        method using find.
    */
    public boolean contains( Triple t ) {
        checkOpen();
        return t.isConcrete() ? triples.contains( t ) : containsByFind( t );
    }

    /**
        Answer true if there's some triple in the graph that (s, p, o) matches.
        Ensures that nulls are not present and then defers to contains(Triple). 
    */
    public boolean contains( Node s, Node p, Node o ) {
        checkOpen();
        if (s == null || p == null || o == null) throw new JenaException( "null not allowed" );
        return contains( Triple.create( s, p, o ) );
    }

    /** Returns an iterator over Triple.
     */
    public ExtendedIterator find( TripleMatch m ) {
        checkOpen();
        Triple tm = m.asTriple();
        Node p = m.getMatchPredicate();
        Node o = m.getMatchObject();
        Node ms = tm.getSubject();
        // @@ some redundant compares in this code which could be improved
        if (ms.isConcrete()) {
            return subjectIterator(tm, ms);
        } else if (o != null && !o.isLiteral()) {
            // der - added guard on isLiteral to support typed literal semantics
            return objectIterator(tm, o);
        } else if (p != null) {
            return predicateIterator( tm, p );
        } else {
            return baseIterator( tm );
        }
    }

    protected TripleMatchIterator objectIterator(Triple tm, Node o)
        { return new TripleFieldIterator
            ( tm, objects.iterator( o ), triples, subjects, predicates ){
            public void remove()
                {
                super.remove();
                subjects.remove( current.getSubject(), current );
                predicates.remove( current.getPredicate(), current );
                }
            }
            ; 
        }

    protected TripleMatchIterator subjectIterator(Triple tm, Node ms)
        { return new TripleFieldIterator
            ( tm, subjects.iterator( ms ), triples, predicates, objects )
            {
            public void remove()
                {
                super.remove();
                predicates.remove( current.getPredicate(), current );
                objects.remove( current.getObject(), current );
                }
            }
            ; 
        }

    protected TripleMatchIterator predicateIterator(Triple tm, Node p)
        { return new TripleFieldIterator
            (tm, predicates.iterator( p ), triples, subjects, objects ){
            public void remove()
                {
                super.remove();
                subjects.remove( current.getSubject(), current );
                objects.remove( current.getObject(), current );
                }
            };
        }

    protected ExtendedIterator baseIterator( Triple t )
        {
        return new TrackingTripleIterator( t, triples.iterator() )
            {
            public void remove()
                {
                super.remove();    
                subjects.remove( current.getSubject(), current );
                predicates.remove( current.getPredicate(), current );
                objects.remove( current.getObject(), current );
                }
            };
        }
    

}

/*
 *  (c) Copyright 2000, 2001 Hewlett-Packard Development Company, LP
 *  All rights reserved.
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