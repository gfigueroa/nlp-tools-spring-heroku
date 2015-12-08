<html>  
<head>  
<script type="text/javascript" src="jquery-1.2.6.min.js"></script>  
<title>Lemmatizer</title>  
</head>  
<body>  
    <center>
        <a href="index.html">Home</a>
        <h1>Lemmatizer</h1>
        <h2>Original text</h2>
        <form method="POST" action="lemmatizer.html">
            <textarea name="text" rows="10" cols="50">${originalText}</textarea>
            <br>
            <input type="submit" value="Lemmatize!" />
        </form>

        <h2>Lemmatized text</h2>
        <textarea name="text" rows="10" cols="50" disabled>${lemmatizedText}</textarea>
    </center>  
</body>  
</html>