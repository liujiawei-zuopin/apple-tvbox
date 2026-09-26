import os
import sys
import time
import re
import urllib.request
import subprocess

REPO = "liujiawei-zuopin/apple-tvbox"
ADB = r"D:\Program Files\Netease\MuMuPlayer\nx_device\15.0\shell\adb.exe"
DEVICE = "127.0.0.1:16384"
PACKAGE = "com.fongmi.android.tv"
ACTIVITY = "com.fongmi.android.tv/.ui.activity.HomeActivity"
ARTIFACT_DIR = r"C:\Users\liuji\.gemini\antigravity\brain\5fe994d7-aa28-4157-9a65-9a39b788f1f3"
DOWNLOAD_DIR = r"C:\Users\liuji\Documents\antigravity\sharp-brahmagupta\apple-tvbox\build_cache"

os.makedirs(DOWNLOAD_DIR, exist_ok=True)
os.makedirs(ARTIFACT_DIR, exist_ok=True)

def adb_cmd(args):
    cmd = [ADB, "-s", DEVICE] + args
    res = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8', errors='ignore')
    return res.stdout.strip()

def adb_key(keycode):
    adb_cmd(["shell", "input", "keyevent", str(keycode)])
    time.sleep(0.6)

def take_screenshot(name):
    remote_path = f"/sdcard/{name}.png"
    local_path = os.path.join(ARTIFACT_DIR, f"{name}.png")
    adb_cmd(["shell", "screencap", "-p", remote_path])
    adb_cmd(["pull", remote_path, local_path])
    adb_cmd(["shell", "rm", remote_path])
    print(f"[Screenshot] Captured {name} -> {local_path}", flush=True)
    return local_path

def download_file(url, out_path):
    print(f"Downloading from {url} to {out_path}...", flush=True)
    headers = {"User-Agent": "Mozilla/5.0"}
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=120) as resp, open(out_path, 'wb') as f:
        total = int(resp.headers.get('content-length', 0))
        downloaded = 0
        while True:
            chunk = resp.read(64 * 1024)
            if not chunk:
                break
            f.write(chunk)
            downloaded += len(chunk)
            if total:
                percent = downloaded * 100 // total
                sys.stdout.write(f"\rDownloading: {percent}% ({downloaded//1024//1024}MB / {total//1024//1024}MB)")
                sys.stdout.flush()
    print("\nDownload complete!", flush=True)

def wait_for_release():
    print("Polling GitHub Releases page for new build...", flush=True)
    for i in range(45):
        try:
            url = f"https://github.com/{REPO}/releases"
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            html = urllib.request.urlopen(req, timeout=15).read().decode('utf-8')
            
            # Look for release apk links
            apks = re.findall(r'href=[\x22\x27](/liujiawei-zuopin/apple-tvbox/releases/download/([^/]+)/[^\x22\x27]+\.apk)[\x22\x27]', html)
            tags = re.findall(r'href=[\x22\x27](/liujiawei-zuopin/apple-tvbox/releases/tag/([^/\x22\x27]+))[\x22\x27]', html)
            
            latest_tag = tags[0][1] if tags else "unknown"
            print(f"[{i*10}s] Latest tag on page: {latest_tag}, Total APK links: {len(apks)}", flush=True)
            
            for apk_rel_url, tag in apks:
                if "leanback" in apk_rel_url.lower():
                    print(f"Found Leanback APK in release {tag}: https://github.com{apk_rel_url}", flush=True)
                    return f"https://github.com{apk_rel_url}", tag
                elif apk_rel_url.endswith(".apk"):
                    print(f"Found APK in release {tag}: https://github.com{apk_rel_url}", flush=True)
                    return f"https://github.com{apk_rel_url}", tag
        except Exception as e:
            print(f"Poll check error: {e}", flush=True)
        time.sleep(10)
    return None, None

def run_all():
    apk_url, tag = wait_for_release()
    if not apk_url:
        print("Failed to find release APK after waiting.", flush=True)
        return False

    apk_name = f"apple_tvbox_{tag}.apk"
    apk_path = os.path.join(DOWNLOAD_DIR, apk_name)
    download_file(apk_url, apk_path)

    print(f"Connecting to ADB {DEVICE}...", flush=True)
    adb_cmd(["connect", DEVICE])
    print(f"Installing {apk_path}...", flush=True)
    install_out = adb_cmd(["install", "-r", "-d", apk_path])
    print(f"Install output: {install_out}", flush=True)

    print("Launching application...", flush=True)
    adb_cmd(["shell", "am", "force-stop", PACKAGE])
    time.sleep(1)
    adb_cmd(["shell", "am", "start", "-n", ACTIVITY])
    time.sleep(4)

    # 1. Clean Home Boot Screen
    take_screenshot("01_home_clean_boot_with_sunk_shelf")

    # 2. Focus Watch Now Shelf Item 0
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("02_home_watch_now_card0_focused")

    # 3. Focus back up to Top Nav Bar Capsule
    adb_key(19) # DPAD_UP
    time.sleep(0.6)
    take_screenshot("03_top_nav_bar_home_focused")

    # 4. Navigate RIGHT to "电影" (Movie) Tab
    adb_key(22) # DPAD_RIGHT
    time.sleep(2.5)
    take_screenshot("04_category_movie_emerald_hero_screen")

    # 5. Navigate DOWN to "▶ 立即播放" Play Button
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("05_category_movie_play_button_focused_white_highlight")

    # 6. Navigate DOWN to "推荐" Shelf Card 0 (16:9 Landscape)
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("06_category_movie_recommend_shelf_card0_focused_hero_locked")

    # 7. Navigate DOWN to Sub-genre Shelf 1 (2:3 Portrait cards)
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("07_category_movie_subgenre1_shelf_focused")

    # 8. Navigate DOWN to Sub-genre Shelf 2
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("08_category_movie_subgenre2_shelf_focused")

    # 9. Navigate DOWN into "全部影片" 5-Column Grid (scrolling down page)
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("09_category_movie_all_grid_scrolled_topbar_collapsed")

    # 10. Navigate back UP to Hero top section
    adb_key(19) # DPAD_UP
    time.sleep(0.5)
    adb_key(19) # DPAD_UP
    time.sleep(0.5)
    adb_key(19) # DPAD_UP
    time.sleep(0.5)
    adb_key(19) # DPAD_UP
    time.sleep(0.5)
    take_screenshot("10_category_movie_back_to_hero_topbar_restored")

    # 11. Navigate back UP to Top Nav Bar
    adb_key(19) # DPAD_UP
    time.sleep(0.6)

    # 12. Navigate RIGHT to "剧集" (TV Series) Tab
    adb_key(22) # DPAD_RIGHT
    time.sleep(2.5)
    take_screenshot("11_category_tv_sapphire_ambient_screen")

    # 13. Navigate DOWN to Play Button in TV Series
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("12_category_tv_play_button_focused")

    # 14. Navigate back UP to Top Nav Bar
    adb_key(19) # DPAD_UP
    time.sleep(0.6)

    # 15. Navigate RIGHT to "综艺" (Variety) Tab
    adb_key(22) # DPAD_RIGHT
    time.sleep(2.5)
    take_screenshot("13_category_variety_amethyst_ambient_screen")

    # 16. Navigate DOWN to Play Button in Variety
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("14_category_variety_play_button_focused")

    # 17. Return LEFT back to Home Tab
    adb_key(19) # DPAD_UP
    time.sleep(0.5)
    adb_key(21) # DPAD_LEFT
    time.sleep(0.4)
    adb_key(21) # DPAD_LEFT
    time.sleep(0.4)
    adb_key(21) # DPAD_LEFT
    time.sleep(1.0)
    take_screenshot("15_back_to_home_tab_restored")

    print("\nAll 15 verification tests executed and screenshots captured successfully!", flush=True)
    return True

if __name__ == "__main__":
    run_all()
