/*
  (c) Copyright 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
  [See end of file]
  $Id: Map1Iterator.java,v 1.12 2009-01-20 15:12:29 chris-dollin Exp $
*/

package com.hp.hpl.jena.util.iterator;

import java.util.Iterator;

/**
    An iterator that consumes an underlying iterator and maps its results before
    delivering them; supports remove if the underlying iterator does.
    @author jjc + kers
    @version  Release='$Name: not supported by cvs2svn $' Revision='$Revision: 1.12 $' Date='$Date: 2009-01-20 15:12:29 $'
*/

public class Map1Iterator extends NiceIterator implements ClosableIterator
    {
	private Map1 map;
	private Iterator base;
	
        /**
         * Construct a list of the converted.
         * @param m The conversion to apply.
         * @param it the iterator of elements to convert
         */
	public Map1Iterator( Map1 map, Iterator base ) 
        {
        this.map = map;
        this.base = base;
        }
    
	public @Override Object next() 
        { return map.map1( base.next() ); }
	
	public @Override boolean hasNext()
	    { return base.hasNext(); }
	
	public @Override void remove()
	    { base.remove(); }
    }
/*
 * (c) Copyright 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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
 *
 *
 */
