/*
 * (c) Copyright 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package dev.idx2;

import static com.hp.hpl.jena.tdb.sys.SystemTDB.SizeOfNodeId;
import static java.lang.String.format;
import iterator.*;

import java.util.Iterator;

import lib.Bytes;
import lib.Tuple;

import com.hp.hpl.jena.sparql.core.Closeable;

import com.hp.hpl.jena.tdb.TDBException;
import com.hp.hpl.jena.tdb.base.record.Record;
import com.hp.hpl.jena.tdb.base.record.RecordFactory;
import com.hp.hpl.jena.tdb.index.RangeIndex;
import com.hp.hpl.jena.tdb.lib.Sync;
import com.hp.hpl.jena.tdb.pgraph.NodeId;

public class TupleIndex implements Sync, Closeable
{
    private static final boolean Check = false ;
    private RangeIndex index ; 
    private final int tupleLength ;
    private RecordFactory factory ;
    private Desc descriptor ;
    private ColumnMap colMap ;
    
    public TupleIndex(int N,  ColumnMap colMapping, RecordFactory factory, RangeIndex index)
    {
        this.tupleLength = N ;
        this.factory = factory ;
        this.descriptor = new Desc(colMapping, factory) ;
        this.colMap = colMapping ;
        this.index = index ;
        if ( factory.keyLength() != N*SizeOfNodeId)
            throw new TDBException(format("Mismatch: TupleIndex of length %d is not comparative with a factory for key length %d", N, factory.keyLength())) ;
    }
    
    /** Insert a tuple - return true if it was really added, false if it was a duplicate */
    public boolean add( Tuple<NodeId> tuple) 
    { 
        if ( Check )
        {
            if ( tupleLength != tuple.size() )
            throw new TDBException(String.format("Mismatch: tuple length %d / index for length %d", tuple.size(), tupleLength)) ;
        }

        Record r = descriptor.record(tuple) ;
        return index.add(r) ;
    }
    /** Delete a tuple - return true if it was deleted, false if it didn't exist */
    public boolean delete( Tuple<NodeId> tuple ) 
    { 
        if ( Check )
        {
            if ( tupleLength != tuple.size() )
            throw new TDBException(String.format("Mismatch: tuple length %d / index for length %d", tuple.size(), tupleLength)) ;
        }

        Record r = descriptor.record(tuple) ;
        return index.delete(r) ;
    }
    
    public Desc getDesc() { return descriptor ;  } 
    public ColumnMap getColMap() { return colMap ;  }
    
    Iterator<Tuple<NodeId>> findOrScan(Tuple<NodeId> pattern)
    {
        return findWorker(pattern, true, true) ;
    }
    
    Iterator<Tuple<NodeId>> findOrPartialScan(Tuple<NodeId> pattern)
    {
        return findWorker(pattern, true, false) ;
    }

    Iterator<Tuple<NodeId>> findByIndex(Tuple<NodeId> pattern)
    {
        return findWorker(pattern, false, false) ;
    }
    
    /** Find all matching tuples - a slot of NodeId.NodeIdAny (or null) means match any.
     *  Return null if a full scan is needed.
     */

    public Iterator<Tuple<NodeId>> find(Tuple<NodeId> pattern)
    {
        return findOrScan(pattern) ;
    }
    
    private Iterator<Tuple<NodeId>> findWorker(Tuple<NodeId> pattern, boolean partialScanAllowed, boolean fullScanAllowed)
    {
        if ( Check )
        {
            if ( tupleLength != pattern.size() )
            throw new TDBException(String.format("Mismatch: tuple length %d / index for length %d", pattern.size(), tupleLength)) ;
        } 
        
        // Convert to index order.
        pattern = colMap.map(pattern) ;
        
        NodeId[] pattern2 = new NodeId[pattern.size()] ;
        
        // Canonical form.
        int numSlots = 0 ;
        int leadingIdx = -2;    // Index of last leading pattern NodeId.  Start less than numSlots-1
        boolean leading = true ;
        // Records.
        Record minRec = factory.createKeyOnly() ;
        Record maxRec = factory.createKeyOnly() ;
        
        for ( int i = 0 ; i < pattern.size() ; i++ )
        {
            //int j = colMap.mapOrder(i) ;        // Map. ????
            int j = i ;
            
            pattern2[i] = pattern.get(j) ;
            if ( pattern2[i] == NodeId.NodeIdAny )
                pattern2[i] = null ;
            
            NodeId X = pattern2[i] ;
            if ( X != null )
            {
                numSlots++ ;
                if ( leading )
                {
                    leadingIdx = i ;
                    Bytes.setLong(X.getId(), minRec.getKey(), i*SizeOfNodeId) ;
                    Bytes.setLong(X.getId(), maxRec.getKey(), i*SizeOfNodeId) ;
                }
            }
            else
                // Not leading key slots.
                leading = false ;
        }

        // Is it a simple existence test?
        if ( numSlots == pattern.size() )
        {
            if ( index.contains(minRec) )
                return new SingletonIterator<Tuple<NodeId>>(pattern) ;  
            else
                return new NullIterator<Tuple<NodeId>>() ; 
        }
        
        Iterator<Record> iter = null ;
        
        if ( leadingIdx < 0 )
        {
            if ( ! fullScanAllowed )
                return null ;
            System.out.println("Full scan") ;
            // Full scan necessary
            iter = index.iterator() ;
        }
        else 
        {
            // Adjust the maxRec.
            NodeId X = pattern2[leadingIdx] ;
            // Set the max Record to the leading NodeIds, +1.
            // Example, SP? inclusive to S(P+1)? exclusive where ? is zero. 
            Bytes.setLong(X.getId()+1, maxRec.getKey(), leadingIdx*SizeOfNodeId) ;
            iter = index.iterator(minRec, maxRec) ;
        }
        
        Iterator<Tuple<NodeId>> tuples = Iter.map(iter, transformToTuple) ;
        
        if ( leadingIdx < numSlots-1 )
        {
            if ( ! partialScanAllowed )
                return null ;
            
            System.out.println("Partial scan") ;
            // Didn't match all defined slots in request.  
            // Partial or full scan needed.
            tuples = scan(tuples, pattern) ;
        }
        
        return tuples ;
    }
    
    public Iterator<Tuple<NodeId>> scan(Tuple<NodeId> pattern)
    {
        return scan(all(), pattern) ;
    }
    
    public Iterator<Tuple<NodeId>> all()
    {
        Iterator<Record> iter = index.iterator() ;
        return Iter.map(iter, transformToTuple) ;
    }
    
    private Transform<Record, Tuple<NodeId>> transformToTuple = new Transform<Record, Tuple<NodeId>>()
    {
        @Override
        public Tuple<NodeId> convert(Record item)
        {
            return descriptor.tuple(item) ;
        }
    } ; 
    
    private Iterator<Tuple<NodeId>> scan(Iterator<Tuple<NodeId>> iter,
                                         final Tuple<NodeId> pattern)
    {
        Filter<Tuple<NodeId>> filter = new Filter<Tuple<NodeId>>()
        {
            @Override
            public boolean accept(Tuple<NodeId> item)
            {
                // Check on pattern
                for ( int i = 0 ; i < tupleLength ; i++ )
                {
                    // The pattern must be null or match the tuple being tested.
                    if ( pattern.get(i) != null )
                        if ( ! item.get(i).equals(pattern.get(i)) ) 
                            return false ;
                }
                return true ;
            }
        } ;
        
        return Iter.filter(iter, filter) ;
    }
    
    public int weight(Tuple<NodeId> pattern)
    {
        if ( Check )
        {
            if ( tupleLength != pattern.size() )
            throw new TDBException(String.format("Mismatch: tuple length %d / index for length %d", pattern.size(), tupleLength)) ;
        } 
        
        for ( int i = 0 ; i < tupleLength ; i++ )
        {
            NodeId X = getSlot(i, pattern) ;
            if ( X == null ) return i ;
        }
        return 0 ;
    }
    
    private NodeId getSlot(int i, Tuple<NodeId> pattern)
    {
        return descriptor.extract(i, pattern.get(0), pattern.get(1), pattern.get(2)) ;
    }

    @Override
    public void close()
    {
        index.close();
    }
    
    @Override
    public void sync(boolean force)
    {
        index.sync(force) ;
    }

    @Override
    public String toString() { return "index:"+descriptor.getLabel() ; }
}

/*
 * (c) Copyright 2008 Hewlett-Packard Development Company, LP
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