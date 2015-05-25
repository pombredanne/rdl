package se.lagrummet.rinfo.base.feed.type;

import se.lagrummet.rinfo.base.feed.util.Utils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by christian on 5/21/15.
 */
public class FeedUrl extends CommonUrl {

    public static FeedUrl create(URL feedUrl) {
        return new FeedUrl(feedUrl);
    }

    public static FeedUrl parse(String feedUrl) throws MalformedURLException {
        return new FeedUrl(new URL(feedUrl));
    }

    private FeedUrl(URL feedUrl) {
        super(feedUrl);
    }

    public DocumentUrl createDocumentUrl(String relativeUrl) throws MalformedURLException {
        return DocumentUrl.parse(getUrl(),relativeUrl);
    }

    public static FeedUrl parse(URL base, String feedUrl) throws MalformedURLException {
        return create(Utils.parse(base, feedUrl));
    }

}
