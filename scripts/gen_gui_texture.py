#!/usr/bin/env python3
import argparse
from pathlib import Path
from PIL import Image

W, H = 176, 186

PANEL       = "#C6C6C6"
HILITE      = "#FFFFFF"
SLOTBG      = "#8B8B8B"
DARK_SHADOW = "#373737"


def hex_to_rgba(hex_color):
    h = hex_color.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


class Canvas:
    def __init__(self, w, h):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))

    def fill(self, x1, y1, x2, y2, hex_color):
        if x2 <= x1 or y2 <= y1:
            return
        block = Image.new("RGBA", (x2 - x1, y2 - y1), hex_to_rgba(hex_color))
        self.img.paste(block, (x1, y1))

    def save(self, path):
        self.img.save(str(path), "PNG")


def slot(c, sx, sy):
    c.fill(sx - 1, sy - 1, sx + 17, sy, DARK_SHADOW)
    c.fill(sx - 1, sy, sx, sy + 17, DARK_SHADOW)
    c.fill(sx - 1, sy + 16, sx + 17, sy + 17, HILITE)
    c.fill(sx + 16, sy, sx + 17, sy + 17, HILITE)
    c.fill(sx, sy, sx + 16, sy + 16, SLOTBG)


def inventory_grid(c):
    for row in range(3):
        for col in range(9):
            slot(c, 8 + col * 18, 104 + row * 18)
    for col in range(9):
        slot(c, 8 + col * 18, 162)


def outer_border(c):
    c.fill(0, 0, W, 1, HILITE)
    c.fill(0, 0, 1, H, HILITE)
    c.fill(0, H - 1, W, H, DARK_SHADOW)
    c.fill(W - 1, 0, W, H, DARK_SHADOW)


def inset_rect(c, x1, y1, x2, y2, fill_hex):
    c.fill(x1, y1, x2, y1 + 1, DARK_SHADOW)
    c.fill(x1, y1, x1 + 1, y2, DARK_SHADOW)
    c.fill(x1, y2 - 1, x2, y2, HILITE)
    c.fill(x2 - 1, y1, x2, y2, HILITE)
    c.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill_hex)


def raised_rect(c, x1, y1, x2, y2, fill_hex):
    c.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill_hex)
    c.fill(x1, y1, x2, y1 + 1, HILITE)
    c.fill(x1, y1, x1 + 1, y2, HILITE)
    c.fill(x1, y2 - 1, x2, y2, SLOTBG)
    c.fill(x2 - 1, y1, x2, y2, SLOTBG)


def well(c, x, y):
    c.fill(x - 2, y - 2, x + 20 + 2, y, DARK_SHADOW)
    c.fill(x - 2, y, x, y + 48, DARK_SHADOW)
    c.fill(x - 2, y + 48, x + 20 + 2, y + 48 + 2, HILITE)
    c.fill(x + 20, y, x + 20 + 2, y + 48, HILITE)


def write_nine_slice_meta(png_path, size):
    content = '{ "gui": { "scaling": { "type": "nine_slice", "width": %d, "height": %d, "border": 1 } } }' % (size, size)
    Path(str(png_path) + ".mcmeta").write_text(content, encoding="utf-8", newline="")


def make_nine_slice_button(path, size, fill_hex):
    c = Canvas(size, size)
    c.fill(0, 0, size, size, fill_hex)
    c.fill(0, 0, size, 1, HILITE)
    c.fill(0, 0, 1, size, HILITE)
    c.fill(0, size - 1, size, size, DARK_SHADOW)
    c.fill(size - 1, 0, size, size, DARK_SHADOW)
    c.save(path)
    write_nine_slice_meta(path, size)


def make_shield_sprite(path, color_hex):
    c = Canvas(8, 8)
    c.fill(1, 0, 7, 1, color_hex)
    c.fill(0, 1, 8, 4, color_hex)
    c.fill(1, 4, 7, 5, color_hex)
    c.fill(2, 5, 6, 6, color_hex)
    c.fill(3, 6, 5, 7, color_hex)
    c.save(path)


def make_sides_sprite(path, color_hex):
    c = Canvas(8, 8)
    c.fill(3, 0, 5, 2, color_hex)
    c.fill(0, 3, 2, 5, color_hex)
    c.fill(3, 3, 5, 5, color_hex)
    c.fill(6, 3, 8, 5, color_hex)
    c.fill(3, 6, 5, 8, color_hex)
    c.save(path)


def make_fixed_button(path, size, fill_hex):
    c = Canvas(size, size)
    c.fill(0, 0, size, size, fill_hex)
    c.fill(0, 0, size, 1, HILITE)
    c.fill(0, 0, 1, size, HILITE)
    c.fill(0, size - 1, size, size, DARK_SHADOW)
    c.fill(size - 1, 0, size, size, DARK_SHADOW)
    c.save(path)


def make_play_icon(path, color_hex):
    c = Canvas(8, 8)
    c.fill(2, 0, 3, 8, color_hex)
    c.fill(3, 1, 4, 7, color_hex)
    c.fill(4, 2, 5, 6, color_hex)
    c.fill(5, 3, 6, 5, color_hex)
    c.save(path)


def make_pause_icon(path, color_hex):
    c = Canvas(8, 8)
    c.fill(1, 1, 3, 7, color_hex)
    c.fill(5, 1, 7, 7, color_hex)
    c.save(path)


def make_drain_icon(path, color_hex):
    c = Canvas(8, 8)
    c.fill(3, 0, 5, 4, color_hex)
    c.fill(1, 4, 7, 5, color_hex)
    c.fill(2, 5, 6, 6, color_hex)
    c.fill(3, 6, 5, 7, color_hex)
    c.save(path)


def make_prev_icon(path, color_hex):
    c = Canvas(8, 8)
    c.fill(3, 1, 5, 2, color_hex)
    c.fill(2, 2, 4, 3, color_hex)
    c.fill(1, 3, 3, 4, color_hex)
    c.fill(1, 4, 3, 5, color_hex)
    c.fill(2, 5, 4, 6, color_hex)
    c.fill(3, 6, 5, 7, color_hex)
    c.save(path)


def make_next_icon(path, color_hex):
    c = Canvas(8, 8)
    c.fill(3, 1, 5, 2, color_hex)
    c.fill(4, 2, 6, 3, color_hex)
    c.fill(5, 3, 7, 4, color_hex)
    c.fill(5, 4, 7, 5, color_hex)
    c.fill(4, 5, 6, 6, color_hex)
    c.fill(3, 6, 5, 7, color_hex)
    c.save(path)


def make_arrow_toggle_sprite(path, size, framed):
    c = Canvas(size, size)
    c.fill(0, 0, size, size, PANEL)
    if framed:
        c.fill(0, 0, size, 1, DARK_SHADOW)
        c.fill(0, 0, 1, size, DARK_SHADOW)
        c.fill(0, size - 1, size, size, DARK_SHADOW)
        c.fill(size - 1, 0, size, size, DARK_SHADOW)
    c.save(path)
    write_nine_slice_meta(path, size)


def make_redstone_sprite(path, head_hex):
    stick = "#6B4226"
    c = Canvas(8, 8)
    c.fill(3, 4, 5, 8, stick)
    c.fill(2, 0, 6, 4, head_hex)
    c.fill(3, 4, 5, 5, head_hex)
    c.save(path)


def make_button_sprite(path, w, h, fill_hex):
    c = Canvas(w, h)
    c.fill(0, 0, w, h, fill_hex)
    c.fill(0, 0, w, 1, HILITE)
    c.fill(0, 0, 1, h, HILITE)
    c.fill(0, h - 1, w, h, DARK_SHADOW)
    c.fill(w - 1, 0, w, h, DARK_SHADOW)
    c.save(path)


def make_main_texture(path):
    c = Canvas(W, H)
    c.fill(0, 0, W, H, PANEL)
    outer_border(c)
    inset_rect(c, 7, 17, 169, 78, SLOTBG)
    well(c, 26, 24)
    well(c, 130, 24)
    inset_rect(c, 7, 78, 169, 91, SLOTBG)
    inventory_grid(c)
    c.save(path)


def make_admin_texture(path):
    c = Canvas(W, H)
    c.fill(0, 0, W, H, PANEL)
    outer_border(c)
    inset_rect(c, 7, 18, 169, 42, SLOTBG)
    slot(c, 44, 22)
    slot(c, 116, 22)
    inset_rect(c, 7, 48, 169, 92, SLOTBG)
    inventory_grid(c)
    c.save(path)


def make_sides_texture(path):
    c = Canvas(W, H)
    c.fill(0, 0, W, H, PANEL)
    outer_border(c)
    raised_rect(c, 4, 16, 172, 98, PANEL)
    inventory_grid(c)
    c.save(path)


def generate_all(tex_dir):
    main_path  = tex_dir / "fluid_converter.png"
    admin_path = tex_dir / "fluid_converter_admin.png"
    sides_path = tex_dir / "fluid_converter_sides.png"
    sprites_dir  = tex_dir / "sprites" / "sides"
    main_spr_dir = tex_dir / "sprites" / "main"
    admin_spr_dir = tex_dir / "sprites" / "admin"
    for d in (sprites_dir, main_spr_dir, admin_spr_dir):
        d.mkdir(parents=True, exist_ok=True)

    make_main_texture(main_path)
    print(f"Main texture saved:  {main_path}")

    make_admin_texture(admin_path)
    print(f"Admin texture saved: {admin_path}")

    make_sides_texture(sides_path)
    print(f"Sides texture saved: {sides_path}")

    make_button_sprite(sprites_dir / "face_none.png",         20, 20, "#6E6E6E")
    make_button_sprite(sprites_dir / "face_none_hover.png",   20, 20, "#8B8B8B")
    make_button_sprite(sprites_dir / "face_input.png",        20, 20, "#2E7298")
    make_button_sprite(sprites_dir / "face_input_hover.png",  20, 20, "#3A8FB8")
    make_button_sprite(sprites_dir / "face_output.png",       20, 20, "#A67B2A")
    make_button_sprite(sprites_dir / "face_output_hover.png", 20, 20, "#C89A3A")
    make_button_sprite(sprites_dir / "icon_button.png",       12, 12, "#8B8B8B")
    make_button_sprite(sprites_dir / "icon_button_hover.png", 12, 12, "#A0A0A0")
    print(f"Sprites saved:       {sprites_dir}")

    make_nine_slice_button(main_spr_dir / "button.png",       12, "#8B8B8B")
    make_nine_slice_button(main_spr_dir / "button_hover.png", 12, "#A0A0A0")
    make_shield_sprite(main_spr_dir / "icon_shield.png", "#E6E6E6")
    make_sides_sprite(main_spr_dir / "icon_sides.png", "#E6E6E6")
    make_redstone_sprite(main_spr_dir / "icon_redstone_ignored.png",  "#777777")
    make_redstone_sprite(main_spr_dir / "icon_redstone_active.png",   "#E03030")
    make_redstone_sprite(main_spr_dir / "icon_redstone_inactive.png", "#802020")
    make_fixed_button(main_spr_dir / "button_small.png",       8, "#8B8B8B")
    make_fixed_button(main_spr_dir / "button_small_hover.png", 8, "#A0A0A0")
    make_play_icon(main_spr_dir / "icon_play.png", "#E6E6E6")
    make_pause_icon(main_spr_dir / "icon_pause.png", "#E6E6E6")
    make_drain_icon(main_spr_dir / "icon_drain.png", "#E6E6E6")
    make_prev_icon(main_spr_dir / "icon_prev.png", "#E6E6E6")
    make_next_icon(main_spr_dir / "icon_next.png", "#E6E6E6")
    print(f"Main sprites saved:  {main_spr_dir}")

    make_arrow_toggle_sprite(admin_spr_dir / "arrow_toggle.png",       12, False)
    make_arrow_toggle_sprite(admin_spr_dir / "arrow_toggle_hover.png", 12, True)
    print(f"Admin sprites saved: {admin_spr_dir}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out-dir", default=None,
                        help="Override target textures/gui directory (for testing).")
    args = parser.parse_args()

    if args.out_dir:
        tex_dir = Path(args.out_dir).resolve()
    else:
        script_dir = Path(__file__).resolve().parent
        tex_dir = (script_dir / ".." / "src" / "main" / "resources"
                   / "assets" / "fluidconverter" / "textures" / "gui").resolve()
    generate_all(tex_dir)


if __name__ == "__main__":
    main()
