var ForReading   = 1;
var ForWriting   = 2;

var files = [
'Lemmini.java',
'Game/Core.java',
'Game/GraphicsPane.java',
'Game/GameController.java',
'Extract/Extract.java',
'Extract/FolderDialog.java'
];


var fs = WScript.CreateObject("Scripting.FileSystemObject");

for(var i=0; i<files.length; i++) {
	var fh1 = fs.OpenTextFile(files[i], ForReading);
	var text = fh1.ReadAll();
	fh1.close();

	text = text.replace(/[\*\/]+IF-NOT-MAC[\*\/]*/g, '//IF-NOT-MAC');
	text = text.replace(/[\*\/]+END-NOT-MAC[\*\/]*/g, '//END-NOT-MAC');

	text = text.replace(/[\*\/]+IF-MAC[\*\/]*/g, '/\*IF-MAC');
	text = text.replace(/[\*\/]+END-MAC[\*\/]*/g, '//END-MAC\*/');

	text = text.replace(/[\*\/]+ELSE-IF-MAC[\*\/]*/g, '/*ELSE-IF-MAC');
	text = text.replace(/[\*\/]+ELSE-IF-NOT-MAC[\*\/]*/g, '//ELSE-IF-NOT-MAC*/');

	var fh2 = fs.OpenTextFile(files[i], ForWriting)
	fh2.WriteLine(text);
	fh2.close();
}