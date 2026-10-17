@echo off
setlocal enabledelayedexpansion
set count=1
for %%f in (*.png) do (
    ren "%%f" "scenario-!count!.png"
    set /a count+=1
)
echo 重命名完成！共重命名 !count! 个文件。
pause
