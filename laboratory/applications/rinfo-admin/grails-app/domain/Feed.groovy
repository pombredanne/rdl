class Feed {

    static constraints = {
        url(url:true, blank:false, maxSize:400)
        identifier(blank:false, maxSize:300)
        organization(blank:false) 
        lastUpdated(nullable: true)
        dateCreated()
    }

    static belongsTo = Organization
    Organization organization    

    String url
    String identifier

    String toString() { 
        return "Källa: " + url
    }

    String rinfoURI() {
        return "http://rinfo.lagrummet.se/source/" + id
   }   

    // Uppdateras automatiskt av Grails
    Date dateCreated
    Date lastUpdated
}
