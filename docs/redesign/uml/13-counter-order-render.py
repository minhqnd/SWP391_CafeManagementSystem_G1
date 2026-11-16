"""Render Counter Order using the Helvetica and blue swimlane style of the reference."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


SCALE = 3
WIDTH, HEIGHT = 1160, 1240
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


def connector(x, y):
    bounds = [scaled((x - 8, y - 8)), scaled((x + 8, y + 8))]
    draw.ellipse(bounds, fill="white", outline=LINE, width=SCALE)
    centered(x, y, "A", label_font)


def dashed_horizontal(left, right, y):
    for x in range(left, right, 9):
        line((x, y), (min(x + 5, right), y))


centered(WIDTH / 2, 17, "Counter Order — Cash, VietQR and Handoff", title_font)
lanes = [(28, 303, "Cashier"), (303, 598, "CMS (Aura Cafe Server)"),
         (598, 899, "Payment Gateway (Cloudflare Worker)"), (899, 1132, "Barista")]
for left, right, name in lanes:
    draw.rectangle([scaled((left, 34)), scaled((right, 54))],
                   fill=BLUE, outline=LINE, width=SCALE)
    centered((left + right) / 2, 44, name, lane_font)
    line((left, 54), (left, 1210))
line((28, 1210), (1132, 1210), (1132, 54))

cashier, cms, gateway, barista = 169, 450, 750, 1016

# Build the order. Member lookup is optional.
terminal(cashier, 68)
arrow((cashier, 76), (cashier, 86))
activity(cashier, 105, 229, 38, "Add products and options to the cart\nChoose Dine-in or Takeaway")
arrow((cashier, 124), (cashier, 138))
activity(cashier, 157, 229, 38, "If requested, look up member by phone\nand apply available points")
dashed_horizontal(284, 325, 157)
activity(cms, 157, 250, 38, "Return available member points")
arrow((cashier, 176), (cashier, 190))
decision(cashier, 202, 140, "Payment method?")

# Cash skips the Gateway. Connector A joins the paid branches.
arrow((cashier, 214), (cashier, 228))
label(178, 216, "Cash (default)")
activity(cashier, 241, 214, 26, "Collect cash and confirm payment")
arrow((276, 241), (cms, 241), (cms, 252))
activity(cms, 277, 260, 50, "Validate prices and cap points used\nSave order CONFIRMED, payment SUCCESS\nDeduct redeemed points if used")
arrow((cms, 302), (cms, 315))
connector(cms, 323)

# VietQR creates a pending order and bank payment before the customer can scan.
line((239, 202), (289, 202), (289, 237))
arrow((289, 245), (289, 344), (cms, 344), (cms, 353))
label(244, 185, "VietQR")
activity(cms, 378, 260, 50, "Validate prices and cap points used\nSave order and payment as PENDING\nDeduct redeemed points if used")
arrow((580, 378), (gateway, 378), (gateway, 395))
activity(gateway, 408, 236, 26, "Register order and amount in D1")
arrow((gateway, 421), (gateway, 433), (cashier, 433), (cashier, 446))
activity(cashier, 459, 238, 26, "Show VietQR for customer to scan")

# Webhook is asynchronous. It writes status; polling reads it independently.
dashed_horizontal(288, 624, 459)
arrow((624, 459), (632, 459))
activity(gateway, 459, 236, 26, "Receive SePay webhook after transfer")
arrow((gateway, 472), (gateway, 481))
activity(gateway, 506, 245, 50, "Match order code and sum transfers\nBelow amount due: PENDING\nAt least amount due: SUCCESS")

arrow((cashier, 472), (cashier, 516))
activity(cashier, 529, 227, 26, "POS polls CMS every 2 seconds")
arrow((282.5, 529), (cms, 529), (cms, 542))
activity(cms, 555, 242, 26, "Query Gateway by order code")
arrow((571, 555), (gateway, 555), (gateway, 570))
activity(gateway, 583, 230, 26, "Return stored payment status")
arrow((gateway, 596), (gateway, 608), (cms, 608), (cms, 621))
decision(cms, 633, 153, "Gateway reports SUCCESS?")

# The pending path loops on the cashier side, clear of the cancellation and paid exits.
arrow((373.5, 633), (246, 633))
label(279, 616, "PENDING")
decision(cashier, 633, 154, "Keep QR open?")
arrow((92, 633), (41, 633), (41, 529), (55.5, 529))
label(51, 615, "yes")
arrow((cashier, 645), (cashier, 661))
label(177, 645, "no")
activity(cashier, 680, 226, 38, "Close QR or switch to Cash\nStop polling")
arrow((cashier, 699), (cashier, 756), (cms, 756), (cms, 767))
activity(cms, 786, 257, 38, "Cancel unpaid QR order\nRestore redeemed points")
arrow((cms, 805), (cms, 817))
terminal(cms, 825, end=True)
label(56, 711, "Switching to Cash starts a new cash order.")

arrow((cms, 645), (cms, 674))
label(460, 649, "SUCCESS")
activity(cms, 693, 248, 38, "Save bank payment SUCCESS\nSet order CONFIRMED")
arrow((cms, 712), (cms, 725))
connector(cms, 733)

# Confirmed orders are available to Barista whether the cashier opens the receipt or not.
connector(cms, 855)
label(465, 847, "Payment confirmed")
arrow((cms, 863), (cms, 873))
activity(cms, 892, 260, 38, "Award eligible member points once\nShow paid order in Barista queue")
dashed_horizontal(286, 320, 892)
activity(cashier, 892, 234, 38, "Optional: open or print receipt")
arrow((cms, 911), (cms, 925), (cashier, 925), (cashier, 938))
decision(cashier, 950, 182, "Cancel before preparation?")
arrow((cashier, 962), (cashier, 976), (430, 976), (430, 989))
label(176, 961, "yes")
activity(435, 1002, 248, 26, "Set CANCELLED and reverse points")
arrow((430, 1015), (430, 1025))
terminal(430, 1033, end=True)

arrow((260, 950), (barista, 950), (barista, 973))
label(269, 932, "no")
activity(barista, 986, 168, 26, "Start preparation")
arrow((barista, 999), (barista, 1046), (483, 1046), (483, 1055))
activity(cms, 1068, 246, 26, "Check and deduct recipe ingredients")
arrow((573, 1068), (barista, 1068), (barista, 1080))
activity(barista, 1093, 195, 26, "Prepare items and mark READY")
arrow((barista, 1106), (barista, 1118), (cashier, 1118), (cashier, 1130))
activity(cashier, 1143, 237, 26, "Hand over order and mark complete")
arrow((287.5, 1143), (cms, 1143), (cms, 1154))
activity(cms, 1167, 218, 26, "Set order COMPLETED")
arrow((cms, 1180), (cms, 1192))
terminal(cms, 1200, end=True)

output = Path(__file__).with_name("13-counter-order-activity.png")
image.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS).save(output)
