import subprocess

def compileModel(*args):
    subprocess.run(["../sdstool.sh", "compile", *args])
