
// To add a setting, you MUST define a default.
var Settings = function (s) {
  var _d = {
    elasticsearch : 'localhost:9200',
    modules       : [],
    types         : [],
    kibana_index  : 'kibana-int',
    formatValue   : format_value,
    getSerieColor : get_serie_color,
    dashboards    : []
  }

  // This initializes a new hash on purpose, to avoid adding parameters to 
  // kibanaconfig.js without providing sane defaults
  var _s = {};
  _.each(_d, function(v, k) {
    _s[k] = typeof s[k] !== 'undefined' ? s[k]  : _d[k];
  });

  return _s;

};

function get_serie_color(label, index) {
  var colors = ['#86B22D','#BF6730','#1D7373','#BFB930','#BF3030','#77207D'];
  return colors[index % colors.length];
}

function format_value(source, key, obj) {
  if(typeof obj == 'object' && _.isArray(obj)) {
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
    return typeof obj === 'undefined' ? null : obj.toString();
  }
}

