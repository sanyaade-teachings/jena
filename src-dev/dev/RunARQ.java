/*
 * (c) Copyright 2007, 2008, 2009 Hewlett-Packard Development  Company, LP
 * (c) Copyright 2010 Talis Systems Ltd
 * All rights reserved.
 * [See end of file]
 */

package dev;

import static org.openjena.atlas.lib.StrUtils.strjoinNL ;

import java.util.HashSet ;
import java.util.Iterator ;
import java.util.Set ;

import org.openjena.atlas.io.IndentedLineBuffer ;
import org.openjena.atlas.io.IndentedWriter ;
import org.openjena.atlas.json.JSON ;
import org.openjena.atlas.json.JsonValue ;
import org.openjena.atlas.lib.StrUtils ;
import org.openjena.atlas.logging.Log ;
import org.openjena.riot.ErrorHandlerFactory ;
import org.openjena.riot.checker.CheckerIRI ;
import org.openjena.riot.pipeline.normalize.CanonicalizeLiteral ;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype ;
import com.hp.hpl.jena.datatypes.xsd.XSDDuration ;
import com.hp.hpl.jena.graph.Node ;
import com.hp.hpl.jena.iri.IRI ;
import com.hp.hpl.jena.iri.IRIFactory ;
import com.hp.hpl.jena.iri.Violation ;
import com.hp.hpl.jena.query.ARQ ;
import com.hp.hpl.jena.query.Dataset ;
import com.hp.hpl.jena.query.DatasetFactory ;
import com.hp.hpl.jena.query.Query ;
import com.hp.hpl.jena.query.QueryExecution ;
import com.hp.hpl.jena.query.QueryExecutionFactory ;
import com.hp.hpl.jena.query.QueryFactory ;
import com.hp.hpl.jena.query.QuerySolutionMap ;
import com.hp.hpl.jena.query.ResultSet ;
import com.hp.hpl.jena.query.ResultSetFormatter ;
import com.hp.hpl.jena.rdf.model.Model ;
import com.hp.hpl.jena.rdf.model.ModelFactory ;
import com.hp.hpl.jena.sparql.ARQConstants ;
import com.hp.hpl.jena.sparql.algebra.Algebra ;
import com.hp.hpl.jena.sparql.algebra.Op ;
import com.hp.hpl.jena.sparql.algebra.OpVars ;
import com.hp.hpl.jena.sparql.algebra.op.OpModifier ;
import com.hp.hpl.jena.sparql.algebra.op.OpProject ;
import com.hp.hpl.jena.sparql.algebra.op.OpSlice ;
import com.hp.hpl.jena.sparql.core.DataSourceImpl ;
import com.hp.hpl.jena.sparql.core.DatasetGraph ;
import com.hp.hpl.jena.sparql.core.DatasetGraphFactory ;
import com.hp.hpl.jena.sparql.core.Quad ;
import com.hp.hpl.jena.sparql.core.Var ;
import com.hp.hpl.jena.sparql.engine.RenamerVars ;
import com.hp.hpl.jena.sparql.engine.VarRename ;
import com.hp.hpl.jena.sparql.expr.Expr ;
import com.hp.hpl.jena.sparql.expr.ExprEvalException ;
import com.hp.hpl.jena.sparql.expr.NodeValue ;
import com.hp.hpl.jena.sparql.function.FunctionEnvBase ;
import com.hp.hpl.jena.sparql.graph.NodeConst ;
import com.hp.hpl.jena.sparql.graph.NodeTransform ;
import com.hp.hpl.jena.sparql.graph.NodeTransformLib ;
import com.hp.hpl.jena.sparql.lang.ParserSPARQL11Update ;
import com.hp.hpl.jena.sparql.mgt.Explain.InfoLevel ;
import com.hp.hpl.jena.sparql.modify.request.UpdateWriter ;
import com.hp.hpl.jena.sparql.serializer.SerializationContext ;
import com.hp.hpl.jena.sparql.sse.SSE ;
import com.hp.hpl.jena.sparql.util.ExprUtils ;
import com.hp.hpl.jena.sparql.util.QueryExecUtils ;
import com.hp.hpl.jena.sparql.util.Timer ;
import com.hp.hpl.jena.update.GraphStore ;
import com.hp.hpl.jena.update.GraphStoreFactory ;
import com.hp.hpl.jena.update.UpdateAction ;
import com.hp.hpl.jena.update.UpdateFactory ;
import com.hp.hpl.jena.update.UpdateRequest ;
import com.hp.hpl.jena.util.FileManager ;

public class RunARQ
{
    static String divider = "----------------------------------------" ;
    static String nextDivider = null ;
    static void divider()
    {
        if ( nextDivider != null )
            System.out.println(nextDivider) ;
        nextDivider = divider ;
    }
    
    static { Log.setLog4j() ; }
    
    public static void testXSDDurationBug() {
        Node d1 = Node.createLiteral("PT110S", null, XSDDatatype.XSDduration);
        Node d2 = Node.createLiteral("PT1M50S", null, XSDDatatype.XSDduration);
        System.out.println(d1.getLiteral().isWellFormed());
        System.out.println(d2.getLiteral().isWellFormed());
        XSDDuration dur1 = (XSDDuration) d1.getLiteralValue();
        XSDDuration dur2 = (XSDDuration) d2.getLiteralValue();
        int cmp = dur1.compare(dur2);
        System.out.println("Compare = " + cmp);
    }
    
    
    public static void main(String[] argv) throws Exception
    {
        
//      rename2() ;

//        arq.rset.main("--set=arq:useSAX=true", "R-dup.srj") ; System.exit(0) ;
//        arq.qparse.main("--print=op", "--query=Q.rq") ; System.exit(0) ;
        arq.sparql.main(/*"--explain",*/ "--data=D.ttl", "--query=Q.rq") ; System.exit(0) ;
       
        
        Dataset ds2 = DatasetFactory.create() ;
        ds2 = new DataSourceImpl(ds2.asDatasetGraph()) 
        { 
            @Override
            public Model getNamedModel(String uri)
            { 
                Model m = super.getNamedModel(uri) ;
                if ( m == null )
                {
                    m = ModelFactory.createDefaultModel() ;
                    super.addNamedModel(uri, m) ;
                }
                return m ;
            }
        } ;
        
        
        System.out.println(ds2.getNamedModel("http://example/foo")) ;
        System.exit(0) ;
       
        
        if ( true )
        {
            ARQ.setExecutionLogging(InfoLevel.ALL) ;
            String qs = StrUtils.strjoinNL("SELECT DISTINCT ?s",
                                           "{ SERVICE <http://dbpedia.org/sparql>",
                                           "    { SELECT ?s { ?s ?p [] . } limit 10 }",
                                           "  SERVICE <http://dbpedia.org/sparql>",
                                               "    { SELECT ?s { ?s ?p [] . } limit 10 }",
            "}") ;
            Query query = QueryFactory.create(qs) ;
            Dataset ds = DatasetFactory.create() ;
            QueryExecution qExec = QueryExecutionFactory.create(query, ds) ;
            ResultSet rs = qExec.execSelect() ;
            ResultSetFormatter.out(rs) ;
            qExec.close() ;
            
            System.exit(0) ;
        }
        
        {
            String qs = StrUtils.strjoinNL("SELECT ?s { ?s ?p ?o . OPTIONAL { [] ?p ?__o } } ORDER BY ?_o limit 10 ") ;
            Query query = QueryFactory.create(qs) ;
            Op op = Algebra.compile(query) ;
            System.out.println(op) ;
            divider() ;
            
//            List<Var> vars = new ArrayList<VWalkerVisitorSkipMinusar>() ;
//            vars.add(Var.alloc("s")) ;
            Set<Var> vars = OpVars.allVars(op) ;  
            System.out.println("Visable vars: "+vars) ;
            
            // Get to real work.
            // Includes order
            Op opSub = op ;
            while( opSub instanceof OpProject || opSub instanceof OpSlice )
                opSub = ((OpModifier)opSub).getSubOp() ;
            
            Set<Var> allVars = OpVars.allVars(opSub) ;      // Need : OpVars.allMentionedVars - ignores project
            System.out.println(allVars) ;
            
            String[] prefixes = { "_", "__", "_X", "/"} ;
            String prefix = "_" ;
            
            while(true)
            {
                String attempt = prefix ;
//            for ( String p : prefixes )
//            {
//                prefix = p ;
                for ( Var v : allVars )
                {
                    if ( v.getName().startsWith(prefix) )
                    {
                        attempt = null ;
                        break ;
                    }
                }
                if ( attempt != null )
                    break ;
                // Try again.
                prefix = prefix+"A_" ; 
            }
            System.out.println("Safe prefix : "+prefix) ;
            
            //AlgebraGenerator.Line 605
            Op op3 = VarRename.rename(op, vars) ;
            
            // Better - find all vars, find safe prefix, use that.
            
            
            System.out.println(op3) ;
            divider() ;
            
        
        }
        
        System.exit(0) ;
//        String[] x = { 
//            "1984-01-01T00:00:00",
//            "1984-01-01T00:00:00.0",
//            "1984-01-01T00:00:00.10",
//        } ;
//        for ( String s : x )
//        {
//            System.out.println(s) ;
//            System.out.println(XSDDatatype.XSDdateTime.isValid(s)) ;
//        }
//            
//            
//        System.exit(0) ;
        
        // ** Double space for end of object, end of object. 
        JsonValue obj = JSON.readAny("D.json") ;
        IndentedWriter out = new IndentedWriter(System.out) ; 
        out.setFlatMode(true) ;
        //out.setEndOfLineMarker("$") ;
        JSON.write(out, obj) ;
        out.flush() ;
        System.exit(0) ;
        
        
        arq.sparql.main("--data=D.ttl", "-query=Q.rq") ;
        System.exit(0) ;
        
        if ( false )
        {
            NodeTransform ntLitCanon = CanonicalizeLiteral.get();
            // To do :
            //   double and floats.
            //   decimals and X.0
            String[] strings = { "123", "0123", "0123.00900" , "-0089", "-0089.0" , "1e5", "+001.5e6", "'fred'"} ;
            for ( String s : strings )
            {
                Node n = SSE.parseNode(s) ;
                Node n2 = ntLitCanon.convert(n) ;
                System.out.println(n+" => "+n2) ;
            }
            System.exit(0) ;
        }
        
        testXSDDurationBug() ; System.exit(0) ;
        
        UpdateRequest request = UpdateFactory.create("INSERT DATA { GRAPH <G> { <s> <p> <o> }}") ;
        DatasetGraph dsg = DatasetGraphFactory.createMem() ;
        GraphStore gs = GraphStoreFactory.create(dsg) ;
        // Why does this auto-insert?
        UpdateAction.execute(request, gs) ;
        SSE.write(gs) ;
        System.exit(0) ;
        
        qparse("@Q.rq") ;
        System.exit(0) ;
        
//        arq.uparse.main("--file=update.ru") ; System.exit(0) ;
        //qparse("--query=Q.rq", "--print=query", "--print=op") ; System.exit(0) ;
        //sparql11update() ; System.exit(0) ; 
        
        
        NodeTransform nt = new NodeTransform() {
            public Node convert(Node node)
            {
                if ( node == Quad.defaultGraphNodeGenerated )
                    return NodeConst.nodeTwo ;
                return node ;
            }
        };

        {
        Quad q = SSE.parseQuad("(_ <s> <p> <o>)") ;
        Quad q2 = NodeTransformLib.transform(nt, q) ;
        SSE.write(q) ;
        System.out.print( "=> ") ;
        SSE.write(q2) ;
        System.out.println() ;
        System.exit(0) ;
        }
        
        String DIR = "WorkSpace/PropertyPathTestCases" ;
        runTest(DIR, "data-path-1.ttl", "pp-all-03.rq") ; System.exit(0) ;

        if ( false )
        {
            Query q = QueryFactory.read("Q.arq") ;
            Op op = Algebra.compile(q) ;
            divider() ;
            System.out.println(op) ;

            Set<Var> fixed = new HashSet<Var>() ;
            fixed.add(Var.alloc("y")) ;
            RenamerVars vrn = new RenamerVars(fixed, ARQConstants.allocVarScopeHiding) ;
            op = NodeTransformLib.transform(vrn, op) ;
            divider() ;
            System.out.println(op) ;
            System.exit(0) ;
        }
        
        /*
         * urn:x-arq:DefaultGraphNode -- generated
         * urn:x-arq:DefaultGraph -- explicit
         * urn:x-arq:UnionGraph
         */
        Op op = SSE.parseOp(strjoinNL("(prefix ((: <http://example/>))",
                                      "(graph <g>",
                                      "  (graph <urn:x-arq:UnionGraph>",
                                      "    (graph <g1>",
                                      "     (bgp (?s ?p ?o))))",
                                      ")",
                                      ")"
        )) ;

        Op op2 = Algebra.unionDefaultGraph(op) ;
        divider() ;
        System.out.println(op) ;
        divider() ;
        System.out.println(op2) ;
        System.exit(0) ;
    }
    
    public static void runTest()
    {
        String dir = "/home/afs/W3C/SPARQL-docs/tests/data-sparql11/negation/" ;
        dir = "testing/ARQ/PropertyFunctions/" ;
        runTest(dir, "data-1.ttl", "list-8.rq") ;
    }

    public static void runTest(String dir, String dataFile, String queryFile)
    {
        if ( ! dir.endsWith("/") )
            dir = dir + "/" ;
        String queryArg = "--query="+dir+queryFile ;
        String dataArg = "--data="+dir+dataFile ;
        arq.sparql.main(/*"--engine=ref",*/ dataArg, queryArg) ;
    }

    private static void processIRI(String iriStr)
    {
        IRI iri = IRIFactory.iriImplementation().create(iriStr) ;
        System.out.println(iri) ;
        System.out.println("Relative: "+iri.isRelative()) ;

        Iterator<Violation> vIter = iri.violations(true) ;
        for ( ; vIter.hasNext() ; )
        {
            System.out.println(vIter.next()) ;
        }
        System.out.println(iriStr + " ==> "+iri) ;
        CheckerIRI.iriViolations(iri, ErrorHandlerFactory.errorHandlerWarn) ;
        System.exit(0) ;
    }
    
    public static void analyseQuery(String ...queryString)
    {
        String qs = StrUtils.strjoinNL(queryString) ;
        Query query = QueryFactory.create(qs) ;
        Op op = Algebra.compile(query) ;
        divider() ;
        System.out.println(op) ;
        Op op2 = Algebra.optimize(op) ;
        divider() ;
        System.out.println(op2) ;
        divider() ;
    }

    
    private static void sparql11update()
    {
        GraphStore graphStore = GraphStoreFactory.create() ;
//        sparql11update_operation(graphStore, "BASE <base:/>",
//                                 "CREATE GRAPH <g>",
//                                 "INSERT DATA { <x> <y> <z> GRAPH <g> { <s> <p> <o1>, <o2> }}",
//                                 //"DELETE WHERE { <x> <y> ?z GRAPH <g> { ?s ?p ?o }}",
//                                 
//                                 "INSERT { ?s ?p ?o } WHERE { GRAPH <g> { ?s ?p ?o FILTER (?o = <o2> )}}",
//                                 //"DROP DEFAULT" ,
//                                 //"CLEAR DEFAULT",
//                                 //"CLEAR ALL",
//                                 "") ;

        sparql11update_operation(graphStore, "BASE <base:/>",
                                 "CREATE GRAPH <g>",
                                 "INSERT DATA { <x> <y> <z> }",
                                 //"DELETE WHERE { <x> <y> ?z GRAPH <g> { ?s ?p ?o }}",
                                 
                                 "INSERT INTO <g> { ?s ?p ?o } WHERE { ?s ?p ?o }",
                                 
                                 "CREATE GRAPH <g2>",
                                 "INSERT { GRAPH <g2> { ?s ?p 1914 } } WHERE { ?s ?p ?o }",
                                 //"DROP DEFAULT" ,
                                 //"CLEAR DEFAULT",
                                 //"CLEAR ALL",
                                 "") ;

                
        
//        sparql11update_1("LOAD  <foo>  INTO  GRAPH  <blah>") ;
//        sparql11update_1("BASE <http://example/> PREFIX : <http://prefix/> LOAD  <foo>  INTO  GRAPH  :local") ;
//        
//        sparql11update_1("LOAD  <foo>") ;
//        sparql11update_1("BASE <http://example/> LOAD  <foo> INTO GRAPH <local>") ;
//        sparql11update_1("BASE <http://example/> CLEAR GRAPH <foo>") ;
//        sparql11update_1("BASE <http://example/> DROP GRAPH <foo>") ;
//        sparql11update_1("DROP  ALL") ;
//        sparql11update_1("DROP  NAMED") ;
//        sparql11update_1("CLEAR  DEFAULT") ;
//        
//        sparql11update_1("DELETE WHERE { ?s ?p ?o }") ;
//        sparql11update_1("DELETE DATA { <?s> <p> <o> }") ;
//        
//        sparql11update_1("BASE <base:> ",
//                         "PREFIX : <http://example/>",
//                         "WITH :g",
//                         "DELETE { <s> ?p ?o }",
//                         "INSERT { ?s ?p <#o> }",
//                         "USING <g>",
//                         "USING NAMED :gn",
//                         "WHERE",
//                         "{ ?s ?p ?o }"
//                         ) ;
//        sparql11update_1("PREFIX : <http://example>",
//                         "WITH :g",
//                         "DELETE { ?s ?p ?o }",
//                         //"INSERT { ?s ?p ?o }",
//                         "USING <g>",
//                         "USING NAMED :gn",
//                         "WHERE",
//                         "{ ?s ?p ?o }"
//                         ) ;
//        sparql11update_1("PREFIX : <http://example>",
//                         //"WITH :g",
//                         //"DELETE { ?s ?p ?o }",
//                         "INSERT { ?s ?p ?o }",
//                         //"USING <g>",
//                         //"USING NAMED :gn",
//                         "WHERE",
//                         "{ ?s ?p ?o }"
//                         ) ;
//        sparql11update_1("PREFIX : <http://example>",
//                         //"WITH :g",
//                         //"DELETE { ?s ?p ?o }",
//                         "INSERT DATA { <s> <p> <o> } ;",
//                         "INSERT DATA { <s> <p> <o> GRAPH <g> { <s> <p> <o> }}"
//                         ) ;
       
        
        System.out.println("# DONE") ;
        
    }
    
    private static void sparql11update_operation(GraphStore graphStore, String... str)
    {
        String str$ = StrUtils.strjoinNL(str) ; 
        divider() ;
        System.out.println("----Input:") ;
        System.out.println(str$);
        
        UpdateRequest update = UpdateFactory.create(str$) ;

        UpdateAction.execute(update, graphStore) ;
        SSE.write(graphStore) ;
    }

    private static void sparql11update_1(String... str)
    {
        String str$ = StrUtils.strjoinNL(str) ; 
        divider() ;
        System.out.println("----Input:") ;
        System.out.println(str$);
        UpdateRequest update = UpdateFactory.create(str$) ; 
        System.out.println("----Output:") ;
        SerializationContext sCxt = new SerializationContext(update) ;
        //SerializationContext sCxt = new SerializationContext() ;
        UpdateWriter.output(update, IndentedWriter.stdout, sCxt) ;
        IndentedWriter.stdout.flush();
        
        IndentedLineBuffer buff = new IndentedLineBuffer() ;
        UpdateWriter.output(update, buff, sCxt) ;
        { // reparse
            String str2 = buff.asString() ;
            ParserSPARQL11Update p2 = new ParserSPARQL11Update() ;
            UpdateRequest update2 = new UpdateRequest() ;
            p2.parse(update2, str2) ;
        }

        { // reparse
            String str2 = buff.asString() ;
            ParserSPARQL11Update p2 = new ParserSPARQL11Update() ;
            UpdateRequest update2 = new UpdateRequest() ;
            p2.parse(update2, str2) ;
        }
    }
    
    private static void execTimed(Query query, Model model)
    {
//        System.out.println(ARQ.VERSION); 
//        System.out.println(Jena.VERSION); 

        Timer timer = new Timer() ;
        timer.startTimer() ;
        exec(query, model) ;
        long time = timer.endTimer() ;
        System.out.printf("Time = %.2fs\n", time/1000.0) ;
    }

    private static void exec(Query query, Model model)
    {
        QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
        QueryExecUtils.executeQuery(query, qexec) ;
    }
    
    public static NodeValue eval(String string)
    {
        try {
            Expr expr = ExprUtils.parse(string) ;
            return expr.eval(null, new FunctionEnvBase()) ;
        } catch (ExprEvalException ex)
        {
            ex.printStackTrace(System.err) ;
            return null ;
        }
    }
    
    public static void evalPrint(String string)
    {
        System.out.print(string) ;
        System.out.print(" ==> ") ;
        try {
            Expr expr = ExprUtils.parse(string) ;
            NodeValue nv = expr.eval(null, new FunctionEnvBase()) ;
            System.out.println(nv) ;
        } catch (ExprEvalException ex)
        {
            System.out.println(" ** "+ex) ;
        }
    }

    private static void qparse(String  ... a)
    {
        arq.qparse.main(a) ;
        System.exit(0) ;
    }
    
    private static void runQTest(String dir, String manifest)
    {
        if ( ! dir.endsWith("/") )
            dir = dir + "/" ;
        String []a1 = { "--strict", dir+manifest } ;
        arq.qtest.main(a1) ;
        System.exit(0 ) ; 
  
    }

    private static void execQueryCode(String datafile, String queryfile)
    {
        Model model = FileManager.get().loadModel(datafile) ;
        Query query = QueryFactory.read(queryfile) ;
        
        QuerySolutionMap initialBinding = new QuerySolutionMap();
        //initialBinding.add("s", model.createResource("http://example/x1")) ;
        initialBinding.add("o", model.createResource("http://example/z")) ;
        
        QueryExecution qExec = QueryExecutionFactory.create(query, model, initialBinding) ;
        ResultSetFormatter.out(qExec.execSelect()) ;
    }

    private static void execRemote()
    {
        System.setProperty("socksProxyHost", "socks-server") ;
    
        String a2[] = { "--service=http://dbpedia.org/sparql",
        "SELECT * WHERE {  <http://dbpedia.org/resource/Angela_Merkel> <http://dbpedia.org/property/reference> ?object.  FILTER  (!isLiteral(?object))}"} ;
        arq.remote.main(a2) ;
        System.exit(0) ;
    }
}

/*
 * (c) Copyright 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * (c) Copyright 2010 Talis Systems Ltd
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