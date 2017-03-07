DEFAULT_API_KEY = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.BGNkz04H_AR4D_OrML6oPv_qu9UdfBOWO_gM3cQmn-U";

function loadComponents() {
	$.ajaxSetup({
		headers: {
			Authorization: DEFAULT_API_KEY
		}
	});

	$("#loading-div").hide();
}

function loadAjaxSimple(ws, dataElement, resultElement, resultField) {
	$.ajax({
        type: "GET",
        url: "/ws/" + ws,
        dataType: "json",
        data: { "text": dataElement.val()
        },
        beforeSend: function() {
        	resultElement.empty(); // Clear the table body
        	$("#loading-div").show();
        },
        complete: function() {
        	$("#loading-div").hide();
        },
        success: function (result) {
            resultElement.val(result[resultField]);
        },
        error: function(jqXHR, textStatus) {
        	console.log("Error: " + textStatus);
        }
    });
}

function loadKeywords(ws, textElement, methodElement, resultElement) {
	$.ajax({
        type: "GET",
        url: "/ws/" + ws,
        dataType: "json",
        data: { "text": textElement.val(),
        		"method": methodElement.val()
        },
        beforeSend: function() {
        	resultElement.empty(); // Clear the table body
        	$("#loading-div").show();
        },
        complete: function() {
        	$("#loading-div").hide();
        },
        success: function (result) {
        	var resultText = "";
        	for (var i = 0; i < result.keywords.length; i++) {
        		var keyword = result.keywords[i];
        		var markup = "<tr><td class='col-left'>" + keyword.text + "</td>" +
        				"<td class='col-right'>" + keyword.score + "</td></tr>";
        		resultElement.append(markup);
        	}
        },
        error: function(jqXHR, textStatus) {
        	console.log("Error: " + textStatus);
        }
    });
}

$(document).ready(function() {
	// Load
	loadComponents();
	
	// Lemmatize
	$("#lemmatize-btn").click(function(e) {
		loadAjaxSimple("lemmatize", $("#original-text"), $("#result-text"), "lemmatizedText");
        e.preventDefault();
        return false;
	});
	
	// Tag
	$("#tag-btn").click(function(e) {
		loadAjaxSimple("tag", $("#original-text"), $("#result-text"), "taggedText");
        e.preventDefault();
        return false;
	});
	
	// Keywords
	$("#keywords-btn").click(function(e) {
		loadKeywords("keywords", $("#original-text"), $("#method"), $("#keywords-table tbody"));
        e.preventDefault();
        return false;
	});
});