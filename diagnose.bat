@echo off
echo ====================================
echo GoVibe Diagnostic Script
echo ====================================
echo.

echo Checking Java installation...
java -version
echo.

echo Checking JAVA_HOME...
echo JAVA_HOME=%JAVA_HOME%
echo.

echo Checking Maven...
call mvnw.cmd --version
echo.

echo Checking if MySQL is accessible...
netstat -an | findstr ":3306"
echo.

echo ====================================
echo Attempting to compile project...
echo ====================================
call mvnw.cmd clean compile

echo.
echo ====================================
echo Diagnostic complete!
echo ====================================
pause
