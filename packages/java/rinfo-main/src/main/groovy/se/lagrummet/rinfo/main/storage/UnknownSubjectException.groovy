package se.lagrummet.rinfo.main.storage


class UnknownSubjectException extends Exception {

    URI subject
    List uriSuggestions

    UnknownSubjectException(URI subject, List uriSuggestions) {
        super("Kan inte tolka URI:n <"+subject+
                ">. Detta kan bero på att URI:n innehåller delar som inte är konfigurerade i RDL eller att delarna är felstavade eller på annat sätt felaktiga.");
        this.subject = subject
        this.uriSuggestions = uriSuggestions
    }

}
