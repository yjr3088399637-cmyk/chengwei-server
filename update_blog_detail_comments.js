const fs = require('fs');

const htmlPath = 'C:\\Users\\abc\\Desktop\\nginx-1.18.0\\html\\hmdp\\blog-detail.html';
const cssPath = 'C:\\Users\\abc\\Desktop\\nginx-1.18.0\\html\\hmdp\\css\\blog-detail.css';

let html = fs.readFileSync(htmlPath, 'utf8');
let css = fs.readFileSync(cssPath, 'utf8');

const newCommentsBlock = `    <div class="blog-comments">
      <div class="comments-head">
        <div>Comments <span>({{commentTotal}})</span></div>
      </div>
      <div class="comment-list" v-if="comments.length">
        <div class="comment-box" v-for="comment in comments" :key="comment.id">
          <div class="comment-icon">
            <img :src="comment.icon || '/imgs/icons/default-icon.png'" alt="">
          </div>
          <div class="comment-info">
            <div class="comment-user">{{comment.name || ('User ' + comment.userId)}}</div>
            <div class="comment-time">{{formatCommentTime(comment.createTime)}}</div>
            <div class="comment-content">{{comment.content}}</div>
            <div class="comment-meta">Likes {{comment.liked || 0}}</div>
          </div>
        </div>
      </div>
      <div v-else class="comment-empty">No comments yet</div>
    </div>`;

html = html.replace(
  / {4}<div class="blog-comments">[\s\S]*? {4}<\/div>\r?\n {4}<div class="blog-divider"><\/div>/,
  `${newCommentsBlock}\n    <div class="blog-divider"></div>`
);

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

if (!html.includes('queryComments(id){')) {
  html = html.replace(
    /      queryLikeList\(id\)\{[\s\S]*?\.catch\(this\.\$message\.error\)\r?\n      \},/,
    `      queryLikeList(id){
        axios.get("/blog/likes/" + id)
          .then(({data}) => this.likes = data)
          .catch(this.$message.error)
      },
      queryComments(id){
        if (!id) {
          this.comments = [];
          this.commentTotal = 0;
          return;
        }
        axios.get("/blog-comments/of/blog/" + id, {
          params: {
            current: 1
          }
        }).then(({data, total}) => {
          this.comments = data || [];
          this.commentTotal = total || 0;
        }).catch(err => {
          this.comments = [];
          this.commentTotal = 0;
          this.$message.error(err);
        })
      },`
  );
}

if (!html.includes('formatCommentTime(value)')) {
  html = html.replace(
    /      formatTime\(b\) \{[\s\S]*?      \},\r?\n      formatMinutes/,
    `      formatTime(b) {
        return b.getFullYear() + "骞? + (b.getMonth() + 1) + "鏈? + b.getDate() + "鏃?";
      },
      formatCommentTime(value) {
        if (!value) {
          return "";
        }
        const text = String(value).replace("T", " ");
        return text.length > 19 ? text.slice(0, 19) : text;
      },
      formatMinutes`
  );
}

html = html.replace(
  `          }).then(() => {
            this.$message.success("Comment posted");
            this.blog.comments = (this.blog.comments || 0) + 1;
          }).catch(this.$message.error)`,
  `          }).then(() => {
            this.$message.success("Comment posted");
            this.blog.comments = (this.blog.comments || 0) + 1;
            return this.queryComments(this.blog.id);
          }).then(() => {
            this.blog.comments = this.commentTotal;
          }).catch(this.$message.error)`
);

if (!css.includes('.comment-time{')) {
  css += `
.comment-time{
    color: #999;
    font-size: 12px;
    margin-top: 4px;
}
.comment-content{
    padding: 8px 0;
    font-size: 14px;
    line-height: 1.5;
    word-break: break-word;
}
.comment-meta{
    color: #999;
    font-size: 12px;
}
.comment-empty{
    padding: 18px 0;
    text-align: center;
    color: #999;
}`;
}

fs.writeFileSync(htmlPath, html, 'utf8');
fs.writeFileSync(cssPath, css, 'utf8');
