param(
  [Parameter(Mandatory=$true)][string]$In,
  [Parameter(Mandatory=$true)][string]$Out
)
$ErrorActionPreference = 'Stop'
# Ambos ensamblados ANTES de cualquier literal de tipo: Windows PowerShell resuelve
# [System.IO.Compression.*] al analizar el script, no al ejecutarlo.
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$utf8 = New-Object System.Text.UTF8Encoding($false)
$lines = [System.IO.File]::ReadAllLines($In, [System.Text.Encoding]::UTF8)

function Esc([string]$s) {
  return $s.Replace('&','&amp;').Replace('<','&lt;').Replace('>','&gt;')
}
# Inline: **bold** toggling -> runs
function Runs([string]$text, [bool]$forceBold=$false, [int]$sz=22) {
  $parts = $text -split '\*\*'
  $sb = New-Object System.Text.StringBuilder
  for ($i=0; $i -lt $parts.Length; $i++) {
    $t = $parts[$i]
    if ($t.Length -eq 0) { continue }
    $bold = $forceBold -or (($i % 2) -eq 1)
    $rpr = "<w:rPr><w:rFonts w:ascii=`"Arial`" w:hAnsi=`"Arial`" w:cs=`"Arial`"/>" + $(if ($bold) {"<w:b/>"} else {""}) + "<w:sz w:val=`"$sz`"/><w:szCs w:val=`"$sz`"/></w:rPr>"
    [void]$sb.Append("<w:r>$rpr<w:t xml:space=`"preserve`">" + (Esc $t) + "</w:t></w:r>")
  }
  return $sb.ToString()
}
function Para([string]$text, [string]$jc='both', [bool]$bold=$false, [int]$sz=22, [int]$before=0, [int]$after=120, [int]$indent=0, [bool]$keepNext=$false) {
  $ind = if ($indent -gt 0) { "<w:ind w:left=`"$indent`"/>" } else { "" }
  $kn  = if ($keepNext) { "<w:keepNext/>" } else { "" }
  return "<w:p><w:pPr>$kn<w:spacing w:before=`"$before`" w:after=`"$after`"/>$ind<w:jc w:val=`"$jc`"/></w:pPr>" + (Runs $text $bold $sz) + "</w:p>"
}
function PageBreak() { return "<w:p><w:r><w:br w:type=`"page`"/></w:r></w:p>" }
function Table([string[]]$rows) {
  # rows: markdown table lines incl. header and separator
  $parsed = @()
  foreach ($r in $rows) {
    if ($r -match '^\|\s*-{3,}') { continue }
    $cells = $r.Trim().TrimStart('|').TrimEnd('|') -split '\|'
    $parsed += ,($cells | ForEach-Object { $_.Trim() })
  }
  if ($parsed.Count -eq 0) { return "" }
  $ncol = ($parsed | ForEach-Object { $_.Count } | Measure-Object -Maximum).Maximum
  $width = [int](9000 / $ncol)
  $sb = New-Object System.Text.StringBuilder
  [void]$sb.Append("<w:tbl><w:tblPr><w:tblW w:w=`"9000`" w:type=`"dxa`"/><w:tblBorders>" +
    "<w:top w:val=`"single`" w:sz=`"4`" w:space=`"0`" w:color=`"808080`"/><w:left w:val=`"single`" w:sz=`"4`" w:space=`"0`" w:color=`"808080`"/>" +
    "<w:bottom w:val=`"single`" w:sz=`"4`" w:space=`"0`" w:color=`"808080`"/><w:right w:val=`"single`" w:sz=`"4`" w:space=`"0`" w:color=`"808080`"/>" +
    "<w:insideH w:val=`"single`" w:sz=`"4`" w:space=`"0`" w:color=`"808080`"/><w:insideV w:val=`"single`" w:sz=`"4`" w:space=`"0`" w:color=`"808080`"/>" +
    "</w:tblBorders><w:tblLayout w:type=`"fixed`"/><w:tblCellMar><w:left w:w=`"80`" w:type=`"dxa`"/><w:right w:w=`"80`" w:type=`"dxa`"/></w:tblCellMar></w:tblPr><w:tblGrid>")
  for ($c=0; $c -lt $ncol; $c++) { [void]$sb.Append("<w:gridCol w:w=`"$width`"/>") }
  [void]$sb.Append("</w:tblGrid>")
  $first = $true
  foreach ($row in $parsed) {
    $trpr = if ($first) { "<w:trPr><w:tblHeader/></w:trPr>" } else { "" }
    [void]$sb.Append("<w:tr>$trpr")
    for ($c=0; $c -lt $ncol; $c++) {
      $txt = if ($c -lt $row.Count) { $row[$c] } else { "" }
      $shade = if ($first) { "<w:shd w:val=`"clear`" w:color=`"auto`" w:fill=`"E7E6E6`"/>" } else { "" }
      [void]$sb.Append("<w:tc><w:tcPr><w:tcW w:w=`"$width`" w:type=`"dxa`"/>$shade</w:tcPr>" +
        "<w:p><w:pPr><w:spacing w:before=`"40`" w:after=`"40`"/><w:jc w:val=`"left`"/></w:pPr>" + (Runs $txt $first 20) + "</w:p></w:tc>")
    }
    [void]$sb.Append("</w:tr>")
    $first = $false
  }
  [void]$sb.Append("</w:tbl>" + (Para "" 'left' $false 22 0 120))
  return $sb.ToString()
}

$body = New-Object System.Text.StringBuilder
$i = 0
$n = $lines.Length
while ($i -lt $n) {
  $l = $lines[$i]
  if ($l.Trim().Length -eq 0) { $i++; continue }
  if ($l.Trim() -eq '&nbsp;') { [void]$body.Append((Para "" 'left' $false 22 0 360)); $i++; continue }
  if ($l -match '^---+\s*$') {
    # page break only before an annex heading
    $j = $i + 1; while ($j -lt $n -and $lines[$j].Trim().Length -eq 0) { $j++ }
    if ($j -lt $n -and $lines[$j] -match '^# ') { [void]$body.Append((PageBreak)) }
    $i++; continue
  }
  if ($l -match '^# (.*)$')   { [void]$body.Append((Para $Matches[1] 'center' $true 28 240 240 0 $true)); $i++; continue }
  if ($l -match '^## (.*)$')  { [void]$body.Append((Para $Matches[1] 'left' $true 24 280 120 0 $true)); $i++; continue }
  if ($l -match '^### (.*)$') { [void]$body.Append((Para $Matches[1] 'left' $true 22 200 100 0 $true)); $i++; continue }
  if ($l -match '^\|') {
    $rows = @()
    while ($i -lt $n -and $lines[$i] -match '^\|') { $rows += $lines[$i]; $i++ }
    [void]$body.Append((Table $rows)); continue
  }
  if ($l -match '^- (.*)$') {
    # bullet item, may wrap onto following indented lines
    $txt = $Matches[1]; $i++
    while ($i -lt $n -and $lines[$i] -match '^\s{2,}\S' -and $lines[$i] -notmatch '^\s*- ') { $txt += ' ' + $lines[$i].Trim(); $i++ }
    [void]$body.Append((Para ("• " + $txt) 'both' $false 22 0 80 360)); continue
  }
  if ($l -match '^\d+\. ') {
    $txt = $l.Trim(); $i++
    while ($i -lt $n -and $lines[$i] -match '^\s{2,}\S') { $txt += ' ' + $lines[$i].Trim(); $i++ }
    [void]$body.Append((Para $txt 'both' $false 22 0 80 360)); continue
  }
  # normal paragraph: join until blank line or structural line
  $txt = $l.Trim(); $i++
  while ($i -lt $n) {
    $nx = $lines[$i]
    if ($nx.Trim().Length -eq 0 -or $nx -match '^(#|\||---|- |&nbsp;)' -or $nx -match '^\d+\. ') { break }
    $txt += ' ' + $nx.Trim(); $i++
  }
  [void]$body.Append((Para $txt 'both' $false 22 0 120))
}

$doc = "<?xml version=`"1.0`" encoding=`"UTF-8`" standalone=`"yes`"?>" +
"<w:document xmlns:w=`"http://schemas.openxmlformats.org/wordprocessingml/2006/main`" xmlns:r=`"http://schemas.openxmlformats.org/officeDocument/2006/relationships`">" +
"<w:body>" + $body.ToString() +
"<w:sectPr><w:pgSz w:w=`"12240`" w:h=`"15840`"/><w:pgMar w:top=`"1440`" w:right=`"1440`" w:bottom=`"1440`" w:left=`"1440`" w:header=`"720`" w:footer=`"720`" w:gutter=`"0`"/></w:sectPr>" +
"</w:body></w:document>"

$styles = "<?xml version=`"1.0`" encoding=`"UTF-8`" standalone=`"yes`"?>" +
"<w:styles xmlns:w=`"http://schemas.openxmlformats.org/wordprocessingml/2006/main`">" +
"<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii=`"Arial`" w:hAnsi=`"Arial`" w:cs=`"Arial`"/><w:sz w:val=`"22`"/><w:szCs w:val=`"22`"/><w:lang w:val=`"es-EC`"/></w:rPr></w:rPrDefault>" +
"<w:pPrDefault><w:pPr><w:spacing w:after=`"120`" w:line=`"276`" w:lineRule=`"auto`"/></w:pPr></w:pPrDefault></w:docDefaults>" +
"<w:style w:type=`"paragraph`" w:default=`"1`" w:styleId=`"Normal`"><w:name w:val=`"Normal`"/><w:qFormat/></w:style>" +
"</w:styles>"

$ct = "<?xml version=`"1.0`" encoding=`"UTF-8`" standalone=`"yes`"?>" +
"<Types xmlns=`"http://schemas.openxmlformats.org/package/2006/content-types`">" +
"<Default Extension=`"rels`" ContentType=`"application/vnd.openxmlformats-package.relationships+xml`"/>" +
"<Default Extension=`"xml`" ContentType=`"application/xml`"/>" +
"<Override PartName=`"/word/document.xml`" ContentType=`"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml`"/>" +
"<Override PartName=`"/word/styles.xml`" ContentType=`"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml`"/>" +
"</Types>"

$rels = "<?xml version=`"1.0`" encoding=`"UTF-8`" standalone=`"yes`"?>" +
"<Relationships xmlns=`"http://schemas.openxmlformats.org/package/2006/relationships`">" +
"<Relationship Id=`"rId1`" Type=`"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument`" Target=`"word/document.xml`"/>" +
"</Relationships>"

$docrels = "<?xml version=`"1.0`" encoding=`"UTF-8`" standalone=`"yes`"?>" +
"<Relationships xmlns=`"http://schemas.openxmlformats.org/package/2006/relationships`">" +
"<Relationship Id=`"rId1`" Type=`"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles`" Target=`"styles.xml`"/>" +
"</Relationships>"

# validate XML well-formedness before packaging
[void]([xml]$doc); [void]([xml]$styles); [void]([xml]$ct); [void]([xml]$rels); [void]([xml]$docrels)

$tmp = Join-Path ([System.IO.Path]::GetTempPath()) ("docx_" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path (Join-Path $tmp '_rels') -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tmp 'word\_rels') -Force | Out-Null
[System.IO.File]::WriteAllText((Join-Path $tmp '[Content_Types].xml'), $ct, $utf8)
[System.IO.File]::WriteAllText((Join-Path $tmp '_rels\.rels'), $rels, $utf8)
[System.IO.File]::WriteAllText((Join-Path $tmp 'word\document.xml'), $doc, $utf8)
[System.IO.File]::WriteAllText((Join-Path $tmp 'word\styles.xml'), $styles, $utf8)
[System.IO.File]::WriteAllText((Join-Path $tmp 'word\_rels\document.xml.rels'), $docrels, $utf8)
if (Test-Path $Out) { Remove-Item $Out -Force }
Add-Type -AssemblyName System.IO.Compression.FileSystem
# Entradas creadas a mano con '/' como separador: CreateFromDirectory en .NET Framework
# escribe 'word\document.xml' con barra invertida y Word lo rechaza como archivo dañado.
$zip = [System.IO.Compression.ZipFile]::Open($Out, [System.IO.Compression.ZipArchiveMode]::Create)
foreach ($f in Get-ChildItem -Path $tmp -Recurse -File) {
  $name = $f.FullName.Substring($tmp.Length + 1).Replace('\', '/')
  $entry = $zip.CreateEntry($name, [System.IO.Compression.CompressionLevel]::Optimal)
  $es = $entry.Open(); $fs = [System.IO.File]::OpenRead($f.FullName); $fs.CopyTo($es); $fs.Dispose(); $es.Dispose()
}
$zip.Dispose()
Remove-Item $tmp -Recurse -Force
Write-Output ("OK " + $Out + " " + (Get-Item $Out).Length + " bytes, paragraphs=" + ([regex]::Matches($doc, '<w:p>').Count) + ", tables=" + ([regex]::Matches($doc, '<w:tbl>').Count))
