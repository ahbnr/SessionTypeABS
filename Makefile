.PHONY: all run jar

# https://stackoverflow.com/a/14061796
# If the first argument is "run"...
ifeq (run,$(firstword $(MAKECMDGOALS)))
  # use the rest as arguments for "run"
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  # ...and turn them into do-nothing targets
  $(eval $(RUN_ARGS):;@:)
endif

all:
	./gradlew --console=rich build

run:
	./gradlew --console=rich run --args "$(RUN_ARGS)"

jar:
	./gradlew --console=rich shadowJar
	echo "Generated JAR: build/libs/sessiontypeabs-1.0-SNAPSHOT-all.jar"
