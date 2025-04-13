class Bullet {
  constructor(x,y, mousex, mousey, img, bullettype) {
    this.pos = createVector(x, y);
    this.velocity = createVector(0, 0);
    this.size = 50;
    this.mousex = mousex;
    this.mousey = mousey;
    this.img = img;
    this.spriteSize = 64;
    this.lives = 1;
    // this.sprite stuff
    var a = 0;
    var b = 0;
    var v = 0;
    a = this.mousex - this.pos.x - 50/2;
    b = this.mousey - this.pos.y - 50/2;
    v = sqrt(a*a+b*b);
    this.velocity.x = a/v*10;
    this.velocity.y = b/v*10;
    this.bullettype = bullettype;
  }
  draw(offset, yOffset) {
    image(tiles_image, this.pos.x - offset, this.pos.y - yOffset, this.size, this.size, this.img[0] * this.spriteSize, this.img[1] * this.spriteSize, this.spriteSize, this.spriteSize);
  }

  updateGravity() {
    
    
    this.pos.add(this.velocity);

    if (this.bulletonSolid() == "In") {
      this.velocity.mult(0);
    }
    
  }

  update() {
  //   if (this.isAlive()) {
  //     this.updateInjured()
  //     this.processInput();
  //     // update position
     this.updateGravity();
       
  //     if (this.touchingEnemy()) {
  //       this.injured = true;
  //       if (this.injuryTimer == 0) {
  //         this.lives--;
  //       }
  //     }
  //     this.touchingItem();
  //   } else {
  //     //處理碰到牆的子彈
  //     textSize(50);
  //     fill(255);
  //     text(this.endingMessage, 250, 200);
  //   }
  }

  getBlockType(offX = 0, offY = 0) {
    var z = this.getLoc(this.pos.x + this.size/2 + offX, this.pos.y + this.size/2 + offY);
    // console.log(this.pos.x);

    return map1.blocks[z[1]][z[0]].constructor.name;
  }

  getLoc(x = this.pos.x + this.size/2, y = this.pos.y + this.size/2) {
    var location = [floor((x) / 50), floor(y / 50)];
    return location;
  }

  getDir(bullet_x = this.pos.x + this.size/2, bullet_y = this.pos.y + this.size/2) {
    var direction ;
    var solidsize = new Solid();
    var block_x = this.getLoc()[0] * 50 + solidsize.size/2;
    var block_y = this.getLoc()[1] * 50 + solidsize.size/2;
    console.log("getDir")

    //子彈從下方打入
    if((bullet_y - block_y) > 0 && Math.abs(bullet_y - block_y) > Math.abs(bullet_x - block_x)){
      // console.log(bullet_x);
      // console.log(bullet_y);
      // console.log(block_x);
      // console.log(block_y);
      // console.log("bulletx-blockx="+ (bullet_x-block_x));
      // console.log("bullety-blocky="+ (bullet_y-block_y));
      // console.log("down");
      return"bottom";
    }
    
    //子彈從上方打入
    if((bullet_y - block_y) < 0 && Math.abs(bullet_y - block_y) > Math.abs(bullet_x - block_x)){
      return"top";
    }

    //子彈從左方打入
    if((bullet_x - block_x) < 0 && Math.abs(bullet_y - block_y) < Math.abs(bullet_x - block_x)){
      return"left";
    }

    //子彈從左方打入
    if((bullet_x - block_x) > 0 && Math.abs(bullet_y - block_y) < Math.abs(bullet_x - block_x)){
      return"right";
    }
  }



  bulletonSolid() {
    
    // checking if the bullet in the "solid"
    if (this.getBlockType(0, 0) == "Solid") {
      var direction = this.getDir();
      this.pos.x = this.getLoc()[0] * 50;
      this.pos.y = this.getLoc()[1] * 50;
      
      //blue bullet generate bluwe portal
      if(this.bullettype === "blue") {
        for (var row = 0; row < map1.blocks.length; row++) {
          for (var col = 0; col < map1.blocks[row].length; col++) {
            if(map1.blocks[row][col].constructor.name == "PortalSolid" && map1.blocks[row][col].type == "blue") {
              map1.blocks[row][col] = new Solid(col * 50, row * 50, [0, 0]);
            }
          }
        }
        map1.blocks[this.getLoc()[1]][this.getLoc()[0]] = new PortalSolid(this.getLoc()[0] * 50, this.getLoc()[1] * 50, [2, 1], direction,"blue");
      }
      
      //red bullet generate red portal
      else if(this.bullettype === "red") {
        for (var row = 0; row < map1.blocks.length; row++) {
          for (var col = 0; col < map1.blocks[row].length; col++) {
            if(map1.blocks[row][col].constructor.name == "PortalSolid" && map1.blocks[row][col].type == "red") {
              map1.blocks[row][col] = new Solid(col * 50, row * 50, [0, 0]);
            }
          }
        }
        map1.blocks[this.getLoc()[1]][this.getLoc()[0]] = new PortalSolid(this.getLoc()[0] * 50, this.getLoc()[1] * 50, [3, 1], direction,"red");
      }

      return "In";
    }
    return false;
  }

  
}