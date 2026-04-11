$htmlPath = 'C:\Users\abc\Desktop\nginx-1.18.0\html\hmdp\blog-detail.html'
$cssPath = 'C:\Users\abc\Desktop\nginx-1.18.0\html\hmdp\css\blog-detail.css'
$encoding = New-Object System.Text.UTF8Encoding($false)

$html = [System.IO.File]::ReadAllText($htmlPath, $encoding)
$css = [System.IO.File]::ReadAllText($cssPath, $encoding)

$oldScroll = '<div style="height: 85%; overflow-y: scroll; overflow-x: hidden">'
$newScroll = '<div class="detail-scroll" style="height: 85%; overflow-y: scroll; overflow-x: hidden">'

if (-not $html.Contains($oldScroll)) {
    throw 'detail scroll container not found'
}

if (-not $html.Contains('class="detail-scroll"')) {
    $html = $html.Replace($oldScroll, $newScroll)
}

if (-not $css.Contains('.detail-scroll{')) {
    $css += @'

.detail-scroll{
    box-sizing: border-box;
    padding-bottom: 70px;
}
.foot{
    position: fixed;
    left: 0;
    bottom: 0;
    z-index: 600;
    background-color: #fff;
}
'@
}

[System.IO.File]::WriteAllText($htmlPath, $html, $encoding)
[System.IO.File]::WriteAllText($cssPath, $css, $encoding)
