@echo off
set scriptDirectory=%~dp0
set exeFileName=HwSys.exe
set exePath=%scriptDirectory%%exeFileName%
set serviceName=HardwareMonitorService
sc create %serviceName% binPath= "%exePath%"
