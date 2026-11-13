@echo off
REM ============================================================
REM  Dev monitor launcher - starts BOTH:
REM   1) scrcpy window  : live phone mirror (smooth, clickable)
REM   2) web dashboard  : http://127.0.0.1:8123  (logs + buttons)
REM  Double-click to run. Ctrl+C in console windows to stop.
REM ============================================================
set PY=C:\Users\Administrator\.workbuddy\binaries\python\versions\3.13.12\python.exe
cd /d %~dp0..
start "scrcpy-mirror" "%~dp0scrcpy\scrcpy.exe"
start "dashboard" "%PY%" "%~dp0dashboard.py"
timeout /t 2 >nul
start http://127.0.0.1:8123
