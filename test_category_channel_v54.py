import os
import sys
import time
import json
import urllib.request
import subprocess

REPO = "liujiawei-zuopin/apple-tvbox"
COMMIT_SHA = "478945c"
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
    print(f"[Screenshot] Captured {name} -> {local_path}")
    return local_path

def get_latest_run():
    url = f"https://api.github.com/repos/{REPO}/actions/runs?per_page=5"
    req = urllib.request.Request(url, headers={"User-Agent": "Antigravity-Agent"})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            runs = data.get("workflow_runs", [])
            for r in runs:
                if COMMIT_SHA in r.get("head_sha", "") or r.get("head_commit", {}).get("id", "").startswith(COMMIT_SHA):
                    return r
            if runs:
                return runs[0]
    except Exception as e:
        print(f"Error checking runs: {e}")
    return None

def get_latest_release():
    url = f"https://api.github.com/repos/{REPO}/releases/latest"
    req = urllib.request.Request(url, headers={"User-Agent": "Antigravity-Agent"})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode('utf-8'))
    except Exception as e:
        print(f"Error checking release: {e}")
    return None

def download_file(url, out_path):
    print(f"Downloading from {url} to {out_path}...")
    headers = {"User-Agent": "Mozilla/5.0"}
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=60) as resp, open(out_path, 'wb') as f:
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
    print("\nDownload complete!")

def wait_for_build_and_install():
    print(f"Polling GitHub Actions for commit {COMMIT_SHA}...")
    target_apk_url = None
    target_tag = None

    for i in range(40):
        run = get_latest_run()
        if run:
            status = run.get("status")
            conclusion = run.get("conclusion")
            print(f"[{i*15}s] CI Run ID: {run.get('id')}, Status: {status}, Conclusion: {conclusion}")
            if status == "completed":
                if conclusion == "success":
                    print("CI Build succeeded! Checking latest release...")
                    break
                else:
                    print(f"CI Build finished with conclusion: {conclusion}")
                    break
        time.sleep(15)

    # Check release assets
    time.sleep(5)
    rel = get_latest_release()
    if rel:
        target_tag = rel.get("tag_name")
        print(f"Latest Release: {target_tag}")
        for asset in rel.get("assets", []):
            name = asset.get("name", "")
            if "leanback" in name.lower() and name.endswith(".apk") and "arm64" not in name.lower():
                target_apk_url = asset.get("browser_download_url")
                break
            elif "leanback" in name.lower() and name.endswith(".apk"):
                target_apk_url = asset.get("browser_download_url")

    if not target_apk_url:
        print("Could not find leanback apk in latest release. Trying arm/universal...")
        if rel:
            for asset in rel.get("assets", []):
                if asset.get("name", "").endswith(".apk"):
                    target_apk_url = asset.get("browser_download_url")
                    break

    if not target_apk_url:
        print("Failed to find APK URL.")
        return False

    apk_name = f"apple_tvbox_{target_tag}_{COMMIT_SHA}.apk"
    apk_path = os.path.join(DOWNLOAD_DIR, apk_name)
    download_file(target_apk_url, apk_path)

    # Install via ADB
    print(f"Connecting to ADB device {DEVICE}...")
    adb_cmd(["connect", DEVICE])
    print(f"Installing {apk_path}...")
    install_res = adb_cmd(["install", "-r", "-d", apk_path])
    print(f"Install result: {install_res}")
    return True

def run_regression_tests():
    print("Starting regression tests on MuMu emulator...")
    adb_cmd(["connect", DEVICE])
    
    # 1. Stop and relaunch app
    adb_cmd(["shell", "am", "force-stop", PACKAGE])
    time.sleep(1)
    adb_cmd(["shell", "am", "start", "-n", ACTIVITY])
    time.sleep(4)

    # Capture 01: Home Screen
    take_screenshot("01_home_screen_clean_boot")

    # Move down to Watch Now shelf card 0
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("02_home_watch_now_focused")

    # Move back up to top nav bar
    adb_key(19) # DPAD_UP
    time.sleep(0.6)
    take_screenshot("03_home_top_nav_bar_focused")

    # Move RIGHT to "电影" (Movie) Tab
    adb_key(22) # DPAD_RIGHT
    time.sleep(2.5)
    take_screenshot("04_category_movie_emerald_hero_screen")

    # Move DOWN into Movie Hero Play button
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("05_category_movie_play_button_focused")

    # Move DOWN into "推荐" Shelf Card 0
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("06_category_movie_recommend_shelf_focused")

    # Move DOWN into Sub-genre Shelf 1 ("动作精选" / "热播精选")
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("07_category_movie_subgenre1_shelf_focused")

    # Move DOWN into Sub-genre Shelf 2 / "全部影片"
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("08_category_movie_subgenre2_or_all_focused")

    # Move DOWN into "全部影片" 5-Column Grid (scrolling down page)
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("09_category_movie_all_grid_scrolled_topbar_collapsed")

    # Step-back UP back to top
    adb_key(19) # DPAD_UP
    time.sleep(0.5)
    adb_key(19) # DPAD_UP
    time.sleep(0.5)
    adb_key(19) # DPAD_UP
    time.sleep(0.5)
    adb_key(19) # DPAD_UP
    time.sleep(0.5)
    take_screenshot("10_category_movie_back_to_hero_topbar_restored")

    # Move back UP to Top Nav Bar
    adb_key(19) # DPAD_UP
    time.sleep(0.6)

    # Move RIGHT to "剧集" (TV Series) Tab
    adb_key(22) # DPAD_RIGHT
    time.sleep(2.5)
    take_screenshot("11_category_tv_sapphire_ambient_screen")

    # Move DOWN to Play Button in TV tab
    adb_key(20) # DPAD_DOWN
    time.sleep(0.8)
    take_screenshot("12_category_tv_play_button_focused")

    # Move back UP to Top Nav Bar
    adb_key(19) # DPAD_UP
    time.sleep(0.6)

    # Move RIGHT to "综艺" (Variety) Tab
    adb_key(22) # DPAD_RIGHT
    time.sleep(2.5)
    take_screenshot("13_category_variety_amethyst_ambient_screen")

    # Move back LEFT to Home Tab
    adb_key(21) # DPAD_LEFT
    time.sleep(0.5)
    adb_key(21) # DPAD_LEFT
    time.sleep(0.5)
    adb_key(21) # DPAD_LEFT
    time.sleep(1.0)
    take_screenshot("14_back_to_home_tab")

    print("\nAll regression tests completed successfully!")

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--test-only":
        run_regression_tests()
    else:
        success = wait_for_build_and_install()
        if success:
            run_regression_tests()
