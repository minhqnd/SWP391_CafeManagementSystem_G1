"""Render BF-08 using the Helvetica and blue swimlane style of the reference."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


SCALE = 3
WIDTH, HEIGHT = 1060, 730
BLUE = (122, 207, 245)
LINE = (30, 52, 61)
BLACK = (0, 0, 0)
FONT = "/System/Library/Fonts/Helvetica.ttc"

image = Image.new("RGB", (WIDTH * SCALE, HEIGHT * SCALE), "white")
draw = ImageDraw.Draw(image)
title_font = ImageFont.truetype(FONT, 13 * SCALE)
lane_font = ImageFont.truetype(FONT, 12 * SCALE)
body_font = ImageFont.truetype(FONT, round(10.5 * SCALE))
label_font = ImageFont.truetype(FONT, 9 * SCALE)


def scaled(point):
    return tuple(round(value * SCALE) for value in point)


def line(*points):
    draw.line([scaled(point) for point in points], fill=LINE, width=SCALE, joint="curve")


def arrow(*points):
    line(*points)
    x, y = points[-1]
    prev_x, prev_y = points[-2]
    dx = (x > prev_x) - (x < prev_x)
    dy = (y > prev_y) - (y < prev_y)
    tip = [(x, y), (x - 5 * dx - 3 * dy, y - 5 * dy + 3 * dx),
           (x - 5 * dx + 3 * dy, y - 5 * dy - 3 * dx)]
    draw.polygon([scaled(point) for point in tip], fill=LINE)


def centered(x, y, text, font=body_font):
    lines = text.splitlines()
    for index, text_line in enumerate(lines):
        left, top, right, bottom = draw.textbbox((0, 0), text_line, font=font)
        assert right - left < WIDTH * SCALE, text_line
        center_y = y + (index - (len(lines) - 1) / 2) * 14
        draw.text((x * SCALE - (left + right) / 2,
                   center_y * SCALE - (top + bottom) / 2),
                  text_line, font=font, fill=BLACK)


def label(x, y, text):
    draw.text(scaled((x, y)), text, font=label_font, fill=BLACK)


def activity(x, y, width, height, text):
    assert all(draw.textlength(part, font=body_font) <= (width - 12) * SCALE
               for part in text.splitlines()), text
    bounds = [scaled((x - width / 2, y - height / 2)),
              scaled((x + width / 2, y + height / 2))]
    draw.rounded_rectangle(bounds, radius=8 * SCALE, fill=BLUE, outline=LINE, width=SCALE)
    centered(x, y, text)


def decision(x, y, width, text):
    assert draw.textlength(text, font=label_font) <= (width - 16) * SCALE, text
    left, right = x - width / 2, x + width / 2
    points = [(left, y), (left + 8, y - 12), (right - 8, y - 12),
              (right, y), (right - 8, y + 12), (left + 8, y + 12)]
    draw.polygon([scaled(point) for point in points], fill=BLUE)
    line(*points, points[0])
    centered(x, y, text, label_font)


def terminal(x, y, *, end=False):
    bounds = [scaled((x - 8, y - 8)), scaled((x + 8, y + 8))]
    draw.ellipse(bounds, fill="white" if end else BLACK, outline=BLACK, width=SCALE)
    if end:
        draw.ellipse([scaled((x - 4.5, y - 4.5)), scaled((x + 4.5, y + 4.5))], fill=BLACK)


centered(WIDTH / 2, 17, "BF-08 Ingredient Restock and Purchasing", title_font)
lanes = [(40, 270, "Barista"), (270, 635, "Admin"), (635, 1020, "CMS (Aura Cafe Server)")]
for left, right, name in lanes:
    draw.rectangle([scaled((left, 32)), scaled((right, 50))],
                   fill=BLUE, outline=LINE, width=SCALE)
    centered((left + right) / 2, 41, name, lane_font)
    line((left, 50), (left, 698))
line((40, 698), (1020, 698), (1020, 50))

barista, admin, cms = 155, 454, 815

terminal(admin, 65)
arrow((admin, 73), (admin, 84))
activity(admin, 104, 278, 40, "Review stock and movement history\nUpdate supplier or ingredient if needed")
arrow((admin, 124), (admin, 137))
decision(admin, 149, 162, "Use a restock request?")

arrow((373, 149), (barista, 149), (barista, 170))
label(339, 132, "yes")
activity(barista, 190, 187, 40, "Submit a restock request\n(Admin can also submit)")
arrow((barista, 210), (barista, 221), (cms, 221), (cms, 235))
activity(cms, 255, 267, 40, "Validate ingredient and quantity\nSave as PENDING_APPROVAL")
arrow((cms, 275), (cms, 289), (admin, 289), (admin, 303))
decision(admin, 315, 170, "Approve & create PO?")
arrow((539, 315), (951, 315), (951, 339))
label(545, 298, "no")
activity(951, 352, 127, 26, "Save as REJECTED")
arrow((951, 365), (951, 378))
terminal(951, 386, end=True)
arrow((admin, 327), (admin, 340), (766, 340), (766, 357))
label(463, 327, "yes")
activity(766, 377, 238, 40, "Save as APPROVED and open PO form\nwith the request selected")

# Keep the independent route clear where it crosses the request workflow.
line((535, 149), (605, 149), (605, 217))
for top, bottom in [(225, 285), (293, 311), (319, 336), (344, 412)]:
    line((605, top), (605, bottom))
line((766, 397), (766, 412), (admin, 412))
line((605, 412), (admin, 412))
arrow((admin, 412), (admin, 425))
label(542, 132, "no")
activity(admin, 445, 281, 40, "Select supplier and enter ingredients,\nquantities and lot totals")
arrow((admin, 465), (admin, 479))
activity(admin, 499, 276, 40, "Place the order now or save as DRAFT\nand place it later")
arrow((592, 499), (681, 499))
activity(790, 499, 218, 26, "Save PO as ORDERED")
arrow((790, 512), (790, 525), (admin, 525), (admin, 537))
activity(admin, 550, 258, 26, "Enter delivered and accepted quantities")
arrow((583, 550), (790, 550), (790, 561))
decision(790, 573, 151, "PO and receipt valid?")
arrow((865.5, 573), (950, 573), (950, 591))
label(872, 556, "no")
activity(950, 611, 126, 40, "Reject duplicate or\ninvalid receipt")
arrow((950, 631), (950, 645))
terminal(950, 653, end=True)
arrow((790, 585), (790, 591))
label(808, 583, "yes")
activity(780, 622, 206, 62, "Commit stock, IN_PURCHASE & PO\nFull → RECEIVED\nPartial → PARTIALLY_RECEIVED\nNone accepted → ORDERED")
arrow((677, 625), (454, 625))
decision(373, 625, 162, "Expect another delivery?")
arrow((292, 625), (290, 625), (290, 550), (325, 550))
label(295, 607, "yes")
arrow((373, 637), (373, 652))
label(386, 636, "no / close")
activity(444, 665, 277, 26, "Review stock and movement history")
arrow((373, 678), (373, 681))
terminal(373, 689, end=True)

output = Path(__file__).with_name("12-ingredient-restock-and-purchasing-activity.png")
image.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS).save(output)
