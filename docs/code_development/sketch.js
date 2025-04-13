var level1 = [
  [4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 ,4, 4, 4, 2, 4, 4, 4, 2, 4, 4, 4, 4, 2, 4],
    [4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 3, 0, 0, 0, 0, 0, 0, 0, 4],
    [4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 8, 8, 0, 3, 0, 0, 0, 0, 8, 8, 8, 0, 4],
    [4, 0, 0, 0, 0, 4, 4, 4, 4, 4, 4, 2, 4, 4, 4, 4, 4, 4, 4, 0, 0, 0, 3, 0, 4, 4, 4, 4, 0, 4],
    [4, 0, 0, 4, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 3, 0, 0, 0, 3, 0, 0, 4, 0, 4],
    [4, 0, 0, 0, 0, 4, 6, 0, 0, 8, 0, 0, 8, 0, 0, 0, 0, 0, 4, 0, 0, 0, 3, 0, 3, 0, 0, 4, 0, 4],
    [4, 4, 0, 0, 0, 4, 4, 0, 4, 4, 4, 4, 4, 4, 4, 4, 0, 5, 8, 0, 0, 0, 4, 0, 0, 0, 0, 4, 7, 4],
    [4, 0, 0, 0, 0, 4, 4, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 4, 4, 0, 0, 0, 4, 0, 0, 0, 0, 4, 4, 4],
    [4, 0, 0, 4, 0, 4, 4, 4, 4, 4, 4, 4, 4, 0, 4, 0, 0, 0, 0, 0, 0, 8, 4, 0, 8, 0, 0, 2, 0, 4],
    [4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4, 4, 4, 4, 4, 0, 4],
    [4, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4],
    [4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 4, 8, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4],
    [4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4]
];

var pistol = 0;

function preload() {

  player_image = loadImage("images/player.png");
  player_injured_image = loadImage("images/player.png");
  tiles_image = loadImage("images/tiles.png");
}

function setup() {
  createCanvas(800, 600);
  player_injured_image.filter(THRESHOLD);
  player = new Player();
  crosshair = new Crosshair([8, 0]);
  map1 = new Map(level1);
  noCursor();
}

function draw() {
  background(0, 246, 255);
  /*
  push();
  let s = 3;
  scale(s);
  let camX =  width/2/s - player.pos.x-25;
  let camY =  height/2/s - player.pos.y-25;
  //let camX = -50;
  //let camY = -450;
  
  translate(camX, camY); // 鎖定攝影機，使其跟隨玩家
  pop();
  */
  map1.draw();
  crosshair.draw();
  
  player.draw();
  player.update();
  if(player.bullet != 0){
    player.bullet.draw(map1.offset, map1.yOffset);  
    if(player.bullet.update() == "undefined") {
      player.bullet = 0;
    }
  }
  
  
}

function keyPressed() {
  //console.log(key);
  player.processInput(key);
}

function mousePressed() {

  if(mouseButton === LEFT && pistol === 0) {
    player.processInput('blue pistol click');
  }
  else if(mouseButton === LEFT && pistol === 1) {
    player.processInput('red pistol click');
  }
}