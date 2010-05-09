package se.lagrummet.rinfo.main.storage

import spock.lang.*

import se.lagrummet.rinfo.store.depot.Depot
import se.lagrummet.rinfo.store.depot.DepotContent
import se.lagrummet.rinfo.store.depot.DepotEntry


class SourceFeedsConfigurationSpeck extends Specification {

    def sourceFeedsEntryId = new URI("http://example.org/sources")
    def sourceFeedUrls = [
        new URL("http://regeringen.se/sfs/feed/current"),
        new URL("http://dom.se/dvfs/feed/current"),
    ]

    def "Collect scheduler gets source feeds from rdf in configured entry"() {
        setup: "configure handler with entry id"
        def collectScheduler = new FeedCollectScheduler(null)
        def sourceFeedsConfigHandler = new SourceFeedsConfigHandler(
                collectScheduler, sourceFeedsEntryId)
        def session = new StorageSession(new StorageCredentials(true),
                Mock(Depot), [], Mock(CollectorLogSession), null)

        when: "an entry with expected id is created"
        sourceFeedsConfigHandler.onModified(session, mockSourcesEntry(), true)

        then: "the collect scheduler gets source feeds from the entry data"
        collectScheduler.sourceFeedUrls == sourceFeedUrls
    }

    def "Source feed entry must come from admin session"() {
        setup: "non-admin credentials"
        def sourceFeedsConfigHandler = new SourceFeedsConfigHandler(
                null, sourceFeedsEntryId)
        def session = new StorageSession(new StorageCredentials(false),
                Mock(Depot), [], Mock(CollectorLogSession), null)

        when: "an entry with expected id is created"
        sourceFeedsConfigHandler.onModified(session, mockSourcesEntry(), true)

        then: "config handler fails on non-admin credentials"
        thrown(Exception) // TODO: NotAllowedException..
    }

    private mockSourcesEntry() {
        DepotEntry entry = Mock()
        entry.getId() >> sourceFeedsEntryId
        entry.getMimeType() >> "application/rdf+xml"
        entry.findContents() >> [
                new DepotContent(new File("src/test/resources/source_feeds.rdf"),
                    "${sourceFeedsEntryId}/rdf", "application/rdf+xml"),
            ]
        return entry
    }

}
