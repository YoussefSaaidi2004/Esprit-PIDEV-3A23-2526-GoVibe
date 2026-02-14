@echo off
REM Set JAVA_HOME to IntelliJ's bundled JDK
set JAVA_HOME=C:\Users\user\AppData\Local\Programs\IntelliJ IDEA 2025.2.5\jbr
set PATH=%JAVA_HOME%\bin;%PATH%

REM Run Maven with IntelliJ parameters
call mvnw.cmd -Didea.version=2025.2.5 -Dmaven.ext.class.path="C:\Users\user\AppData\Local\Programs\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven-event-listener.jar" -Djansi.passthrough=true -Dstyle.color=always -Dmaven.repo.local=C:\Users\user\.m2\repository org.openjfx:javafx-maven-plugin:0.0.8:run -f pom.xml

pause
