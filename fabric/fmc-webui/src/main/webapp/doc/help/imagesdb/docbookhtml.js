// Function to add a listener to an object in the DOM
// Needs two versions so it works on all browsers 

function addEvent(obj, evType, fn){ 
 if (obj.addEventListener) { 
   obj.addEventListener(evType, fn, false); 
   return true; 
 } else if (obj.attachEvent){ 
   var r = obj.attachEvent("on"+evType, fn); 
   return r; 
 } else { 
   return false; 
 } 
}

// Return an array of all the elements of a given classname that are children of
// the node passed in

function getElementsByClassName(node, classname) {
  var a = [];
  var re = new RegExp('\\b' + classname + '\\b');
  var els = node.getElementsByTagName("*");
  for(var i=0,j=els.length; i<j; i++) {
    if(re.test(els[i].className)) {a.push(els[i]);}
  }
  return a; 
}

// Show elements of class 'needsjavascript'
// The page's stylesheet should set these to be display:none
// They will then not display unless javascript is enabled

function showJavascriptElements() {
  var needjs=getElementsByClassName(document,"needsjavascript");
  for (var i=0; i<needjs.length; i++) {
    needjs[i].className="";
  }
}

addEvent(window, 'load', showJavascriptElements);

// Code for the 'toggleframes' button
// Depending on whether we are currently in the top frame or not
// the URL for the toggle frames button is changed so that it either
// opens the current frame as a page on its own, or
// loads the frame, passing in the address of which page to open
// in the body frame.

function addToggleFrames() {
  var toggle=document.getElementById("toggleframes");
  if (!toggle) {
    return;
  }
  toggle.target='_top';
  if (top.location==self.location) {
    toggle.href=toggle.href+"?url=" + location;
  } else {
    toggle.href=self.location.href;
  }
  var linktothis=document.getElementById("linktothis");
  if (!linktothis) {
    return;
  }
  linktothis.target='_top';
  if (top.location==self.location) {
    linktothis.style.display='none';
  } else {
    linktothis.href=linktothis.href+"?url=" + location;
  }
}

// Hide elements of a given class if they are empty

// function hideEmptyElementsOfClass(classname) {
//   var elems=getElementsByClassName(document,classname);
//   for (var i=0; i<elems.length; i++) {
//     if (elems[i].childNodes.length==0) {
//       elems[i].style.display="none"; }
//   }
// }

// Go through the list of frames looking for one containing
// the string 'body' - if the body frame has something else
// then change the implementation of this

function findBodyFrame() {
  var bodyFrame=null;
  for (var i=0; i<top.frames.length && top.frames[i].name.toLowerCase().indexOf("body")==-1; i++) {
  }
  if (i<top.frames.length) {
    return(top.frames[i]); 
  }
}

// Go through the list of frames looking for one containing
// the string 'toc'

function findTocFrame() {
  var bodyFrame=null;
  for (var i=0; i<top.frames.length && top.frames[i].name.toLowerCase().indexOf("toc")==-1; i++) {
  }
  if (i<top.frames.length) {
    return(top.frames[i]); 
  }
}

// Find how many pixels from the top of the document the
// top line of the window is showing (ie how far down
// we have scrolled)

function getScrollPos() {
  // find how many pixels down the content has been scrolled
  
  var currentScrollPos=0;
  if (document.documentElement.scrollTop) {
    currentScrollPos=document.documentElement.scrollTop;
  } else if (document.body.scrollTop) {
    currentScrollPos=document.body.scrollTop;
  }

  return(currentScrollPos);
}

function getCombinedOffsetTop(node) {
  var offset=0;
  while (node!=null) {
    offset=offset+node.offsetTop;
    node=node.offsetParent;
  }
  return(offset);
}

var lastUserClick;

function highlightCurrentScrollPosInToc() {

  var currentScrollPos=getScrollPos();

  // Get the body frame URL without # or bits after it

  var bodyhref=location.href+"#";
  bodyhref=bodyhref.substring(0,bodyhref.indexOf("#"));
    
  var bodyName=window.name;
  var bookmarkName;

  // Get all the links in the contents frame, then iterate over them
  // We are only interested in links to sections of the page that
  // is currently in the body frame.

  // We then want to find the first link that is at or above the
  // current scroll position in the document. This is then
  // market as the current section. 
  
  // We allow for the current item to be slightly down the page by adding a buffer.

  var anchors=findTocFrame().document.getElementsByTagName('a');
  var foundMatch=false;
  for (var i=anchors.length-1; i>=0; i--) {
    if (anchors[i].href.substring(0,bodyhref.length)==bodyhref) {
      bookmarkName=anchors[i].href.substring((anchors[i].href+"#").indexOf("#")+1);
      var bookmark;
      if (bookmarkName=="") {
        bookmark=document.documentElement;
      } else {
        bookmark=document.getElementById(bookmarkName);
      }
      if (!foundMatch && currentScrollPos>=getCombinedOffsetTop(bookmark)-100) {
        anchors[i].className="currentlink";
        foundMatch=true;
      } else {
        anchors[i].className=null;
      }
    } else {
      anchors[i].className=null;
    }
  }
}

function highlightURLInToc(url) {
  var anchors=findTocFrame().document.getElementsByTagName('a');
  for (var i=0; i<anchors.length; i++) {
    if (anchors[i].href==url) {
      anchors[i].className="currentlink";
    } else {
      anchors[i].className=null;
    }
  }
}

// Variable to record where the scrollbar was last time we highlighted
// the contents

var lastScrollPos=-100;

function contentScroll() {
  // If the user has loaded a new body page or clicked on an entry in the contents in the last second, then ignore any scrolling
  // it must be generated by their last activity
  
  if ((new Date())-findTocFrame().lastUserClick<1000) {
    return;
  }
  
  var currentScrollPos=getScrollPos();

  // If the page has scrolled less than 40 pixels vertically since we
  // last checked the right contents entry was highlighted,
  // then don't do anything, don't want to slow the user's computer
  
  if (currentScrollPos<lastScrollPos+40 && currentScrollPos>lastScrollPos-40) {
    return;
  } else {
    lastScrollPos=currentScrollPos;
    highlightCurrentScrollPosInToc();
  }
}

// If the body has changed, update the window's title to match it
// The original title of the page containing the frameset is appended
// to provide more details

function setTitle() {
  var bod=findBodyFrame();
  if (!bod || self.location!=bod.location) {
    return;
  }
  var bookTitle=top.document.getElementsByTagName('frameset')[0].title;
  top.document.title=document.title + (bookTitle?" - " +bookTitle:"");
}

function tocClick() {
  lastUserClick=new Date();
  highlightURLInToc(this.href);
  findBodyFrame().focus();
}

// Do general page setup activities

function setupPage() {
  if (this.frames.length==0) { // this isn't the outer frameset
    addToggleFrames();
    lastUserClick=new Date();
    if (window==findBodyFrame() && this.location!=top.location) {
      lastScrollPos=-100; // make sure the following call does not get shortcutted
      highlightURLInToc(location.href);
      findTocFrame().lastUserClick=new Date();
      window.onscroll=contentScroll;
    }
    if (window==findTocFrame()) {
      var anchors=document.getElementsByTagName('a');
      for (a in anchors)
        anchors[a].onclick=tocClick;
    }
    setTitle();
  } else { // this is the outer frameset
    document.getElementsByTagName('frameset')[0].title=document.title; // store the title as an attribute of the frameset so we can retrieve it later
    var contentUri=getParamValue(parent.location.href,"url"); // read any URL that was specified
    if (contentUri=="") {
      return;
    }
    var bodyFrame=findBodyFrame();
    if (bodyFrame.location!=contentUri) { // if the content pane isn't currently looking at the URL passed in, change it so it does
      bodyFrame.location=contentUri; 
    }
  }
}

// Get the setupPage() function to be run when any window
// (outer frameset or inner frame) loads

addEvent(window, 'load', setupPage);

// Given a URI, parse the bit after the ? to return the value of
// parameter <paramName>

function getParamValue(uri,paramName) {
  var p=uri.indexOf("?");
  if (p==-1) {
    return(''); 
	}
  else {
    var params=uri.substring(p+1).split('&');
    for (var i=0; i<params.length; i++) {
      nameValue=params[i].split("=");
      if (nameValue[0]==paramName) {
        return(nameValue[1]);
      }
    }
  }
  return('');
}

// Prints the current frame or page.
// Called by the Print Page button in each page header.
function printPage() {
  if (self == top) {
    window.print();
  } else {
    self.print();
  }
}

// To avoid web spiders harvesting plain-text e-mail addresses,
// this function obfuscates the address to which document 
// error reports are sent. This code is modified from the 
// code generated by http://www.closetnoc.com/mungemaster/mungemaster.pl
function sendMail()
{
var prkaaaiwpppwj = "om;"// "&#111;&#109;";
var wog8kalwl = "pport"; // "&#112;p&#111;&#114;t";
var gBniHikxFvQRegGuZnFXZwEiIGL = "@"; "&#64;";
var FiXXnFJMDOgnPzh="docs-su"; // "&#100;&#111;c&#115;&#45;s&#117;";
var juQUXxDNmnACEKwnFhHYBVZHdIInjVQv = "fusesource.c"; //"&#105;&#111;n&#97;&#46;c";
var TextWithLocation = "Feedback%20on%20" + document.URL;
var sendto="mailto:" +FiXXnFJMDOgnPzh +wog8kalwl 
  +gBniHikxFvQRegGuZnFXZwEiIGL 
  +juQUXxDNmnACEKwnFhHYBVZHdIInjVQv +prkaaaiwpppwj
  + "?subject=Documentation%20Feedback" 
  + "&"
  + "body=" + TextWithLocation;
document.location.assign(sendto);
}
