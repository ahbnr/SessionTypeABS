
 0         -f-> File.User:main
.File.User -f2-> File.File:open
.File.File resolves f2
.File.User -f3-> File.File:read
.File.File resolves f3
.File.User -f4-> File.File:close
.File.File resolves f4
