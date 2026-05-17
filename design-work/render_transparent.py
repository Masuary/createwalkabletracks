"""Render logo_transparent.html with alpha background at 512x512."""
from playwright.sync_api import sync_playwright
from pathlib import Path

OUT = Path(__file__).parent / "logo_transparent.png"
URL = "http://localhost:8765/logo_transparent.html"
SIZE = 512

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(viewport={"width": SIZE, "height": SIZE}, device_scale_factor=1)
    page = context.new_page()
    page.goto(URL)
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(800)
    page.screenshot(path=str(OUT), full_page=False, omit_background=True)
    browser.close()

print(f"Saved {OUT}")
