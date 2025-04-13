class Map {
  constructor(b) {

    this.blocks = b;
    this.offset = -150
    this.enemyList = [];
    this.itemList = [];



    for (var row = 0; row < this.blocks.length; row++) {
      for (var col = 0; col < this.blocks[row].length; col++) {
        //portal wall
        if (this.blocks[row][col] == 1) {
          this.blocks[row][col] = new Solid(col * 50, row * 50, [0, 0]);

        }
        //standard wall
        if (this.blocks[row][col] == 2) {
          this.blocks[row][col] = new Solid(col * 50, row * 50, [1, 0]);

        }
        //mirror wall
        if (this.blocks[row][col] == 3) {
          this.blocks[row][col] = new Decor(col * 50, row * 50, [2, 0]);

        }
        if (this.blocks[row][col] == 4) {
          this.blocks[row][col] = new Decor(col * 50, row * 50, [2, 5]);

        }
        if (this.blocks[row][col] == 5) {
          this.blocks[row][col] = new Item(col * 50, row * 50, [11, 4], "heart");
          this.itemList.push(this.blocks[row][col])
        }
        if (this.blocks[row][col] == 6) {
          this.blocks[row][col] = new Item(col * 50, row * 50, [1, 2], "key");
          this.itemList.push(this.blocks[row][col])
        }
        if (this.blocks[row][col] == 7) {
          this.blocks[row][col] = new Item(col * 50, row * 50, [0, 1], "door");
          this.itemList.push(this.blocks[row][col])
        }
        if (this.blocks[row][col] == 8) {
          this.blocks[row][col] = new Enemy(col * 50, row * 50, [0, 5], false);
        this.enemyList.push(this.blocks[row][col])
        }
        if (this.blocks[row][col] == 9) {
          this.blocks[row][col] = new Enemy(col * 50, row * 50, [1, 5], true);
        this.enemyList.push(this.blocks[row][col])
        }
        //portal blue
        if (this.blocks[row][col] == 10) {
          this.blocks[row][col] = new PortalSolid(col * 50, row * 50 [2, 1], "","blue");
        }
        //portal red
        if (this.blocks[row][col] == 11) {
          this.blocks[row][col] = new PortalSolid(col * 50, row * 50, [3, 1], "", "red");
        }
      }
    
    }
  }



  draw() {
    for (var row = 0; row < this.blocks.length; row++) {
      for (var col = 0; col < this.blocks[row].length; col++) {
        if (this.blocks[row][col] != 0) {
          this.blocks[row][col].draw(this.offset, 0);
        }
      }

    }

  }
}