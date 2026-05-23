@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo   BOOTSTRAP NOT-FOR-TOMORROW (Windows)
echo ===================================================

where npm >nul 2>nul
if errorlevel 1 (
  echo Missing required command: npm
  exit /b 1
)

where python >nul 2>nul
if errorlevel 1 (
  echo Missing required command: python
  exit /b 1
)

echo [1/4] Installing root Node.js dependencies...
call npm install

echo [2/4] Installing FE dependencies...
pushd FE
call npm install
popd

echo [3/4] Installing Windows (Electron) dependencies...
pushd Windows
call npm install
popd

echo [4/4] Installing Mobile (Expo) dependencies...
pushd Mobile
call npm install
popd

set VENV_PATH=Py\.venv
if not exist %VENV_PATH% (
  echo Creating Python virtual environment in Py\.venv...
  python -m venv %VENV_PATH%
)

if exist Py\requirements.txt (
  echo Installing Python dependencies...
  call %VENV_PATH%\Scripts\python.exe -m pip install -r Py\requirements.txt
) else (
  echo Py\requirements.txt not found, skipping Python deps.
)

echo ===================================================
echo   DONE. You can start dev scripts now.
echo ===================================================
endlocal
