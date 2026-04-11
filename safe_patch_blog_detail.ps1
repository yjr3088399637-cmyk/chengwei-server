$path = 'C:\Users\abc\Desktop\nginx-1.18.0\html\hmdp\blog-detail.html'
$encoding = New-Object System.Text.UTF8Encoding($false)
$content = [System.IO.File]::ReadAllText($path, $encoding)

$oldIcon = '<div class="foot-view"><i class="el-icon-chat-square"></i></div>'
$newIcon = '<div class="foot-view" @click="publishComment"><i class="el-icon-chat-square"></i></div>'

$oldMethod = @'
      isFollowed(){
'@

$newMethod = @'
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
      isFollowed(){
'@

if (-not $content.Contains($oldIcon)) {
    throw 'Comment icon block not found'
}

if ($content.Contains('publishComment(){')) {
    throw 'publishComment already exists'
}

if (-not $content.Contains($oldMethod)) {
    throw 'Method insertion point not found'
}

$content = $content.Replace($oldIcon, $newIcon)
$content = $content.Replace($oldMethod, $newMethod)

[System.IO.File]::WriteAllText($path, $content, $encoding)
