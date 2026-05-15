Add-Type -AssemblyName System.Drawing

$texturesDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\src\main\resources\assets\fluidconverter\textures\gui"))
$mainPath  = Join-Path $texturesDir "fluid_converter.png"
$adminPath = Join-Path $texturesDir "fluid_converter_admin.png"

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

foreach ($b in $brushes.Values) { $b.Dispose() }
