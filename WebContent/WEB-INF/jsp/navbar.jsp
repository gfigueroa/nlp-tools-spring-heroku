<!--NAVBAR SECTION BEGIN-->
<div class="navbar navbar-inverse navbar-fixed-top " id="menu">
	<div class="container">
		<div class="navbar-header">
			<button type="button" class="navbar-toggle" data-toggle="collapse"
				data-target=".navbar-collapse">
				<span class="icon-bar"></span> <span class="icon-bar"></span> <span
					class="icon-bar"></span>
			</button>
			<a class="navbar-brand" href="#"><img class="logo-custom"
				src="resources/img/logo.jpg" alt="" /></a>
		</div>
		<div class="navbar-collapse collapse move-me">
			<ul class="nav navbar-nav navbar-right">
				<li><a href="<%= !request.getAttribute("javax.servlet.forward.request_uri").toString().endsWith("index") ? "index" : "" %>#home">HOME</a></li>
				<li><a href="<%= !request.getAttribute("javax.servlet.forward.request_uri").toString().endsWith("index") ? "index" : "" %>#features-sec">FEATURES</a></li>
				<li><a href="<%= !request.getAttribute("javax.servlet.forward.request_uri").toString().endsWith("index") ? "index" : "" %>#contact-sec">CONTACT</a></li>
			</ul>
		</div>

	</div>
</div>
<!--NAVBAR SECTION END-->