<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">

<%@include file='head.jsp'%>

<body>

	<%@include file='navbar.jsp'%>
	
	<br><br>
	
	<center>
		<h1>Lemmatizer</h1>
		<h2>Original text</h2>
		<form method="POST" action="lemmatizer.html">
			<textarea name="text" rows="10" cols="100">${originalText}</textarea>
			<br>
			<input type="submit" value="Lemmatize!" />
		</form>

		<h2>Lemmatized text</h2>
		<textarea name="text" rows="10" cols="100" disabled>${lemmatizedText}</textarea>
	</center>
	
	<%@include file='footer.jsp'%>
	
</body>
</html>