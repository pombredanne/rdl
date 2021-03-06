package se.lagrummet.rinfo.base.rdf;

import java.util.*;

import org.openrdf.OpenRDFException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.model.Statement;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.XMLSchema;


public class Describer {

    public static final String RDF_NS = RDF.NAMESPACE;
    public static final String RDFS_NS = RDFS.NAMESPACE;
    public static final String OWL_NS = OWL.NAMESPACE;
    public static final String XSD_NS = XMLSchema.NAMESPACE;

    RepositoryConnection conn;
    ValueFactory vf;
    Resource[] contextRefs;

    Map<String, String> prefixes = new HashMap<String, String>();
    Map<String, String> uriPrefixMap;
    boolean storePrefixes = false;
    boolean inferred = false;

    public Describer(RepositoryConnection conn, String... contexts) {
        this(conn, false, contexts);
    }

    public Describer(RepositoryConnection conn, boolean storePrefixes, String... contexts) {
        this.conn = conn;
        this.storePrefixes = storePrefixes;
        this.vf = conn.getValueFactory();
        this.contextRefs = new Resource[contexts.length];
        for (int i=0; i < contexts.length; i++) {
            this.contextRefs[i] = toRef(contexts[i]);
        }
        setPrefix("rdf", RDF_NS);
        setPrefix("rdfs", RDFS_NS);
        setPrefix("owl", OWL_NS);
        setPrefix("xsd", XSD_NS);
    }

    public RepositoryConnection getConnection() {
        return conn;
    }

    void addFromConnection(RepositoryConnection otherConn) {
        addFromConnection(otherConn, false);
    }

    void addFromConnection(RepositoryConnection otherConn, boolean preserveBNodeIDs) {
        try {
            RDFInserter inserter =  new RDFInserter(conn);
            inserter.enforceContext(contextRefs);
            inserter.setPreserveBNodeIDs(preserveBNodeIDs);
            otherConn.export(inserter);
        } catch (OpenRDFException e) {
            throw new DescriptionException(e);
        }
    }

    public void close() {
        try {
            conn.close();
        } catch (RepositoryException e) {
            throw new DescriptionException(e);
        }
    }

    public String getPrefix(String prefix) {
        String uri = prefixes.get(prefix);
        if (uri == null) {
            throw new NullPointerException("Undefined prefix: " + prefix);
        }
        return uri;
    }
    public Describer setPrefix(String prefix, String uri) {
        prefixes.put(prefix, uri);
        if (storePrefixes) {
            try {
                conn.setNamespace(prefix, uri);
            } catch (RepositoryException e) {
                throw new DescriptionException(e);
            }
        }
        return this;
    }

    public String expandCurie(String curie) {
        try {
            int i = curie.indexOf(":");
            String pfx = curie.substring(0, i);
            String term = curie.substring(i+1);
            return getPrefix(pfx) + term;
        } catch (Exception e) {
            throw new DescriptionException("Malformed curie: " + curie, e);
        }
    }

    public Description newDescription() {
        return newDescription(null);
    }

    public Description newDescription(String about) {
        if (about == null) {
            about = fromRef(blankRef());
        }
        return new Description(this, about);
    }

    public Description newDescription(String about, String typeCurie) {
        Description description = newDescription(about);
        description.addType(typeCurie);
        return description;
    }

    public Description findDescription(String about) {
        try {
            if (!conn.hasStatement(toRef(about), null, null, inferred, contextRefs))
                return null;
            return newDescription(about);
        } catch (RepositoryException e) {
            throw new DescriptionException(e);
        }
    }

    public Set<Description> subjects(String pCurie, String oUri) {
        Set<Description> things = new HashSet<Description>();
        for (Object ref : subjectUris(pCurie, oUri)) {
            things.add(newDescription((String) ref));
        }
        return things;
    }

    public Set<Object> subjectUris(String pCurie, String oUri) {
        Value o = (oUri != null)? toRef(oUri) : null;
        return subjectUrisByObject(pCurie, o);
    }

    public Set<Object> subjectUrisByLiteral(String pCurie, Object value) {
        Value o = (value != null)? toLiteral(value) : null;
        return subjectUrisByObject(pCurie, o);
    }

    protected Set<Object> subjectUrisByObject(String pCurie, Value o) {
        org.openrdf.model.URI p = (pCurie != null)?
                (org.openrdf.model.URI) toRef(expandCurie(pCurie)) : null;
        try {
            RepositoryResult<Statement> stmts = conn.getStatements(null, p, o,
                    inferred, contextRefs);
            Set<Object> values = new HashSet<Object>();
            while (stmts.hasNext()) {
                values.add(castValue(stmts.next().getSubject()));
            }
            stmts.close();
            return values;
        } catch (RepositoryException e) {
            throw new DescriptionException(e);
        }
    }

    public Set<Description> getByType(String typeCurie) {
        return subjects("rdf:type", expandCurie(typeCurie));
    }


    public Set<Description> objects(String sUri, String pCurie) {
        Set<Description> things = new HashSet<Description>();
        for (Object ref : objectValues(sUri, pCurie)) {
            things.add(newDescription((String) ref));
        }
        return things;
    }

    public Set<Object> objectValues(String sUri, String pCurie) {
        Resource s = (sUri != null)? toRef(sUri) : null;
        org.openrdf.model.URI p = (pCurie != null)?
                (org.openrdf.model.URI) toRef(expandCurie(pCurie)) : null;
        try {
            RepositoryResult<Statement> stmts = conn.getStatements(s, p, null,
                    inferred, contextRefs);
            Set<Object> values = new HashSet<Object>();
            while (stmts.hasNext()) {
                values.add(castValue(stmts.next().getObject()));
            }
            stmts.close();
            return values;
        } catch (RepositoryException e) {
            throw new DescriptionException(e);
        }
    }

    public Set<Triple> triples() {
      return triples(null);
    }

    public Set<Triple> triples(String sUri) {
      return triples(sUri, null, null);
    }

    public Set<Triple> triples(String sUri, String pCurie, String oUri) {
        Resource s = sUri != null? toRef(sUri) : null;
        org.openrdf.model.URI p = (pCurie != null)?
                (org.openrdf.model.URI) toRef(expandCurie(pCurie)) : null;
        Resource o = oUri != null? toRef(oUri) : null;
        try {
            RepositoryResult<Statement> stmts = conn.getStatements(s, p, o,
                    inferred, contextRefs);
            Set<Triple> values = new HashSet<Triple>();
            while (stmts.hasNext()) {
                Statement stmt = stmts.next();
                Triple triple = new Triple(this, fromRef(stmt.getSubject()),
                        fromRef(stmt.getPredicate()),
                        castValue(stmt.getObject()));
                values.add(triple);
            }
            stmts.close();
            return values;
        } catch (RepositoryException e) {
            throw new DescriptionException(e);
        }
    }


    Resource curieToRef(String curie) {
        return toRef(expandCurie(curie));
    }

    Resource toRef(String uriStr) {
        if (uriStr.startsWith("_:"))
            return vf.createBNode(uriStr.substring(2));
        return vf.createURI(uriStr);
    }

    BNode blankRef() {
        return vf.createBNode();
    }

    String fromRef(Resource ref) {
        if (ref instanceof BNode)
            return "_:"+((BNode)ref).getID();
        else
            return ref.stringValue();
    }

    Literal toLiteral(Object value) {
        return RDFLiteral.toRDFApiLiteral(vf, value);
    }

    Literal toLiteral(String value, String langOrDatatype) {
        if (langOrDatatype.startsWith("@"))
            return vf.createLiteral(value, langOrDatatype.substring(1));
        else
            return vf.createLiteral(value, vf.createURI(expandCurie(langOrDatatype)));
    }

    RDFLiteral fromLiteral(Literal literal) {
        return new RDFLiteral(literal);
    }


    void addRel(String about, String curie, String uri) {
        add(toRef(about), curieToRef(curie), toRef(uri));
    }

    String addBlankRel(String about, String curie) {
        BNode ref = blankRef();
        add(toRef(about), curieToRef(curie), ref);
        return fromRef(ref);
    }

    String addBlankRev(String curie, String about) {
        BNode ref = blankRef();
        add(ref, curieToRef(curie), toRef(about));
        return fromRef(ref);
    }

    void addLiteral(String about, String curie, Object value) {
        add(toRef(about), curieToRef(curie), toLiteral(value));
    }

    void addLiteral(String about, String curie, String value, String langOrDatatype) {
        add(toRef(about), curieToRef(curie), toLiteral(value, langOrDatatype));
    }

    void add(Resource s, Value p, Value o) {
        try {
            conn.add(s, (org.openrdf.model.URI)p, o, contextRefs);
        } catch (RepositoryException e) {
            throw new DescriptionException(e);
        }
    }


    void remove(String s, String curie) {
        remove(toRef(s), curieToRef(curie), null);
    }

    void remove(String s, String curie, Object value) {
        remove(toRef(s), curieToRef(curie), toLiteral(value));
    }

    void remove(Resource s, Value p, Value o) {
        try {
            conn.remove(s, (org.openrdf.model.URI)p, o, contextRefs);
        } catch (RepositoryException e) {
            throw new DescriptionException(e);
        }
    }


    Object castValue(Value value) {
        return (value instanceof Literal)?
                fromLiteral((Literal)value) : fromRef((Resource)value);
    }


    public String toCurie(String uri) {
        if (uriPrefixMap == null) {
            uriPrefixMap = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : prefixes.entrySet()) {
                uriPrefixMap.put(entry.getValue(), entry.getKey());
            }
        }
        int lastDelimIdx = findLastDelimIdx(uri);
        if (lastDelimIdx == -1)
            return null;
        int offset = lastDelimIdx + 1;
        String prefix = uriPrefixMap.get(uri.substring(0, offset));
        if (prefix == null)
            return null;
        return prefix + ":" + uri.substring(offset, uri.length());
    }

    public static String getUriTerm(String uri) {
        return splitVocabTerm(uri)[1];
    }

    public static String[] splitVocabTerm(String uri) {
        String[] result = new String[] {null, null};
        int lastDelimIdx = findLastDelimIdx(uri);
        if (lastDelimIdx > -1) {
            int offset = lastDelimIdx + 1;
            result[0] = uri.substring(0, offset);
            if (offset < uri.length()) {
                result[1] = uri.substring(offset, uri.length());
            }
        }
        return result;
    }

    private static int findLastDelimIdx(String uri) {
        int lastDelimIdx = uri.lastIndexOf('#');
        if (lastDelimIdx == -1)
            lastDelimIdx = uri.lastIndexOf('/');
        if (lastDelimIdx == -1)
            lastDelimIdx = uri.lastIndexOf(':');
        return lastDelimIdx;
    }

}
