var fs = require("fs");

//=========================================================
// Filesystem
//=========================================================

print("[\"fs\" Testbench]\n");

print("fs-regression.js filesize: " + fs.open("fs-test.js").read().length);