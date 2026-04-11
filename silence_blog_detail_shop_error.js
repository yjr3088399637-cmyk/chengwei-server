const fs = require('fs');

const htmlPath = 'C:\\Users\\abc\\Desktop\\nginx-1.18.0\\html\\hmdp\\blog-detail.html';
let html = fs.readFileSync(htmlPath, 'utf8');

html = html.replace(
  /      queryShopById\(shopId\)\s*\{[\s\S]*?      \},\r?\n      queryLikeList/,
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
          .catch(() => {
            this.shop = {}
          })
      },
      queryLikeList`
);

fs.writeFileSync(htmlPath, html, 'utf8');
