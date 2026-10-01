@echo off
REM URMIX post-auth push helper — run AFTER GitHub auth is restored
REM (gh auth login -h github.com, or browser sign-in as winatra).
REM Sequential: push main, push tag v1.0.0, verify remote refs.
REM No Gradle builds are executed by this script.
setlocal
cd /d "%~dp0"
echo === [1/3] git push urmix main ===
git push urmix main
if errorlevel 1 (
  echo FAILED: push main. Check auth (gh auth status) and retry.
  exit /b 1
)
echo === [2/3] git push urmix v1.0.0 ===
git push urmix v1.0.0
if errorlevel 1 (
  echo FAILED: push tag v1.0.0. Check auth and retry.
  exit /b 2
)
echo === [3/3] git ls-remote urmix (expect main + v1.0.0) ===
git ls-remote urmix
echo DONE: both refs should list the same commit (d932f439b).
endlocal
