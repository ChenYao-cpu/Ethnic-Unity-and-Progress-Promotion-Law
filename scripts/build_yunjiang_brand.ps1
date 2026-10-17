if (Test-Path (Join-Path $PSScriptRoot '../frontend/images/brand/approved-brand-board.png')) {
  node (Join-Path $PSScriptRoot 'install-approved-brand.cjs')
  node (Join-Path $PSScriptRoot 'build-approved-mini-icon.cjs')
  return
}
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path $PSScriptRoot -Parent
$out = Join-Path $root 'frontend/images/brand'
New-Item -ItemType Directory -Path $out -Force | Out-Null
# Convert locally installed Chinese font outlines into editable vector paths.
# Font is not redistributed. The wordmark is outlined to render consistently.
$font = [System.Drawing.FontFamily]::new('Microsoft YaHei')
$path = [System.Drawing.Drawing2D.GraphicsPath]::new()
$path.AddString('云疆知法', $font, [int][System.Drawing.FontStyle]::Bold, 80, [System.Drawing.PointF]::new(0,0), [System.Drawing.StringFormat]::GenericTypographic)
$bounds = $path.GetBounds()
$points = $path.PathPoints
$types = $path.PathTypes
$culture = [System.Globalization.CultureInfo]::InvariantCulture
function N($v) { ([double]$v).ToString('0.###',$culture) }
$parts = [System.Collections.Generic.List[string]]::new()
for($i=0;$i -lt $points.Length;$i++) {
    $kind = $types[$i] -band 7
    if($kind -eq 0) { $parts.Add('M'+(N $points[$i].X)+','+(N $points[$i].Y)) }
    elseif($kind -eq 1) { $parts.Add('L'+(N $points[$i].X)+','+(N $points[$i].Y)) }
    elseif($kind -eq 3) {
        $parts.Add('C'+(N $points[$i].X)+','+(N $points[$i].Y)+' '+(N $points[$i+1].X)+','+(N $points[$i+1].Y)+' '+(N $points[$i+2].X)+','+(N $points[$i+2].Y))
        $i+=2
    }
    if(($types[$i] -band 128) -ne 0) { $parts.Add('Z') }
}
$textPath = $parts -join ' '
$scale = 362 / $bounds.Width
$tx = 75 - $bounds.X * $scale
$ty = 438 - $bounds.Y * $scale
$lettering = '<g transform="translate('+(N $tx)+','+(N $ty)+') scale('+(N $scale)+')"><path fill="#791817" d="'+$textPath+'"/></g>'
# Original emblem: five connected people/cloud arcs sheltering an open law book.
$mark = @'
<g fill="none" stroke-linecap="round" stroke-linejoin="round">
 <path d="M128 196C106 171 111 133 143 119C150 79 199 66 225 91C253 54 310 68 320 109C356 104 387 134 379 169C409 190 400 231 372 242" stroke="#791817" stroke-width="22"/>
 <path d="M134 226C111 262 136 306 177 309C191 343 232 353 258 333C290 350 330 335 338 305C371 304 391 277 382 250" stroke="#D07367" stroke-width="22"/>
 <path d="M173 171C203 168 231 177 256 191C282 177 310 168 339 171L339 273C310 271 282 281 256 297C231 281 202 271 173 273Z" fill="#FAF7EB" stroke="#791817" stroke-width="15"/>
 <path d="M256 192V294M197 202L232 213M197 231L232 242M279 213L315 202M279 242L315 231" stroke="#791817" stroke-width="11"/>
 <path d="M199 145C216 133 237 136 249 149C263 126 294 127 308 146" stroke="#D07367" stroke-width="12"/>
</g>
'@
$svgHead='<svg xmlns="http://www.w3.org/2000/svg" '
$web=$svgHead+'viewBox="0 0 512 560"><title>云疆知法 · Web竖版Logo</title>'+$mark+$lettering+'</svg>'
$symbol=$svgHead+'viewBox="0 0 512 400"><title>云疆知法 · 同心云典</title>'+$mark+'</svg>'
$app=$svgHead+'viewBox="0 0 512 512"><title>云疆知法 · 小程序方标</title><rect x="8" y="8" width="496" height="496" rx="112" fill="#FAF7EB"/><g transform="translate(0 36)">'+$mark+'</g></svg>'
$horizontal=$svgHead+'viewBox="0 0 900 280"><title>云疆知法 · 横版Logo</title><g transform="translate(-26 -20) scale(.68)">'+$mark+'</g><g transform="translate(340 94) scale(1.5)"><path fill="#791817" d="'+$textPath+'"/></g></svg>'
foreach($item in @(@('logo-web.svg',$web),@('logo-symbol.svg',$symbol),@('app-icon.svg',$app),@('logo-horizontal.svg',$horizontal))) {
    [System.IO.File]::WriteAllText((Join-Path $out $item[0]),$item[1],[System.Text.UTF8Encoding]::new($false))
}
$webOut=Join-Path $root 'src/main/resources/static/console/brand'
New-Item -ItemType Directory -Path $webOut -Force | Out-Null
Copy-Item -Path (Join-Path $out '*.svg') -Destination $webOut
$path.Dispose(); $font.Dispose()
Write-Output 'Created original outlined SVG logo variants'
