$path = 'C:\Users\abc\Desktop\nginx-1.18.0\html\hmdp\blog-detail.html'
$encoding = New-Object System.Text.UTF8Encoding($false)
$content = [System.IO.File]::ReadAllText($path, $encoding)

$pattern = '<div class="shop-avg">.*?</div>'
$replacement = '<div class="shop-avg">￥{{shop.avgPrice}}/人</div>'

$updated = [System.Text.RegularExpressions.Regex]::Replace(
    $content,
    $pattern,
    $replacement,
    [System.Text.RegularExpressions.RegexOptions]::Singleline
)

if ($updated -eq $content) {
    throw 'shop-avg block not found'
}

[System.IO.File]::WriteAllText($path, $updated, $encoding)
