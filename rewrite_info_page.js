const fs = require('fs');

const htmlPath = 'C:\\Users\\abc\\Desktop\\nginx-1.18.0\\html\\hmdp\\info.html';
const cssPath = 'C:\\Users\\abc\\Desktop\\nginx-1.18.0\\html\\hmdp\\css\\info.css';

const html = `<!DOCTYPE html>
<html lang="en">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=0">
  <title>黑马点评</title>
  <link rel="stylesheet" href="./css/element.css">
  <link href="./css/main.css" rel="stylesheet">
  <link href="./css/info.css" rel="stylesheet">

  <style type="text/css">
      .el-tabs--bottom .el-tabs__item.is-bottom:nth-child(2), .el-tabs--bottom .el-tabs__item.is-top:nth-child(2), .el-tabs--top .el-tabs__item.is-bottom:nth-child(2), .el-tabs--top .el-tabs__item.is-top:nth-child(2) {
          padding-left: 15px;
      }

      .el-tabs, .el-tab-pane {
          height: 100%;
      }

      .el-tabs__header {
          height: 10%;
      }

      .el-tabs__content {
          height: 90%;
      }
  </style>

</head>

<body>
<div id="app">
  <div class="header">
    <div class="header-back-btn" @click="goBack"><i class="el-icon-arrow-left"></i></div>
    <div class="header-title">个人主页&nbsp;&nbsp;&nbsp;</div>
  </div>
  <div class="basic">
    <div class="basic-icon">
      <img :src="user.icon || '/imgs/icons/default-icon.png'" alt="">
    </div>
    <div class="basic-info">
      <div class="name">{{user.nickName}}</div>
      <span>杭州</span>
      <div class="edit-btn" @click="toEdit">
        编辑资料
      </div>
    </div>
    <div class="logout-btn" @click="logout">
      退出登录
    </div>
  </div>
  <div class="introduce">
    <span v-if="info.introduce">{{info.introduce}}</span>
    <span v-else>添加个人简介，让大家更好的认识你 <i class="el-icon-edit"></i></span>
  </div>
  <div class="content">
    <el-tabs v-model="activeName" @tab-click="handleClick">
      <el-tab-pane label="笔记" name="1">
        <div v-for="b in blogs" :key="b.id" class="blog-item">
          <div class="blog-img"><img :src="b.images.split(',')[0]" alt=""></div>
          <div class="blog-info">
            <div class="blog-title">{{b.title}}</div>
            <div class="blog-liked"><img src="/imgs/thumbup.png" alt=""> {{b.liked}}</div>
            <div class="blog-comments"><i class="el-icon-chat-dot-round"></i> {{b.comments}}</div>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="评价" name="2">评价</el-tab-pane>
      <el-tab-pane :label="'粉丝(' + fans.length + ')'" name="3">
        <div v-if="fans.length">
          <div class="follow-info" v-for="u in fans" :key="u.id">
            <div class="follow-info-icon" @click="toOtherInfo(u.id)">
              <img :src="u.icon || '/imgs/icons/default-icon.png'" alt="">
            </div>
            <div class="follow-info-name">{{u.nickName}}</div>
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
            <div class="follow-info-name">{{u.nickName}}</div>
            <div class="follow-info-btn" @click="toOtherInfo(u.id)">
              查看主页
            </div>
          </div>
        </div>
        <div v-else class="empty-block">还没有关注任何人</div>
      </el-tab-pane>
    </el-tabs>
  </div>
  <foot-bar :active-btn="4"></foot-bar>
</div>
<script src="./js/vue.js"></script>
<script src="./js/axios.min.js"></script>
<script src="./js/element.js"></script>
<script src="./js/common.js"></script>
<script src="./js/footer.js"></script>
<script>
  const app = new Vue({
    el: "#app",
    data: {
      user: "",
      activeName: "1",
      info: {},
      blogs: [],
      fans: [],
      follows: [],
    },
    created() {
      this.queryUser();
    },
    methods: {
      queryBlogs() {
        axios.get("/blog/of/me")
          .then(({data}) => this.blogs = data)
          .catch(this.$message.error)
      },
      queryUser() {
        axios.get("/user/me")
          .then(({data}) => {
            this.user = data;
            this.queryUserInfo();
            this.queryBlogs();
            this.queryFans();
            this.queryFollows();
          })
          .catch(() => {
            location.href = "login.html"
          })
      },
      goBack() {
        history.back();
      },
      queryUserInfo() {
        axios.get("/user/info/" + this.user.id)
          .then(({data}) => {
            if (!data) {
              return
            }
            this.info = data;
            sessionStorage.setItem("userInfo", JSON.stringify(data))
          })
          .catch(this.$message.error)
      },
      toEdit() {
        location.href = "info-edit.html"
      },
      toOtherInfo(id) {
        location.href = "/other-info.html?id=" + id
      },
      logout() {
        axios.post("/user/logout")
          .then(() => {
            sessionStorage.removeItem("token")
            location.href = "/"
          })
          .catch(this.$message.error)
      },
      handleClick(tab) {
        if (tab.name === "3") {
          this.queryFans();
        }
        if (tab.name === "4") {
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
      }
    },

  })
</script>
</body>

</html>
`;

let css = fs.readFileSync(cssPath, 'utf8');
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
