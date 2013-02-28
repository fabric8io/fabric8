/*
 * smartresize: debounced resize event for jQuery
 *
 * latest version and complete README available on Github:
 * https://github.com/louisremi/jquery.smartresize.js
 *
 * Copyright 2011 @louis_remi
 * Licensed under the MIT license.
 *
 * This saved you an hour of work? 
 * Send me music http://www.amazon.co.uk/wishlist/HNTU0468LQON
 */
(function($) {

var event = $.event,
	resizeTimeout;
	
event.special[ "smartresize" ] = {
	setup: function() {
		$( this ).bind( "resize", event.special.smartresize.handler );
	},
	teardown: function() {
		$( this ).unbind( "resize", event.special.smartresize.handler );
	},
	handler: function( event, execAsap ) {
		// Save the context
		var context = this,
			args = arguments;
		
		// set correct event type
        event.type = "smartresize";
		
		if(resizeTimeout)
			clearTimeout(resizeTimeout);
		resizeTimeout = setTimeout(function() {
			jQuery.event.handle.apply( context, args );
		}, execAsap === "execAsap"? 0 : 100);
	}
}

$.fn.smartresize = function( fn ) {
	return fn ? this.bind( "smartresize", fn ) : this.trigger( "smartresize", ["execAsap"] );
};
	
})(jQuery);