const fs = require('fs');

const htmlPath = 'C:\\Users\\abc\\Desktop\\nginx-1.18.0\\html\\hmdp\\blog-detail.html';
let html = fs.readFileSync(htmlPath, 'utf8');

if (!html.includes('comments: []')) {
  html = html.replace(
    '      likes: [],',
    '      likes: [],\n      comments: [],\n      commentTotal: 0,'
  );
}

if (!html.includes('this.queryComments(id);')) {
  html = html.replace(
    '      this.queryBlogById(id);',
    '      this.queryBlogById(id);\n      this.queryComments(id);'
  );
}

fs.writeFileSync(htmlPath, html, 'utf8');
