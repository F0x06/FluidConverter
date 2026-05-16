Add-Type -AssemblyName System.Drawing

$texturesDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\src\main\resources\assets\fluidconverter\textures\gui"))
$mainPath  = Join-Path $texturesDir "fluid_converter.png"
$adminPath = Join-Path $texturesDir "fluid_converter_admin.png"
$sidesPath = Join-Path $texturesDir "fluid_converter_sides.png"
$spritesDir = Join-Path $texturesDir "sprites\sides"
$mainSprDir = Join-Path $texturesDir "sprites\main"
$adminSprDir = Join-Path $texturesDir "sprites\admin"
if (-not (Test-Path $spritesDir)) { New-Item -ItemType Directory -Force -Path $spritesDir | Out-Null }
if (-not (Test-Path $mainSprDir)) { New-Item -ItemType Directory -Force -Path $mainSprDir | Out-Null }
if (-not (Test-Path $adminSprDir)) { New-Item -ItemType Directory -Force -Path $adminSprDir | Out-Null }

$W = 176
$H = 186

$panel      = "#C6C6C6"
$hilite     = "#FFFFFF"
$slotbg     = "#8B8B8B"
$darkShadow = "#373737"

$brushes = @{}
function Br($hex) {
    if (-not $brushes.ContainsKey($hex)) {
        $c = [System.Drawing.ColorTranslator]::FromHtml($hex)
        $brushes[$hex] = New-Object System.Drawing.SolidBrush $c
    }
    return $brushes[$hex]
}
function Fill($x1, $y1, $x2, $y2, $hex) {
    $script:g.FillRectangle((Br $hex), $x1, $y1, $x2 - $x1, $y2 - $y1)
}

function NewBitmap {
    $bmp = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $script:g = [System.Drawing.Graphics]::FromImage($bmp)
    $script:g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $script:g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $script:g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    return $bmp
}

function Slot($sx, $sy) {
    Fill ($sx-1) ($sy-1) ($sx+17) $sy $darkShadow
    Fill ($sx-1) $sy ($sx) ($sy+17) $darkShadow
    Fill ($sx-1) ($sy+16) ($sx+17) ($sy+17) $hilite
    Fill ($sx+16) $sy ($sx+17) ($sy+17) $hilite
    Fill $sx $sy ($sx+16) ($sy+16) $slotbg
}

function InventoryGrid {
    for ($row = 0; $row -lt 3; $row++) {
        for ($col = 0; $col -lt 9; $col++) {
            Slot (8 + $col * 18) (104 + $row * 18)
        }
    }
    for ($col = 0; $col -lt 9; $col++) {
        Slot (8 + $col * 18) 162
    }
}

function OuterBorder {
    Fill 0 0 $W 1 $hilite
    Fill 0 0 1 $H $hilite
    Fill 0 ($H-1) $W $H $darkShadow
    Fill ($W-1) 0 $W $H $darkShadow
}

function InsetRect($x1, $y1, $x2, $y2, $fillHex) {
    Fill $x1 $y1 $x2 ($y1+1) $darkShadow
    Fill $x1 $y1 ($x1+1) $y2 $darkShadow
    Fill $x1 ($y2-1) $x2 $y2 $hilite
    Fill ($x2-1) $y1 $x2 $y2 $hilite
    Fill ($x1+1) ($y1+1) ($x2-1) ($y2-1) $fillHex
}

function RaisedRect($x1, $y1, $x2, $y2, $fillHex) {
    Fill ($x1+1) ($y1+1) ($x2-1) ($y2-1) $fillHex
    Fill $x1 $y1 $x2 ($y1+1) $hilite
    Fill $x1 $y1 ($x1+1) $y2 $hilite
    Fill $x1 ($y2-1) $x2 $y2 $slotbg
    Fill ($x2-1) $y1 $x2 $y2 $slotbg
}

function StartBitmap($w, $h) {
    $bmp = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $script:g = [System.Drawing.Graphics]::FromImage($bmp)
    $script:g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $script:g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $script:g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    return $bmp
}

function WriteNineSliceMeta($pngPath, $size) {
    $mcmeta = "{ ""gui"": { ""scaling"": { ""type"": ""nine_slice"", ""width"": $size, ""height"": $size, ""border"": 1 } } }"
    [System.IO.File]::WriteAllText($pngPath + ".mcmeta", $mcmeta, [System.Text.UTF8Encoding]::new($false))
}

function MakeNineSliceButton($path, $size, $fillHex) {
    $bmp = StartBitmap $size $size
    Fill 0 0 $size $size $fillHex
    Fill 0 0 $size 1 $hilite
    Fill 0 0 1 $size $hilite
    Fill 0 ($size-1) $size $size $darkShadow
    Fill ($size-1) 0 $size $size $darkShadow
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $script:g.Dispose()
    $bmp.Dispose()
    WriteNineSliceMeta $path $size
}

function MakeShieldSprite($path, $colorHex) {
    $bmp = StartBitmap 8 8
    Fill 1 0 7 1 $colorHex
    Fill 0 1 8 4 $colorHex
    Fill 1 4 7 5 $colorHex
    Fill 2 5 6 6 $colorHex
    Fill 3 6 5 7 $colorHex
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $script:g.Dispose()
    $bmp.Dispose()
}

function MakeSidesSprite($path, $colorHex) {
    $bmp = StartBitmap 8 8
    Fill 3 0 5 2 $colorHex
    Fill 0 3 2 5 $colorHex
    Fill 3 3 5 5 $colorHex
    Fill 6 3 8 5 $colorHex
    Fill 3 6 5 8 $colorHex
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $script:g.Dispose()
    $bmp.Dispose()
}

function MakeFixedButton($path, $size, $fillHex) {
    $bmp = StartBitmap $size $size
    Fill 0 0 $size $size $fillHex
    Fill 0 0 $size 1 $hilite
    Fill 0 0 1 $size $hilite
    Fill 0 ($size-1) $size $size $darkShadow
    Fill ($size-1) 0 $size $size $darkShadow
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $script:g.Dispose()
    $bmp.Dispose()
}

function MakePlayIcon($path, $colorHex) {
    $bmp = StartBitmap 8 8
    Fill 1 1 2 8 $colorHex
    Fill 2 2 3 7 $colorHex
    Fill 3 3 4 6 $colorHex
    Fill 4 4 5 5 $colorHex
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $script:g.Dispose()
    $bmp.Dispose()
}

function MakePauseIcon($path, $colorHex) {
    $bmp = StartBitmap 8 8
    Fill 2 1 4 7 $colorHex
    Fill 5 1 7 7 $colorHex
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $script:g.Dispose()
    $bmp.Dispose()
}

function MakeDrainIcon($path, $colorHex) {
    $bmp = StartBitmap 8 8
    Fill 3 0 5 4 $colorHex
    Fill 1 4 7 5 $colorHex
    Fill 2 5 6 6 $colorHex
    Fill 3 6 5 7 $colorHex
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $script:g.Dispose()
    $bmp.Dispose()
}

function MakePrevIcon($path, $colorHex) {
    $bmp = StartBitmap 8 8
    Fill 3 1 5 2 $colorHex
    Fill 2 2 4 3 $colorHex
    Fill 1 3 3 4 $colorHex
    Fill 1 4 3 5 $colorHex
    Fill 2 5 4 6 $colorHex
    Fill 3 6 5 7 $colorHex
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $script:g.Dispose()
    $bmp.Dispose()
}

function MakeNextIcon($path, $colorHex) {
    $bmp = StartBitmap 8 8
    Fill 3 1 5 2 $colorHex
    Fill 4 2 6 3 $colorHex
    Fill 5 3 7 4 $colorHex
    Fill 5 4 7 5 $colorHex
    Fill 4 5 6 6 $colorHex
    Fill 3 6 5 7 $colorHex
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $script:g.Dispose()
    $bmp.Dispose()
}

function MakeArrowToggleSprite($path, $size, $framed) {
    $bmp = StartBitmap $size $size
    Fill 0 0 $size $size $panel
    if ($framed) {
        Fill 0 0 $size 1 $darkShadow
        Fill 0 0 1 $size $darkShadow
        Fill 0 ($size-1) $size $size $darkShadow
        Fill ($size-1) 0 $size $size $darkShadow
    }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $script:g.Dispose()
    $bmp.Dispose()
    WriteNineSliceMeta $path $size
}

function MakeRedstoneSprite($path, $headHex) {
    $stick = "#6B4226"
    $bmp = StartBitmap 8 8
    Fill 3 4 5 8 $stick
    Fill 2 0 6 4 $headHex
    Fill 3 4 5 5 $headHex
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $script:g.Dispose()
    $bmp.Dispose()
}

function MakeButtonSprite($path, $w, $h, $fillHex) {
    $bmp = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $script:g = [System.Drawing.Graphics]::FromImage($bmp)
    $script:g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $script:g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $script:g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    Fill 0 0 $w $h $fillHex
    Fill 0 0 $w 1 $hilite
    Fill 0 0 1 $h $hilite
    Fill 0 ($h-1) $w $h $darkShadow
    Fill ($w-1) 0 $w $h $darkShadow
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $script:g.Dispose()
    $bmp.Dispose()
}

function Well($x, $y) {
    Fill ($x-2) ($y-2) ($x+20+2) $y $darkShadow
    Fill ($x-2) $y ($x) ($y+48) $darkShadow
    Fill ($x-2) ($y+48) ($x+20+2) ($y+48+2) $hilite
    Fill ($x+20) $y ($x+20+2) ($y+48) $hilite
}

# Static arrow at given (x, y), width — matches GuiHelpers.drawArrow (gray, no progress).
function StaticArrow($x, $y, $width) {
    $midY = $y + 3
    $headSize = 4
    $shaftEnd = $x + $width - $headSize
    Fill $x $midY $shaftEnd ($midY + 2) $darkShadow
    for ($i = 0; $i -lt $headSize; $i++) {
        $half = $headSize - $i
        Fill ($shaftEnd + $i) ($midY - $half + 1) ($shaftEnd + $i + 1) ($midY + $half + 1) $darkShadow
    }
}

# ============================================================
# Main texture
# ============================================================
$bmp = NewBitmap
Fill 0 0 $W $H $panel
OuterBorder
InsetRect 7 17 169 78 $slotbg
Well 26 24
Well 130 24
InsetRect 7 78 169 91 $slotbg
InventoryGrid
$bmp.Save($mainPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Output "Main texture saved:  $mainPath"

# ============================================================
# Admin texture
# ============================================================
$bmp = NewBitmap
Fill 0 0 $W $H $panel
OuterBorder

# Learn pair panel (matches FluidConverterAdminMenu LEARN_*).
# Arrow between slots is drawn as a clickable widget at runtime (ArrowToggleButton),
# so it's intentionally omitted here.
InsetRect 7 18 169 42 $slotbg
Slot 44 22
Slot 116 22

# Recipe list panel (matches RECIPE_LIST_HEADER_Y / FIRST_ROW_Y / MAX_ROWS / ROW_HEIGHT).
InsetRect 7 48 169 92 $slotbg

InventoryGrid
$bmp.Save($adminPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Output "Admin texture saved: $adminPath"

# ============================================================
# Sides texture
# ============================================================
$bmp = NewBitmap
Fill 0 0 $W $H $panel
OuterBorder
RaisedRect 4 16 172 98 $panel
InventoryGrid
$bmp.Save($sidesPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Output "Sides texture saved: $sidesPath"

# ============================================================
# Sides button sprites
# ============================================================
MakeButtonSprite (Join-Path $spritesDir "face_none.png")         20 20 "#6E6E6E"
MakeButtonSprite (Join-Path $spritesDir "face_none_hover.png")   20 20 "#8B8B8B"
MakeButtonSprite (Join-Path $spritesDir "face_input.png")        20 20 "#2E7298"
MakeButtonSprite (Join-Path $spritesDir "face_input_hover.png")  20 20 "#3A8FB8"
MakeButtonSprite (Join-Path $spritesDir "face_output.png")       20 20 "#A67B2A"
MakeButtonSprite (Join-Path $spritesDir "face_output_hover.png") 20 20 "#C89A3A"
MakeButtonSprite (Join-Path $spritesDir "icon_button.png")       12 12 "#8B8B8B"
MakeButtonSprite (Join-Path $spritesDir "icon_button_hover.png") 12 12 "#A0A0A0"
Write-Output "Sprites saved:       $spritesDir"

# ============================================================
# Main screen button + icon sprites
# ============================================================
MakeNineSliceButton (Join-Path $mainSprDir "button.png")       12 "#8B8B8B"
MakeNineSliceButton (Join-Path $mainSprDir "button_hover.png") 12 "#A0A0A0"
MakeShieldSprite    (Join-Path $mainSprDir "icon_shield.png")          "#E6E6E6"
MakeSidesSprite     (Join-Path $mainSprDir "icon_sides.png")           "#E6E6E6"
MakeRedstoneSprite  (Join-Path $mainSprDir "icon_redstone_ignored.png")  "#777777"
MakeRedstoneSprite  (Join-Path $mainSprDir "icon_redstone_active.png")   "#E03030"
MakeRedstoneSprite  (Join-Path $mainSprDir "icon_redstone_inactive.png") "#802020"
MakeFixedButton (Join-Path $mainSprDir "button_small.png")       8 "#8B8B8B"
MakeFixedButton (Join-Path $mainSprDir "button_small_hover.png") 8 "#A0A0A0"
MakePlayIcon  (Join-Path $mainSprDir "icon_play.png")  "#E6E6E6"
MakePauseIcon (Join-Path $mainSprDir "icon_pause.png") "#E6E6E6"
MakeDrainIcon (Join-Path $mainSprDir "icon_drain.png") "#E6E6E6"
MakePrevIcon  (Join-Path $mainSprDir "icon_prev.png")  "#E6E6E6"
MakeNextIcon  (Join-Path $mainSprDir "icon_next.png")  "#E6E6E6"
Write-Output "Main sprites saved:  $mainSprDir"

# ============================================================
# Admin screen sprites
# ============================================================
MakeArrowToggleSprite (Join-Path $adminSprDir "arrow_toggle.png")       12 $false
MakeArrowToggleSprite (Join-Path $adminSprDir "arrow_toggle_hover.png") 12 $true
Write-Output "Admin sprites saved: $adminSprDir"

foreach ($b in $brushes.Values) { $b.Dispose() }
