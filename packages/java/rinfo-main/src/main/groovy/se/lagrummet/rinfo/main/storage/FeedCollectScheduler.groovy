package se.lagrummet.rinfo.main.storage

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.ConfigurationException

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import se.lagrummet.rinfo.collector.AbstractCollectScheduler


class FeedCollectScheduler extends AbstractCollectScheduler {

    private final Logger logger = LoggerFactory.getLogger(FeedCollectScheduler.class)

    private URL adminFeedUrl
    private Collection<URL> publicSourceFeedUrls

    private Collection<URL> sourceFeedUrls

    Runnable batchCompletedCallback

    private FeedCollector feedCollector

    FeedCollectScheduler(FeedCollector feedCollector) {
        this.feedCollector = feedCollector
        this.sourceFeedUrls = Collections.emptyList()
    }

    @Override
    public Collection<URL> getSourceFeedUrls() {
        return sourceFeedUrls
    }

    public URL getAdminFeedUrl() {
        return adminFeedUrl
    }

    public void setAdminFeedUrl(URL adminFeedUrl) {
        this.adminFeedUrl = adminFeedUrl
        refreshSourceFeedUrls()
    }

    public Collection<URL> getPublicSourceFeedUrls() {
        return publicSourceFeedUrls
    }

    public void setPublicSourceFeedUrls(Collection<URL> publicSourceFeedUrls) {
        this.publicSourceFeedUrls = publicSourceFeedUrls
        refreshSourceFeedUrls()
    }

    @Override
    protected void collectFeed(URL feedUrl, boolean lastInBatch) {
        def credentials = newStorageCredentials(feedUrl)
        feedCollector.readFeed(feedUrl, credentials)
        // TODO:? Ok to ping after each collect?
        // Else, use if (lastInBatch && ...) to only run after last batch run.
        if (batchCompletedCallback != null) {
            batchCompletedCallback.run()
        }
    }

    protected StorageCredentials newStorageCredentials(URL feedUrl) {
        return new StorageCredentials(feedUrl.equals(adminFeedUrl))
    }

    private void refreshSourceFeedUrls() {
        boolean wasStarted = isStarted()
        if (wasStarted) {
            shutdown()
        }
        updateSourceFeedUrls()
        if (wasStarted) {
            startup()
        }
    }

    private void updateSourceFeedUrls() {
        Collection<URL> mergedSources = new ArrayList<URL>()
        if (adminFeedUrl != null) {
            mergedSources.add(adminFeedUrl)
        }
        if (publicSourceFeedUrls != null) {
            mergedSources.addAll(publicSourceFeedUrls)
        }
        this.sourceFeedUrls = Collections.unmodifiableList(mergedSources)
    }

}
