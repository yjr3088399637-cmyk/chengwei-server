const fs = require('fs');

const htmlPath = 'C:\\Users\\abc\\Desktop\\nginx-1.18.0\\html\\hmdp\\blog-detail.html';
let html = fs.readFileSync(htmlPath, 'utf8');

html = html.replace(
  /      queryShopById\(shopId\) \{[\s\S]*?      \},\r?\n      queryLikeList/,
  `      queryShopById(shopId) {
        if (!shopId) {
          this.shop = {}
          return;
        }
        axios.get("/shop/" + shopId)
          .then(({data}) => {
            data.image = data.images.split(",")[0]
            this.shop = data
          })
          .catch(err => {
            this.shop = {}
            if (err) {
              this.$message.error(err)
            }
          })
      },
      queryLikeList`
);

html = html.replace(
  /      follow\(\)\{[\s\S]*?      \},\r?\n      formatTime/,
  `      follow(){
        axios.put("/follow/" + this.blog.userId + "/" + !this.followed)
          .then(() => {
            this.$message.success(this.followed ? "Unfollowed" : "Followed")
            this.followed = !this.followed
          })
          .catch(this.$message.error)
      },
      formatTime`
);

html = html.replace(
  /      formatTime\(b\) \{[\s\S]*?      \},\r?\n      formatCommentTime/,
  `      formatTime(b) {
        return b.getFullYear() + "-" + (b.getMonth() + 1) + "-" + b.getDate();
      },
      formatCommentTime`
);

html = html.replace(
  /<div class="shop-avg">[\s\S]*?<\/div>/,
  '<div class="shop-avg">￥{{shop.avgPrice}}/person</div>'
);

html = html.replace('      },      goBack() {', '      },\n      goBack() {');

fs.writeFileSync(htmlPath, html, 'utf8');
