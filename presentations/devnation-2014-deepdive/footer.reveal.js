/*
  A printale footer (optionally with slide numbers) for reveal.js
  
  # Limitations
  - at present, does not count basement slides... all slides in a vertical
    stack will have the same footer on the screen, and only the first
    one will get a page number on the PDF.
  
  # Usage: TL;DR
      ...
      <footer style="display:none;">
        <div style="float:left;">{{ CURRENT_SLIDE }} of {{ NUM_SLIDES }}</div>
      </footer>
      ...
      Reveal.initialize({
        ...
        dependencies: [
        ...
          {
            src: 'footer.reveal.js',
            condition: function() { return !!document.querySelector( 'footer' ); } 
          }
        ...
        ]
        ...
      })
  
  # Or, if you like bullets...
  - drop this file next to index.html
  - add a `<footer style="display:none;"></footer>` to your `index.html`
    - it shouldn't matter where, but right before the `<script>` tags seems to
      work just fine
  - fill it with what you want in your footer
  - optionally, add mustache-like fields:
    - `{{ CURRENT_SLIDE }}` the current number of the horizontal slide
    - `{{ NUM_SLIDES }}` the total number of slides
  - in the call to `Reveal.initialize`, add this to `dependencies`

      {
        src: 'footer.reveal.js',
        condition: function() { return !!document.querySelector( 'footer' ); } 
      }

  - F5 :)
*/

;(function(Reveal){
  "use strict";
  
  // just say no to superglobals
  var window = this,
    document = window.document;
  
  // install events
  Reveal.addEventListener('ready', on_ready);  
  Reveal.addEventListener('slidechanged', on_slidechanged);  
  
  
  function on_slidechanged(evt){
    var section = evt.currentSlide,
      footer = section.querySelector("footer");
    if(!footer){return;}
    
    update_style(section, footer);
  }
  
  function on_ready(){
    var factory = footer_factory(),
      // things get all crazy if you don't specify just children of .slides
      sections = document.querySelectorAll(".reveal .slides > section"),
      idx,
      len = sections.length;
      
    // add some best-guess styles for printing
    patch_print_css(document.querySelector(".slides"));
    
    for(idx = 0; idx < len; idx++){
      var section = sections[idx],
        // gin up the templated footer
        footer = factory({
          NUM_SLIDES: sections.length,
          CURRENT_SLIDE: idx + 1
        });
      // a bit of bookkeeping, if you need it later
      section.classList.add("has_footer");
      // we do this on_ready as well, as some of the metrics are inaccurate
      // the first time out
      update_style(section, footer);
      // add the styled footer... it must be prepended, as only `margin-top`
      // seems to not break printing
      section.insertBefore(footer, section.firstChild);
    }
  }
  
  function update_style(section, footer){
    // relevant metrics
    var sec_h = section.clientHeight,
      stack = document.querySelector(".slides"),
      stack_w = stack.clientWidth,
      stack_h = stack.clientHeight;
    
    // this is what reveal is doing for visible slides
    // slide.style.top = Math.max( - ( slide.offsetHeight / 2 ) - 20, -slideHeight / 2 ) + 'px';
    
    // i miss you jQuery
    var styles = {
      position: "fixed",
      display: "block",
      fontSize: "50%",
      marginTop: Math.min(
          (stack_h/2) + (Math.min(sec_h, stack_h)/2),
          stack_h) + "px",
      width: stack_w + "px"
    },
    style_idx;
    
    for(style_idx in styles){
      footer.style[style_idx] = styles[style_idx];
    }
  }
  
  function patch_print_css(slides){
    // this is not great
    document.querySelector("head").appendChild(
      document.createTextNode(  
        '<style>@media print{section footer{margin-top: ' +
        slides.style.height +
        'px !important;}}</style>'
      )
    );
  }
  
  function footer_factory(){
    var footer = document.querySelector("footer"),
      tmpl_re = /\{{2}\s*([^}\s]+)\s*\}{2}/g,
      repl = function(opts){
        return function(match, opt){
          return opts[opt];
        }
      };
    
    return function(opts){
      var clone = footer.cloneNode();
      clone.innerHTML = footer.innerHTML.replace(tmpl_re, repl(opts));
      return clone;
    }
  }

}).call(this, Reveal);

