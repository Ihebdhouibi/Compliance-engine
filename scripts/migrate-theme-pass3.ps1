# Third-pass: replace residual hardcoded hex/rgba literals with CSS vars.
# Strategy: map known recurring colors to their semantic CSS variable.

$files = Get-ChildItem -Path src/app, src/styles -Recurse -Filter *.scss

# Each rule: regex -> replacement
$rules = @(
    # Indigo (#6366f1, #818cf8, #a5b4fc, rgba(99,102,241,*)) -> accent
    @('rgba\(\s*99\s*,\s*102\s*,\s*241\s*,\s*([0-9.]+)\s*\)', 'rgb(var(--accent-rgb) / $1)'),
    @('#6366f1', 'rgb(var(--accent-rgb))'),
    @('#818cf8', 'rgb(var(--accent-strong-rgb))'),
    @('#a5b4fc', 'rgb(var(--accent-strong-rgb))'),

    # Mint #00e5a0 -> success
    @('#00e5a0', 'rgb(var(--success-rgb))'),

    # Red #ff4d6a -> danger
    @('#ff4d6a', 'rgb(var(--danger-rgb))'),

    # Amber #f5b800 -> warning
    @('#f5b800', 'rgb(var(--warning-rgb))'),

    # Black / dark void rgba(4,15,30,X) -> bg-deep
    @('rgba\(\s*4\s*,\s*15\s*,\s*30\s*,\s*([0-9.]+)\s*\)', 'rgb(var(--bg-deep-rgb) / $1)'),

    # Pure black #000 used as video-bg/contrast -> bg-deep
    @('background:\s*#000\b',          'background: rgb(var(--bg-deep-rgb))'),
    @('color:\s*#000\b',               'color: rgb(var(--fg-strong-rgb))'),

    # Pure white if any leftover
    @('background:\s*#fff\b',          'background: rgb(var(--bg-card-rgb))'),
    @('color:\s*#fff\b',               'color: rgb(var(--fg-strong-rgb))')
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
Write-Host "Third pass complete. $total files modified."
