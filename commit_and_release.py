#!/usr/bin/env python3
"""
SoundBlocker - Build and Release Script
"""
import subprocess
import os
import sys

def run(cmd):
    print(f">> {cmd}")
    res = subprocess.run(cmd, shell=True)
    if res.returncode != 0:
        print(f"Error: Command failed with code {res.returncode}")
        sys.exit(res.returncode)

def main():
    repo_dir = r"C:\Users\n1150\.gemini\antigravity\scratch\SoundBlocker"
    os.chdir(repo_dir)

    print("1. Building Debug APK...")
    gradle_cmd = r'cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr && gradlew.bat assembleDebug"'
    run(gradle_cmd)

    src_apk = os.path.join(repo_dir, "app", "build", "outputs", "apk", "debug", "app-debug.apk")
    dst_apk = os.path.join(repo_dir, "SoundBlocker-v1.0.0.apk")
    if os.path.exists(src_apk):
        import shutil
        shutil.copy2(src_apk, dst_apk)
        print(f"Copied APK to {dst_apk}")

    print("Done!")

if __name__ == "__main__":
    main()
