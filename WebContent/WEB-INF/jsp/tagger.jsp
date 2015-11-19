<html>  
<head>  
<script type="text/javascript" src="jquery-1.2.6.min.js"></script>  
<title>POS Tagger</title>  
</head>  
<body>
    <center>
        <a href="index.jsp">Home</a>
        <h1>Tagger</h1>
        <h2>Original text</h2>
        <form method="POST" action="tagger.html">
            <textarea name="text" rows="10" cols="50">${originalText}</textarea>
            <br>
            <input type="submit" value="Tag!" />
        </form>

        <h2>Tagged text</h2>
        <textarea name="text" rows="10" cols="50" disabled>${taggedText}</textarea>
    </center>  
</body>
</html>