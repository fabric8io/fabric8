/**
 * Date.parse with progressive enhancement for ISO 8601 <https://github.com/csnover/js-iso8601>
 * © 2011 Colin Snover <http://zetafleet.com>
 * Released under MIT license.
 */
(function (Date, undefined) {
    var origParse = Date.parse, numericKeys = [ 1, 4, 5, 6, 7, 10, 11 ];
    Date.parse = function (date) {
        var timestamp, struct, minutesOffset = 0;

        // ES5 §15.9.4.2 states that the string should attempt to be parsed as a Date Time String Format string
        // before falling back to any implementation-specific date parsing, so that’s what we do, even if native
        // implementations could be faster
        //              1 YYYY                2 MM       3 DD           4 HH    5 mm       6 ss        7 msec        8 Z 9 ±    10 tzHH    11 tzmm
        if ((struct = /^(\d{4}|[+\-]\d{6})(?:-(\d{2})(?:-(\d{2}))?)?(?:T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{3}))?)?(?:(Z)|([+\-])(\d{2})(?::(\d{2}))?)?)?$/.exec(date))) {
            // avoid NaN timestamps caused by “undefined” values being passed to Date.UTC
            for (var i = 0, k; (k = numericKeys[i]); ++i) {
                struct[k] = +struct[k] || 0;
            }

            // allow undefined days and months
            struct[2] = (+struct[2] || 1) - 1;
            struct[3] = +struct[3] || 1;

            if (struct[8] !== 'Z' && struct[9] !== undefined) {
                minutesOffset = struct[10] * 60 + struct[11];

                if (struct[9] === '+') {
                    minutesOffset = 0 - minutesOffset;
                }
            }

            timestamp = Date.UTC(struct[1], struct[2], struct[3], struct[4], struct[5] + minutesOffset, struct[6], struct[7]);
        }
        else {
            timestamp = origParse ? origParse(date) : NaN;
        }

        return timestamp;
    };
}(Date));

/*
 * Date Format 1.2.3
 * (c) 2007-2009 Steven Levithan <stevenlevithan.com>
 * MIT license
 *
 * Includes enhancements by Scott Trenda <scott.trenda.net>
 * and Kris Kowal <cixar.com/~kris.kowal/>
 *
 * Accepts a date, a mask, or a date and a mask.
 * Returns a formatted version of the given date.
 * The date defaults to the current date/time.
 * The mask defaults to dateFormat.masks.default.
 */

var dateFormat = function () {
	var	token = /d{1,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|[LloSZ]|"[^"]*"|'[^']*'/g,
		timezone = /\b(?:[PMCEA][SDP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g,
		timezoneClip = /[^-+\dA-Z]/g,
		pad = function (val, len) {
			val = String(val);
			len = len || 2;
			while (val.length < len) val = "0" + val;
			return val;
		};

	// Regexes and supporting functions are cached through closure
	return function (date, mask, utc) {
		var dF = dateFormat;

		// You can't provide utc if you skip other args (use the "UTC:" mask prefix)
		if (arguments.length == 1 && Object.prototype.toString.call(date) == "[object String]" && !/\d/.test(date)) {
			mask = date;
			date = undefined;
		}

		// Passing date through Date applies Date.parse, if necessary
		date = date ? new Date(date) : new Date;

		if (isNaN(date)) throw SyntaxError("invalid date");

		mask = String(dF.masks[mask] || mask || dF.masks["default"]);

		// Allow setting the utc argument via the mask
		if (mask.slice(0, 4) == "UTC:") {
			mask = mask.slice(4);
			utc = true;
		}

		var	_ = utc ? "getUTC" : "get",
			d = date[_ + "Date"](),
			D = date[_ + "Day"](),
			m = date[_ + "Month"](),
			y = date[_ + "FullYear"](),
			H = date[_ + "Hours"](),
			M = date[_ + "Minutes"](),
			s = date[_ + "Seconds"](),
			L = date[_ + "Milliseconds"](),
			o = utc ? 0 : date.getTimezoneOffset(),
			flags = {
				d:    d,
				dd:   pad(d),
				ddd:  dF.i18n.dayNames[D],
				dddd: dF.i18n.dayNames[D + 7],
				m:    m + 1,
				mm:   pad(m + 1),
				mmm:  dF.i18n.monthNames[m],
				mmmm: dF.i18n.monthNames[m + 12],
				yy:   String(y).slice(2),
				yyyy: y,
				h:    H % 12 || 12,
				hh:   pad(H % 12 || 12),
				H:    H,
				HH:   pad(H),
				M:    M,
				MM:   pad(M),
				s:    s,
				ss:   pad(s),
				l:    pad(L, 3),
				L:    pad(L > 99 ? Math.round(L / 10) : L),
				t:    H < 12 ? "a"  : "p",
				tt:   H < 12 ? "am" : "pm",
				T:    H < 12 ? "A"  : "P",
				TT:   H < 12 ? "AM" : "PM",
				Z:    utc ? "UTC" : (String(date).match(timezone) || [""]).pop().replace(timezoneClip, ""),
				o:    (o > 0 ? "-" : "+") + pad(Math.floor(Math.abs(o) / 60) * 100 + Math.abs(o) % 60, 4),
				S:    ["th", "st", "nd", "rd"][d % 10 > 3 ? 0 : (d % 100 - d % 10 != 10) * d % 10]
			};

		return mask.replace(token, function ($0) {
			return $0 in flags ? flags[$0] : $0.slice(1, $0.length - 1);
		});
	};
}();

// Some common format strings
dateFormat.masks = {
	"default":      "ddd mmm dd yyyy HH:MM:ss",
	shortDate:      "m/d/yy",
	mediumDate:     "mmm d, yyyy",
	longDate:       "mmmm d, yyyy",
	fullDate:       "dddd, mmmm d, yyyy",
	shortTime:      "h:MM TT",
	mediumTime:     "h:MM:ss TT",
	longTime:       "h:MM:ss TT Z",
	isoDate:        "yyyy-mm-dd",
	isoTime:        "HH:MM:ss",
	isoDateTime:    "yyyy-mm-dd'T'HH:MM:ss",
	isoUtcDateTime: "UTC:yyyy-mm-dd'T'HH:MM:ss'Z'"
};

// Internationalization strings
dateFormat.i18n = {
	dayNames: [
		"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",
		"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
	],
	monthNames: [
		"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
		"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
	]
};

// For convenience...
Date.prototype.format = function (mask, utc) {
	return dateFormat(this, mask, utc);
};

/**
* Date.parse with progressive enhancement for ISO 8601 <https://github.com/csnover/js-iso8601>
* © 2011 Colin Snover <http://zetafleet.com>
* Released under MIT license.
*/
(function (Date, undefined) {
    var origParse = Date.parse, numericKeys = [ 1, 4, 5, 6, 7, 10, 11 ];
    Date.parse = function (date) {
        var timestamp, struct, minutesOffset = 0;

        // ES5 §15.9.4.2 states that the string should attempt to be parsed as a Date Time String Format string
        // before falling back to any implementation-specific date parsing, so that’s what we do, even if native
        // implementations could be faster
        // 1 YYYY 2 MM 3 DD 4 HH 5 mm 6 ss 7 msec 8 Z 9 ± 10 tzHH 11 tzmm
        if ((struct = /^(\d{4}|[+\-]\d{6})(?:-(\d{2})(?:-(\d{2}))?)?(?:T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{3,6}))?)?(?:(Z)|([+\-])(\d{2})(?::(\d{2}))?)?)?$/.exec(date))) {
            // avoid NaN timestamps caused by “undefined” values being passed to Date.UTC
            for (var i = 0, k; (k = numericKeys[i]); ++i) {
                struct[k] = +struct[k] || 0;
            }

            // allow undefined days and months
            struct[2] = (+struct[2] || 1) - 1;
            struct[3] = +struct[3] || 1;

            if (struct[8] !== 'Z' && struct[9] !== undefined) {
                minutesOffset = struct[10] * 60 + struct[11];

                if (struct[9] === '+') {
                    minutesOffset = 0 - minutesOffset;
                }
            }
            if(struct[7] > 999)
            	struct[7] = parseInt(struct[7].toString().substring(0,2))

            timestamp = Date.UTC(struct[1], struct[2], struct[3], struct[4], struct[5] + minutesOffset, struct[6], struct[7]);
        }
        else {
            timestamp = origParse ? origParse(date) : NaN;
        }

        return timestamp;
    };
}(Date));

/*http://keith-wood.name/datetimeEntry.html
   Date and time entry for jQuery v1.0.1.
   Written by Keith Wood (kbwood{at}iinet.com.au) September 2010.
   Dual licensed under the GPL (http://dev.jquery.com/browser/trunk/jquery/GPL-LICENSE.txt) and
   MIT (http://dev.jquery.com/browser/trunk/jquery/MIT-LICENSE.txt) licenses.
   Please attribute the author if you use it.
   */
(function($){function DatetimeEntry(){this._disabledInputs=[];this.regional=[];this.regional['']={datetimeFormat:'O/D/Y H:Ma',datetimeSeparators:'.',monthNames:['January','February','March','April','May','June','July','August','September','October','November','December'],monthNamesShort:['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'],dayNames:['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'],dayNamesShort:['Sun','Mon','Tue','Wed','Thu','Fri','Sat'],ampmNames:['AM','PM'],spinnerTexts:['Today','Previous field','Next field','Increment','Decrement'],isRTL:false};this._defaults={appendText:'',initialField:0,useMouseWheel:true,shortYearCutoff:'+10',defaultDatetime:null,minDatetime:null,maxDatetime:null,minTime:null,maxTime:null,timeSteps:[1,1,1],spinnerImage:'spinnerDefault.png',spinnerSize:[20,20,8],spinnerBigImage:'',spinnerBigSize:[40,40,16],spinnerIncDecOnly:false,spinnerRepeat:[500,250],beforeShow:null,altField:null,altFormat:null};$.extend(this._defaults,this.regional[''])}var v='datetimeEntry';$.extend(DatetimeEntry.prototype,{markerClassName:'hasDatetimeEntry',setDefaults:function(a){extendRemove(this._defaults,a||{});return this},_connectDatetimeEntry:function(b,c){var d=$(b);if(d.hasClass(this.markerClassName)){return}var e={};e.options=$.extend({},c);e._selectedYear=0;e._selectedMonth=0;e._selectedDay=0;e._selectedHour=0;e._selectedMinute=0;e._selectedSecond=0;e._field=0;this._decodeDatetimeFormat(e);e.input=$(b);$.data(b,v,e);var f=this._get(e,'spinnerImage');var g=this._get(e,'spinnerText');var h=this._get(e,'spinnerSize');var i=this._get(e,'appendText');var j=(!f?null:$('<span class="datetimeEntry_control" style="display: inline-block; '+'background: url(\''+f+'\') 0 0 no-repeat; '+'width: '+h[0]+'px; height: '+h[1]+'px;'+($.browser.mozilla&&$.browser.version<'1.9'?' padding-left: '+h[0]+'px; padding-bottom: '+(h[1]-18)+'px;':'')+'"></span>'));d.wrap('<span class="datetimeEntry_wrap"></span>').after(i?'<span class="datetimeEntry_append">'+i+'</span>':'').after(j||'');d.addClass(this.markerClassName).bind('focus.datetimeEntry',this._doFocus).bind('blur.datetimeEntry',this._doBlur).bind('click.datetimeEntry',this._doClick).bind('keydown.datetimeEntry',this._doKeyDown).bind('keypress.datetimeEntry',this._doKeyPress);if($.browser.mozilla){d.bind('input.datetimeEntry',function(a){$.datetimeEntry._extractDatetime(e)})}if($.browser.msie){d.bind('paste.datetimeEntry',function(a){setTimeout(function(){$.datetimeEntry._extractDatetime(e)},1)})}if(this._get(e,'useMouseWheel')&&$.fn.mousewheel){d.mousewheel(this._doMouseWheel)}if(j){j.mousedown(this._handleSpinner).mouseup(this._endSpinner).mouseover(this._expandSpinner).mouseout(this._endSpinner).mousemove(this._describeSpinner)}},_enableDatetimeEntry:function(a){this._enableDisable(a,false)},_disableDatetimeEntry:function(a){this._enableDisable(a,true)},_enableDisable:function(b,c){var d=$.data(b,v);if(!d){return}b.disabled=c;if(b.nextSibling&&b.nextSibling.nodeName.toLowerCase()=='span'){$.datetimeEntry._changeSpinner(d,b.nextSibling,(c?5:-1))}$.datetimeEntry._disabledInputs=$.map($.datetimeEntry._disabledInputs,function(a){return(a==b?null:a)});if(c){$.datetimeEntry._disabledInputs.push(b)}},_isDisabledDatetimeEntry:function(a){return $.inArray(a,this._disabledInputs)>-1},_changeDatetimeEntry:function(a,b,c){var d=$.data(a,v);if(d){if(typeof b=='string'){var e=b;b={};b[e]=c}var f=this._parseDatetime(d,$(a).val());extendRemove(d.options,b||{});this._decodeDatetimeFormat(d);if(f){this._setDatetime(d,f)}}$.data(a,v,d)},_decodeDatetimeFormat:function(a){var b=this._get(a,'datetimeFormat');a._fields=[];a._ampmField=-1;for(var i=0;i<b.length;i++){if(b.charAt(i).match(/[yYoOnNdDwWhHmMsSa]/)){a._fields.push(i)}if(b.charAt(i)=='a'){a._ampmField=a._fields.length-1}}},_destroyDatetimeEntry:function(b){$input=$(b);if(!$input.hasClass(this.markerClassName)){return}$input.removeClass(this.markerClassName).unbind('.datetimeEntry');if($.fn.mousewheel){$input.unmousewheel()}this._disabledInputs=$.map(this._disabledInputs,function(a){return(a==b?null:a)});$input.parent().replaceWith($input);$.removeData(b,v)},_setDatetimeDatetimeEntry:function(a,b){var c=$.data(a,v);if(c){if(b===null||b===''){c.input.val('')}else{this._setDatetime(c,b?(typeof b=='object'?new Date(b.getTime()):b):null)}}},_getDatetimeDatetimeEntry:function(a){var b=$.data(a,v);return(b?this._parseDatetime(b,$(a).val()):null)},_getOffsetDatetimeEntry:function(a){var b=$.data(a,v);var c=(b?$.datetimeEntry._parseDatetime(b,b.input.val()):null);return(!c?0:(c.getHours()*3600+c.getMinutes()*60+c.getSeconds())*1000)},_doFocus:function(a){var b=(a.nodeName&&a.nodeName.toLowerCase()=='input'?a:this);if($.datetimeEntry._lastInput==b||$.datetimeEntry._isDisabledDatetimeEntry(b)){$.datetimeEntry._focussed=false;return}var c=$.data(b,v);$.datetimeEntry._focussed=true;$.datetimeEntry._lastInput=b;$.datetimeEntry._blurredInput=null;var d=$.datetimeEntry._get(c,'beforeShow');extendRemove(c.options,(d?d.apply(b,[b]):{}));$.data(b,v,c);$.datetimeEntry._extractDatetime(c);setTimeout(function(){$.datetimeEntry._showField(c)},10)},_doBlur:function(a){$.datetimeEntry._blurredInput=$.datetimeEntry._lastInput;$.datetimeEntry._lastInput=null},_doClick:function(b){var c=b.target;var d=$.data(c,v);if(!$.datetimeEntry._focussed){var e=$.datetimeEntry._get(d,'datetimeFormat');d._field=0;if(c.selectionStart!=null){var f=0;for(var i=0;i<e.length;i++){f+=$.datetimeEntry._fieldLength(d,e.charAt(i));if(c.selectionStart<f){break}d._field+=(e.charAt(i).match(/[yondwhmsa]/i)?1:0)}}else if(c.createTextRange){var g=$(b.srcElement);var h=c.createTextRange();var j=function(a){return{thin:2,medium:4,thick:6}[a]||a};var k=b.clientX+document.documentElement.scrollLeft-(g.offset().left+parseInt(j(g.css('border-left-width')),10))-h.offsetLeft;var f=0;for(var i=0;i<e.length;i++){f+=$.datetimeEntry._fieldLength(d,e.charAt(i));h.collapse();h.moveEnd('character',f);if(k<h.boundingWidth){break}d._field+=(e.charAt(i).match(/[yondwhmsa]/i)?1:0)}}}$.data(c,v,d);$.datetimeEntry._showField(d);$.datetimeEntry._focussed=false},_doKeyDown:function(a){if(a.keyCode>=48){return true}var b=$.data(a.target,v);switch(a.keyCode){case 9:return(a.shiftKey?$.datetimeEntry._changeField(b,-1,true):$.datetimeEntry._changeField(b,+1,true));case 35:if(a.ctrlKey){$.datetimeEntry._setValue(b,'')}else{b._field=b._fields.length-1;$.datetimeEntry._adjustField(b,0)}break;case 36:if(a.ctrlKey){$.datetimeEntry._setDatetime(b)}else{b._field=0;$.datetimeEntry._adjustField(b,0)}break;case 37:$.datetimeEntry._changeField(b,-1,false);break;case 38:$.datetimeEntry._adjustField(b,+1);break;case 39:$.datetimeEntry._changeField(b,+1,false);break;case 40:$.datetimeEntry._adjustField(b,-1);break;case 46:$.datetimeEntry._setValue(b,'');break}return false},_doKeyPress:function(a){var b=String.fromCharCode(a.charCode==undefined?a.keyCode:a.charCode);if(b<' '){return true}var c=$.data(a.target,v);$.datetimeEntry._handleKeyPress(c,b);return false},_doMouseWheel:function(a,b){if($.datetimeEntry._isDisabledDatetimeEntry(a.target)){return}b=($.browser.opera?-b/Math.abs(b):($.browser.safari?b/Math.abs(b):b));var c=$.data(a.target,v);c.input.focus();if(!c.input.val()){$.datetimeEntry._extractDatetime(c)}$.datetimeEntry._adjustField(c,b);a.preventDefault()},_expandSpinner:function(b){var c=$.datetimeEntry._getSpinnerTarget(b);var d=$.data($.datetimeEntry._getInput(c),v);if($.datetimeEntry._isDisabledDatetimeEntry(d.input[0])){return}var e=$.datetimeEntry._get(d,'spinnerBigImage');if(e){d._expanded=true;var f=$(c).offset();var g=null;$(c).parents().each(function(){var a=$(this);if(a.css('position')=='relative'||a.css('position')=='absolute'){g=a.offset()}return!g});var h=$.datetimeEntry._get(d,'spinnerSize');var i=$.datetimeEntry._get(d,'spinnerBigSize');$('<div class="datetimeEntry_expand" style="position: absolute; left: '+(f.left-(i[0]-h[0])/2-(g?g.left:0))+'px; top: '+(f.top-(i[1]-h[1])/2-(g?g.top:0))+'px; width: '+i[0]+'px; height: '+i[1]+'px; background: #fff url('+e+') no-repeat 0px 0px; z-index: 10;"></div>').mousedown($.datetimeEntry._handleSpinner).mouseup($.datetimeEntry._endSpinner).mouseout($.datetimeEntry._endExpand).mousemove($.datetimeEntry._describeSpinner).insertAfter(c)}},_getInput:function(a){return $(a).siblings('.'+$.datetimeEntry.markerClassName)[0]},_describeSpinner:function(a){var b=$.datetimeEntry._getSpinnerTarget(a);var c=$.data($.datetimeEntry._getInput(b),v);b.title=$.datetimeEntry._get(c,'spinnerTexts')[$.datetimeEntry._getSpinnerRegion(c,a)]},_handleSpinner:function(a){var b=$.datetimeEntry._getSpinnerTarget(a);var c=$.datetimeEntry._getInput(b);if($.datetimeEntry._isDisabledDatetimeEntry(c)){return}if(c==$.datetimeEntry._blurredInput){$.datetimeEntry._lastInput=c;$.datetimeEntry._blurredInput=null}var d=$.data(c,v);$.datetimeEntry._doFocus(c);var e=$.datetimeEntry._getSpinnerRegion(d,a);$.datetimeEntry._changeSpinner(d,b,e);$.datetimeEntry._actionSpinner(d,e);$.datetimeEntry._timer=null;$.datetimeEntry._handlingSpinner=true;var f=$.datetimeEntry._get(d,'spinnerRepeat');if(e>=3&&f[0]){$.datetimeEntry._timer=setTimeout(function(){$.datetimeEntry._repeatSpinner(d,e)},f[0]);$(b).one('mouseout',$.datetimeEntry._releaseSpinner).one('mouseup',$.datetimeEntry._releaseSpinner)}},_actionSpinner:function(a,b){if(!a.input.val()){$.datetimeEntry._extractDatetime(a)}switch(b){case 0:this._setDatetime(a);break;case 1:this._changeField(a,-1,false);break;case 2:this._changeField(a,+1,false);break;case 3:this._adjustField(a,+1);break;case 4:this._adjustField(a,-1);break}},_repeatSpinner:function(a,b){if(!$.datetimeEntry._timer){return}$.datetimeEntry._lastInput=$.datetimeEntry._blurredInput;this._actionSpinner(a,b);this._timer=setTimeout(function(){$.datetimeEntry._repeatSpinner(a,b)},this._get(a,'spinnerRepeat')[1])},_releaseSpinner:function(a){clearTimeout($.datetimeEntry._timer);$.datetimeEntry._timer=null},_endExpand:function(a){$.datetimeEntry._timer=null;var b=$.datetimeEntry._getSpinnerTarget(a);var c=$.datetimeEntry._getInput(b);var d=$.data(c,v);$(b).remove();d._expanded=false},_endSpinner:function(a){$.datetimeEntry._timer=null;var b=$.datetimeEntry._getSpinnerTarget(a);var c=$.datetimeEntry._getInput(b);var d=$.data(c,v);if(!$.datetimeEntry._isDisabledDatetimeEntry(c)){$.datetimeEntry._changeSpinner(d,b,-1)}if($.datetimeEntry._handlingSpinner){$.datetimeEntry._lastInput=$.datetimeEntry._blurredInput}if($.datetimeEntry._lastInput&&$.datetimeEntry._handlingSpinner){$.datetimeEntry._showField(d)}$.datetimeEntry._handlingSpinner=false},_getSpinnerTarget:function(a){return a.target||a.srcElement},_getSpinnerRegion:function(a,b){var c=this._getSpinnerTarget(b);var d=($.browser.opera||$.browser.safari?$.datetimeEntry._findPos(c):$(c).offset());var e=($.browser.safari?$.datetimeEntry._findScroll(c):[document.documentElement.scrollLeft||document.body.scrollLeft,document.documentElement.scrollTop||document.body.scrollTop]);var f=this._get(a,'spinnerIncDecOnly');var g=(f?99:b.clientX+e[0]-d.left-($.browser.msie?2:0));var h=b.clientY+e[1]-d.top-($.browser.msie?2:0);var i=this._get(a,(a._expanded?'spinnerBigSize':'spinnerSize'));var j=(f?99:i[0]-1-g);var k=i[1]-1-h;if(i[2]>0&&Math.abs(g-j)<=i[2]&&Math.abs(h-k)<=i[2]){return 0}var l=Math.min(g,h,j,k);return(l==g?1:(l==j?2:(l==h?3:4)))},_changeSpinner:function(a,b,c){$(b).css('background-position','-'+((c+1)*this._get(a,(a._expanded?'spinnerBigSize':'spinnerSize'))[0])+'px 0px')},_findPos:function(a){var b=curTop=0;if(a.offsetParent){b=a.offsetLeft;curTop=a.offsetTop;while(a=a.offsetParent){var c=b;b+=a.offsetLeft;if(b<0){b=c}curTop+=a.offsetTop}}return{left:b,top:curTop}},_findScroll:function(a){var b=false;$(a).parents().each(function(){b|=$(this).css('position')=='fixed'});if(b){return[0,0]}var c=a.scrollLeft;var d=a.scrollTop;while(a=a.parentNode){c+=a.scrollLeft||0;d+=a.scrollTop||0}return[c,d]},_get:function(a,b){return(a.options[b]!=null?a.options[b]:$.datetimeEntry._defaults[b])},_extractDatetime:function(a){var b=this._parseDatetime(a,$(a.input).val())||this._normaliseDatetime(this._determineDatetime(a,this._get(a,'defaultDatetime'))||new Date());var c=this._constrainTime(a,[b.getHours(),b.getMinutes(),b.getSeconds()]);a._selectedYear=b.getFullYear();a._selectedMonth=b.getMonth();a._selectedDay=b.getDate();a._selectedHour=c[0];a._selectedMinute=c[1];a._selectedSecond=c[2];a._lastChr='';a._field=Math.max(0,this._get(a,'initialField'));if(a.input.val()!=''){this._showDatetime(a)}},_parseDatetime:function(a,b){if(!b){return null}var c=0;var d=0;var e=0;var f=0;var g=0;var h=0;var k=0;var l=this._get(a,'datetimeFormat');var m=function(){while(k<b.length&&b.charAt(k).match(/^[0-9]/)){k++}};var i;for(i=0;i<l.length&&k<b.length;i++){var n=l.charAt(i);var o=parseInt(b.substring(k),10);if(n.match(/y|o|d|h|m|s/i)&&isNaN(o)){throw'Invalid date';}o=(isNaN(o)?0:o);switch(n){case'y':case'Y':c=o;m();break;case'o':case'O':d=o;m();break;case'n':case'N':var p=this._get(a,n=='N'?'monthNames':'monthNamesShort');for(var j=0;j<p.length;j++){if(b.substring(k).substr(0,p[j].length).toLowerCase()==p[j].toLowerCase()){d=j+1;k+=p[j].length;break}}break;case'w':case'W':var q=this._get(a,n=='W'?'dayNames':'dayNamesShort');for(var j=0;j<q.length;j++){if(b.substring(k).substr(0,q[j].length).toLowerCase()==q[j].toLowerCase()){k+=q[j].length+1;break}}o=parseInt(b.substring(k),10);o=(isNaN(o)?0:o);case'd':case'D':e=o;m();break;case'h':case'H':f=o;m();break;case'm':case'M':g=o;m();break;case's':case'S':h=o;m();break;case'a':var r=this._get(a,'ampmNames');var s=(b.substr(k,r[1].length).toLowerCase()==r[1].toLowerCase());f=(f==12?0:f)+(s?12:0);k+=r[0].length;break;default:k++}}if(i<l.length){throw'Invalid date';}c+=(c>=100||l.indexOf('y')==-1?0:(c>this._shortYearCutoff(a)?1900:2000));var t=this._constrainTime(a,[f,g,h]);var u=new Date(c,Math.max(0,d-1),e,t[0],t[1],t[2]);if(l.match(/y|o|n|d|w/i)&&(u.getFullYear()!=c||u.getMonth()+1!=d||u.getDate()!=e)){throw'Invalid date';}return u},_showDatetime:function(a){this._setValue(a,this._formatDatetime(a,this._get(a,'datetimeFormat')));this._showField(a)},_formatDatetime:function(a,b){var c='';var d=b.indexOf('a')>-1;for(var i=0;i<b.length;i++){var e=b.charAt(i);switch(e){case'y':c+=this._formatNumber(a._selectedYear%100);break;case'Y':c+=this._formatNumber(a._selectedYear,4);break;case'o':case'O':c+=this._formatNumber(a._selectedMonth+1,e=='o'?1:2);break;case'n':case'N':c+=this._get(a,(e=='N'?'monthNames':'monthNamesShort'))[a._selectedMonth];break;case'd':case'D':c+=this._formatNumber(a._selectedDay,e=='d'?1:2);break;case'w':case'W':c+=this._get(a,(e=='W'?'dayNames':'dayNamesShort'))[new Date(a._selectedYear,a._selectedMonth,a._selectedDay).getDay()]+' '+this._formatNumber(a._selectedDay);break;case'h':case'H':c+=this._formatNumber(!d?a._selectedHour:a._selectedHour%12||12,e=='h'?1:2);break;case'm':case'M':c+=this._formatNumber(a._selectedMinute,e=='m'?1:2);break;case's':case'S':c+=this._formatNumber(a._selectedSecond,e=='s'?1:2);break;case'a':c+=this._get(a,'ampmNames')[a._selectedHour<12?0:1];break;default:c+=e;break}}return c},_showField:function(a){var b=a.input[0];if(a.input.is(':hidden')||$.datetimeEntry._lastInput!=b){return}var c=this._get(a,'datetimeFormat');var d=0;for(var i=0;i<a._fields[a._field];i++){d+=this._fieldLength(a,c.charAt(i))}var e=d+this._fieldLength(a,c.charAt(i));if(b.setSelectionRange){b.setSelectionRange(d,e)}else if(b.createTextRange){var f=b.createTextRange();f.moveStart('character',d);f.moveEnd('character',e-a.input.val().length);f.select()}if(!b.disabled){b.focus()}},_fieldLength:function(a,b){switch(b){case'Y':return 4;case'n':case'N':return this._get(a,(b=='N'?'monthNames':'monthNamesShort'))[a._selectedMonth].length;case'w':case'W':return this._get(a,(b=='W'?'dayNames':'dayNamesShort'))[new Date(a._selectedYear,a._selectedMonth,a._selectedDay).getDay()].length+3;case'y':case'O':case'D':case'H':case'M':case'S':return 2;case'o':return(''+(a._selectedMonth+1)).length;case'd':return(''+a._selectedDay).length;case'h':return(''+(a._ampmField==-1?a._selectedHour:a._selectedHour%12||12)).length;case'm':return(''+a._selectedMinute).length;case's':return(''+a._selectedSecond).length;case'a':return this._get(a,'ampmNames')[0].length;default:return 1}},_formatNumber:function(a,b){a=''+a;b=b||2;while(a.length<b){a='0'+a}return a},_setValue:function(a,b){if(b!=a.input.val()){var c=this._get(a,'altField');if(c){$(c).val(!b?'':this._formatDatetime(a,this._get(a,'altFormat')||this._get(a,'datetimeFormat')))}a.input.val(b).trigger('change')}},_changeField:function(a,b,c){var d=(a.input.val()==''||a._field==(b==-1?0:a._fields.length-1));if(!d){a._field+=b}this._showField(a);a._lastChr='';$.data(a.input[0],v,a);return(d&&c)},_adjustField:function(a,b){if(a.input.val()==''){b=0}var c=this._get(a,'datetimeFormat').charAt(a._fields[a._field]);var d=a._selectedYear+(c.match(/y/i)?b:0);var e=a._selectedMonth+(c.match(/o|n/i)?b:0);var f=(c.match(/d|w/i)?a._selectedDay+b:Math.min(a._selectedDay,this._getDaysInMonth(d,e)));var g=this._get(a,'timeSteps');var h=a._selectedHour+(c.match(/h/i)?b*g[0]:0)+(c=='a'&&b!=0?(a._selectedHour<12?+12:-12):0);var i=a._selectedMinute+(c.match(/m/i)?b*g[1]:0);var j=a._selectedSecond+(c.match(/s/i)?b*g[2]:0);this._setDatetime(a,new Date(d,e,f,h,i,j))},_getDaysInMonth:function(a,b){return new Date(a,b+1,0).getDate()},_setDatetime:function(a,b){b=this._normaliseDatetime(this._determineDatetime(a,b||this._get(a,'defaultDatetime'))||new Date());var c=this._constrainTime(a,[b.getHours(),b.getMinutes(),b.getSeconds()]);b.setHours(c[0],c[1],c[2]);var d=this._normaliseDatetime(this._determineDatetime(a,this._get(a,'minDatetime')));var e=this._normaliseDatetime(this._determineDatetime(a,this._get(a,'maxDatetime')));var f=this._normaliseDatetime(this._determineDatetime(a,this._get(a,'minTime')),'d');var g=this._normaliseDatetime(this._determineDatetime(a,this._get(a,'maxTime')),'d');b=(d&&b<d?d:(e&&b>e?e:b));if(f&&this._normaliseDatetime(new Date(b.getTime()),'d')<f){this._copyTime(f,b)}if(g&&this._normaliseDatetime(new Date(b.getTime()),'d')>g){this._copyTime(g,b)}a._selectedYear=b.getFullYear();a._selectedMonth=b.getMonth();a._selectedDay=b.getDate();a._selectedHour=b.getHours();a._selectedMinute=b.getMinutes();a._selectedSecond=b.getSeconds();this._showDatetime(a);$.data(a.input[0],v,a)},_copyDate:function(a,b){b.setFullYear(a.getFullYear());b.setMonth(a.getMonth());b.setDate(a.getDate())},_copyTime:function(a,b){b.setHours(a.getHours());b.setMinutes(a.getMinutes());b.setSeconds(a.getSeconds())},_determineDatetime:function(l,m){var n=function(a){var b=new Date();b.setSeconds(b.getSeconds()+a);return b};var o=function(a){var b;try{b=$.datetimeEntry._parseDatetime(l,a);if(b){return b}}catch(e){}a=a.toLowerCase();b=new Date();var c=b.getFullYear();var d=b.getMonth();var f=b.getDate();var g=b.getHours();var h=b.getMinutes();var i=b.getSeconds();var j=/([+-]?[0-9]+)\s*(s|m|h|d|w|o|y)?/g;var k=j.exec(a);while(k){switch(k[2]||'s'){case's':i+=parseInt(k[1],10);break;case'm':h+=parseInt(k[1],10);break;case'h':g+=parseInt(k[1],10);break;case'd':f+=parseInt(k[1],10);break;case'w':f+=parseInt(k[1],10)*7;break;case'o':d+=parseInt(k[1],10);break;case'y':c+=parseInt(k[1],10);break}k=j.exec(a)}return new Date(c,d,f,g,h,i)};return(m?(typeof m=='string'?o(m):(typeof m=='number'?n(m):m)):null)},_normaliseDatetime:function(a,b){if(!a){return null}if(b=='d'){a.setFullYear(0);a.setMonth(0);a.setDate(0)}if(b=='t'){a.setHours(0);a.setMinutes(0);a.setSeconds(0)}a.setMilliseconds(0);return a},_handleKeyPress:function(a,b){b=b.toLowerCase();var c=this._get(a,'datetimeFormat');var d=this._get(a,'datetimeSeparators');var e=c.charAt(a._fields[a._field]);var f=c.charAt(a._fields[a._field]+1);f=('yYoOnNdDwWhHmMsSa'.indexOf(f)==-1?f:'');if((d+f).indexOf(b)>-1){this._changeField(a,+1,false)}else if(b>='0'&&b<='9'){var g=parseInt(b,10);var h=parseInt(a._lastChr+b,10);var j=(!e.match(/y/i)?a._selectedYear:h);var k=(!e.match(/o|n/i)?a._selectedMonth+1:(h>=1&&h<=12?h:(g>0?g:a._selectedMonth+1)));var l=(!e.match(/d|w/i)?a._selectedDay:(h>=1&&h<=this._getDaysInMonth(j,k-1)?h:(g>0?g:a._selectedDay)));var m=(!e.match(/h/i)?a._selectedHour:(a._ampmField==-1?(h<24?h:g):(h>=1&&h<=12?h:(g>0?g:a._selectedHour))%12+(a._selectedHour>=12?12:0)));var n=(!e.match(/m/i)?a._selectedMinute:(h<60?h:g));var o=(!e.match(/s/i)?a._selectedSecond:(h<60?h:g));var p=this._constrainTime(a,[m,n,o]);var q=this._shortYearCutoff(a);this._setDatetime(a,new Date(j+(j>=100||e!='y'?0:(j>q?1900:2000)),k-1,l,p[0],p[1],p[2]));a._lastChr=(e!='Y'?'':a._lastChr.substr(Math.max(0,a._lastChr.length-2)))+b}else if(e.match(/n/i)){a._lastChr+=b;var r=this._get(a,(e=='n'?'monthNamesShort':'monthNames'));var s=function(){for(var i=0;i<r.length;i++){if(r[i].toLowerCase().substring(0,a._lastChr.length)==a._lastChr){return i;break}}return-1};var k=s();if(k==-1){a._lastChr=b;k=s()}if(k==-1){a._lastChr=''}else{var j=a._selectedYear;var l=Math.min(a._selectedDay,this._getDaysInMonth(j,k));this._setDatetime(a,this._normaliseDatetime(new Date(j,k,l,a._selectedHour,a._selectedMinute,a._selectedSecond)))}}else if(a._ampmField>-1){var t=this._get(a,'ampmNames');if((b==t[0].substring(0,1).toLowerCase()&&a._selectedHour>=12)||(b==t[1].substring(0,1).toLowerCase()&&a._selectedHour<12)){var u=a._field;a._field=a._ampmField;this._adjustField(a,+1);a._field=u;this._showField(a)}}},_shortYearCutoff:function(a){var b=this._get(a,'shortYearCutoff');if(typeof b=='string'){b=new Date().getFullYear()+parseInt(b,10)}return b%100},_constrainTime:function(a,b){var c=(b!=null);if(!c){var d=this._determineTime(a,this._get(a,'defaultTime'))||new Date();b=[d.getHours(),d.getMinutes(),d.getSeconds()]}var e=false;var f=this._get(a,'timeSteps');for(var i=0;i<f.length;i++){if(e){b[i]=0}else if(f[i]>1){b[i]=Math.round(b[i]/f[i])*f[i];e=true}}return b}});function extendRemove(a,b){$.extend(a,b);for(var c in b){if(b[c]==null){a[c]=null}}return a}var w=['getDatetime','getOffset','isDisabled'];$.fn.datetimeEntry=function(c){var d=Array.prototype.slice.call(arguments,1);if(typeof c=='string'&&$.inArray(c,w)>-1){return $.datetimeEntry['_'+c+'DatetimeEntry'].apply($.datetimeEntry,[this[0]].concat(d))}return this.each(function(){var a=this.nodeName.toLowerCase();if(a=='input'){if(typeof c=='string'){$.datetimeEntry['_'+c+'DatetimeEntry'].apply($.datetimeEntry,[this].concat(d))}else{var b=($.fn.metadata?$(this).metadata():{});$.datetimeEntry._connectDatetimeEntry(this,$.extend(b,c))}}})};$.datetimeEntry=new DatetimeEntry()})(jQuery);