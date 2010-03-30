package se.lagrummet.rinfo.service


import org.restlet.Context
import org.restlet.data.CharacterSet
import org.restlet.data.Language
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Request
import org.restlet.data.Response
import org.restlet.data.Status
import org.restlet.resource./*.representation.*/InputRepresentation
import org.restlet.resource./*.representation.*/StringRepresentation
import org.restlet.resource./*.representation.*/Representation
import org.restlet.resource./*.representation.*/Variant
import org.restlet./*resource.*/Finder
import org.restlet./*resource.*/Handler
import org.restlet.resource.Resource
import org.restlet./*routing.*/Router
import org.restlet.util.Template
import org.restlet.util.Variable

import javax.xml.transform.Templates
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import org.apache.commons.configuration.ConfigurationUtils

import org.openrdf.repository.Repository

import org.antlr.stringtemplate.StringTemplateGroup

import net.sf.json.JSONSerializer
//import net.sf.json.groovy.JsonSlurper

import se.lagrummet.rinfo.service.dataview.TemplateUtil
import se.lagrummet.rinfo.service.dataview.RDataSparqlTree


class SparqlTreeRouter extends Router {

    SparqlTreeRouter(Context context, Repository repo) {
        super(context)

        // TODO: app-specific global data
        // .. reasonably updated by certain incoming resources (e.g. model, datasets)
        Map appData = [
            "encoding": "utf-8",
            "resourceBaseUrl": "http://rinfo.lagrummet.se/",
            "basePath": "/rdata",
            "profile": [
                "prefix": [
                    "rpubl": "http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#",
                    "dct": "http://purl.org/dc/terms/",
                ],
                //"default": "rpubl",
                //"define": ["a": "rdf:type"],
                //"define": ["publisher": "dct:publisher"],
            ]
        ]

        def templates = new StringTemplateGroup("sparqltrees")
        templates.setFileCharEncoding(appData["encoding"])
        templates.setRefreshInterval(0) // TODO: cache-control; make configurable
        def tpltUtil = new TemplateUtil(templates)

        // complete list
        attach("/org", new RDataFinder(context,
                    repo, appData, tpltUtil,
                    "sparqltrees/org/org-tree-rq",
                    "sparqltrees/org/org_html"))

        // TODO:IMPROVE:
        // - nicer paths? But even "/rpubl/-/{path}" goes to "/rpubl/{path}"!

        // filtering + narrowing
        def browseFinder = new RDataFinder(context,
                repo, appData, tpltUtil,
                "sparqltrees/rdata/publ-list_all_params-rq",
                "sparqltrees/rdata/publ_params_html") {

                @Override
                Map getQueryData(Request request) {
                    def queryData = [
                        "docType": request.attributes["type"],
                        "publisher": request.attributes["publisher"],
                    ]
                    return queryData
                }

            }
        attach("/browse/publ", browseFinder)
        attach("/browse/publ/{type}", browseFinder)
        attach("/browse/publ/{type}/{publisher}", browseFinder)
        //"/years/publ?"

        // list of
        attach("/list/publ/{type}/{publisher}/{dateProperty}@{year}",
            new RDataFinder(context,
                repo, appData, tpltUtil,
                "sparqltrees/rdata/rpubl-tree-rq",
                "sparqltrees/rdata/publ_list_html") {

                @Override
                Map getQueryData(Request request) {
                    def queryData = [:]
                    def attrs = request.attributes
                    def filters = [
                        ["typeSelector": attrs["type"]],
                        ["dateSelector": attrs["dateProperty"], "value": attrs["year"]],
                        ["publisherSelector": true, "value": attrs["publisher"]],
                    ]
                    if (filters) {
                        queryData["filter_parts"] = filters
                    }
                    return queryData
                }

            })

        // one
        attach("/publ/{path}",
            new RDataFinder(context,
                    repo, appData, tpltUtil,
                    "sparqltrees/rdata/rpubl-tree-rq",
                    "sparqltrees/rdata/publ_doc_html") {

                @Override
                Map getQueryData(Request request) {
                    // TODO:? configure metadataService to remove extension?
                    // (+ how does conneg work right now?)
                    def path = request.attributes["path"]
                    if (path) {
                        def ext = request.resourceRef.extensions
                        if (ext && path.endsWith("."+ext)) {
                            path = path.substring(0, path.length()-(ext.length()+1))
                        }
                    }
                    return [
                        "path": path,
                        'details': true,
                    ]
                }

            }
        // TODO:IMPROVE: find a nicer way to capture path param.. Like {path:anyPath}..
        ).template.variables.put("path", new Variable(Variable.TYPE_URI_PATH))

    }

}


class RDataFinder extends Finder {

    static final DEFAULT_LOCALE = "sv"
    static final DEFAULT_MAX_LIMIT = 4096

    Repository repo
    Map appData
    TemplateUtil tpltUtil
    String queryTpltPath
    String viewTpltPath

    RDataFinder(Context context, Repository repo,
            appData, tpltUtil, queryTpltPath, viewTpltPath) {
        super(context)
        this.repo = repo
        this.appData = appData
        this.tpltUtil = tpltUtil
        this.queryTpltPath = queryTpltPath
        this.viewTpltPath = viewTpltPath
    }

    /**
     * Template method used to extract params to query from request.
     */
    Map getQueryData(Request request) {
        return [:]
    }

    Map runQuery(Request request) {
        def locale = DEFAULT_LOCALE // or from Variant..
        def dataRqTree = new RDataSparqlTree(appData, locale)
        def queryData = getQueryData(request)
        queryData["max_limit"] = DEFAULT_MAX_LIMIT
        def query = tpltUtil.runTemplate(queryTpltPath, queryData)
        def tree = dataRqTree.runQuery(repo, query)
        if (queryData["path"] && !tree.any { k, v -> v } )
            return null
        tree.locale = locale
        tree.putAll(appData)
        return tree
    }

    String makeHtmlView(Map tree) {
        return tpltUtil.runTemplate(viewTpltPath, tree)
    }

    @Override
    Handler findTarget(Request request, Response response) {
        return new Resource(getContext(), request, response) {

            @Override
            List<Variant> getVariants() {
                return [
                    new Variant(MediaType.TEXT_HTML),
                    new Variant(MediaType.APPLICATION_JSON),
                ]
            }

            @Override
            Representation represent(Variant variant) {
                def tree = runQuery(getRequest())
                if (tree == null) {
                    return null
                }
                if (variant.mediaType.equals(MediaType.APPLICATION_JSON)) {
                    return toRepresentation(JSONSerializer.toJSON(tree).toString(4),
                            MediaType.APPLICATION_JSON, tree.locale)
                } else {
                    return toRepresentation(makeHtmlView(tree),
                            MediaType.TEXT_HTML, tree.locale)
                }
            }

            def toRepresentation(repr, mediaType, locale) {
                return new StringRepresentation(repr, mediaType,
                        new Language(locale), new CharacterSet("utf-8"))
            }

        }

    }

}
