# Bulk-migrate hardcoded colour literals in component SCSS files
# to themed CSS variables via rgb(var(--TOKEN-rgb) / alpha) form.
#
# Mappings:
#   rgba(t.$cyan-400, X) / rgba(0,212,237, X)   -> rgb(var(--accent-rgb) / X)
#   rgba(t.$cyan-300, X)                        -> rgb(var(--accent-strong-rgb) / X)
#   rgba(t.$navy-700|800, X) / rgba(7,21,40, X) -> rgb(var(--bg-card-rgb) / X)
#   rgba(t.$navy-900|950, X) / rgba(2,9,18, X)  -> rgb(var(--bg-app-rgb) / X)
#   rgba(4,13,26, X)                            -> rgb(var(--bg-card-strong-rgb) / X)
#   rgba(255,255,255, X)                        -> rgb(var(--fg-rgb) / X)

$files = Get-ChildItem -Path src/app, src/styles -Recurse -Filter *.scss

# (pattern, replacement) pairs. Order matters: do specific tokens before generic rgba.
$rules = @(
    @('rgba\(t\.\$cyan-4\d{2},\s*([0-9.]+)\)',  'rgb(var(--accent-rgb) / $1)'),
    @('rgba\(t\.\$cyan-3\d{2},\s*([0-9.]+)\)',  'rgb(var(--accent-strong-rgb) / $1)'),
    @('rgba\(t\.\$cyan-[56]\d{2},\s*([0-9.]+)\)','rgb(var(--accent-strong-rgb) / $1)'),
    @('rgba\(t\.\$cyan-2\d{2},\s*([0-9.]+)\)',  'rgb(var(--accent-rgb) / $1)'),

    @('rgba\(t\.\$navy-(7|8)\d{2},\s*([0-9.]+)\)', 'rgb(var(--bg-card-rgb) / $2)'),
    @('rgba\(t\.\$navy-(9)\d{2},\s*([0-9.]+)\)',   'rgb(var(--bg-app-rgb) / $2)'),
    @('rgba\(t\.\$navy-[1-6]\d{2},\s*([0-9.]+)\)', 'rgb(var(--bg-card-rgb) / $1)'),

    @('rgba\(t\.\$slate-\d{3},\s*([0-9.]+)\)', 'rgb(var(--fg-mute-rgb) / $1)'),

    @('rgba\(0,\s*212,\s*237,\s*([0-9.]+)\)',   'rgb(var(--accent-rgb) / $1)'),
    @('rgba\(7,\s*21,\s*40,\s*([0-9.]+)\)',     'rgb(var(--bg-card-rgb) / $1)'),
    @('rgba\(2,\s*9,\s*18,\s*([0-9.]+)\)',      'rgb(var(--bg-app-rgb) / $1)'),
    @('rgba\(4,\s*13,\s*26,\s*([0-9.]+)\)',     'rgb(var(--bg-card-strong-rgb) / $1)'),
    @('rgba\(10,\s*31,\s*61,\s*([0-9.]+)\)',    'rgb(var(--bg-card-rgb) / $1)'),
    @('rgba\(13,\s*40,\s*80,\s*([0-9.]+)\)',    'rgb(var(--bg-card-rgb) / $1)'),

    # White (was used both as text on dark surface AND as subtle dividers)
    @('rgba\(255,\s*255,\s*255,\s*([0-9.]+)\)', 'rgb(var(--fg-rgb) / $1)'),

    # Bare cyan literal (rare)
    @('#00d4ed\b', 'rgb(var(--accent-rgb))'),
    @('#00bcd4\b', 'rgb(var(--accent-rgb))'),
    @('#00a8b8\b', 'rgb(var(--accent-strong-rgb))'),
    @('#18e8ff\b', 'rgb(var(--accent-strong-rgb))'),

    # Bare navy literals
    @('#020912\b', 'rgb(var(--bg-app-rgb))'),
    @('#040d1a\b', 'rgb(var(--bg-app-rgb))'),
    @('#040f1e\b', 'rgb(var(--bg-card-strong-rgb))'),
    @('#071528\b', 'rgb(var(--bg-card-rgb))'),
    @('#0a1f3d\b', 'rgb(var(--bg-card-rgb))'),

    # White text colour -> primary fg (so it goes slate in light, near-white in dark)
    @('color:\s*#fff(?!\w)',          'color: rgb(var(--fg-strong-rgb))'),
    @('color:\s*#ffffff(?!\w)',       'color: rgb(var(--fg-strong-rgb))'),
    @('color:\s*white(?![a-zA-Z])',   'color: rgb(var(--fg-strong-rgb))'),

    # Cyan glow that occasionally appears bare in box-shadows
    @('rgba\(0,\s*229,\s*160,\s*([0-9.]+)\)',  'rgb(var(--success-rgb) / $1)'),
    @('rgba\(255,\s*77,\s*106,\s*([0-9.]+)\)', 'rgb(var(--danger-rgb) / $1)'),
    @('rgba\(245,\s*184,\s*0,\s*([0-9.]+)\)',  'rgb(var(--warning-rgb) / $1)')
)

$total = 0
foreach ($f in $files) {
    $text = Get-Content -Raw -Path $f.FullName
    $orig = $text
    foreach ($r in $rules) {
        $text = [regex]::Replace($text, $r[0], $r[1])
    }
    if ($text -ne $orig) {
        Set-Content -Path $f.FullName -Value $text -Encoding UTF8 -NoNewline
        $total++
        Write-Host "Updated: $($f.FullName)"
    }
}
Write-Host ""
Write-Host "DONE. $total files modified."
