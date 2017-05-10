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
    $("#refreshButton").click(function () {
        location.reload();
    });

    $("#addCreatureButton").click(function () {
        addCreature();
    });

    $("#killAllCreaturesButton").click(function () {
        killAllCreatures();
    });

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


    createHiDPICanvas = function(w, h, ratio) {
        if (!ratio) { ratio = PIXEL_RATIO; }
        var can = document.getElementById('simulation-canvas');
        can.width = w * ratio;
        can.height = h * ratio;
        can.style.width = w + "px";
        can.style.height = h + "px";
        can.getContext("2d").setTransform(ratio, 0, 0, ratio, 0, 0);
        return can;
    };

    // var canvas = document.getElementById('simulation-canvas');
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

    exampleSocket.onopen = function (event) {
        console.log("Connected to server web socket.");
        $("#simulation-logs").prepend("Connected to server web socket.");
    };
    exampleSocket.onmessage = function (event) {
        var data = JSON.parse(event.data);
        switch(data.$type) {
            case "pl.mowczarek.love.backend.actors.Field.CreatureSpawned":
                console.log("Creature " + data.creature.id + " spawned.");
                $("#simulation-logs").prepend("Creature " + data.creature.id + " spawned.<br>");
                sex = "undefined";
                if(data.creature.sex != undefined){
                    sex = data.creature.sex.$type.toString().includes(".Male") ? "male" : "female";
                }
                creatures.push(new Creature(data.creature.id, sex, data.x, data.y, "spawned"));
                repaint();
                break;
            case "pl.mowczarek.love.backend.actors.Field.CreatureMature":
                console.log("Creature " + data.creature.id + " matured.");
                $("#simulation-logs").prepend("Creature " + data.creature.id + " matured.<br>");
                found = false;
                for(i=0; i<creatures.length;i++){
                    if(creatures[i].id == data.creature.id){
                        found = true;
                        creatures[i].state = "mature";
                        if(creatures[i].sex == "undefined"){
                            if(data.creature.sex != undefined){
                                creatures[i].sex = data.creature.sex.$type.toString().includes(".Male") ? "male" : "female";
                            }
                        }
                    }
                }
                if(found == false){
                    sex = "undefined";
                    if(data.creature.sex != undefined){
                        sex = data.creature.sex.$type.toString().includes(".Male") ? "male" : "female";
                    }
                    creatures.push(new Creature(data.creature.id, sex, data.x, data.y, "mature"));
                }
                repaint();
                break;
            case "pl.mowczarek.love.backend.actors.Field.CreatureEmigrated":
                console.log("Creature " + data.creature.id + " emigrated.");
                $("#simulation-logs").prepend("Creature " + data.creature.id + " emigrated.<br>");
                found = false;
                for(i=0; i<creatures.length;i++){
                    if(creatures[i].id == data.creature.id){
                        found = true;
                        creatures[i].state = "emigrated";
                        creatures[i].xCanvas = -1;
                        creatures[i].yCanvas = -1;
                        if(creatures[i].sex == "undefined"){
                            if(data.creature.sex != undefined){
                                creatures[i].sex = data.creature.sex.$type.toString().includes(".Male") ? "male" : "female";
                            }
                        }
                    }
                }
                if(found == false){
                    sex = "undefined";
                    if(data.creature.sex != undefined){
                        sex = data.creature.sex.$type.toString().includes(".Male") ? "male" : "female";
                    }
                    creatures.push(new Creature(data.creature.id, sex, data.x, data.y, "emigrated"));
                }
                repaint();
                break;
            case "pl.mowczarek.love.backend.actors.Field.CreatureImmigrated":
                console.log("Creature " + data.creature.id + " immigrated.");
                $("#simulation-logs").prepend("Creature " + data.creature.id + " immigrated.<br>");
                found = false;
                for(i=0; i<creatures.length;i++){
                    if(creatures[i].id == data.creature.id){
                        found = true;
                        creatures[i].state = "immigrated";
                        if(creatures[i].sex == "undefined"){
                            if(data.creature.sex != undefined){
                                creatures[i].sex = data.creature.sex.$type.toString().includes(".Male") ? "male" : "female";
                            }
                        }
                    }
                }
                if(found == false){
                    sex = "undefined";
                    if(data.creature.sex != undefined){
                        sex = data.creature.sex.$type.toString().includes(".Male") ? "male" : "female";
                    }
                    creatures.push(new Creature(data.creature.id, sex, data.x, data.y, "immigrated"));
                }
                repaint();
                break;
            case "pl.mowczarek.love.backend.actors.Field.CreatureDied":
                console.log("Creature " + data.creature.id + " died.");
                $("#simulation-logs").prepend("Creature " + data.creature.id + " died.<br>");
                found = false;
                for(i=0; i<creatures.length;i++){
                    if(creatures[i].id == data.creature.id){
                        found = true;
                        creatures[i].state = "dead";
                    }
                }
                if(found == false){
                    sex = "undefined";
                    if(data.creature.sex != undefined){
                        sex = data.creature.sex.$type.toString().includes(".Male") ? "male" : "female";
                    }
                    creatures.push(new Creature(data.creature.id, sex, data.x, data.y, "dead"));
                }
                repaint();
                break;
        }
    };
    exampleSocket.onclose = function (event) {
        console.log("Disconnected from server web socket.");
        $("#simulation-logs").prepend("Disconnected from server web socket.<br>");
    };

    function getRandomInt(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
    }

    function drawCreature(element, index, array) {
        if(element.xCanvas == -1){
            element.xCanvas = getRandomInt((canvas.width/2) * (element.x - 1), (canvas.width/2) * element.x - 30);
        }
        if(element.yCanvas == -1){
            element.yCanvas = getRandomInt((canvas.height/2) * (element.y - 1), (canvas.height/2) * element.y - 30);
        }
        if(element.state == "mature" || element.state == "immigrated") {
            if (element.sex == "male") {
                context.drawImage(maleImage, element.xCanvas, element.yCanvas, 30, 30)
            } else if(element.sex == "female") {
                context.drawImage(femaleImage, element.xCanvas, element.yCanvas, 20, 30)
            } else {
                context.fillStyle = 'black';
                context.fillText("?", element.xCanvas, element.yCanvas);
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

});
