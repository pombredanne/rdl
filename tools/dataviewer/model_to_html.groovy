import org.apache.commons.configuration.ConfigurationUtils
import net.sf.json.groovy.JsonSlurper
import org.antlr.stringtemplate.StringTemplateGroup

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.sparqltree.SmartLens
import se.lagrummet.rinfo.base.rdf.sparqltree.GraphBuilder

import se.lagrummet.rinfo.service.dataview.SparqlTreeViewer
import se.lagrummet.rinfo.service.dataview.ModelViewHandler


def repo = RDFUtil.slurpRdf(
        "../../resources/base/model",
        "../../resources/base/extended/rdf",
        "../../resources/external/rdf")
//def repo = getRepo("http://localhost:8080/sesame-workbench", "rinfo")

def templates = new StringTemplateGroup("sparqltrees")
def rqViewer = new SparqlTreeViewer(repo, templates,
        "sparqltrees/model/model-tree-rq", "sparqltrees/model/model-html")

def labelTree = new JsonSlurper().parse(ConfigurationUtils.locate(
                "sparqltrees/model/model-labels.json"))

def (fname, locale, mediabase) = args as List
locale = locale ?: 'sv'
options = [
    mediabase: mediabase ?: '.'
]

def out = {
    rqViewer.execute(new ModelViewHandler(locale, null, labelTree, options))
}
if (fname) {
    println "Writing to: $fname"
    new File(fname).text = out()
} else {
    println out()
}

