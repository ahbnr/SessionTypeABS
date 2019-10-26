#!/usr/bin/env python3
import jinja2
import os

def renderTemplate(filePath: str, targetPath: str, env: dict):
    dirpath = os.path.dirname(filePath)
    filename = os.path.basename(filePath)

    templateLoader = jinja2.FileSystemLoader(searchpath=dirpath)
    templateEnv = jinja2.Environment(loader=templateLoader)
    template = templateEnv.get_template(filename)

    result = template.render(env)

    targetFile = open(targetPath, "w")
    targetFile.write(result)
    targetFile.close()
