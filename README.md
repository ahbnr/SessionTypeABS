# Semi-Dynamic Session Type Tool for ABS

This tool complements the ABS modeling language with support for session types.
It can statically verify ABS models against session types and modifies them to
enforce those parts of the specified behavior, which can not be statically
guaranteed.

I developed this tool during my bachelor thesis. Please consult my thesis for
more information on the tool.
Link to thesis: [Link](thesis/thesis_final_pdfa.pdf)
(This is the first version as it had been originally submitted for review.)

## Prerequisites

Please make sure that the following dependencies are available on your system:

* Kotlin compiler version 1.3.50
* Erlang version 22.1
* OpenJDK 11
* Git version 2.24.0

Furthermore, it is assumed, that the commands in the following sections are
executed on the `bash` shell of a linux system.

## Building :hammer_and_wrench:

To build the tool, first, a modified version of ABS has to be downloaded and
be built.

```sh
git clone https://github.com/ahbnr/SessionTypeABS.git
git clone --branch thisDestiny https://github.com/ahbnr/abstools.git

cd abstools/frontend
../gradlew assemble

cd ../../SessionTypeABS
./gradlew shadowJar

chmod +x sdstool
```

The SDS-tool can now be found as an executable `.jar` file in `build/libs`.
We provide a wrapper script `sdstool` which can be invoked instead of the JAR
file.

## Tests :clipboard:

Issue the command
```sh
./gradlew test
```
to run all tests.

## Usage

The following command

* statically verifies an ABS model against the given session types
  * (see [link](session_type_syntax.md) for more information on the syntax of `.st` session type specification files)
* applies our dynamic enforcement techniques to it
* compiles the modified model to Erlang

```sh
./sdstool compile [flags] [.abs files] [.st files]
```

With the optional flags, verification or enforcement can be deactivated etc.
Use `./sdstool compile --help` for further information on them.

After compiling the ABS model, it can be executed like this:
```sh
gen/erl/run
```

## Other commands:

* `./sdstool printModel [.abs files] [.st files]` prints the parts of the given
  ABS model, which are modified by our dynamic enforcement methods.

* `./sdstool printGlobalTypes [.st files]` parses the given session type files
  and outputs them again.

* `./sdstool printLocalTypes [.st files]` prints the object local session types
  projected from the given global ones.

## Evaluation Scripts

See `evalutation/README.md`

## License

See LICENSE.txt.
This license only applies to the files belonging to this project, not to
abstools or the other libraries being used.

## Used External Libraries And Tools

* our modified variation of abstools, version 1.8.1
* ANTLR, version 4.7
* picocli, version 4.0.0-beta-2
* Apache Commons IO, version 2.6
* JUnit 5, version 5.5.0
* NuProcess, version 1.2.3
