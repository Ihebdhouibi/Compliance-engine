# Second-pass migration: handle rgba() calls that wrap SCSS tokens
# which now resolve to rgb(var(...)) strings (incompatible with sass rgba()).

$files = Get-ChildItem -Path src/app, src/styles -Recurse -Filter *.scss

$rules = @(
    @('rgba\(\s*t\.\$success\s*,\s*([0-9.]+)\s*\)',     'rgb(var(--success-rgb) / $1)'),
    @('rgba\(\s*t\.\$warning\s*,\s*([0-9.]+)\s*\)',     'rgb(var(--warning-rgb) / $1)'),
    @('rgba\(\s*t\.\$error\s*,\s*([0-9.]+)\s*\)',       'rgb(var(--danger-rgb) / $1)'),
    @('rgba\(\s*t\.\$accent\b[^,]*,\s*([0-9.]+)\s*\)',  'rgb(var(--accent-rgb) / $1)'),
    @('rgba\(\s*t\.\$fg\b[^,]*,\s*([0-9.]+)\s*\)',      'rgb(var(--fg-rgb) / $1)'),
    @('rgba\(\s*t\.\$bg[a-z-]*\s*,\s*([0-9.]+)\s*\)',   'rgb(var(--bg-card-rgb) / $1)'),
    @('rgba\(\s*t\.\$border\b\s*,\s*([0-9.]+)\s*\)',    'rgb(var(--border-rgb) / $1)'),
    @('rgba\(\s*t\.\$navy-\d{3}\s*,\s*([0-9.]+)\s*\)',  'rgb(var(--bg-card-rgb) / $1)'),
    @('rgba\(\s*t\.\$cyan-\d{3}\s*,\s*([0-9.]+)\s*\)',  'rgb(var(--accent-rgb) / $1)'),
    @('rgba\(\s*t\.\$slate-\d{3}\s*,\s*([0-9.]+)\s*\)', 'rgb(var(--fg-mute-rgb) / $1)'),

    # Any remaining rgba() wrapping a SCSS variable
    @('rgba\(\s*t\.\$text-primary\s*,\s*([0-9.]+)\s*\)',   'rgb(var(--fg-strong-rgb) / $1)'),
    @('rgba\(\s*t\.\$text-secondary\s*,\s*([0-9.]+)\s*\)', 'rgb(var(--fg-rgb) / $1)'),
    @('rgba\(\s*t\.\$text-muted\s*,\s*([0-9.]+)\s*\)',     'rgb(var(--fg-mute-rgb) / $1)')
)

$total = 0
foreach ($f in $files) {
    if ($f.Length -eq 0) { continue }
    $text = Get-Content -Raw -Path $f.FullName
    if ($null -eq $text) { continue }
    $orig = $text
    foreach ($r in $rules) {
        $text = [regex]::Replace($text, $r[0], $r[1])
    }
    if ($text -ne $orig) {
        Set-Content -Path $f.FullName -Value $text -Encoding UTF8 -NoNewline
        $total++
    }
}
Write-Host "Second pass complete. $total files modified."
