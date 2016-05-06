<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">

<%@include file='head.jsp'%>

<% 
Object methodObject = request.getAttribute("method");
String method = methodObject != null ? (String) methodObject : "rankup";
%>

<body>

	<%@include file='navbar.jsp'%>
	
	<br><br>
	
	<center>
		<h1>Keyword Extraction</h1>
		<h2>Text</h2>
		<form method="POST" action="keywords.html">
			Method:
			<select name="method" required>
			  <option value="rankup" <%= method.equalsIgnoreCase("rankup") ? "selected" : "" %>>RankUp</option>
			  <option value="textrank" <%= method.equalsIgnoreCase("textrank") ? "selected" : "" %>>TextRank</option>
			  <option value="rake" <%= method.equalsIgnoreCase("rake") ? "selected" : "" %>>RAKE</option>
			  <option value="tfidf" <%= method.equalsIgnoreCase("tfidf") ? "selected" : "" %>>TFIDF</option>
			  <option value="ridf" <%= method.equalsIgnoreCase("ridf") ? "selected" : "" %>>RIDF</option>
			  <option value="clusteredness" <%= method.equalsIgnoreCase("clusteredness") ? "selected" : "" %>>Clusteredness</option>
			</select>
			<br>
			<textarea name="text" rows="10" cols="100">${text}</textarea>
			<br> 
			<input type="submit" value="Extract keywords!" />
		</form>

		<h2>Keywords</h2>
		<textarea name="keywords" rows="10" cols="100" disabled>${keywordsText}</textarea>
	</center>
	
	<%@include file='footer.jsp'%>
	
</body>
</html>