.PHONY: all run

all:
	./gradlew --console=rich build

run:
	./gradlew --console=rich run
