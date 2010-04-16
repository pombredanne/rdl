"""
Application Tools
"""
from fabric.api import *


def ping_main_collector():
    #require('roledefs', provided_by=targetenvs)
    collector_url = "http://%s/collector/" % env.roledefs['main'][0]
    feed_url = "http://%s:8182/feed/current" % env.roledefs['examples'][0]
    local("curl --data 'feed=%(feed_url)s' %(collector_url)s"%vars())

def ping_service_collector():
    #require('roledefs', provided_by=targetenvs)
    collector_url = "http://%s/collector/" % env.roledefs['service'][0]
    feed_url = "http://%s/feed/current" % env.roledefs['main'][0]
    local("curl --data 'feed=%(feed_url)s' %(collector_url)s"%vars())
