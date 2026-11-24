#!/usr/bin/env python3
"""Capture the README screenshots automatically.

Launches Picasso on a connected device/emulator, taps through every screen by
reading the on-screen UI (uiautomator) and saves PNGs to assets/screenshots/.
No manual navigation: just run it.

    ./gradlew installDebug      # optional, the script installs if missing
    python3 scripts/screenshots.py [--serial emulator-5554] [--install]

Notes
  * It runs `pm clear` first, so the app starts from a clean first-run state
    (library empty, tutorial pending). Do not run it on a device whose
    Picasso data you want to keep.
  * The Trace shots show whatever the camera sees. A real device pointed at a
    desk looks best; the emulator shows its virtual scene.
"""
from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PKG = "app.picasso.tracer"
ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "assets" / "screenshots"

SERIAL: str | None = None


# --------------------------------------------------------------------------- adb
def adb(*args: str, check: bool = True, text: bool = True) -> str | bytes:
    cmd = ["adb"] + (["-s", SERIAL] if SERIAL else []) + list(args)
    res = subprocess.run(cmd, capture_output=True, text=text)
    if check and res.returncode != 0:
        err = res.stderr if text else res.stderr.decode(errors="replace")
        sys.exit(f"adb {' '.join(args)} failed: {err.strip()}")
    return res.stdout


def shell(*args: str, check: bool = False) -> str:
    return str(adb("shell", *args, check=check)).strip()


# ------------------------------------------------------------------- UI helpers
class Node:
    def __init__(self, text: str, desc: str, bounds: tuple[int, int, int, int]):
        self.text, self.desc, self.bounds = text, desc, bounds

    @property
    def center(self) -> tuple[int, int]:
        l, t, r, b = self.bounds
        return (l + r) // 2, (t + b) // 2

    def __repr__(self) -> str:  # for debugging
        return f"Node(text={self.text!r}, desc={self.desc!r}, bounds={self.bounds})"


_BOUNDS = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")


def parse_nodes(xml: str) -> list[Node]:
    nodes: list[Node] = []
    try:
        root = ET.fromstring(xml)
    except ET.ParseError:
        return nodes
    for el in root.iter("node"):
        text = el.get("text", "") or ""
        desc = el.get("content-desc", "") or ""
        if not text and not desc:
            continue
        m = _BOUNDS.fullmatch(el.get("bounds", ""))
        if not m:
            continue
        l, t, r, b = map(int, m.groups())
        if r <= l or b <= t:
            continue
        nodes.append(Node(text.strip(), desc.strip(), (l, t, r, b)))
    return nodes


def dump_ui() -> list[Node]:
    # uiautomator can fail with "could not get idle state" while something
    # animates (progress spinners, camera). Retry a few times.
    for _ in range(4):
        out = shell("uiautomator", "dump", "/sdcard/picasso_ui.xml")
        if "dumped" in out.lower():
            xml = str(adb("exec-out", "cat", "/sdcard/picasso_ui.xml", check=False))
            nodes = parse_nodes(xml)
            if nodes:
                return nodes
        time.sleep(0.6)
    return []


def find(nodes: list[Node], label: str) -> Node | None:
    """Exact (case-sensitive) match on text/content-desc, then case-insensitive."""
    for n in nodes:
        if n.text == label or n.desc == label:
            return n
    low = label.casefold()
    for n in nodes:
        if n.text.casefold() == low or n.desc.casefold() == low:
            return n
    return None


def wait_for(label: str, timeout: float = 20.0, required: bool = True) -> Node | None:
    end = time.time() + timeout
    while time.time() < end:
        node = find(dump_ui(), label)
        if node:
            return node
        time.sleep(0.4)
    if required:
        sys.exit(
            f"Timed out waiting for '{label}' on screen. Last screen contents:\n  "
            + "\n  ".join(f"{n.text or n.desc}" for n in dump_ui()[:30])
        )
    return None


def tap(label: str, timeout: float = 20.0) -> None:
    node = wait_for(label, timeout)
    assert node
    x, y = node.center
    shell("input", "tap", str(x), str(y))
    time.sleep(0.8)


def back() -> None:
    shell("input", "keyevent", "KEYCODE_BACK")
    time.sleep(0.8)


def screenshot(name: str, settle: float = 1.0) -> None:
    time.sleep(settle)
    OUT.mkdir(parents=True, exist_ok=True)
    data = adb("exec-out", "screencap", "-p", text=False)
    assert isinstance(data, bytes)
    if len(data) < 1000:
        sys.exit(f"Screenshot {name} came back empty.")
    (OUT / f"{name}.png").write_bytes(data)
    print(f"  saved assets/screenshots/{name}.png")


# --------------------------------------------------------------------- device
def preflight(install: bool) -> None:
    if shutil.which("adb") is None:
        sys.exit("adb not found. Install Android platform-tools and add it to PATH.")
    devices = [
        l.split()[0]
        for l in str(adb("devices")).splitlines()[1:]
        if l.strip().endswith("device")
    ]
    if not devices:
        sys.exit("No device attached. Start an emulator or plug in a phone (adb devices).")
    if len(devices) > 1 and not SERIAL:
        sys.exit(f"Several devices attached {devices}; pass --serial <id>.")

    installed = PKG in shell("pm", "list", "packages", PKG)
    if install or not installed:
        print("Installing debug build (./gradlew installDebug)...")
        gradlew = "gradlew.bat" if sys.platform.startswith("win") else "./gradlew"
        env = {**os.environ, "ANDROID_SERIAL": SERIAL} if SERIAL else None
        res = subprocess.run([gradlew, "installDebug"], cwd=ROOT, env=env)
        if res.returncode != 0:
            sys.exit("Gradle install failed.")


def prepare_device() -> dict[str, str]:
    saved: dict[str, str] = {}
    for key in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
        saved[key] = shell("settings", "get", "global", key)
        shell("settings", "put", "global", key, "0")  # no animation waits

    shell("input", "keyevent", "KEYCODE_WAKEUP")
    shell("wm", "dismiss-keyguard")
    shell("settings", "put", "system", "accelerometer_rotation", "0")
    shell("settings", "put", "system", "user_rotation", "0")

    # Clean status bar (demo mode).
    shell("settings", "put", "global", "sysui_demo_allowed", "1")
    for extra in (
        ["-e", "command", "enter"],
        ["-e", "command", "clock", "-e", "hhmm", "1200"],
        ["-e", "command", "battery", "-e", "level", "100", "-e", "plugged", "false"],
        ["-e", "command", "network", "-e", "wifi", "show", "-e", "level", "4"],
        ["-e", "command", "notifications", "-e", "visible", "false"],
    ):
        shell("am", "broadcast", "-a", "com.android.systemui.demo", *extra)
    return saved


def restore_device(saved: dict[str, str]) -> None:
    shell("am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "exit")
    for key, val in saved.items():
        shell("settings", "put", "global", key, val if val not in ("", "null") else "1.0")


def fresh_start() -> None:
    shell("am", "force-stop", PKG)
    shell("pm", "clear", PKG)
    for perm in ("CAMERA", "RECORD_AUDIO"):
        shell("pm", "grant", PKG, f"android.permission.{perm}")
    shell("monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1")


# ------------------------------------------------------------------------ main
def main() -> None:
    global SERIAL
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
    ap.add_argument("--serial", help="adb device serial (needed if several are attached)")
    ap.add_argument("--install", action="store_true", help="always run ./gradlew installDebug first")
    args = ap.parse_args()
    SERIAL = args.serial

    preflight(args.install)
    saved = prepare_device()
    try:
        print("Launching Picasso from a clean state...")
        fresh_start()

        # Sample images are copied on first launch, so allow extra time.
        wait_for("Try a sample", timeout=40)
        wait_for("Rose", timeout=40)

        # Open the Rose sample: creates a project and goes to the Trace screen.
        tap("Rose")
        wait_for("Tools", timeout=30)

        # First-run tutorial overlay: dismiss it so it doesn't cover the shot.
        skip = wait_for("Skip", timeout=5, required=False)
        if skip:
            tap("Skip")
        time.sleep(2.0)  # let the camera preview and overlay render

        if wait_for("Camera unavailable", timeout=0.1, required=False):
            print("  ! The camera is unavailable on this device/emulator; the Trace shots will show an error.")

        print("Capturing Trace and Tools...")
        screenshot("02-trace")

        tap("Tools")
        wait_for("Guides")           # tab row is visible
        screenshot("03-tools-image")  # Image tab is the default

        tap("Guides")
        wait_for("Training wheels")  # matches the section label case-insensitively
        screenshot("04-tools-guides")

        tap("Outline")                # training wheels -> Outline level
        wait_for("Just where the edges are.", timeout=10)
        screenshot("05-training", settle=2.0)  # outline filter needs a moment

        # Close the sheet, leave Trace, back on Home with a project in the library.
        back()
        back()
        wait_for("Settings", timeout=15)
        print("Capturing Home and Settings...")
        screenshot("01-home")

        tap("Settings")
        wait_for("Appearance")
        screenshot("06-settings")
    finally:
        restore_device(saved)

    print(f"\nDone. {len(list(OUT.glob('*.png')))} images in {OUT.relative_to(ROOT)}. Review them, then commit.")


if __name__ == "__main__":
    main()
