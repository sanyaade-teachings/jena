/*
 * (c) Copyright 2005 Hewlett-Packard Development Company, LP
 * [See end of file]
 */

package com.hp.hpl.jena.ontology.tidy.errors;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.tidy.impl.MultipleTripleProblem;

/**
 * @author Jeremy J. Carroll
 *
 */
public class DeepMultiTripleDuplicateNodeProblem extends MultipleTripleProblem {

    final private int meet;
    /**
     * @param msg
     */
    public DeepMultiTripleDuplicateNodeProblem(int meetCase) {
        super("multi triple duplicate node");
        meet = meetCase;
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see com.hp.hpl.jena.ontology.tidy.impl.MultipleTripleProblem#getNode1()
     */
    public Node getNode1() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.hp.hpl.jena.ontology.tidy.impl.MultipleTripleProblem#getNode2()
     */
    public Node getNode2() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.hp.hpl.jena.ontology.tidy.SyntaxProblem2#getTypeCode()
     */
    public int getTypeCode() {
        return 10+MULTIPLE_TRIPLE_DUPLICATE_NODE+meet;
    }

}


/*
 *  (c) Copyright 2005 Hewlett-Packard Development Company, LP
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
 
