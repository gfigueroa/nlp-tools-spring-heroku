<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">

<%@include file='head.jsp'%>

<body>

	<%@include file='navbar.jsp'%>
	
	<br><br>
	
    <main>
        <h1>Tagger</h1>
        <h2>Original text</h2>
        <form method="POST">
            <textarea id="original-text" name="text" rows="10" cols="100" placeholder="Enter text here...">${originalText}</textarea>
            <br>
            <input id="tag-btn" type="submit" value="Tag!" />
        </form>
        
        <div id="loading-div">
			<img width="30px" src="resources/img/hourglass.svg" />
		</div> 

        <h2>Tagged text</h2>
        <textarea id="result-text" name="text" rows="10" cols="100" disabled>${taggedText}</textarea>
    </main>  
    
    <%@include file='footer.jsp'%>
    
</body>
</html>