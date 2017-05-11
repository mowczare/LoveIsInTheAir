/**
 * Created by mw on 04/05/17.
 * 
 * Frontend js
 */
var creatures = [];

var Creature = class {
    constructor(id, sex, x, y, state) {
        this.id = id;
        this.sex = sex;
        this.x = x;
        this.y = y;
        this.state = state;
        this.xCanvas = -1;
        this.yCanvas = -1;
    }
};

$( document ).ready(function() {

    //CANVAS HANDLERS
    //--------------------------------------------------------
    var PIXEL_RATIO = (function () {
        var ctx = document.getElementById('simulation-canvas').getContext("2d"),
            dpr = window.devicePixelRatio || 1,
            bsr = ctx.webkitBackingStorePixelRatio ||
                ctx.mozBackingStorePixelRatio ||
                ctx.msBackingStorePixelRatio ||
                ctx.oBackingStorePixelRatio ||
                ctx.backingStorePixelRatio || 1;

        return dpr / bsr;
    })();


    var createHiDPICanvas = function(w, h, ratio) {
        if (!ratio) { ratio = PIXEL_RATIO; }
        var can = document.getElementById('simulation-canvas');
        can.width = w * ratio;
        can.height = h * ratio;
        can.style.width = w + "px";
        can.style.height = h + "px";
        can.getContext("2d").setTransform(ratio, 0, 0, ratio, 0, 0);
        return can;
    };

    function getRandomInt(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
    }

    function drawCreature(element, index, array) {
        if(element.xCanvas == -1){
            element.xCanvas = getRandomInt((canvas.width/sizeX) * (element.x - 1), (canvas.width/sizeX) * element.x - 30);
        }
        if(element.yCanvas == -1){
            element.yCanvas = getRandomInt((canvas.height/sizeY) * (element.y - 1), (canvas.height/sizeY) * element.y - 30);
        }
        if(element.state == "mature" || element.state == "immigrated") {
            if (element.sex == "male") {
                context.drawImage(maleImage, element.xCanvas, element.yCanvas, 30, 30)
            } else {
                context.drawImage(femaleImage, element.xCanvas, element.yCanvas, 20, 30)
            }
        } else if(element.state == "spawned"){
            context.drawImage(babyImage, element.xCanvas, element.yCanvas, 30, 30)
        } else if(element.state == "dead"){
            context.drawImage(ripImage, element.xCanvas, element.yCanvas, 30, 30)
        }
    }

    var repaint = function() {
        context.clearRect(0, 0, canvas.width, canvas.height);
        creatures.forEach(drawCreature);
    };

    var removeDeadCreatures = function(){
        for(var i=0; i<creatures.length;i++){
            if(creatures[i].state == "dead"){
                creatures[i].state = "finalized";
            }
        }
        repaint();
    };

    //GLOBAL VARIABLES
    //--------------------------------------------------------
    var canvas =  createHiDPICanvas(1000, 500);
    var context = canvas.getContext('2d');
    context.font="20px Verdana";

    var maleImage = new Image();
    maleImage.src = "./static/img/male.png";
    var femaleImage = new Image();
    femaleImage.src = "./static/img/female.png";
    var babyImage = new Image();
    babyImage.src = "./static/img/baby.png";
    var ripImage = new Image();
    ripImage.src = "./static/img/rip.png";

    var exampleSocket = new WebSocket("ws://localhost:9000/ws");

    var sizeX, sizeY;

    var countChart;

    var livingCreatures = ["Living creatures"];
    var livingCreaturesCount = 0;

    //BUTTON HANDLERS
    //--------------------------------------------------------
    $("#refreshButton").click(function () {
        location.reload();
    });

    $("#addCreatureButton").click(function () {
        addCreature();
    });

    $("#killAllCreaturesButton").click(function () {
        killAllCreatures();
    });

    //REST HANDLERS
    //--------------------------------------------------------
    var addCreature = function() {
        $.ajax({
            type: "POST",
            url: "http://localhost:9000/creature"
        });
    };

    var killAllCreatures = function() {
        $.ajax({
            type: "DELETE",
            url: "http://localhost:9000/creature"
        });
    };

    var getCurrentSimulationConfig = function() {
        $.ajax({
            type: "GET",
            url: "http://localhost:9000/creature",
            success: function(data){
                console.log("Got current simulation config.");
                $("#simulation-logs").prepend("Got current simulation config.<br>");
                data = JSON.parse(data);
                sizeX = data.sizeX;
                sizeY = data.sizeY;
                for(var i = 0; i<data.states.length; i++){
                    for(var j = 0; j<data.states[i].creatures.length; j++){
                        var coordinates = data.states[i].coordinates;
                        var creature = data.states[i].creatures[j];
                        if(creature.sex != undefined){
                            var sex = creature.sex.$type.toString().includes(".Male") ? "male" : "female";
                        }
                        if(creature.state != "dead"){
                            creatures.push(new Creature(creature.id, sex, coordinates.x, coordinates.y, creature.state));
                            livingCreaturesCount++;
                        } else {
                            creatures.push(new Creature(creature.id, sex, coordinates.x, coordinates.y, "finalized"));
                        }

                    }
                }
                livingCreatures.push(livingCreaturesCount);
                generateCountChart();
                repaint();
            }
        });
    };

    //WEBSOCKET HANDLERS
    //--------------------------------------------------------

    exampleSocket.onopen = function (event) {
        console.log("Connected to server web socket.");
        $("#simulation-logs").prepend("Connected to server web socket.");
        getCurrentSimulationConfig();
    };

    exampleSocket.onmessage = function (event) {
        var data = JSON.parse(event.data);
        switch(data.$type) {
            case "pl.mowczarek.love.backend.actors.Field.CreatureSpawned":
                console.log("Creature " + data.creature.id + " spawned.");
                $("#simulation-logs").prepend("Creature " + data.creature.id + " spawned.<br>");
                var sex = data.creature.sex.$type.toString().includes(".Male") ? "male" : "female";
                creatures.push(new Creature(data.creature.id, sex, data.x, data.y, data.creature.state));
                livingCreaturesCount++;
                repaint();
                break;
            case "pl.mowczarek.love.backend.actors.Field.CreatureMature":
                console.log("Creature " + data.creature.id + " matured.");
                $("#simulation-logs").prepend("Creature " + data.creature.id + " matured.<br>");
                var found = false;
                for(var i=0; i<creatures.length;i++){
                    if(creatures[i].id == data.creature.id){
                        found = true;
                        creatures[i].state = data.creature.state;
                    }
                }
                if(found == false){
                    sex = data.creature.sex.$type.toString().includes(".Male") ? "male" : "female";
                    creatures.push(new Creature(data.creature.id, sex, data.x, data.y, data.creature.state));
                }
                repaint();
                break;
            case "pl.mowczarek.love.backend.actors.Field.CreatureEmigrated":
                console.log("Creature " + data.creature.id + " emigrated.");
                $("#simulation-logs").prepend("Creature " + data.creature.id + " emigrated.<br>");
                found = false;
                for(i=0; i<creatures.length;i++){
                    if(creatures[i].id == data.creature.id){
                        creatures[i].state = data.creature.state;
                        creatures[i].xCanvas = -1;
                        creatures[i].yCanvas = -1;
                        found = true;
                    }
                }
                if(found == false){
                    sex = data.creature.sex.$type.toString().includes(".Male") ? "male" : "female";
                    creatures.push(new Creature(data.creature.id, sex, data.x, data.y, data.creature.state));
                }
                repaint();
                break;
            case "pl.mowczarek.love.backend.actors.Field.CreatureImmigrated":
                console.log("Creature " + data.creature.id + " immigrated.");
                $("#simulation-logs").prepend("Creature " + data.creature.id + " immigrated.<br>");
                found = false;
                for(i=0; i<creatures.length;i++){
                    if(creatures[i].id == data.creature.id){
                        creatures[i].state = data.creature.state;
                        found = true;
                    }
                }
                if(found == false){
                    sex = data.creature.sex.$type.toString().includes(".Male") ? "male" : "female";
                    creatures.push(new Creature(data.creature.id, sex, data.x, data.y, data.creature.state));
                }
                repaint();
                break;
            case "pl.mowczarek.love.backend.actors.Field.CreatureDied":
                console.log("Creature " + data.creature.id + " died.");
                $("#simulation-logs").prepend("Creature " + data.creature.id + " died.<br>");
                found = false;
                for(i=0; i<creatures.length;i++){
                    if(creatures[i].id == data.creature.id){
                        creatures[i].state = data.creature.state;
                        found = true;
                    }
                }
                if(found == false){
                    sex = data.creature.sex.$type.toString().includes(".Male") ? "male" : "female";
                    creatures.push(new Creature(data.creature.id, sex, data.x, data.y, data.creature.state));
                }
                livingCreaturesCount--;
                repaint();
                setTimeout(removeDeadCreatures, 3000);
                break;
        }
    };

    exampleSocket.onclose = function (event) {
        console.log("Disconnected from server web socket.");
        $("#simulation-logs").prepend("Disconnected from server web socket.<br>");
    };

    //CHART HANDLERS
    //---------------------------------------------------------

    var generateCountChart = function() {
        countChart = c3.generate({
            bindto: '#chart',
            data: {
                columns: [
                    livingCreatures
                ]
            }
        });
        setInterval(redrawCountChart, 1000);
    };

    var redrawCountChart = function() {
        livingCreatures.push(livingCreaturesCount);
        countChart.load({
            columns: [
                livingCreatures
            ]
        });
    };
});
