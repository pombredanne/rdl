package se.lagrummet.rinfo.main.storage

import org.junit.runner.RunWith; import spock.lang.*


@Speck @RunWith(Sputnik) class FeedCollectorSpeck {

    def "should collect new since last"() {
    }

    /* TODO:

    "should verify md5 and length"

    "should sort unsorted feed page"

    "should check all entries with equal timestamps"
    "should collect entries with same timestamp as stopping entry"

    "should continue storing after collect even if stopOnEntry"
        // may be more than 1 with same timestamp

    "should fail and log on missing rdf"

    ...

    */

}
