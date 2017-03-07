<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">

<%@include file='head.jsp'%>

<body>

	<%@include file='navbar.jsp'%>
	
	<br><br>
	
	<main>
		<h1>Lemmatizer</h1>
		<h2>Original text</h2>
		<form method="POST">
			<textarea id="original-text" name="text" rows="10" cols="100" placeholder="Enter text here...">${originalText}</textarea>
			<br>
			<input id="lemmatize-btn" type="submit" value="Lemmatize!" />
		</form>
		
		<div id="loading-div">
			<img width="30px" src="resources/img/hourglass.svg" />
		</div> 

		<h2>Lemmatized text</h2>
		<textarea id="result-text" name="text" rows="10" cols="100" disabled>${lemmatizedText}</textarea>
	</main>
	
	<%@include file='footer.jsp'%>
	
</body>
</html>