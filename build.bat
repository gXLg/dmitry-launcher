@echo off

REM Remove the existing build folder if it exists
rmdir /s /q .\build
del /s /q .\Launcher.jar
mkdir .\build

javac -cp ".\libs\json-20250107.jar;.\libs\ant-1.10.15.jar" .\Launcher.java -Xlint:deprecation && (

cd build
jar xfv ..\libs\ant-1.10.15.jar
rmdir /s /q .\images
rmdir /s /q .\META-INF
jar xfv ..\libs\json-20250107.jar
rmdir /s /q .\META-INF

cd ..
mkdir .\build\com\dmitry
move .\Launcher.class .\build\com\dmitry
move .\Launcher$TextAreaOutputStream.class .\build\com\dmitry
move .\Launcher$1.class .\build\com\dmitry
move .\Launcher$2.class .\build\com\dmitry
robocopy .\images .\build\images
echo Manifest-Version: 1.0 > MANIFEST.MF
echo Main-Class: com.dmitry.Launcher >> MANIFEST.MF
echo Class-Path: org/json/ org/apache/ant/ >> MANIFEST.MF

cd build
jar cvfm ..\Launcher.jar ..\MANIFEST.MF -C . .

cd ..
rmdir /s /q .\build
del /q MANIFEST.MF

)

pause
