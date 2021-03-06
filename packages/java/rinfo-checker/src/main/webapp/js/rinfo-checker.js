$(document).ready(function () {
    setupSubmitButtonEnabler();
    overrideFormSubmit();
});

function setupSubmitButtonEnabler() {
    if(!$("#feedUrl").val()) {
        $("#submitButton").attr("disabled", true);
    }

    $("#feedUrl").on("change", function() {
        if(!$("#feedUrl").val()) {
            $("#submitButton").attr("disabled", true);
        } else {
            $("#submitButton").attr("disabled", false);
        }
    });
}

function overrideFormSubmit() {
    $("#html-form").submit(function () {
        $.ajax({
            type:"POST",
            url:$("#html-form").attr("action"),
            data:$("#html-form").serialize(),
            beforeSend:function () {
                blockUI();
            },
            success:function (data) {
                $('#target').html(data);
                addErrorFilters();
                unBlockUI();
            },
            error:function (xhr, ajaxOptions, thrownError) {
                $('#target').html(xhr.responseText);
                unBlockUI();
            }
        });
        return false;
    });
}

function addErrorFilters() {
    removeHeader();
    wrapForSliding();
    hightlightUriSuggestions();

    var filter_div = $("<div class='filterBox'><h3>Filtrera</h3><div class='filter'></div></div>");
    $("h4:contains('Poster')").before(filter_div);

    allFilterMatches.reset();

    addFilterForError(new Filter({errorType:ErrorTypes.PROPERTY, pattern:"Saknar obligatoriskt värde för egenskap"}));
    addFilterForError(new Filter({errorType:ErrorTypes.PROPERTY, pattern:"Värdet .* matchar inte datatyp för egenskap"}));
    addFilterForError(new Filter({errorType:ErrorTypes.URI, pattern:"Angiven URI matchar inte den URI som beräknats utifrån egenskaper i dokumentet"}));
    addFilterForError(new Filter({errorType:ErrorTypes.PROPERTY, pattern:"Saknar svenskt språkattribut (xml:lang) för egenskap"}));
    addFilterForError(new Filter({errorType:ErrorTypes.URI, pattern:"Kan inte tolka URI:n"}));
    addFilterForError(new Filter({errorType:ErrorTypes.PROPERTY, pattern:"Okänd egenskap"}));
    addFilterForError(new Filter({errorType:ErrorTypes.URI, pattern:"Angiven identifierare är ingen korrekt IRI"}));

    removeURIFromCodeElements();

    createFilterForPostsWithoutErrors();
    createFilterForUnmatchedRows();

    if (allFilterMatches.length == 0) {
        $('.filterBox').hide();
    } else if (allFilterMatches.length > 1) {
        addHideShowAllButtons();
    }

    hideAll();
}

function removeHeader() {
    $('#target').find('h1').first().remove();
}

function wrapForSliding() {
    $('table.report').find('tr').find('td').wrapInner('<div style="display: block;" />');
}

function hightlightUriSuggestions() {
    $('div.uriSuggestion').each(function () {
        var data =$(this).text();
        var arr = data.split('/');
        var replace = "";
        for (i = 0; i < arr.length; i++) {
            if (arr[i].indexOf("{") >= 0) {
                replace += "<span class='highlight'>" + arr[i] + "</span>"
            } else {
                replace += "<span>" + arr[i] + "</span>"
            }
            if (i < arr.length-1 ) {
                replace += "<span>/</span>"
            }
        }
        $(this).html(replace);
    });
}

function addHideShowAllButtons() {
    $('div.filter').prepend(
        $("<div class='hide_show_all'>" +
            "<span id='show_all'><button onclick='showAll()'>Visa alla</button></span>" +
            "<span id='hide_all'><button onclick='hideAll()'>Dölj alla</button></span>" +
        "</div>")
    );
}

function addFilterForError(filterType) {
    var matches = getMatchesForError(filterType);
    var unique_matches = matches.unique();

    var i;
    for (i = 0; i < unique_matches.length; ++i) {
        var match = unique_matches[i];
        var match_count = getCount(matches, match);

        var filterMatch = new FilterMatch({
            pattern:filterType.get('pattern'),
            errorType:filterType.get('errorType'),
            match:match,
            isDisplayed:false,
            rows:[],
            id:filterType.get('pattern').hashCode() + '_' + i});

        var title = replaceAdditionalChars(htmlEscape(filterType.get('pattern'))) + ": " + match + " - " + match_count + "st";
        addButtonForFilterMatch(title, filterMatch);

        filterMatch.setupRows();

        allFilterMatches.add(filterMatch);
    }
}

function getMatchesForError(filterType) {
    var matches = [];
    var pattern = encodeForRegexPattern(filterType.get('pattern'));

    switch(filterType.get('errorType')) {
        case ErrorTypes.PROPERTY:
            matches = getMatchesForPatternWithProperty(pattern);
            break;
        case ErrorTypes.URI:
            matches = getMatchesForPatternWithURI(pattern);
            break;
        default:
            break;
    }

    return matches;
}

function getMatchesForPatternWithProperty(pattern) {
    var matches = [];

    $('table.report').find('tr').find('dl').each(function () {
        if ($(this).text().match(pattern)) {
            var matchWithNameSpace = $(this).find('code').text();
            var matchWithoutNameSpace = matchWithNameSpace
                                            .trimBeforeLastOccurenceOfChar("/")
                                            .trimBeforeLastOccurenceOfChar("#");
            matches.push(matchWithoutNameSpace);
        }
    });

    return matches;
}

function getMatchesForPatternWithURI(pattern) {
    var matches = [];

    $('table.report').find('tr').each(function () {
        if ($(this).text().match(pattern)) {
            matches.push("");
        }
    });

    return matches;
}

function addButtonForFilterMatch(title, filterMatch) {
    var div = $("<div id='filtrera'></div>");
    var title = $("<span>" + title + "</span>");
    var button = $("<span class='filtermatch_button'><button id='filter_" + filterMatch.get('id') + "'>Visa</button></span>");
    button.click(createCallbackForError(filterMatch));

    div.append(button);
    div.append(title);
    $('div.filter').append(div);
}

function createCallbackForError(filterMatch) {
    return function () {
        filterMatch.toggleVisibility();
    }
}

function hideAll() {
    logToConsole("hideAll");
    for (var i = 0, l = allFilterMatches.length; i < l; i++) {
        allFilterMatches.at(i).hide();
    }
}

function showAll() {
    logToConsole("showAll");
    for (var i = 0, l = allFilterMatches.length; i < l; i++) {
        allFilterMatches.at(i).show();
        allFilterMatches.at(i).setDisplayed(false);
    }
}

function createFilterForPostsWithoutErrors() {
    var rowsWithoutErrors = getRowsWithoutErrors();

    if(rowsWithoutErrors.length > 0) {

        var filterMatch = new FilterMatch({
            pattern:"Korrekta poster",
            errorType:ErrorTypes.NONE,
            match:"",
            isDisplayed:false,
            rows:"",
            id:'rowsWithoutErrors'});

        var title = htmlEscape(filterMatch.get('pattern')) + " - " + rowsWithoutErrors.length + "st";
        addButtonForFilterMatch(title, filterMatch);

        filterMatch.setupRowsFromList(rowsWithoutErrors);

        allFilterMatches.add(filterMatch);
    }
}

function createFilterForUnmatchedRows() {
    var unmatchedRows = getUnmatchedRows();

    if(unmatchedRows.length > 0) {

        var filterMatch = new FilterMatch({
            pattern:"Övriga",
            errorType:ErrorTypes.NONE,
            match:"",
            isDisplayed:false,
            rows:"",
            id:'unmatchedRows'});

        var title = htmlEscape(filterMatch.get('pattern')) + " - " + unmatchedRows.length + "st";
        addButtonForFilterMatch(title, filterMatch);

        filterMatch.setupRowsFromList(unmatchedRows);

        allFilterMatches.add(filterMatch);
    }
}

function getRowsWithoutErrors() {

    var rowsWithoutErrors = [];

    $('table.report').find('tr').find('.status').find('div').each(function () {
        var status = $(this).text();
        if (status == "OK") {
            var row = $(this).closest('tr').find('.position').find('div').text();
            rowsWithoutErrors.push(row);
        }
    });

    logToConsole("rowsWithoutErrors: " + rowsWithoutErrors);

    return rowsWithoutErrors;
}

function getUnmatchedRows() {

    var unmatchedRows = [];

    $('table.report').find('tr').find('.position').find('div').each(function () {
        var row = $(this).text();
        var isMatched = false;

        allFilterMatches.each(function (filterMatch) {
            filterMatch.get('rows').each(function (rowModel) {
                if(row == rowModel.get("position")) {
                    isMatched = true;
                }
            });
        });

        if(!isMatched) {
            unmatchedRows.push(row);
        }
    });

    logToConsole("unmatchedRows: " + unmatchedRows);

    return unmatchedRows;
}

function removeURIFromCodeElements() {
    logToConsole("removeURIFromMessages");

    $('table.report').find('tr').find('code').text(function (i, t) {
        return t.replace("http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#","")
                .replace("http://purl.org/dc/terms/","");
    });
}

Array.prototype.unique = function () {
    var arrVal = this;
    var uniqueArr = [];
    for (var i = arrVal.length; i--;) {
        var val = arrVal[i];
        if ($.inArray(val, uniqueArr) === -1) {
            uniqueArr.unshift(val);
        }
    }
    return uniqueArr;
}

String.prototype.hashCode = function(){
    var hash = 0, i, char;
    if (this.length == 0) return hash;
    for (i = 0, l = this.length; i < l; i++) {
        char  = this.charCodeAt(i);
        hash  = ((hash<<5)-hash)+char;
        hash |= 0; // Convert to 32bit integer
    }
    return hash;
};

String.prototype.trimBeforeLastOccurenceOfChar = function(char){
    var splitResult = this.split(char);
    return splitResult[splitResult.length-1];
}

function getCount(arr, val) {
    var ob = {};
    var len = arr.length;
    for (var k = 0; k < len; k++) {
        if (ob.hasOwnProperty(arr[k])) {
            ob[arr[k]]++;
            continue;
        }
        ob[arr[k]] = 1;
    }
    return ob[val];
}

var ErrorTypes = {"NONE" : 0, "PROPERTY" : 1, "URI": 2};

var Filter = Backbone.Model.extend({
    defaults:{
        pattern:"",
        errorType:ErrorTypes.NONE
    },
    initialize:function () {
        this.logToConsole();
    },
    logToConsole:function () {
        logToConsole("pattern: " + this.get("pattern") + "," +
            " errorType: " + this.get("errorType"));
    }
});

var FilterMatch = Filter.extend({
    defaults:{
        match:"",
        isDisplayed:false,
        id:"",
        rows:""
    },
    initialize:function () {
        this.logToConsole();
        this.set('rows', new RowCollection());
    },
    logToConsole:function () {
        logToConsole("pattern: " + this.get("pattern") +
            ", errorType: " + this.get("errorType") +
            ", match: " + this.get("match") +
            ", id: " + this.get("id") +
            ", rows: " + this.get("rows") +
            ", isDisplayed: " + this.get("isDisplayed"));
    },
    toggleVisibility:function () {
        if (this.get("isDisplayed")) {
            this.hide();
        } else {
            hideAll();
            this.show();
        }
    },
    hide:function () {
        this._toggleAllRows("hide");
        this.setDisplayed(false);
    },
    show:function () {
        this._toggleAllRows("show");
        this.setDisplayed(true);
    },
    setupRows:function () {
        var that = this;
        $('table.report').find('tr').each(function () {
            if (that._hasFilterMatch(this)) {
                var position = $(this).find('.position').find('div').text();
                var row = $(this).find('div');
                that._addRow(row, position);
            }
        });
    },
    setupRowsFromList:function (rowsList) {
        var that = this;
        for (var i = 0, l = rowsList.length; i < l; i++) {
            var position = rowsList[i];
            $('table.report').find('tr').find('.position').find('div').each(function () {
                if(position == $(this).text()) {
                    var row = $(this).closest('tr').find('div');
                    that._addRow(row, position);
                }
            });
        }
    },
    setDisplayed:function (display) {
        this.set("isDisplayed", display);
        var text = display ? "D&ouml;lj" : "Visa";
        $('#filter_' + this.get('id')).html(text);
    },
    _toggleAllRows: function (mode) {
        var rowModels = this.get("rows");
        for (var i = 0, l = rowModels.length; i < l; i++) {
            if(mode == "show") {
                rowModels.at(i).show();
            } else {
                rowModels.at(i).hide();
            }
        }
    },
    _addRow: function(row, position) {
        var rowModel = new RowModel({
            position: position
        });
        var rowView = new RowView({
            model:rowModel,
            el:row
        });
        var newRows = _.clone(this.get('rows'));
        newRows.add(rowModel);
        this.set('rows', newRows);
    },
    _hasFilterMatch:function (row) {
        var that = this;
        return $(row).text().match(encodeForRegexPattern(that.get("pattern")))
            && $(row).text().match(encodeForRegexPattern(that.get("match")));
    }
});

var FilterMatchCollection = Backbone.Collection.extend({
    model:FilterMatch
});

var RowModel = Backbone.Model.extend({
    defaults:{
        position:""
    },
    initialize: function() {
        logToConsole("rowModel created for position: " + this.get("position"));
    },
    hide: function(){
        this.trigger("hide");
    },
    show: function(){
        this.trigger("show");
    }
});

var RowView = Backbone.View.extend({
    initialize: function() {
        logToConsole("rowView created! " + this.$el.text());
        this.model.view = this;
        this.model.bind("hide", this.hide, this);
        this.model.bind("show", this.show, this);
    },
    hide: function() {
        this.$el.hide();
    },
    show: function() {
        this.$el.show();
    }
});

var RowCollection = Backbone.Collection.extend({
    model:RowModel
});

var allFilterMatches = new FilterMatchCollection();

var spinner = new Spinner({
    lines:13, // The number of lines to draw
    length:20, // The length of each line
    width:10, // The line thickness
    radius:30, // The radius of the inner circle
    corners:1, // Corner roundness (0..1)
    rotate:0, // The rotation offset
    direction:1, // 1: clockwise, -1: counterclockwise
    color:'#FFF', // #rgb or #rrggbb or array of colors
    speed:1, // Rounds per second
    trail:60, // Afterglow percentage
    shadow:false, // Whether to render a shadow
    hwaccel:false, // Whether to use hardware acceleration
    className:'spinner', // The CSS class to assign to the spinner
    zIndex:2e9, // The z-index (defaults to 2000000000)
    top:10, // Top position relative to parent in px
    left:'auto' // Left position relative to parent in px
});

//console.log might not be defined for i.e. IE8
var alertFallback = false;
if (typeof console === "undefined" || typeof console.log === "undefined") {
    console = {};
    if (alertFallback) {
        console.log = function (msg) {
            alert(msg);
        };
    } else {
        console.log = function () {
        };
    }
}

var logEnabled = false;

function logToConsole(message) {
    if (logEnabled) {
        console.log(message);
    }
}

function htmlEscape(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/å/g, '&aring;')
        .replace(/ä/g, '&auml;')
        .replace(/ö/g, '&ouml;')
        .replace(/Å/g, '&Aring;')
        .replace(/Ä/g, '&Auml;')
        .replace(/Ö/g, '&Ouml;');
}

function replaceAdditionalChars(str) {
    return String(str)
        .replace(' .*', '');
}

function encodeForRegexPattern(str) {
    return String(str)
        .replace(/\s/g, '\\s')
        .replace(/\(/g, '\\(')
        .replace(/\)/g, '\\)')
        .replace(/å/g, '.')
        .replace(/ä/g, '.')
        .replace(/ö/g, '.')
        .replace(/Å/g, '.')
        .replace(/Ä/g, '.')
        .replace(/Ö/g, '.');
}

function isBlank(str) {
    return (!str || /^\s*$/.test(str));
}

function blockUI() {
    logToConsole("blockUI!");
    var target = document.getElementById('spinner');
    spinner.spin(target);
    $('#spinner').show();
    $.blockUI({
        message: $('#spinner'),
        css: {
            border: 'none'
        }
    });
}

function unBlockUI() {
    logToConsole("unBlockUI!");
    spinner.stop();
    $('#spinner').hide();
    $.unblockUI();
}
