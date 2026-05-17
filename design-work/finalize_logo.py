"""Downscale rendered 1024x1024 logo to a crisp 512x512 with high-quality resampling."""
from PIL import Image
from pathlib import Path

SRC = Path(__file__).parent / "logo_rendered.png"
DEST_512 = Path(__file__).parent / "logo_512.png"
DEST_INSTALL = Path(__file__).parent.parent / "src" / "main" / "resources" / "logo.png"

img = Image.open(SRC).convert("RGBA")
print(f"Source: {img.size}")

scaled = img.resize((512, 512), Image.Resampling.LANCZOS)
scaled.save(DEST_512)
scaled.save(DEST_INSTALL)
print(f"Saved {DEST_512}")
print(f"Installed -> {DEST_INSTALL}")
