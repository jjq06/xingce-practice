$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
$p = (Get-ChildItem -LiteralPath 'C:\Users\Akiha\Desktop\xingce-practice-main' -Filter *.pdf | Select-Object -First 1).FullName
"pdf: $p"
$bytes = [System.IO.File]::ReadAllBytes($p)
$latin1 = [System.Text.Encoding]::GetEncoding(28591)
$ascii = $latin1.GetString($bytes)
"len: $($bytes.Length)"

$out = New-Object System.Text.StringBuilder
$rx = [regex]'stream\r?\n'
$pos = 0
$count = 0
while ($true) {
    $m = $rx.Match($ascii, $pos)
    if (-not $m.Success) { break }
    $start = $m.Index + $m.Length
    $end = $ascii.IndexOf('endstream', $start)
    if ($end -lt 0) { break }
    $count++
    $len = $end - $start
    $chunk = New-Object byte[] $len
    [Array]::Copy($bytes, $start, $chunk, 0, $len)
    try {
        $ms = New-Object System.IO.MemoryStream(,$chunk[2..($len-1)])
        $ds = New-Object System.IO.Compression.DeflateStream($ms, [System.IO.Compression.CompressionMode]::Decompress)
        $sr = New-Object System.IO.StreamReader($ds, $latin1)
        $text = $sr.ReadToEnd()
        $sr.Close()
        [void]$out.AppendLine("=== STREAM $count ===")
        [void]$out.AppendLine($text)
    } catch {
        [void]$out.AppendLine("=== STREAM $count (failed) ===")
    }
    $pos = $end + 9
}
"streams: $count"
$out.ToString()
