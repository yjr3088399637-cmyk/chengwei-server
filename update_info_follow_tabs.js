const fs = require('fs');

const htmlPath = 'C:\\Users\\abc\\Desktop\\nginx-1.18.0\\html\\hmdp\\info.html';
const cssPath = 'C:\\Users\\abc\\Desktop\\nginx-1.18.0\\html\\hmdp\\css\\info.css';

let html = fs.readFileSync(htmlPath, 'utf8');
let css = fs.readFileSync(cssPath, 'utf8');

html = html.replace(
  /<el-tab-pane label="[^"]*" name="3">[\s\S]*?<\/el-tab-pane>\s*<el-tab-pane label="[^"]*" name="4">[\s\S]*?<\/el-tab-pane>/,
  `<el-tab-pane :label="'粉丝(' + fans.length + ')'" name="3">
        <div v-if="fans.length">
          <div class="follow-info" v-for="u in fans" :key="u.id">
            <div class="follow-info-icon" @click="toOtherInfo(u.id)">
              <img :src="u.icon || '/imgs/icons/default-icon.png'" alt="">
            </div>
            <div class="follow-info-name">
              {{u.nickName}}
            </div>
            <div class="follow-info-btn" @click="toOtherInfo(u.id)">
              查看主页
            </div>
          </div>
        </div>
        <div v-else class="empty-block">还没有粉丝</div>
      </el-tab-pane>
      <el-tab-pane :label="'关注(' + follows.length + ')'" name="4">
        <div v-if="follows.length">
          <div class="follow-info" v-for="u in follows" :key="u.id">
            <div class="follow-info-icon" @click="toOtherInfo(u.id)">
              <img :src="u.icon || '/imgs/icons/default-icon.png'" alt="">
            </div>
            <div class="follow-info-name">
              {{u.nickName}}
            </div>
            <div class="follow-info-btn" @click="toOtherInfo(u.id)">
              查看主页
            </div>
          </div>
        </div>
        <div v-else class="empty-block">还没有关注任何人</div>
      </el-tab-pane>`
);

html = html.replace(
  /      blogs2: \[\],[\s\S]*?      isReachBottom: false,/,
  `      fans: [],
      follows: [],`
);

html = html.replace(
  /      load\(\) \{[\s\S]*?      queryUser\(\) \{/,
  `      queryUser() {`
);

html = html.replace(
  '            this.queryBlogs();',
  `            this.queryBlogs();
            this.queryFans();
            this.queryFollows();`
);

html = html.replace(
  `      toEdit() {
        location.href = 'info-edit.html'
      },`,
  `      toEdit() {
        location.href = 'info-edit.html'
      },
      toOtherInfo(id) {
        location.href = "/other-info.html?id=" + id
      },`
);

html = html.replace(
  /      handleClick\(r\) \{[\s\S]*?      addLike\(b\) \{/,
  `      handleClick(r) {
        if (r.name === '3') {
          this.queryFans();
        }
        if (r.name === '4') {
          this.queryFollows();
        }
      },
      queryFans() {
        axios.get("/follow/me/fans")
          .then(({data}) => {
            this.fans = data || [];
          })
          .catch(this.$message.error)
      },
      queryFollows() {
        axios.get("/follow/me/follows")
          .then(({data}) => {
            this.follows = data || [];
          })
          .catch(this.$message.error)
      },
      addLike(b) {`
);

html = html.replace(
  /      onScroll\(e\) \{[\s\S]*?      \}\s*    \},/,
  `    },`
);

if (!css.includes('.empty-block')) {
  css += `

.empty-block{
    padding: 30px 0;
    text-align: center;
    color: #999;
}
`;
}

fs.writeFileSync(htmlPath, html, 'utf8');
fs.writeFileSync(cssPath, css, 'utf8');
