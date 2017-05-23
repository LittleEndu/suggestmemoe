/**
 * Created by Endrik on 23-May-17.
 */
"use strict";
(function () {
    var backgrounds = ['705197.png', '796554.jpg', '800259.jpg'];
    var rand = backgrounds[Math.floor(Math.random() * backgrounds.length)];
    $("body").css('background-image', 'url(/' + rand + ')')
    $("#search_box").on("keydown", function (key) {
        if (key.keyCode == 13) {
            var query = $("#search_box").val()
            console.log("Getting " + query);
            $.get("/search?q=" + query, function (data) {
                console.log("Got " + query)
                for (var i = 0; i < data.length; i++) {
                    console.log(data[i].name)
                }
            })

        }
    })
})();