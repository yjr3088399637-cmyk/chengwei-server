$path = 'C:\Users\abc\Desktop\nginx-1.18.0\html\hmdp\blog-detail.html'
$encoding = New-Object System.Text.UTF8Encoding($false)
$html = [System.IO.File]::ReadAllText($path, $encoding)

$html = $html.Replace(
    '<div class="foot-view comment-action" @click.stop.prevent="publishComment" @touchend.stop.prevent="publishComment"><i class="el-icon-chat-square"></i></div>',
    '<div ref="commentButton" class="foot-view comment-action" @click.stop.prevent="publishComment" @touchend.stop.prevent="publishComment"><i class="el-icon-chat-square"></i></div>'
)

if (-not $html.Contains('mounted() {')) {
    $html = [regex]::Replace(
        $html,
        "created\(\)\s*\{\s*let id = util\.getUrlParam\(""id""\);\s*this\.queryBlogById\(id\);\s*\},",
        "created() {`r`n      let id = util.getUrlParam(""id"");`r`n      this.queryBlogById(id);`r`n    },`r`n    mounted() {`r`n      this.bindCommentButton();`r`n    },"
    )
}

if (-not $html.Contains('bindCommentButton() {')) {
    $html = [regex]::Replace(
        $html,
        "init\(\)\s*\{\s*//.*?this\.setTransition\('none'\)\s*\},",
        "init() {`r`n        // 鑾峰緱鐖跺鍣ㄨ妭鐐?`r`n        this.container = this.`$refs.swiper`r`n        // 鑾峰緱鎵€鏈夌殑瀛愯妭鐐?`r`n        this.items = this.container.querySelectorAll('.swiper-item')`r`n        this.updateItemWidth()`r`n        this.setTransform()`r`n        this.setTransition('none')`r`n        this.bindCommentButton()`r`n      },`r`n      bindCommentButton() {`r`n        const button = this.`$refs.commentButton;`r`n        if (!button || button.dataset.bound === ""1"") {`r`n          return;`r`n        }`r`n        const handler = (e) => {`r`n          if (e) {`r`n            e.preventDefault();`r`n            e.stopPropagation();`r`n          }`r`n          this.publishComment();`r`n        };`r`n        button.addEventListener(""click"", handler, { passive: false });`r`n        button.addEventListener(""touchend"", handler, { passive: false });`r`n        button.dataset.bound = ""1"";`r`n      },",
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )
}

[System.IO.File]::WriteAllText($path, $html, $encoding)
