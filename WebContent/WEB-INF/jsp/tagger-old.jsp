<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">

<%@include file='head.jsp'%>

<body>

	<%@include file='navbar.jsp'%>
	
	<br><br>
	
    <center>
        <h1>Tagger</h1>
        <h2>Original text</h2>
        <form method="POST" action="tagger.html">
            <textarea name="text" rows="10" cols="100">${originalText}</textarea>
            <br>
            <input type="submit" value="Tag!" />
        </form>

        <h2>Tagged text</h2>
        <textarea name="text" rows="10" cols="100" disabled>${taggedText}</textarea>
    </center>  
    
    <%@include file='footer.jsp'%>
    
</body>
</html>