$path = 'C:\Users\abc\Desktop\nginx-1.18.0\html\hmdp\blog-detail.html'
$encoding = New-Object System.Text.UTF8Encoding($false)
$html = [System.IO.File]::ReadAllText($path, $encoding)

if (-not $html.Contains('mounted() {')) {
    $marker = "    methods: {"
    $insert = "    mounted() {`r`n      this.bindCommentButton();`r`n    },`r`n"
    $index = $html.IndexOf($marker)
    if ($index -lt 0) { throw 'methods marker not found' }
    $html = $html.Insert($index, $insert)
}

if (-not $html.Contains('this.bindCommentButton()')) {
    $html = $html.Replace("        this.setTransition('none')", "        this.setTransition('none')`r`n        this.bindCommentButton()")
}

if (-not $html.Contains('bindCommentButton() {')) {
    $marker = "      goBack() {"
    $insert = @'
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
    $index = $html.IndexOf($marker)
    if ($index -lt 0) { throw 'goBack marker not found' }
    $html = $html.Insert($index, $insert)
}

[System.IO.File]::WriteAllText($path, $html, $encoding)
