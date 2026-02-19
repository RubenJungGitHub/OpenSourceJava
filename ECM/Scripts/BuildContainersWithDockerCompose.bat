cls
cd C:\ContainOpenSource\Java\OpenSourceJava\ECM
docker build -t ils-app:latest .
docker compose  -f C:\ContainOpenSource\Java\OpenSourceJava\ECM\docker-compose.yml down 
docker compose -f C:\ContainOpenSource\Java\OpenSourceJava\ECM\docker-compose.yml up --build


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
pause

:: Generate Javadoc
javadoc -d ../../docs -sourcepath C:\ContainOpenSource\Java\OpenSourceJava\ECM\src\main\java -subpackages contain.opensource -classpath @classpath.txt -Xdoclint:none
echo JavaDocs generated in ../../docs
pause


#netstat -ano | findstr :5433
#Get-Process -Id 32148
#taskkill /PID 7404 /F


#docker rm -f postgres
#docker volume ls        # find volume used for Postgres
#docker volume rm  main_postgres-data