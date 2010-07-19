
$(function(){


		/* IF HREF IS BLANK, RETURN FALSE */
			var ele_globalanchors = $('a'); $(ele_globalanchors).bind('click', function(){ var getHref = $(this).attr('href'); if (getHref == "") { return false } });

		/* REMOVE/ADD BORDERS FOR VARIOUS ELEMENTS */      									
			var ele_navtoplast = $('#topnav li:last, #titlenav li:last'); if (ele_navtoplast[0]) { $(ele_navtoplast).css('border','none').css('margin-right','0px'); }
			var ele_glossaryitemfirst	= $('.glossary_item:first'); if(ele_glossaryitemfirst[0]) { $(ele_glossaryitemfirst).css('border-top','1px solid #ebe8d8').css('margin','20px 0 0 0'); }
				
		/* DECREASE TOP PADDING OF FIRST COLUMN ITEMS WITHIN FOOTER */  		
			$('#footer .ftr_sub').each(function(){ var ele_footercolfirst = $('strong:first',this); $(ele_footercolfirst).css('margin-top','25px'); });
		
		/* CHANGE BREADCRUMB MARGIN WHEN A PAGE CONTAINS A HERO */
			var ele_h1hero = $('h1.hero'); if (ele_h1hero[0]) { $('#breadcrumb').css('margin','15px 0 20px 0'); }
				
		/* APPEND DOUBLE ARROWS TO VARIOUS ANCHOR TAGS (content h3 a, rightcontentpod a  etc...) */
			$('.pod_2colA p a').append(' &raquo;');
			$('.pod_2colB h3 a').append(' &raquo;');
			$('.rightcontentpod a strong').append(' &raquo;');
			$('.related_links li a').append(' &raquo;');
				
		/* HIDE/SHOW SEARCH INPUT LABEL AS APPROPRIATE */
			var ele_searchInput = $('#input_sitesearch');
			if ( ele_searchInput[0]) {
				var ele_searchInputValue = $(ele_searchInput).attr('value');	
				$('#submit_sitesearch').val("");
				if(ele_searchInputValue != ""){ $(ele_searchInput).prev().css('display','none'); } else { $(ele_searchInput).prev().css('display','block'); }				
				$(ele_searchInput).focus(function(){
					$(this).prev().css('display','none');
					$(this).blur(function(){
						var getInputValue = $(this).attr('value');
						if (getInputValue == ""){ $(this).prev().css('display','block'); }
					});
				});
			}
				
		/* HIDE/SHOW FIND LOCAL INFO LABEL AS APPROPRIATE */
			var ele_floInput = $('#input_findlocalinfo');
			if ( ele_floInput[0]) {
				var ele_floInputValue = $(ele_floInput).attr('value');	
				$('#submit_findlocalinfo').val("");
				if(ele_floInputValue != ""){ $(ele_floInput).prev().css('display','none'); } else { $(ele_floInput).prev().css('display','block'); }				
				$(ele_floInput).focus(function(){
					$(this).prev().css('display','none');
					$(this).blur(function(){
						var getfloValue = $(this).attr('value');
						if (getfloValue == ""){ $(this).prev().css('display','block'); }
					});
				});
			}
			
		/* ZIP/STATE SEARCH AUTOCOMPLETE */
		  // TO DO //
			
		
		
		
});