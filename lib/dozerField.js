var io_hawt_dozer_schema_Field = {
  "type" : "object",
  "properties" : {
    "a" : {
      "type" : "object",
      "properties" : {
        "value" : {
          "type" : "string"
        },
        "type" : {
          "type" : "string",
          "enum" : [ "ITERATE", "GENERIC" ]
        },
        "key" : {
          "type" : "string"
        },
        "date-format" : {
          "type" : "string"
        },
        "set-method" : {
          "type" : "string"
        },
        "get-method" : {
          "type" : "string"
        },
        "map-set-method" : {
          "type" : "string"
        },
        "map-get-method" : {
          "type" : "string"
        },
        "is-accessible" : {
          "type" : "boolean"
        },
        "create-method" : {
          "type" : "string"
        }
      },
      "required" : true
    },
    "b" : {
      "type" : "object",
      "properties" : {
        "value" : {
          "type" : "string"
        },
        "type" : {
          "type" : "string",
          "enum" : [ "ITERATE", "GENERIC" ]
        },
        "key" : {
          "type" : "string"
        },
        "date-format" : {
          "type" : "string"
        },
        "set-method" : {
          "type" : "string"
        },
        "get-method" : {
          "type" : "string"
        },
        "map-set-method" : {
          "type" : "string"
        },
        "map-get-method" : {
          "type" : "string"
        },
        "is-accessible" : {
          "type" : "boolean"
        },
        "create-method" : {
          "type" : "string"
        }
      },
      "required" : true
    },
    "a-hint" : {
      "type" : "string"
    },
    "b-hint" : {
      "type" : "string"
    },
    "a-deep-index-hint" : {
      "type" : "string"
    },
    "b-deep-index-hint" : {
      "type" : "string"
    },
    "type" : {
      "type" : "string",
      "enum" : [ "ONE_WAY", "BI_DIRECTIONAL" ]
    },
    "relationship-type" : {
      "type" : "string",
      "enum" : [ "CUMULATIVE", "NON_CUMULATIVE" ]
    },
    "remove-orphans" : {
      "type" : "boolean"
    },
    "map-id" : {
      "type" : "string"
    },
    "copy-by-reference" : {
      "type" : "boolean"
    },
    "custom-converter" : {
      "type" : "string"
    },
    "custom-converter-id" : {
      "type" : "string"
    },
    "custom-converter-param" : {
      "type" : "string"
    }
  }
};

