<html>
<head>
<script type="text/javascript" src="jquery-1.2.6.min.js"></script>
<title>Keyword Extraction</title>
</head>
<body>
	<center>
		<a href="index.html">Home</a>
		<h1>Keyword Extraction</h1>
		<h2>Text</h2>
		<form method="POST" action="keywords.html">
			<textarea name="text" rows="10" cols="50">${text}</textarea>
			<br> <input type="submit" value="Extract keywords!" />
		</form>

		<h2>Keywords</h2>
		<textarea name="text" rows="10" cols="50" disabled>${keywordsText}</textarea>
	</center>
</body>
</html>