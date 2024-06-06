@echo off
setlocal
set current_dir=%~dp0
set binPath=%current_dir%ConsoleApp2.exe
sc create HardwareMonitorService binPath= "%binPath%" start= auto
echo Service HardwareMonitorService đã được tạo với binPath=%binPath%
pause

