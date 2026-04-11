$path = 'C:\Users\abc\Desktop\nginx-1.18.0\html\hmdp\blog-detail.html'
$encoding = New-Object System.Text.UTF8Encoding($false)
$html = [System.IO.File]::ReadAllText($path, $encoding)

$oldButton = '<div class="foot-view comment-action" @click.stop.prevent="publishComment" @touchend.stop.prevent="publishComment"><i class="el-icon-chat-square"></i></div>'
$newButton = '<div ref="commentButton" class="foot-view comment-action" @click.stop.prevent="publishComment" @touchend.stop.prevent="publishComment"><i class="el-icon-chat-square"></i></div>'
$html = $html.Replace($oldButton, $newButton)

$oldCreated = @'
    created() {
      let id = util.getUrlParam("id");
      this.queryBlogById(id);
    },
'@
$newCreated = @'
    created() {
      let id = util.getUrlParam("id");
      this.queryBlogById(id);
    },
    mounted() {
      this.bindCommentButton();
    },
'@
if (-not $html.Contains('mounted() {')) {
    $html = $html.Replace($oldCreated, $newCreated)
}

$oldInit = @'
      init() {
        // 鑾峰緱鐖跺鍣ㄨ妭鐐?
        this.container = this.$refs.swiper
        // 鑾峰緱鎵€鏈夌殑瀛愯妭鐐?
        this.items = this.container.querySelectorAll('.swiper-item')
        this.updateItemWidth()
        this.setTransform()
        this.setTransition('none')
      },
'@
$newInit = @'
      init() {
        // 鑾峰緱鐖跺鍣ㄨ妭鐐?
        this.container = this.$refs.swiper
        // 鑾峰緱鎵€鏈夌殑瀛愯妭鐐?
        this.items = this.container.querySelectorAll('.swiper-item')
        this.updateItemWidth()
        this.setTransform()
        this.setTransition('none')
        this.bindCommentButton()
      },
      bindCommentButton() {
        const button = this.$refs.commentButton;
        if (!button || button.dataset.bound === "1") {
          return;
        }
        const handler = (e) => {
          if (e) {
            e.preventDefault();
            e.stopPropagation();
          }
          this.publishComment();
        };
        button.addEventListener("click", handler, { passive: false });
        button.addEventListener("touchend", handler, { passive: false });
        button.dataset.bound = "1";
      },
'@
if (-not $html.Contains('bindCommentButton() {')) {
    $html = $html.Replace($oldInit, $newInit)
}

[System.IO.File]::WriteAllText($path, $html, $encoding)
