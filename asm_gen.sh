
# First, run the reference parser to get an AST JSON:


# assembly_file=

#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=r <chocopy_input_file> --out <ast_json_file>
#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=.r <ast_json_file> --out <typed_ast_json_file>
#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=..s <typed_ast_json_file> --out <assembly_file>


#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=r src/test/data/pa3/sample/call.py --out src/test/data/pa3/sample/call.json
#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=.r src/test/data/pa3/sample/call.json --out src/test/data/pa3/sample/call.typed.json
#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=..s src/test/data/pa3/sample/call.typed.json --out src/test/data/pa3/sample/call.s
#java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --run src/test/data/pa3/sample/call.s

java -cp "chocopy-ref.jar:target/assignment.jar" chocopy.ChocoPy --pass=rrs --run src/test/data/pa3/sample/call.py
