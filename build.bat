if not exist ./bin/ mkdir bin

javac -target 1.8 -source 1.8 -d ./bin/ -sourcepath ^
./src/main/java/com/dattue/airport/*.java
