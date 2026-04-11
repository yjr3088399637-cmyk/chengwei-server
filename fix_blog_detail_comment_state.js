const fs = require('fs');

const htmlPath = 'C:\\Users\\abc\\Desktop\\nginx-1.18.0\\html\\hmdp\\blog-detail.html';
let html = fs.readFileSync(htmlPath, 'utf8');

if (!html.includes('comments: [],')) {
  html = html.replace(
    '      likes: [],\n',
    '      likes: [],\n      comments: [],\n      commentTotal: 0,\n'
  );
}

if (!html.includes('this.queryComments(id);')) {
  html = html.replace(
    '    created() {\n      let id = util.getUrlParam("id");\n      this.queryBlogById(id);\n    },',
    '    created() {\n      let id = util.getUrlParam("id");\n      this.queryBlogById(id);\n      this.queryComments(id);\n    },'
  );
}

fs.writeFileSync(htmlPath, html, 'utf8');
