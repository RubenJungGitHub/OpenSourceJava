mvn dependency:build-classpath -Dmdep.outputFile=classpath.txt

echo Current directory: %CD%
if exist classpath.txt (
    echo classpath.txt FOUND
) else (
    echo classpath.txt NOT FOUND
)
setlocal enabledelayedexpansion
type classpath.txt
for /F "usebackq delims=" %%i in ("classpath.txt") do (
    echo LINE: [%%i]
)

:: Build CLASSPATH from classpath.txt
set CLASSPATH=
for /f "delims=" %%i in (classpath.txt) do (
    set CLASSPATH=!CLASSPATH!;%%i
)

:: DEBUG: print the classpath
echo CLASSPATH:
echo !CLASSPATH!

:: Generate Javadoc
javadoc -d ../../docs -sourcepath C:\ContainOpenSource\Java\OpenSourceJava\ils\src\main\java -subpackages contain.opensource -classpath @classpath.txt -Xdoclint:none
echo JavaDocs generated in ../../docs
pause
