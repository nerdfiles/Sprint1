jQuery(function($) {
	
	/* Remove @title from Main Nav
	 **************************************************/
	
	//$(".navigation a[title]").removeAttr("title");
	
	/* Browser Sniffing to support CSS for Main Nav
	 **************************************************/
	
	if ($.browser.opera) {
		// this needs investigation; the class is being added because opera
		// disagrees about the "top" property
		$(".navigation ul ul ul,.navigation ul ul div").addClass("opera");
	} else if ($.browser.webkit) {
		$(".navigation ul ul ul,.navigation ul ul div").addClass("webkit");
	}
	
	if ($.browser.msie) {
		// ie6 can't do hovers on non-anchor elements
		// and i cannot find the right element to invoke
		// hasLayout on to give the tier-3 element left: auto
		$(".navigation > ul > li > ul > li").hover(function() {
			var w = $(this).find("a:eq(0)").width()+48;
			w = "-"+w;
			w = w+"px";
			$(this).find('ul,div').css({
				display:"block",
				marginLeft:w
			})
		}, function() {
			$(this).find('ul,div').css({display:"none"})
		});
	}

	/* Site Search Behavior
	 ******************************************************/

	var searchInput = $("#q");
	var defaultValue = searchInput.val();
	$("#site_search").submit(function(e) {
		var userValue = searchInput.val();
		if (defaultValue !== userValue) $(this).submit();
		e.preventDefault();
	});
	searchInput.bind("focus blur", function(e) {
		var userValue = searchInput.val();
		if(defaultValue === userValue) $(this).val('');
		if(userValue === "") $(this).val(defaultValue);
	});
});
