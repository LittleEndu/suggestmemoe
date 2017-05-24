/**
 * Created by Endrik on 23-May-17.
 */
"use strict";
(function () {
    var backgrounds = ['705197.png', '796554.jpg', '800259.jpg'];
    var rand = backgrounds[Math.floor(Math.random() * backgrounds.length)];
    var searchResults = [];
    var animeList = [];
    var suggestions = [];
    var resultsContainer = document.getElementById("results_container");
    var animeContainer = document.getElementById("anime_container");
    var suggestionsContainer = document.getElementById("suggestion_container");
    $("body").css('background-image', 'url(/' + rand + ')');
    var socket = new WebSocket("ws://" + location.host + "/ws");
    socket.onmessage = function (event) {
        var json = JSON.parse(event.data);
        suggestions = json;
        while (suggestionsContainer.children.length!=0){
            suggestionsContainer.removeChild(suggestionsContainer.children[0]);
        }
        for (var index=0; index<suggestions.length; index++){
            var anime = createAnime(suggestions[index], "sugbutton");
            suggestionsContainer.appendChild(anime)
        }
        $(".sugbutton").on("click", function (t) {
            var button = t.target;
            var anime = button.parentNode;
            animeContainer.appendChild(anime);
            anime.removeChild(button);
            var newButton = document.createElement("button");
            newButton.setAttribute("type", "button");
            newButton.setAttribute("class", "removebutton");
            newButton.appendChild(document.createTextNode("Remove"));
            anime.appendChild(newButton);
            for (var index = 0; index < suggestions.length; index++) {
                if (suggestions[index].id == anime.getAttribute("data-id")) {
                    animeList.push(suggestions[index]);
                    suggestions.splice(index, 1);
                }
            }
            updateButtons();
            updateSuggestions();
        });
    };


    function updateResults() {
        while (resultsContainer.children.length != 0) {
            resultsContainer.removeChild(resultsContainer.children[0]);
        }
        for (var index in searchResults) {
            var anime = createAnime(searchResults[index], "serbutton");
            resultsContainer.appendChild(anime);
        }
        $(".serbutton").on("click", function (t) {
            var button = t.target;
            var anime = button.parentNode;
            animeContainer.appendChild(anime);
            anime.removeChild(button);
            var newButton = document.createElement("button");
            newButton.setAttribute("type", "button");
            newButton.setAttribute("class", "removebutton");
            newButton.appendChild(document.createTextNode("Remove"));
            anime.appendChild(newButton);
            for (var index = 0; index < searchResults.length; index++) {
                if (searchResults[index].id == anime.getAttribute("data-id")) {
                    animeList.push(searchResults[index]);
                    searchResults.splice(index, 1);
                }
            }
            updateButtons();
            updateSuggestions();
        });
    }

    function updateButtons() {
        $(".removebutton").on("click", function (tt) {
            var anime = tt.target.parentNode;
            for (var index = 0; index < animeList.length; index++) {
                if (animeList[index].id == anime.getAttribute("data-id")) {
                    animeList.splice(index, 1);
                    break;
                }
            }
            animeContainer.removeChild(anime);
            updateSuggestions();
        });
    }

    function updateSuggestions() {
        var msg = {};
        msg.type = "animelist";
        msg.animelist = animeList;
        var json = JSON.stringify(msg);
        while (socket.readyState==false){
            //wait here until we connect
        }
        socket.send(json);
    }

    function createAnime(data, classname) {
        var animeDiv = document.createElement("div");
        animeDiv.setAttribute("class", "anime");
        animeDiv.setAttribute("data-id", data.id);

        var animeP = document.createElement("p");
        animeP.setAttribute("class", "animetext");
        animeP.appendChild(document.createTextNode(data.name));

        var animeButton = document.createElement("button");
        animeButton.setAttribute("type", "button");
        animeButton.setAttribute("class", classname);
        animeButton.appendChild(document.createTextNode("Save"));

        animeDiv.appendChild(animeP);
        animeDiv.appendChild(animeButton);
        return animeDiv;
    }

    $("#search_box").on("keydown", function (key) {
        if (key.keyCode == 13) {
            while (resultsContainer.children.length != 0) {
                resultsContainer.removeChild(resultsContainer.children[0]);
            }
            var loadingText = document.createElement("div");
            loadingText.appendChild(document.createTextNode("Loading"));
            resultsContainer.appendChild(loadingText);
            var query = $("#search_box").val();
            console.log("Getting " + query);
            $.get("/search?q=" + query, function (data) {
                console.log("Got " + query);
                searchResults = data;
                updateResults();
            });
            resultsContainer.removeChild(loadingText);
        }
    });

    function handleResize() {
        if ($("#search_header").height() < 50) {
            $("#search_title").css("display", "none");
        } else {
            $("#search_title").css("display", "");
        }
    }

    $(window).resize(function () {
        handleResize()
    });
    handleResize();
})();