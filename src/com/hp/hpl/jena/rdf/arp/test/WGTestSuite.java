/*
    (c) Copyright 2001, 2002, 2003, 2004, 2005 Hewlett-Packard Development Company, LP
    [See end of file]
    $Id: WGTestSuite.java,v 1.29 2005-08-09 03:30:17 jeremy_carroll Exp $
*/

package com.hp.hpl.jena.rdf.arp.test;

import junit.framework.*;
import java.io.*;

import com.hp.hpl.jena.iri.IRIFactory;
import com.hp.hpl.jena.iri.RDFURIReference;
import com.hp.hpl.jena.iri.impl.XercesURIWrapper;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.*;
import java.util.*;
import com.hp.hpl.jena.rdf.arp.*;
import com.hp.hpl.jena.vocabulary.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.shared.impl.JenaParameters;
import com.hp.hpl.jena.shared.wg.*;

import com.hp.hpl.jena.reasoner.test.*;
import com.hp.hpl.jena.reasoner.rulesys.*;

import org.xml.sax.*;
/**
 *
 * @author  jjc
 */
class WGTestSuite extends TestSuite implements ARPErrorNumbers {
	static private Resource jena2;
	static private Model testResults;
	static private void initResults() {
		logging = true;
		testResults = ModelFactory.createDefaultModel();
		jena2 = testResults.createResource(BASE_RESULTS_URI + "#jena2");
		jena2.addProperty(RDFS.comment, 
			testResults.createLiteral(
				"<a xmlns=\"http://www.w3.org/1999/xhtml\" href=\"http://jena.sourceforce.net/\">Jena2</a> is a" +
				" Semantic Web framework in Java" +
				" available from <a xmlns=\"http://www.w3.org/1999/xhtml\" href=\"http://www.sourceforce.net/projects/jena\">" +
				"sourceforge</a> CVS.",
				true)
		);
		jena2.addProperty(RDFS.label, "Jena2");
		testResults.setNsPrefix("results", OWLResults.NS);
	}
	static void logResult(Resource test, int type) {
		if (!logging) return;
		Resource rslt;
		switch (type) {
			case WGReasonerTester.NOT_APPLICABLE:
			return;
			case WGReasonerTester.FAIL:
			rslt = OWLResults.FailingRun;
			break;
			case WGReasonerTester.PASS:
			rslt = OWLResults.PassingRun;
			break;
			case WGReasonerTester.INCOMPLETE:
			  rslt = OWLResults.IncompleteRun;
			  break;
			default:
			throw new BrokenException("Unknown result type");
		}
//		Resource result =
//			testResults
//				.createResource()
//				.addProperty(RDF.type, OWLResults.TestRun)
//				.addProperty(RDF.type, rslt)
//				.addProperty(OWLResults.test, test )
//				.addProperty(OWLResults.system, jena2);
	}
	private static boolean logging = false;
	private static String BASE_RESULTS_URI = "http://jena.sourceforge.net/data/rdf-results.rdf";
    static public boolean checkMessages = false;
    static private boolean doSemanticTests() {
    	return ARPTests.internet;
    }
    static private boolean inDevelopment = false;
     Model loadRDF(InFactoryX in, RDFErrorHandler eh, String base)
        throws IOException {
        Model model = ModelFactory.createDefaultModel();
        JenaReader jr = new JenaReader();

        if (eh != null)
            jr.setErrorHandler(eh);
        jr.setProperty("error-mode", "strict");
        
        if ( base.indexOf("/xmlns/") != -1 
          || base.indexOf("/comments/") != -1 )
              jr.setProperty("embedding","true");
        InputStream inx = in.open();
        jr.read(model, inx, base);
        inx.close();
        return model;
    }
    
    static Model loadNT(InputStream in, String base) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        model.read(in, base, "N-TRIPLE");
        in.close();
        return model;
    }

	static private class DummyTest extends TestCase {
		DummyTest() {
			super("save results");
		}
		public void runTest()  throws IOException {
			if (logging) {	    
				RDFWriter w = testResults.getWriter("RDF/XML-ABBREV");
				w.setProperty("xmlbase",BASE_RESULTS_URI );
				OutputStream out;
				try {
				out = new FileOutputStream("/tmp/rdf-results.rdf");
				}
				catch (Exception e){
					out = System.out;
				}
				w.write(testResults,out,BASE_RESULTS_URI);
				out.close();
			}
		}
	}
    static String testNS =
        "http://www.w3.org/2000/10/rdf-tests/rdfcore/testSchema#";
        
    static String jjcNS = "http://jcarroll.hpl.hp.com/testSchema#";
    
 //   static private String approved = "APPROVED";
    static private Property status;
    static private Property input;
    static private Property output;
    static private Property warning;
    static private Property errorCodes;
    
    static {
            status = new PropertyImpl(testNS, "status");
            input = new PropertyImpl(testNS, "inputDocument");
            output = new PropertyImpl(testNS, "outputDocument");
            warning = new PropertyImpl(testNS, "warning");
            errorCodes = new PropertyImpl(jjcNS, "error");
    }
    
    static private Resource rdfxml =
        new ResourceImpl(testNS, "RDF-XML-Document");
        
    static private Resource ntriple = new ResourceImpl(testNS, "NT-Document");
	//  static private Resource falseDoc = new ResourceImpl(testNS, "False-Document");

    private RDFURIReference testDir;
    
    private Act noop = new Act() {
        public void act(Resource r) {
        }
    };
    
    private Act semTest = new Act() {
		  public void act(Resource r) {
		  	if (doSemanticTests()){
//		  		addTest(r, new ReasoningTest(r));
		  	}
		  }
    };
    
    TestInputStreamFactory factory;
    
    static private Collection misc =
        Arrays.asList(
            new String[] { "http://www.w3.org/2000/10/rdf-tests/rdfcore/rdfms-uri-substructure/error001" });
            
    private Map behaviours = new HashMap();
    
    {
        behaviours
            .put(new ResourceImpl(testNS + "PositiveParserTest"), new Act() {
            public void act(Resource r)  {
                //		if (r.getProperty(status).getString().equals(approved))
                //  if (r.getURI().endsWith("rdfms-xmllang/test004"))
                if (r.hasProperty(warning)) {
                    addTest(r, new WarningTest(r));
                } else {
                    addTest(r, new PositiveTest(r));
                }
            }
        });
        behaviours
            .put(new ResourceImpl(testNS + "NegativeParserTest"), new Act() {
            public void act(Resource r)  {
                //		if (r.getProperty(status).getString().equals(approved))
                addTest(r, new NegativeTest(r));
            }
        });
		    behaviours.put(new ResourceImpl(testNS + "False-Document"), noop);
        behaviours.put(new ResourceImpl(testNS + "RDF-XML-Document"), noop);
        behaviours.put(new ResourceImpl(testNS + "NT-Document"), noop);
        behaviours.put(
            new ResourceImpl(testNS + "PositiveEntailmentTest"),
            semTest);
        behaviours.put(
            new ResourceImpl(testNS + "NegativeEntailmentTest"),
		        semTest);
        behaviours
            .put(new ResourceImpl(testNS + "MiscellaneousTest"), new Act() {
            public void act(Resource r) {
                String uri = r.getURI();
                if (!misc.contains(uri))
                    System.err.println(
                        "MiscellaneousTest: " + uri + " - ignored!");
            }
        });
    }

    private Model loadRDF(final TestInputStreamFactory fact, 
      final String file) {
        Model m = null;
        String base = fact.getBase().toString();
        if (!base.endsWith("/"))
            base = base + "/";

        try {
            InputStream in = fact.fullyOpen(file);
            if (in == null )
                return null;
            in.close();
            m = loadRDF(new InFactoryX(){
            	public InputStream open() throws IOException {
            		return fact.fullyOpen(file);
            } }, null, base + file);
        } catch (JenaException e) {
            //	System.out.println(e.getMessage());
            throw e;
        } catch (Exception e) {
            //	e.printStackTrace();
            if (file.equals("Manifest.rdf")) {
                System.err.println("Failed to open Manifest.rdf");
                e.printStackTrace();
            }
        }
        return m;
    }
    
    /** Creates new WGTestSuite
        This is a private snapshot of the RDF Test Cases Working Draft's
        data.
     */
    String createMe;
    
    WGTestSuite(TestInputStreamFactory fact, String name, boolean dynamic) {
        super(name);
        factory = fact;
        testDir = fact.getBase();
        if (dynamic)
            try {
            	String wgDir = ARPTests.wgTestDir.toString();
            	System.err.println(testDir+", "+fact.getMapBase());
            	  wgReasoner = new WGReasonerTester("Manifest.rdf",
                          "testing/wg/");
//                          wgDir);
                createMe =
                    "new "
                        + this.getClass().getName()
                        + "("
                        + fact.getCreationJava()
                        + ", \""
                        + name
                        + "\", false )";
                Model m = loadRDF(fact, "Manifest.rdf");
                //	System.out.println("OK2");
                Model extra = loadRDF(fact, "Manifest-extra.rdf");
                //	System.out.println("OK3");
                Model wrong = loadRDF(fact, "Manifest-wrong.rdf");
                //	System.out.println("OK4");

                if (extra != null)
                    m = m.add(extra);
                if (wrong != null)
                    m = m.difference(wrong);
                //	if (m == null)
                //		System.out.println("uggh");
                StmtIterator si =
                    m.listStatements( null, RDF.type, (RDFNode) null );

                while (si.hasNext()) {
                    Statement st = si.nextStatement();
                    Act action = (Act) behaviours.get(st.getObject());
                    if (action == null) {
                        System.err.println(
                            "Unknown test class: "
                                + ((Resource) st.getObject()).getURI());
                    } else {
                        action.act(st.getSubject());
                    }
                }
                if ( ARPTests.internet) {
                	initResults();
                	addTest(new DummyTest());
                }
            } catch (RuntimeException re) {
                re.printStackTrace();
                throw re;
            } catch (Exception e) {
                e.printStackTrace();
                throw new JenaException( e );

            }
    }
    
   // private ZipFile zip;
    
    static TestSuite suite(RDFURIReference testDir, String d, String nm) {
        return new WGTestSuite(
            new TestInputStreamFactory(testDir, d),
            nm,
            true);
    }

    static TestSuite suite(RDFURIReference testDir, XercesURIWrapper d, String nm) {
        return new WGTestSuite(
            new TestInputStreamFactory(testDir, d),
            nm,
            true);
    }

    private Map parts = new HashMap();
    
    private void addTest(Resource key, TestCase test)  {
        String keyName =
            key.hasProperty(status)
                ? key.getRequiredProperty(status).getString()
                : "no status";
        TestSuite sub = (TestSuite) parts.get(keyName);
        if (sub == null) {
            if ( keyName.equals("OBSOLETED"))
              return;
			      if ( keyName.equals("OBSOLETE"))
			        return;
			      if ( keyName.equals("NOT_APPROVED"))
			        return;
            sub = new TestSuite();
            sub.setName(keyName);
            parts.put(keyName, sub);
            addTest(sub);
        }
        sub.addTest(test);
    }

    final static String errorLevelName[] =
        new String[] { "warning", "error", "fatal error" };
        
    interface Act {
        void act(Resource r) ;
    }
    private WGReasonerTester wgReasoner;
    class ReasoningTest extends Test {
    	 ReasoningTest(Resource r) {
    	 	super(r);
    	 }
	 protected void runTest() throws IOException {
	       int rslt = WGReasonerTester.FAIL;
	       try {
                   JenaParameters.enableWhitespaceCheckingOfTypedLiterals = true;
                    Resource config = ModelFactory.createDefaultModel().createResource()
                         .addProperty(ReasonerVocabulary.PROPsetRDFSLevel, "full");
	            rslt = wgReasoner.runTestDetailedResponse(testID.getURI(),
	            RDFSRuleReasonerFactory.theInstance(),this,config);
                }  finally {
                    logResult(testID,rslt);
	        }
			// assertTrue(rslt>=0);
	}
	/* (non-Javadoc)
         * @see com.hp.hpl.jena.rdf.arp.test.WGTestSuite.Test#createMe()
	 */
	String createMe() {
		throw new UnsupportedOperationException();
	}
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.rdf.arp.test.WGTestSuite.Test#reallyRunTest()
	 */
	void reallyRunTest() {
		throw new BrokenException("");
	}
    	 
    }
    
    abstract class Test extends TestCase implements RDFErrorHandler {
        Resource testID;
        String createURI() {
            return "\"" + testID.getURI() + "\"";
        }
        abstract String createMe();
        Test(Resource r) {
            super(
                WGTestSuite
                    .this
                    .testDir
                    .relativize(IRIFactory.defaultFactory().create(r.getURI()),
                            RDFURIReference.RELATIVE)
                    .toString());
            testID = r;
        }
        String create(Property p) {
            Resource file = testID.getRequiredProperty(p).getResource();
            Resource t = file.getRequiredProperty(RDF.type).getResource();
            if (ntriple.equals(t)) {
                return "\"" + file.getURI() + "\",false";
            } else if (rdfxml.equals(t)) {
                return "\"" + file.getURI() + "\",true";
            } else {
                return "Unrecognized file type: " + t;
            }
        }
        Model read(Property p) throws IOException {
            Resource file = testID.getRequiredProperty(p).getResource();
            Resource t = file.getRequiredProperty(RDF.type).getResource();
            final String uri = file.getURI();
            if (ntriple.equals(t)) {
                return loadNT(factory.open(uri),uri);
            } else if (rdfxml.equals(t)) {
                return loadRDF(
                new InFactoryX(){

					public InputStream open() throws IOException {
						return factory.open(uri);
					}
                }
                , this, uri);
            } else {
                fail("Unrecognized file type: " + t);
            }
            return null;
        }
        protected void runTest()  throws IOException {
        	int rslt = WGReasonerTester.FAIL;
        	try {
        		reallyRunTest();
        		rslt = WGReasonerTester.PASS;
        	}
        	finally {
        		logResult(testID,rslt);
        	}
        }
        abstract void reallyRunTest();
        public void warning(Exception e) {
            error(0, e);
        }
        public void error(Exception e) {
            error(1, e);
        }
        public void fatalError(Exception e) {
            error(2, e);
        }
        private void error(int level, Exception e) {
            //		println(e.getMessage());
            if (e instanceof ParseException) {
                int eCode = ((ParseException) e).getErrorNumber();
                if (eCode == ERR_SYNTAX_ERROR) {
                    String msg = e.getMessage();
                    if (msg.indexOf("Unusual") != -1
                        || msg.indexOf("Internal") != -1) {
                        System.err.println(testID.getURI());
                        System.err.println(msg);
                        fail(msg);
                    }
                    if (checkMessages) {
                        System.err.println(testID.getURI());
                        System.err.println(msg);
                    }
                }
                onError(level, eCode);
            }
            /*else if (e instanceof SAXParseException) {
                onError(level, ARPErrorNumbers.WARN_BAD_XML);
            } */
            else if (e instanceof SAXException) {
                fail("Not expecting a SAXException: " + e.getMessage());
            } else {
                fail("Not expecting an Exception: " + e.getMessage());
            }
        }

        private void println(String m) {
            System.err.println(m);
        }
        void onError(int level, int num) {
            String msg =
                "Parser reports unexpected "
                    + errorLevelName[level]
                    + ": "
                    + JenaReader.errorCodeName(num);
            println(msg);
            fail(msg);
        }
    }
    
    class PositiveTest extends NegativeTest {
        String createMe() {
            return createURI() + "," + create(input) + "," + create(output);
        }
        PositiveTest(Resource nm)  {
            super(nm);
            expectedLevel = -1;
        }
        protected void reallyRunTest() {
            try {
                Model m2 = read(output);
                super.reallyRunTest();
                if (!m1.equals(m2)) {
                    save(output);
                    assertTrue(m1.isIsomorphicWith( m2 ) );
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
        void initExpected()  {
            expected = new HashSet();
        }
    }
    
    class WarningTest extends PositiveTest {
        String createMe() {
            return createURI()
                + ","
                + create(input)
                + ","
                + create(output)
                + ","
                + createExpected();
        }
        WarningTest(Resource nm)  {
            super(nm);
            expectedLevel = 0;
        }
        void initExpected()  {
            initExpectedFromModel();
        }
    }
    
    class NegativeTest extends Test {
        Model m1;
        Set expected;
        int expectedLevel = 1;
        private Set found = new HashSet();
        private int errorCnt[] = new int[] { 0, 0, 0 };
        String createExpected() {
            String rslt = "new int[]{";
            if ( expected == null)
               return "null";
            Iterator it = expected.iterator();
            while (it.hasNext())
                rslt += it.next() + ", ";
            return rslt + "}";
        }
        String createMe() {
            return createURI() + "," + create(input) + "," + createExpected();
        }
        NegativeTest(Resource nm)  {
            super(nm);
            initExpected();
        }
        void save(Property p)  {
            if (factory.savable()) {
                String uri = testID.getRequiredProperty(p).getResource().getURI();
                int suffix = uri.lastIndexOf('.');
                String saveUri = uri.substring(0, suffix) + ".ntx";
                //   System.out.println("Saving to " + saveUri);
                try {
                    OutputStream w = factory.openOutput(saveUri);
                    m1.write(w, "N-TRIPLE");
                    w.close();
                } catch (IOException e) {
                    throw new JenaException( e );
                }
            }
        }
        void initExpectedFromModel()  {
            StmtIterator si = testID.listProperties(errorCodes);
            if (si.hasNext()) {
                expected = new HashSet();
                while (si.hasNext()) {
                    String uri = si.nextStatement().getResource().getURI();
                    String fieldName = uri.substring(uri.lastIndexOf('#') + 1);
                    expected.add(new Integer(JenaReader.errorCode(fieldName)));
                }
            }
        }
        void initExpected()  {
            initExpectedFromModel();
        }
        protected void reallyRunTest() {
            try {
                m1 = read(input);

                if (expectedLevel == 1
                    && expected == null
                    && errorCnt[2] == 0
                    && errorCnt[1] == 0)
                    save(input);
            } catch (JenaException re) {
                if (re.getCause() instanceof SAXException) {
                    // ignore.
                } else {
                    fail(re.getMessage());
                }
            } catch (IOException ioe) {
                fail(ioe.getMessage());
            }
            if (expected != null && !expected.equals(found)) {
                Set dup = new HashSet();
                dup.addAll(found);
                dup.removeAll(expected);
                expected.removeAll(found);
                Iterator it = expected.iterator();
                while (it.hasNext()) {
                    int eCode = ((Integer) it.next()).intValue();
                    String msg =
                        "Expected error  "
                            + JenaReader.errorCodeName(eCode)
                            + ", was not detected.";
                    if (errorCnt[2] == 0)
                        fail(msg);
                    else if (
                        eCode == ERR_SYNTAX_ERROR
                            && getName().startsWith("rdf-nnn/67_")
                            && "1234".indexOf(
                                getName().charAt("rdf-nnn/67_".length()))
                                != -1) {
                        // ignore
                        //  System.err.println("Last message probably reflects a benign race condition on ARP teardown after fatal error that can be ignored.");
                        //  System.err.println("It is known to happen with tests rdf-nnn/67_[1234] and ERR_SYNTAX_ERROR.");

                    } else {
                        System.err.println("Test: " + getName());
                        System.err.println(msg);
                    }
                }
                it = dup.iterator();
                while (it.hasNext())
                    fail(
                        "Detected error  "
                            + JenaReader.errorCodeName(
                                ((Integer) it.next()).intValue())
                            + ", was not expected.");
            }
            for (int j = 2; j >= 0; j--)
                if (j == expectedLevel) {
                    if (errorCnt[j] == 0 && (j != 1 || errorCnt[2] == 0))
                        fail(
                            "No "
                                + errorLevelName[expectedLevel]
                                + " in input file of class "
                                + getClass().getName());
                } else if (expected == null) {
                    if (errorCnt[j] != 0)
                        fail(
                            "Inappropriate "
                                + errorLevelName[j]
                                + " in input file of class "
                                + getClass().getName());
                }

        }
        void onError(int level, int id) {
            Integer err = new Integer(id);
            found.add(err);
            errorCnt[level]++;
            if (expected != null) {
                if (!expected.contains(err))
                    super.onError(level, id);
            } else if (inDevelopment) {
                System.err.println(
                    "<rdf:Description rdf:about='"
                        + testID.getURI()
                        + "'>\n"
                        + "<jjc:error rdf:resource='"
                        + jjcNS
                        + JenaReader.errorCodeName(id)
                        + "'/>\n</rdf:Description>");
            }
        }
    }
    class Test2 extends TestCase implements RDFErrorHandler {
        //Resource testID;
        Test2(String r) {
            super(
                WGTestSuite.this.testDir.relativize(r,
                        RDFURIReference.RELATIVE));
            //   testID = r;
        }
        Model read(String file, boolean type) throws IOException {
            if (!type) {
                return loadNT(factory.open(file),file);
            } 
                final String uri = file;
                return loadRDF(
                new InFactoryX(){

					public InputStream open() throws IOException {
						return factory.open(uri);
					}
                }
                
                , this, uri);
            
        }
        
        public void warning(Exception e) {
            error(0, e);
        }
        
        public void error(Exception e) {
            error(1, e);
        }
        
        public void fatalError(Exception e) {
            error(2, e);
        }
        
        private void error(int level, Exception e) {
            //      println(e.getMessage());
            if (e instanceof ParseException) {
                int eCode = ((ParseException) e).getErrorNumber();
                if (eCode == ERR_SYNTAX_ERROR) {
                    String msg = e.getMessage();
                    if (msg.indexOf("Unusual") != -1
                        || msg.indexOf("Internal") != -1) {
                        System.err.println(getName());
                        System.err.println(msg);
                        fail(msg);
                    }
                    /*
                    if (checkMessages) {
                        System.err.println(testID.getURI());
                        System.err.println(msg);
                    }
                    */
                }
                onError(level, eCode);
            } /*else if (e instanceof SAXParseException) {
                onError(level, ARPErrorNumbers.WARN_BAD_XML);
            } */
            else if (e instanceof SAXException) {
                fail("Not expecting a SAXException: " + e.getMessage());
            } else {
                fail("Not expecting an Exception: " + e.getMessage());
            }
        }

        private void println(String m) {
            System.err.println(m);
        }
        void onError(int level, int num) {
            String msg =
                "Parser reports unexpected "
                    + errorLevelName[level]
                    + ": "
                    + JenaReader.errorCodeName(num);
            println(msg);
            fail(msg);
        }
    }
    
    class PositiveTest2 extends NegativeTest2 {
        String out;
        boolean outtype;
        PositiveTest2(
            String uri,
            String in,
            boolean intype,
            String out,
            boolean outtype) {
            this(uri, in, intype, out, outtype, new int[] {
            });
        }

        PositiveTest2(
            String uri,
            String in,
            boolean intype,
            String out,
            boolean outtype,
            int errs[]) {
            super(uri, in, intype, errs);
                expectedLevel = -1;
            this.out = out;
            this.outtype = outtype;
        }
        protected void runTest() {
            try {
                Model m2 = read(out, outtype);
                super.runTest();
                if (!m1.isIsomorphicWith(m2)) {
                    //  save(output);
                    System.err.println("=====");
                    m1.write(System.err,"N-TRIPLE");
                    System.err.println("=====");
                    m2.write(System.err,"N-TRIPLE");
                    System.err.println("=====");
                    fail("Models were not equal.");
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
        void initExpected()  {
            expected = new HashSet();
        }
    }
    
    class WarningTest2 extends PositiveTest2 {
        WarningTest2(
            String uri,
            String in,
            boolean intype,
            String out,
            boolean outtype,
            int errs[]) {
            super(uri, in, intype, out, outtype, errs);
                expectedLevel = 0;
        }
    }
    
    class NegativeTest2 extends Test2 {
        Model m1;
        Set expected;
        int expectedLevel = 1;
        String in;
        boolean intype;
        private Set found = new HashSet();
        private int errorCnt[] = new int[] { 0, 0, 0 };
        NegativeTest2(String uri, String in, boolean intype, int errs[]) {
            super(uri);
            this.in = in;
            this.intype = intype;

            initExpected(errs);
        }
        /*
        void save(Property p)  {
            if (factory.savable()) {
                String uri = testID.getProperty(p).getResource().getURI();
                int suffix = uri.lastIndexOf('.');
                String saveUri = uri.substring(0, suffix) + ".ntx";
                //   System.out.println("Saving to " + saveUri);
                try {
                    OutputStream w = factory.openOutput(saveUri);
                    m1.write(w, "N-TRIPLE");
                    w.close();
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
        */
        void initExpected(int errs[]) {
            if ( errs == null )
               return;
            if (errs.length != 0)
                expected = new HashSet();
            for (int i = 0; i < errs.length; i++) {

                expected.add(new Integer(errs[i]));
            }
        }
        protected void runTest() {
            try {
                m1 = read(in, intype);
                /*
                                if (expectedLevel == 1
                                    && expected == null
                                    && errorCnt[2] == 0
                                    && errorCnt[1] == 0)
                                    save(input);
                                    */
            } catch (JenaException re) {
                //   System.out.println(re.toString());
                if (re.getCause() instanceof SAXException) {
                    // ignore.
                } else {
                    fail(re.getMessage());
                }
            } catch (IOException ioe) {
                fail(ioe.getMessage());
            }
            // TODO: tidy up this code a bit, I don't understand it.
            HashSet ex2 = expected==null?null:new HashSet(expected);
            if (expected==null)
            for (int j = 2; j >= 0; j--)
                if (j != expectedLevel)  {
                    if (errorCnt[j] != 0)
                        ex2 = new HashSet();
                }
            if (ex2 != null && !ex2.equals(found)) {
                Set dup = new HashSet();
                dup.addAll(found);
                dup.removeAll(ex2);
                ex2.removeAll(found);
                if (expected != null)
                    expected.removeAll(found);
                Iterator it = ex2.iterator();
                while (it.hasNext()) {
                    int eCode = ((Integer) it.next()).intValue();
                    String msg =
                        "Expected error  "
                            + JenaReader.errorCodeName(eCode)
                            + ", was not detected.";
                    if (errorCnt[2] == 0) {
                        fail(msg);
                    } else {
                        System.err.println("Test: " + getName());
                        System.err.println(msg);
                    }
                }
                it = dup.iterator();
                while (it.hasNext())
                    fail(
                        "Detected error  "
                            + JenaReader.errorCodeName(
                                ((Integer) it.next()).intValue())
                            + ", was not expected.");
            }
            for (int j = 2; j >= 0; j--)
                if (j == expectedLevel) {
                    if (errorCnt[j] == 0 && (j != 1 || errorCnt[2] == 0))
                        fail(
                            "No "
                                + errorLevelName[expectedLevel]
                                + " in input file of class "
                                + getClass().getName());
                } else if (expected == null) {
                    if (errorCnt[j] != 0)
                        fail(
                            "Inappropriate "
                                + errorLevelName[j]
                                + " in input file of class "
                                + getClass().getName());
                }

        }
        void onError(int level, int id) {
            Integer err = new Integer(id);
            found.add(err);
            errorCnt[level]++;
            if (expected != null) {
                if (!expected.contains(err))
                    super.onError(level, id);
            }
            /*else if ( inDevelopment ) {
                System.err.println(
                    "<rdf:Description rdf:about='"
                        + testID.getURI()
                        + "'>\n"
                        + "<jjc:error rdf:resource='"
                        + jjcNS
                        + JenaReader.errorCodeName(id)
                        + "'/>\n</rdf:Description>");
            }
            */
        }
    }
    TestCase createPositiveTest(
        String uri,
        String in,
        boolean intype,
        String out,
        boolean outtype) {
        return new PositiveTest2(uri, in, intype, out, outtype);
    }
    TestCase createWarningTest(
        String uri,
        String in,
        boolean intype,
        String out,
        boolean outtype,
        int e[]) {
        return new WarningTest2(uri, in, intype, out, outtype, e);
    }
    TestCase createNegativeTest(
        String uri,
        String in,
        boolean intype,
        int e[]) {
        return new NegativeTest2(uri, in, intype, e);
    }
}
/*
 *  (c) Copyright 2001, 2002, 2003, 2004, 2005 Hewlett-Packard Development Company, LP
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
 * WGTestSuite.java
 *
 * Created on November 28, 2001, 10:00 AM
 */
