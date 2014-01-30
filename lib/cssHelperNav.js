  var resizeByScreen = function () {
    if (window.innerWidth < 900) {
        document.getElementsByClassName("nav-tabs")[1].style.width="900px";
    } else {
        document.getElementsByClassName("nav-tabs")[1].style.width="100%";
    }
  };

  (function () {
      window.addEventListener("resize", resizeByScreen, true);
  })();