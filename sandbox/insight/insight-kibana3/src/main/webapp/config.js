/*

elasticsearch:  URL to your elasticsearch server. You almost certainly don't 
                want 'http://localhost:9200' here. Even if Kibana and ES are on
                the same host
kibana_index:   The default ES index to use for storing Kibana specific object
                such as stored dashboards 
modules:        Panel modules to load. In the future these will be inferred 
                from your initial dashboard, though if you share dashboards you
                will probably need to list them all here 

If you need to configure the default dashboard, please see dashboards/default

*/
var config = new Settings(
{
  // By default this will attempt to reach ES at the same host you have
  // elasticsearch installed on. You probably want to set it to the FQDN of your
  // elasticsearch host
  elasticsearch:    "/kibana3/es",
  types:            [],
  // elasticsearch: 'http://localhost:9200',
  kibana_index:     "kibana-int", 
  modules:          ['histogram','map','pie','table','stringquery','sort',
                    'timepicker','text','fields','hits','dashcontrol',
                    'column','derivequeries','trends'],
  formatValue:      doFormatValue,
  getSerieColor:    doGetSerieColor,
  dashboards:       [ {
    content:   "Log",
    title:     "Search in log events",
    file:      "log",
    default:   true,
    isVisible: function() { return true; }
  }, {
    content:   "Camel",
    title:     "Search in camel exchanges",
    file:      "camel",
    isVisible: function() { return true; }
  }]
});

function doGetSerieColor(label, index) {
  var colors = ['#86B22D','#BF6730','#1D7373','#BFB930','#BF3030','#77207D'];
  if (label == "info" || label == "completed") {
    return colors[0];
  } else if (label == "warn") {
    return colors[1];
  } else if (label == "error" || label == "failed") {
    return colors[4];
  } else {
    return colors[index % colors.length];
  }
}

function doFormatValue(source, key, obj) {
    if (key == 'exception') {
        return obj.map(formatStackLine).join('\n');
    } else if (key == 'timestamp' && source == 'table') {
        return reformatDate(obj);
    } else if (key == 'exchange.id' && source == 'table') {
        return "<a href=\"#/camin/" + obj + "\">" + obj + "</a>";
    } else if(typeof obj == 'object' ) {
      if(_.isArray(obj)) {
        if(obj.length > 0 && typeof obj[0] === 'object') {
          var strval = '';
          for (var objidx = 0, objlen = obj.length; objidx < objlen; objidx++) {
            if (objidx > 0) {
              strval = strval + ', ';
            }
            strval = strval + JSON.stringify(obj[objidx]);
          }
          return strval;
        } else if(obj.length === 1 && _.isNumber(obj[0])) {
          return parseFloat(obj[0]);
        } else {
          return typeof obj === 'undefined' ? null : obj.join(',');
        }
      } else {
          obj = sortObj(obj);
          var strval = '';
          for (var key in obj) {
            if (key.substring(0, 2) != '$$') {
              if (strval != '') {
                strval = strval + '\n';
              }
              strval = strval + key + ": " + JSON.stringify(obj[key]);
            }
          }
        return strval;
      }
    } else {
      return typeof obj === 'undefined' ? null : obj.toString();
    }
}

var _dateRegex = /^(\d{4}|[+\-]\d{6})(?:-(\d{2})(?:-(\d{2}))?)?(?:T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{3}))?)?(?:(Z)|([+\-])(\d{2})(?::(\d{2}))?)?)?$/;
//                   1 YYYY                2 MM       3 DD           4 HH    5 mm       6 ss        7 msec        8 Z 9 ±    10 tzHH    11 tzmm
var numericKeys = [ 1, 4, 5, 6, 7, 10, 11 ];

function reformatDate(date) {
    var timestamp, struct, minutesOffset = 0;
    if ((struct = _dateRegex.exec(date))) {
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
        timestamp = moment(Date.UTC(struct[1], struct[2], struct[3], struct[4], struct[5] + minutesOffset, struct[6], struct[7]));
		if (timestamp.year() == moment().year()) {
			if (timestamp.month() == moment().month() && timestamp.date() == moment().date()) {
	          return timestamp.format('HH:mm:ss');
			} else {
	          return timestamp.format('MM/DD HH:mm:ss');
			}
		} else {
          return timestamp.format('YYYY/MM/DD HH:mm:ss');
		}
    }
    else {
        return date;
    }
}

var _stackRegex = /\s*at\s+([\w\.$_]+(\.([\w$_]+))*)\((.*)?:(\d+)\).*\[(.*)\]/;

function formatStackLine(line) {
  var match = _stackRegex.exec(line);
  if (match && match.length > 6) {
    var classAndMethod = match[1];
    var fileName = match[4];
    var line = match[5];
    var mvnCoords = match[6];
    // we can ignore line if its not present...
    if (classAndMethod && fileName && mvnCoords) {
      var className = classAndMethod;
      var idx = classAndMethod.lastIndexOf('.');
      if (idx > 0) {
        className = classAndMethod.substring(0, idx);
      }
      var link = "../hawtio/index.html#/source/view/" + mvnCoords + "/class/" + className + "/" + fileName;
      if (angular.isDefined(line)) {
        link += "?line=" + line;
      }
      return "\tat <a href='" + link + "'>" + classAndMethod + "</a>(<span class='fileName'>" + fileName + "</span>:<span class='lineNumber'>" + line + "</span>)";
    }
  }
  return line;
}


