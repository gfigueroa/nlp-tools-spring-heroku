<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">

<%@include file='head.jsp'%>

<body>

	<%@include file='navbar.jsp'%>
	
	<br><br>
	
	<center>
		<h1>Keyword Extraction</h1>
		<h2>Text</h2>
		<form method="POST" action="keywords.html">
			<textarea name="text" rows="10" cols="100">${text}</textarea>
			<br> 
			<input type="submit" value="Extract keywords!" />
		</form>

		<h2>Keywords</h2>
		<textarea name="text" rows="10" cols="100" disabled>${keywordsText}</textarea>
	</center>
	
	<%@include file='footer.jsp'%>
	
</body>
</html>