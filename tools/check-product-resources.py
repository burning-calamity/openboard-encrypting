#!/usr/bin/env python3
"""Fail CI when the product name/tutorial or Japanese layout wiring regresses."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
FULL_NAME = (
    "ars per occultam scripturam animi voluntatem absentibus"
)

def fail(message):
    print(f"product resource check failed: {message}", file=sys.stderr)
    raise SystemExit(1)

strings = ET.parse(ROOT / "app/src/main/res/values/strings.xml").getroot()
name = next((item.text for item in strings.findall("string")
             if item.attrib.get("name") == "english_ime_name"), None)
if name != FULL_NAME:
    fail("english_ime_name does not contain the required full title")

manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
if 'android:label="@string/english_ime_name"' not in manifest:
    fail("the application is not labelled with english_ime_name")

setup = (ROOT / "app/src/main/java/org/dslul/openboard/inputmethod/latin/setup/SetupWizardActivity.java").read_text(encoding="utf-8")
for required in ("getApplicationInfo().labelRes", "setup_welcome_title, applicationName",
                 "setup_steps_title, applicationName"):
    if required not in setup:
        fail(f"setup tutorial no longer displays the application title: {required}")

resources = ROOT / "app/src/main/res"
for relative in (
    "xml/keyboard_layout_set_japanese_kana.xml", "xml/kbd_japanese_kana.xml",
    "xml/rows_japanese_kana.xml", "xml/rowkeys_japanese_kana1.xml",
    "xml/rowkeys_japanese_kana2.xml", "xml/rowkeys_japanese_kana3.xml",
    "xml/rowkeys_japanese_kana4.xml",
):
    ET.parse(resources / relative)

subtypes = (resources / "values/predefined-subtypes.xml").read_text(encoding="utf-8")
layout_map = (resources / "values/donottranslate.xml").read_text(encoding="utf-8")
if "ja:japanese_kana:EmojiCapable" not in subtypes:
    fail("Japanese Kana is not a predefined Japanese subtype")
if "<item>ja:SupportTouchPositionCorrection,EmojiCapable</item>" not in layout_map:
    fail("Japanese locale is not mapped to the Kana layout")

suggestion_strip_path = ROOT / (
    "app/src/main/java/org/dslul/openboard/inputmethod/latin/suggestions/"
    "SuggestionStripView.java"
)
suggestion_strip = suggestion_strip_path.read_text(encoding="utf-8")
if "import org.dslul.openboard.inputmethod.latin.ciphers.AesGcmCipher" in suggestion_strip:
    fail("API-21-only AES-GCM must not be linked from the API-19 startup view")
if "new AesGcmCipher" in suggestion_strip:
    fail("AES-GCM must be loaded reflectively after the API-level check")

print("Product name, setup tutorial, and Japanese Kana resources are valid.")
