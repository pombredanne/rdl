package se.lagrummet.rinfo.store.depot


import org.junit.runner.RunWith
import spock.lang.*


@Speck @RunWith(Sputnik)
class FileDepotReadTest {

    Depot depot

    def setup() {
        depot = DepotUtil.depotFromConfig(
                "src/test/resources/rinfo-depot.properties")
    }


    def "should contain entry"() {
        when:
        def entry = depot.getEntry("/publ/1901/100")
        then:
        entry != null
        entry.id == new URI("http://example.org/publ/1901/100")
        entry.updated == entry.published
    }

    def "should find entry content"() {
        when:
        def entry = depot.getEntry("/publ/1901/100")
        def contents = entry.findContents("application/pdf")
        def i = 0
        then:
        contents.each {
            assert it.mediaType == "application/pdf"
            if (it.lang == "en") {
                i++
               assert it.depotUriPath == "/publ/1901/100/pdf,en"
            } else if (it.lang == "sv") {
                i++
                assert it.depotUriPath == "/publ/1901/100/pdf,sv"
            }
        }
        i == 2

        when:
        contents = entry.findContents("application/rdf+xml")
        def content = contents[0]
        then:
        contents.size() == 1
        content.mediaType == "application/rdf+xml"
        content.depotUriPath == "/publ/1901/100/rdf"
        content.lang == null
    }

    def "should find enclosure"() {
        when:
        def enclosures = depot.getEntry("/publ/1901/100").findEnclosures()
        then:
        enclosures.size() == 1
        when:
        def encl = enclosures[0]
        then:
        encl.depotUriPath == "/publ/1901/100/icon.png"
        encl.mediaType == "image/png"
    }

    def "should find nested enclosure"() {
        when:
        def entryPath = "/publ/1901/100/revisions/1902/200"
        def enclosures = depot.getEntry(entryPath).findEnclosures()
        def encl = enclosures[0]
        then:
        encl.depotUriPath == "${entryPath}/styles/screen.css"
        // TODO: needs to configure FileDepot computeMediaType
        //assertEquals "text/css", encl.mediaType
    }

    def "should not get deleted"() {
        when:
        def entry = depot.getEntry("/publ/1901/0")
        then:
        thrown(DeletedDepotEntryException)
    }

    def "should find all entry content as list"() {
        when:
        def results = depot.find("/publ/1901/100")
        then:
        results.size() == 3
    }

    def "should find direct content"() {
        when:
        def results = depot.find("/publ/1901/100/icon.png")
        then:
        results.size() == 1
    }

    // TODO: getHistoricalEntries..

    def "should iterate entries"() {
        when:
        def entries = depot.iterateEntries().toList()
        then:
        entries.size() == 5

        when: "include deleted"
        entries = depot.iterateEntries(false, true)
        then:
        entries.size() == 6

        // TODO: historical: depot.iterateEntries(true, false)
    }


    // negative tests

    def "should disallow uris not within base uri"() {
        when:
        def entry = depot.getEntry(
                new URI("http://example.com/some/path"))
        then:
        thrown(DepotUriException)
    }

    @Unroll("should fail on <#path>")
    def "should disallow non absolute or full uri paths"() {
        when:
        def entry = depot.getEntry(path)
        then:
        thrown(DepotUriException)
        where:
        path << [
            "http://example.org/some/path",
            "../some/path",
            "some/path"
        ]
    }

    // edge cases / regressions

    def "should find rdf in top entry"() {
        when:
        def entry = depot.getEntry("/dataset")
        def contents = entry.findContents("application/rdf+xml", null)
        then:
        contents.size() == 1
        def content = contents[0]
        content.mediaType == "application/rdf+xml"
        content.depotUriPath == "/dataset/rdf"
    }

}
