
# First, run the reference parser to get an AST JSON:


# assembly_file=

#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=r <chocopy_input_file> --out <ast_json_file>
#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=.r <ast_json_file> --out <typed_ast_json_file>
#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=..s <typed_ast_json_file> --out <assembly_file>

java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --run src/test/data/pa3/subtests/literal_bool.s

#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=r src/test/data/pa3/sample/call.py --out src/test/data/pa3/sample/call.json
#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=.r src/test/data/pa3/sample/call.json --out src/test/data/pa3/sample/call.typed.json
#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=..s src/test/data/pa3/sample/call.typed.json --out src/test/data/pa3/sample/call.s
#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --run src/test/data/pa3/sample/call.s

#Chained commands
#For quick development, you can chain all the stages to directly execute a ChocoPy program:

java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=rrs --run src/test/data/pa3/subtests/literal_bool.py
#You can omit the --run in the above chain to print the generated assembly program instead of executing it.

java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=rrs src/test/data/pa3/subtests/literal_bool.py &> shiqi.s
#Running the reference implementation
#To observe the output of the reference implementation of the code generator, replace --pass=rrs with --pass=rrr in any command where applicable.
java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=rrr src/test/data/pa3/subtests/literal_bool.py &> ref.s


java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=rrs src/test/data/pa3/subtests/literal_bool.py
java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=rrr src/test/data/pa3/subtests/literal_bool.py



#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=rrs --run src/test/data/pa3/sample/call.py
java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=rrr --run src/test/data/pa3/sample/call.py

#python src/test/data/pa3/benchmarks/exp.py
java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy \
--pass=rrs --run --profile src/test/data/pa3/benchmarks/exp.py