package se.lagrummet.rinfo.main.storage

import org.junit.runner.RunWith; import spock.lang.*

import se.lagrummet.rinfo.store.depot.DepotContent
import se.lagrummet.rinfo.store.depot.DepotEntry


@Speck @RunWith(Sputnik) class SourceFeedsConfigurationSpeck {

    def sourceFeedsEntryId = new URI("http://example.org/sources")
    def sourceFeedUrls = [
        new URL("http://regeringen.se/sfs/feed/current"),
        new URL("http://dom.se/dvfs/feed/current"),
    ]

    def "Collect scheduler gets source feeds from rdf in configured entry"() {

        setup: "configure handler with entry id"
        def collectScheduler = testScheduler()
        def sourceFeedsConfigHandler = new SourceFeedsConfigHandler(
                collectScheduler, sourceFeedsEntryId)

        when: "an entry with expected id is created"
        sourceFeedsConfigHandler.onCreate(null, mockSourcesEntry())

        then: "the collect scheduler gets source feeds from the entry data"
        collectScheduler.sourceFeedUrls == sourceFeedUrls
    }

    def testScheduler() {
        new FeedCollectScheduler(null)
    }

    def mockSourcesEntry() {
        [
            getId: {sourceFeedsEntryId},
            getMimeType: {"application/rdf+xml"},
            findContents: {
                [
                    new DepotContent(new File("src/test/resources/source_feeds.rdf"),
                        "${sourceFeedsEntryId}/rdf", "application/rdf+xml"),
                ]
            }
        ] as DepotEntry
    }

}