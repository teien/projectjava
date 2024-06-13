@echo off
REM Lấy đường dẫn hiện tại của file BAT
set "currentPath=%~dp0"

REM Tạo dịch vụ với binPath là đường dẫn hiện tại của file BAT và ứng dụng
sc create HardwareMonitorService binPath= "%currentPath%\ConsoleApp2.exe"

REM Thông báo hoàn thành
echo Dịch vụ HardwareMonitorService đã được tạo với binPath= "%currentPath%\ConsoleApp2.exe"
pause
