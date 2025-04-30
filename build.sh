rm -rf ./build
rm -f ./Launcher.jar
mkdir ./build

javac -cp "./libs/json-20250107.jar:./libs/ant-1.10.15.jar" ./Launcher.java -Xlint:deprecation || exit

mkdir -p ./build/com/dmitry
mv ./Launcher.class ./build/com/dmitry
mv './Launcher$TextAreaOutputStream.class' ./build/com/dmitry
mv './Launcher$1.class' ./build/com/dmitry

cat << EOF > MANIFEST.MF
Manifest-Version: 1.0
Main-Class: com.dmitry.Launcher
Class-Path: org/json/ org/apache/ant/
EOF

cd build

jar xfv ../libs/ant-1.10.15.jar
rm -rf ./images ./META-INF

jar xfv ../libs/json-20250107.jar
rm -rf ./META-INF

jar cvfm ../Launcher.jar ../MANIFEST.MF -C . .

cd ..
rm -rf ./build ./MANIFEST.MF
