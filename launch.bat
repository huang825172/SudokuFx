IF NOT EXIST "out" (
    mkdir "out"
)
javac -d out src/SudokuFx.java
java -classpath out SudokuFx
pause