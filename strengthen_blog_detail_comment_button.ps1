$path = 'C:\Users\abc\Desktop\nginx-1.18.0\html\hmdp\blog-detail.html'
$cssPath = 'C:\Users\abc\Desktop\nginx-1.18.0\html\hmdp\css\blog-detail.css'
$encoding = New-Object System.Text.UTF8Encoding($false)

$html = [System.IO.File]::ReadAllText($path, $encoding)
$css = [System.IO.File]::ReadAllText($cssPath, $encoding)

$oldButton = '<div class="foot-view" @click="publishComment"><i class="el-icon-chat-square"></i></div>'
$newButton = '<div class="foot-view comment-action" @click.stop.prevent="publishComment" @touchend.stop.prevent="publishComment"><i class="el-icon-chat-square"></i></div>'

if (-not $html.Contains($oldButton)) {
    throw 'comment button block not found'
}

$html = $html.Replace($oldButton, $newButton)

$oldData = "      resistance: 0.3`r`n    },"
$newData = "      resistance: 0.3,`r`n      commentOpening: false`r`n    },"

if (-not $html.Contains('commentOpening: false')) {
    if (-not $html.Contains($oldData)) {
        throw 'data block not found'
    }
    $html = $html.Replace($oldData, $newData)
}

$oldMethod = @'
      publishComment(){
        if (!this.blog.id) {
          return;
        }
        this.$prompt("Please enter your comment", "Post Comment", {
          confirmButtonText: "Post",
          cancelButtonText: "Cancel",
          inputType: "textarea",
          inputPlaceholder: "Say something...",
          inputPattern: /\S+/,
          inputErrorMessage: "Comment cannot be empty"
        }).then(({ value }) => {
          const content = value ? value.trim() : "";
          if (!content) {
            this.$message.error("Comment cannot be empty");
            return;
          }
          axios.post("/blog-comments", {
            blogId: this.blog.id,
            content: content,
            parentId: 0,
            answerId: 0
          }).then(() => {
            this.$message.success("Comment posted");
            this.blog.comments = (this.blog.comments || 0) + 1;
          }).catch(this.$message.error)
        }).catch(() => {});
      },
'@

$newMethod = @'
      publishComment(){
        if (!this.blog.id || this.commentOpening) {
          return;
        }
        this.commentOpening = true;
        this.$prompt("Please enter your comment", "Post Comment", {
          confirmButtonText: "Post",
          cancelButtonText: "Cancel",
          inputType: "textarea",
          inputPlaceholder: "Say something...",
          inputPattern: /\S+/,
          inputErrorMessage: "Comment cannot be empty"
        }).then(({ value }) => {
          const content = value ? value.trim() : "";
          if (!content) {
            this.$message.error("Comment cannot be empty");
            return;
          }
          return axios.post("/blog-comments", {
            blogId: this.blog.id,
            content: content,
            parentId: 0,
            answerId: 0
          }).then(() => {
            this.$message.success("Comment posted");
            this.blog.comments = (this.blog.comments || 0) + 1;
          }).catch(this.$message.error)
        }).catch(() => {
        }).then(() => {
          this.commentOpening = false;
        })
      },
'@

if (-not $html.Contains('if (!this.blog.id || this.commentOpening)')) {
    if (-not $html.Contains($oldMethod)) {
        throw 'publishComment method block not found'
    }
    $html = $html.Replace($oldMethod, $newMethod)
}

if (-not $css.Contains('.comment-action{')) {
    $css += @'

.foot-box{
    position: relative;
    z-index: 601;
}
.foot-view{
    position: relative;
    z-index: 601;
    min-height: 32px;
    line-height: 32px;
    pointer-events: auto;
}
.comment-action{
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 40px;
    cursor: pointer;
    -webkit-tap-highlight-color: transparent;
    touch-action: manipulation;
}
'@
}

[System.IO.File]::WriteAllText($path, $html, $encoding)
[System.IO.File]::WriteAllText($cssPath, $css, $encoding)
