/*
 * (c) Copyright 2004 Hewlett-Packard Development Company, LP
 * [See end of file]
 */

package com.hp.hpl.jena.rdf.arp.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.io.*;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.arp.*;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.*;

/**
 * @author Jeremy J. Carroll
 *
 */
public class SAX2RDFTest extends TestCase {
	static public Test suite() {
		TestSuite s = new TestSuite("SAX2RDF");
			
		s.addTest(new SAX2RDFTest("wg/",ARPTests.wgTestDir.toString(),"Manifest.rdf"));
		
		return s;
	}
	
	//final private String dir;
	final private String base;
	final private String file;
	SAX2RDFTest(String dir, String base0, String file){
		super(file);
		//this.dir = dir;
		this.base = base0+file;
		this.file = "testing/" +dir+file;
	}
	
	public void runTest() throws Exception {
		
		Model m = ModelFactory.createDefaultModel();
		Model m2 = ModelFactory.createDefaultModel();
		InputStream in = new FileInputStream( file);
		m.read(in,base);
		in.close();
		in = new FileInputStream( file);
		
		XMLReader saxParser = new SAXParser();
		SAX2RDF handler = SAX2Model.newInstance(base,m2);
		SAX2RDF.initialize(saxParser,handler);
		InputSource ins = new InputSource(in);
		ins.setSystemId(base);
		saxParser.parse(ins);
		in.close();
		
		assertTrue("Not isomorphic",m.isIsomorphicWith(m2));
		
	}

}


/*
 *  (c) Copyright 2004 Hewlett-Packard Development Company, LP
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
 
