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
    $("body").css('background-image', 'url(/' + rand + ')');
    function updateResults() {
        while (resultsContainer.children.length != 0) {
            resultsContainer.removeChild(resultsContainer.children[0]);
        }
        for (var index in searchResults) {
            var anime = createAnime(searchResults[index]);
            resultsContainer.appendChild(anime);
        }
        $(".animebutton").on("click", function (t) {
            animeContainer.appendChild(t.currentTarget.parentNode);
        })
    }

    function createAnime(data) {
        var animeDiv = document.createElement("div");
        animeDiv.setAttribute("class", "anime");
        var animeP = document.createElement("p");
        animeP.setAttribute("class", "animetext");
        animeP.appendChild(document.createTextNode(data.name));
        var animeButton = document.createElement("button");
        animeButton.setAttribute("type", "button");
        animeButton.setAttribute("class", "animebutton");
        animeButton.appendChild(document.createTextNode("Save"));

        animeDiv.appendChild(animeP);
        animeDiv.appendChild(animeButton);

        return animeDiv;
    }

    $("#search_box").on("keydown", function (key) {
        if (key.keyCode == 13) {
            var query = $("#search_box").val();
            console.log("Getting " + query);
            $.get("/search?q=" + query, function (data) {
                console.log("Got " + query);
                searchResults = data;
                updateResults();
            })
        }
    });
})();