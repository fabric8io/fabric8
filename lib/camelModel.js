var _apacheCamelModel = {
  "definitions": {
    "endpoint": {
      "title": "Endpoint",
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "group": "Endpoints",
      "description": "Represents a camel endpoint which is used to consume messages or send messages to some kind of middleware or technology",
      "icon": "endpoint24.png",
      "properties": {
        "uri": {
          "type": "string"
        },
        "ref": {
          "type": "string"
        }
      }
    },
    "from": {
      "title": "From",
      "type": "object",
      "extends": {
        "type": "endpoint"
      },
      "description": "Consumes from an endpoint",
      "tooltip": "Consumes from an endpoint",
      "icon": "endpoint24.png"
    },
    "to": {
      "title": "To",
      "type": "object",
      "extends": {
        "type": "endpoint"
      },
      "description": "Sends messages to an endpoint",
      "tooltip": "Sends messages to an endpoint",
      "icon": "endpoint24.png"
    },
    "route": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Route",
      "group": "Miscellaneous",
      "description": "Route",
      "tooltip": "Route",
      "icon": "route24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "autoStartup": {
          "type": "java.lang.String",
          "description": "autoStartup",
          "tooltip": "autoStartup",
          "optional": true,
          "title": "autoStartup"
        },
        "delayer": {
          "type": "java.lang.String",
          "description": "delayer",
          "tooltip": "delayer",
          "optional": true,
          "title": "delayer"
        },
        "errorHandlerRef": {
          "type": "java.lang.String",
          "description": "errorHandlerRef",
          "tooltip": "errorHandlerRef",
          "optional": true,
          "title": "errorHandlerRef"
        },
        "group": {
          "type": "java.lang.String",
          "description": "group",
          "tooltip": "group",
          "optional": true,
          "title": "group"
        },
        "handleFault": {
          "type": "java.lang.String",
          "description": "handleFault",
          "tooltip": "handleFault",
          "optional": true,
          "title": "handleFault"
        },
        "messageHistory": {
          "type": "java.lang.String",
          "description": "messageHistory",
          "tooltip": "messageHistory",
          "optional": true,
          "title": "messageHistory"
        },
        "routePolicyRef": {
          "type": "java.lang.String",
          "description": "routePolicyRef",
          "tooltip": "routePolicyRef",
          "optional": true,
          "title": "routePolicyRef"
        },
        "streamCache": {
          "type": "java.lang.String",
          "description": "streamCache",
          "tooltip": "streamCache",
          "optional": true,
          "title": "streamCache"
        },
        "trace": {
          "type": "java.lang.String",
          "description": "trace",
          "tooltip": "trace",
          "optional": true,
          "title": "trace"
        }
      }
    },
    "org.apache.camel.model.OptionalIdentifiedDefinition": {
      "type": "object",
      "properties": {
        "id": {
          "type": "string"
        },
        "description": {
          "type": "string",
          "formTemplate": "<textarea class='input-xxlarge' rows='8'></textarea>"
        },
        "inheritErrorHandler": {
          "type": "java.lang.Boolean"
        }
      }
    },
    "org.apache.camel.model.language.ExpressionDefinition": {
      "description": "A Camel expression",
      "tooltip": "Pick an expression language and enter an expression",
      "type": "object",
      "properties": {
        "expression": {
          "type": "java.lang.String",
          "description": "The expression",
          "tooltip": "Enter the expression in your chosen language syntax",
          "title": "Expression"
        },
        "language": {
          "type": "string",
          "enum": [
            "constant",
            "el",
            "header",
            "javaScript",
            "jxpath",
            "method",
            "mvel",
            "ognl",
            "groovy",
            "property",
            "python",
            "php",
            "ref",
            "ruby",
            "simple",
            "spel",
            "sql",
            "tokenize",
            "xpath",
            "xquery"
          ],
          "description": "The camel expression language ot use",
          "tooltip": "Pick the expression language you want to use",
          "title": "Language"
        }
      }
    },
    "aggregate": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Aggregate",
      "group": "Routing",
      "description": "Aggregate",
      "tooltip": "Aggregate",
      "icon": "aggregate24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "correlationExpression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "correlationExpression",
          "tooltip": "correlationExpression",
          "title": "correlationExpression"
        },
        "completionPredicate": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "completionPredicate",
          "tooltip": "completionPredicate",
          "title": "completionPredicate"
        },
        "completionTimeoutExpression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "completionTimeoutExpression",
          "tooltip": "completionTimeoutExpression",
          "title": "completionTimeoutExpression"
        },
        "completionSizeExpression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "completionSizeExpression",
          "tooltip": "completionSizeExpression",
          "title": "completionSizeExpression"
        },
        "executorServiceRef": {
          "type": "java.lang.String",
          "description": "executorServiceRef",
          "tooltip": "executorServiceRef",
          "title": "executorServiceRef"
        },
        "timeoutCheckerExecutorServiceRef": {
          "type": "java.lang.String",
          "description": "timeoutCheckerExecutorServiceRef",
          "tooltip": "timeoutCheckerExecutorServiceRef",
          "title": "timeoutCheckerExecutorServiceRef"
        },
        "aggregationRepositoryRef": {
          "type": "java.lang.String",
          "description": "aggregationRepositoryRef",
          "tooltip": "aggregationRepositoryRef",
          "title": "aggregationRepositoryRef"
        },
        "strategyRef": {
          "type": "java.lang.String",
          "description": "strategyRef",
          "tooltip": "strategyRef",
          "title": "strategyRef"
        },
        "optimisticLockRetryPolicyDefinition": {
          "type": "org.apache.camel.model.OptimisticLockRetryPolicyDefinition",
          "description": "optimisticLockRetryPolicyDefinition",
          "tooltip": "optimisticLockRetryPolicyDefinition",
          "title": "optimisticLockRetryPolicyDefinition"
        },
        "parallelProcessing": {
          "type": "java.lang.Boolean",
          "description": "parallelProcessing",
          "tooltip": "parallelProcessing",
          "title": "parallelProcessing"
        },
        "optimisticLocking": {
          "type": "java.lang.Boolean",
          "description": "optimisticLocking",
          "tooltip": "optimisticLocking",
          "title": "optimisticLocking"
        },
        "completionSize": {
          "type": "java.lang.Integer",
          "description": "completionSize",
          "tooltip": "completionSize",
          "title": "completionSize"
        },
        "completionInterval": {
          "type": "java.lang.Long",
          "description": "completionInterval",
          "tooltip": "completionInterval",
          "title": "completionInterval"
        },
        "completionTimeout": {
          "type": "java.lang.Long",
          "description": "completionTimeout",
          "tooltip": "completionTimeout",
          "title": "completionTimeout"
        },
        "completionFromBatchConsumer": {
          "type": "java.lang.Boolean",
          "description": "completionFromBatchConsumer",
          "tooltip": "completionFromBatchConsumer",
          "title": "completionFromBatchConsumer"
        },
        "groupExchanges": {
          "type": "java.lang.Boolean",
          "description": "groupExchanges",
          "tooltip": "groupExchanges",
          "title": "groupExchanges"
        },
        "eagerCheckCompletion": {
          "type": "java.lang.Boolean",
          "description": "eagerCheckCompletion",
          "tooltip": "eagerCheckCompletion",
          "title": "eagerCheckCompletion"
        },
        "ignoreInvalidCorrelationKeys": {
          "type": "java.lang.Boolean",
          "description": "ignoreInvalidCorrelationKeys",
          "tooltip": "ignoreInvalidCorrelationKeys",
          "title": "ignoreInvalidCorrelationKeys"
        },
        "closeCorrelationKeyOnCompletion": {
          "type": "java.lang.Integer",
          "description": "closeCorrelationKeyOnCompletion",
          "tooltip": "closeCorrelationKeyOnCompletion",
          "title": "closeCorrelationKeyOnCompletion"
        },
        "discardOnCompletionTimeout": {
          "type": "java.lang.Boolean",
          "description": "discardOnCompletionTimeout",
          "tooltip": "discardOnCompletionTimeout",
          "title": "discardOnCompletionTimeout"
        },
        "forceCompletionOnStop": {
          "type": "java.lang.Boolean",
          "description": "forceCompletionOnStop",
          "tooltip": "forceCompletionOnStop",
          "title": "forceCompletionOnStop"
        }
      }
    },
    "AOP": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "AOP",
      "group": "Miscellaneous",
      "description": "AOP",
      "tooltip": "AOP",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "beforeUri": {
          "type": "java.lang.String",
          "description": "beforeUri",
          "tooltip": "beforeUri",
          "title": "beforeUri"
        },
        "afterUri": {
          "type": "java.lang.String",
          "description": "afterUri",
          "tooltip": "afterUri",
          "title": "afterUri"
        },
        "afterFinallyUri": {
          "type": "java.lang.String",
          "description": "afterFinallyUri",
          "tooltip": "afterFinallyUri",
          "title": "afterFinallyUri"
        }
      }
    },
    "bean": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Bean",
      "group": "Endpoints",
      "description": "Bean",
      "tooltip": "Bean",
      "icon": "bean24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "ref": {
          "control": "combo",
          "kind": "beanRef",
          "title": "ref",
          "type": "java.lang.String",
          "description": "ref",
          "tooltip": "ref"
        },
        "method": {
          "control": "combo",
          "kind": "beanMethod",
          "title": "method",
          "type": "java.lang.String",
          "description": "method",
          "tooltip": "method"
        },
        "beanType": {
          "type": "java.lang.String",
          "description": "beanType",
          "tooltip": "beanType",
          "title": "beanType"
        }
      }
    },
    "catch": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Catch",
      "group": "Control Flow",
      "description": "Catch",
      "tooltip": "Catch",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "exceptions": {
          "type": "java.util.List",
          "description": "exceptions",
          "tooltip": "exceptions",
          "title": "exceptions"
        },
        "handled": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "handled",
          "tooltip": "handled",
          "title": "handled"
        }
      }
    },
    "choice": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Choice",
      "group": "Routing",
      "description": "Choice",
      "tooltip": "Choice",
      "icon": "choice24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {}
    },
    "convertBody": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Convert Body",
      "group": "Transformation",
      "description": "Convert Body",
      "tooltip": "Convert Body",
      "icon": "convertBody24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "type": {
          "type": "java.lang.String",
          "description": "type",
          "tooltip": "type",
          "title": "type"
        },
        "charset": {
          "type": "java.lang.String",
          "description": "charset",
          "tooltip": "charset",
          "title": "charset"
        }
      }
    },
    "delay": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Delay",
      "group": "Control Flow",
      "description": "Delay",
      "tooltip": "Delay",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        },
        "executorServiceRef": {
          "type": "java.lang.String",
          "description": "executorServiceRef",
          "tooltip": "executorServiceRef",
          "title": "executorServiceRef"
        },
        "asyncDelayed": {
          "type": "java.lang.Boolean",
          "description": "asyncDelayed",
          "tooltip": "asyncDelayed",
          "title": "asyncDelayed"
        },
        "callerRunsWhenRejected": {
          "type": "java.lang.Boolean",
          "description": "callerRunsWhenRejected",
          "tooltip": "callerRunsWhenRejected",
          "title": "callerRunsWhenRejected"
        }
      }
    },
    "dynamicRouter": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Dynamic Router",
      "group": "Routing",
      "description": "Dynamic Router",
      "tooltip": "Dynamic Router",
      "icon": "dynamicRouter24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        },
        "uriDelimiter": {
          "type": "java.lang.String",
          "description": "uriDelimiter",
          "tooltip": "uriDelimiter",
          "title": "uriDelimiter"
        },
        "ignoreInvalidEndpoints": {
          "type": "java.lang.Boolean",
          "description": "ignoreInvalidEndpoints",
          "tooltip": "ignoreInvalidEndpoints",
          "title": "ignoreInvalidEndpoints"
        }
      }
    },
    "enrich": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Enrich",
      "group": "Transformation",
      "description": "Enrich",
      "tooltip": "Enrich",
      "icon": "enrich24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "resourceUri": {
          "type": "java.lang.String",
          "description": "resourceUri",
          "tooltip": "resourceUri",
          "title": "resourceUri"
        },
        "aggregationStrategyRef": {
          "type": "java.lang.String",
          "description": "aggregationStrategyRef",
          "tooltip": "aggregationStrategyRef",
          "title": "aggregationStrategyRef"
        }
      }
    },
    "filter": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Filter",
      "group": "Routing",
      "description": "Filter",
      "tooltip": "Filter",
      "icon": "filter24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        }
      }
    },
    "finally": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Finally",
      "group": "Control Flow",
      "description": "Finally",
      "tooltip": "Finally",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {}
    },
    "idempotentConsumer": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Idempotent Consumer",
      "group": "Routing",
      "description": "Idempotent Consumer",
      "tooltip": "Idempotent Consumer",
      "icon": "idempotentConsumer24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        },
        "messageIdRepositoryRef": {
          "type": "java.lang.String",
          "description": "messageIdRepositoryRef",
          "tooltip": "messageIdRepositoryRef",
          "title": "messageIdRepositoryRef"
        },
        "eager": {
          "type": "java.lang.Boolean",
          "description": "eager",
          "tooltip": "eager",
          "title": "eager"
        },
        "skipDuplicate": {
          "type": "java.lang.Boolean",
          "description": "skipDuplicate",
          "tooltip": "skipDuplicate",
          "title": "skipDuplicate"
        },
        "removeOnFailure": {
          "type": "java.lang.Boolean",
          "description": "removeOnFailure",
          "tooltip": "removeOnFailure",
          "title": "removeOnFailure"
        }
      }
    },
    "inOnly": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "In Only",
      "group": "Transformation",
      "description": "In Only",
      "tooltip": "In Only",
      "icon": "transform24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "uri": {
          "type": "java.lang.String",
          "description": "uri",
          "tooltip": "uri",
          "title": "uri"
        }
      }
    },
    "inOut": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "In Out",
      "group": "Transformation",
      "description": "In Out",
      "tooltip": "In Out",
      "icon": "transform24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "uri": {
          "type": "java.lang.String",
          "description": "uri",
          "tooltip": "uri",
          "title": "uri"
        }
      }
    },
    "intercept": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Intercept",
      "group": "Control Flow",
      "description": "Intercept",
      "tooltip": "Intercept",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {}
    },
    "interceptFrom": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Intercept From",
      "group": "Control Flow",
      "description": "Intercept From",
      "tooltip": "Intercept From",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "uri": {
          "type": "java.lang.String",
          "description": "uri",
          "tooltip": "uri",
          "title": "uri"
        }
      }
    },
    "interceptSendToEndpoint": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Intercept Send To Endpoint",
      "group": "Control Flow",
      "description": "Intercept Send To Endpoint",
      "tooltip": "Intercept Send To Endpoint",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "uri": {
          "type": "java.lang.String",
          "description": "uri",
          "tooltip": "uri",
          "title": "uri"
        },
        "skipSendToOriginalEndpoint": {
          "type": "java.lang.Boolean",
          "description": "skipSendToOriginalEndpoint",
          "tooltip": "skipSendToOriginalEndpoint",
          "title": "skipSendToOriginalEndpoint"
        }
      }
    },
    "loadBalance": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Load Balance",
      "group": "Routing",
      "description": "Load Balance",
      "tooltip": "Load Balance",
      "icon": "loadBalance24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "ref": {
          "type": "java.lang.String",
          "description": "ref",
          "tooltip": "ref",
          "title": "ref"
        },
        "loadBalancerType": {
          "type": [
            "org.apache.camel.model.loadbalancer.FailoverLoadBalancerDefinition",
            "org.apache.camel.model.loadbalancer.RandomLoadBalancerDefinition",
            "org.apache.camel.model.loadbalancer.CustomLoadBalancerDefinition",
            "org.apache.camel.model.loadbalancer.RoundRobinLoadBalancerDefinition",
            "org.apache.camel.model.loadbalancer.StickyLoadBalancerDefinition",
            "org.apache.camel.model.loadbalancer.TopicLoadBalancerDefinition",
            "org.apache.camel.model.loadbalancer.WeightedLoadBalancerDefinition"
          ],
          "description": "loadBalancerType",
          "tooltip": "loadBalancerType",
          "title": "loadBalancerType"
        }
      }
    },
    "log": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Log",
      "group": "Endpoints",
      "description": "Log",
      "tooltip": "Log",
      "icon": "log24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "message": {
          "type": "java.lang.String",
          "description": "message",
          "tooltip": "message",
          "title": "message"
        },
        "logName": {
          "type": "java.lang.String",
          "description": "logName",
          "tooltip": "logName",
          "title": "logName"
        },
        "marker": {
          "type": "java.lang.String",
          "description": "marker",
          "tooltip": "marker",
          "title": "marker"
        },
        "loggingLevel": {
          "type": "org.apache.camel.LoggingLevel",
          "description": "loggingLevel",
          "tooltip": "loggingLevel",
          "title": "loggingLevel"
        }
      }
    },
    "loop": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Loop",
      "group": "Control Flow",
      "description": "Loop",
      "tooltip": "Loop",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        },
        "copy": {
          "type": "java.lang.Boolean",
          "description": "copy",
          "tooltip": "copy",
          "title": "copy"
        }
      }
    },
    "marshal": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Marshal",
      "group": "Transformation",
      "description": "Marshal",
      "tooltip": "Marshal",
      "icon": "marshal24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "ref": {
          "type": "java.lang.String",
          "description": "ref",
          "tooltip": "ref",
          "title": "ref"
        },
        "dataFormatType": {
          "type": [
            "org.apache.camel.model.dataformat.AvroDataFormat",
            "org.apache.camel.model.dataformat.Base64DataFormat",
            "org.apache.camel.model.dataformat.BeanioDataFormat",
            "org.apache.camel.model.dataformat.BindyDataFormat",
            "org.apache.camel.model.dataformat.CastorDataFormat",
            "org.apache.camel.model.dataformat.C24IODataFormat",
            "org.apache.camel.model.dataformat.CryptoDataFormat",
            "org.apache.camel.model.dataformat.CsvDataFormat",
            "org.apache.camel.model.dataformat.CustomDataFormat",
            "org.apache.camel.model.dataformat.FlatpackDataFormat",
            "org.apache.camel.model.dataformat.GzipDataFormat",
            "org.apache.camel.model.dataformat.HL7DataFormat",
            "org.apache.camel.model.dataformat.JaxbDataFormat",
            "org.apache.camel.model.dataformat.JibxDataFormat",
            "org.apache.camel.model.dataformat.JsonDataFormat",
            "org.apache.camel.model.dataformat.ProtobufDataFormat",
            "org.apache.camel.model.dataformat.RssDataFormat",
            "org.apache.camel.model.dataformat.XMLSecurityDataFormat",
            "org.apache.camel.model.dataformat.SerializationDataFormat",
            "org.apache.camel.model.dataformat.SoapJaxbDataFormat",
            "org.apache.camel.model.dataformat.StringDataFormat",
            "org.apache.camel.model.dataformat.SyslogDataFormat",
            "org.apache.camel.model.dataformat.TidyMarkupDataFormat",
            "org.apache.camel.model.dataformat.XMLBeansDataFormat",
            "org.apache.camel.model.dataformat.XmlJsonDataFormat",
            "org.apache.camel.model.dataformat.XmlRpcDataFormat",
            "org.apache.camel.model.dataformat.XStreamDataFormat",
            "org.apache.camel.model.dataformat.PGPDataFormat",
            "org.apache.camel.model.dataformat.ZipDataFormat",
            "org.apache.camel.model.dataformat.ZipFileDataFormat"
          ],
          "description": "dataFormatType",
          "tooltip": "dataFormatType",
          "title": "dataFormatType"
        }
      }
    },
    "multicast": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Multicast",
      "group": "Routing",
      "description": "Multicast",
      "tooltip": "Multicast",
      "icon": "multicast24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "strategyRef": {
          "type": "java.lang.String",
          "description": "strategyRef",
          "tooltip": "strategyRef",
          "title": "strategyRef"
        },
        "executorServiceRef": {
          "type": "java.lang.String",
          "description": "executorServiceRef",
          "tooltip": "executorServiceRef",
          "title": "executorServiceRef"
        },
        "onPrepareRef": {
          "type": "java.lang.String",
          "description": "onPrepareRef",
          "tooltip": "onPrepareRef",
          "title": "onPrepareRef"
        },
        "parallelProcessing": {
          "type": "java.lang.Boolean",
          "description": "parallelProcessing",
          "tooltip": "parallelProcessing",
          "title": "parallelProcessing"
        },
        "streaming": {
          "type": "java.lang.Boolean",
          "description": "streaming",
          "tooltip": "streaming",
          "title": "streaming"
        },
        "stopOnException": {
          "type": "java.lang.Boolean",
          "description": "stopOnException",
          "tooltip": "stopOnException",
          "title": "stopOnException"
        },
        "timeout": {
          "type": "java.lang.Long",
          "description": "timeout",
          "tooltip": "timeout",
          "title": "timeout"
        },
        "shareUnitOfWork": {
          "type": "java.lang.Boolean",
          "description": "shareUnitOfWork",
          "tooltip": "shareUnitOfWork",
          "title": "shareUnitOfWork"
        }
      }
    },
    "onCompletion": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "On Completion",
      "group": "Control Flow",
      "description": "On Completion",
      "tooltip": "On Completion",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "executorServiceRef": {
          "type": "java.lang.String",
          "description": "executorServiceRef",
          "tooltip": "executorServiceRef",
          "title": "executorServiceRef"
        },
        "onCompleteOnly": {
          "type": "java.lang.Boolean",
          "description": "onCompleteOnly",
          "tooltip": "onCompleteOnly",
          "title": "onCompleteOnly"
        },
        "onFailureOnly": {
          "type": "java.lang.Boolean",
          "description": "onFailureOnly",
          "tooltip": "onFailureOnly",
          "title": "onFailureOnly"
        },
        "useOriginalMessagePolicy": {
          "type": "java.lang.Boolean",
          "description": "useOriginalMessagePolicy",
          "tooltip": "useOriginalMessagePolicy",
          "title": "useOriginalMessagePolicy"
        }
      }
    },
    "onException": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "On Exception",
      "group": "Control Flow",
      "description": "On Exception",
      "tooltip": "On Exception",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "exceptions": {
          "type": "java.util.List",
          "description": "exceptions",
          "tooltip": "exceptions",
          "title": "exceptions"
        },
        "retryWhile": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "retryWhile",
          "tooltip": "retryWhile",
          "title": "retryWhile"
        },
        "redeliveryPolicyRef": {
          "type": "java.lang.String",
          "description": "redeliveryPolicyRef",
          "tooltip": "redeliveryPolicyRef",
          "title": "redeliveryPolicyRef"
        },
        "handled": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "handled",
          "tooltip": "handled",
          "title": "handled"
        },
        "continued": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "continued",
          "tooltip": "continued",
          "title": "continued"
        },
        "onRedeliveryRef": {
          "type": "java.lang.String",
          "description": "onRedeliveryRef",
          "tooltip": "onRedeliveryRef",
          "title": "onRedeliveryRef"
        },
        "redeliveryPolicy": {
          "type": "org.apache.camel.model.RedeliveryPolicyDefinition",
          "description": "redeliveryPolicy",
          "tooltip": "redeliveryPolicy",
          "title": "redeliveryPolicy"
        },
        "useOriginalMessagePolicy": {
          "type": "java.lang.Boolean",
          "description": "useOriginalMessagePolicy",
          "tooltip": "useOriginalMessagePolicy",
          "title": "useOriginalMessagePolicy"
        }
      }
    },
    "otherwise": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Otherwise",
      "group": "Routing",
      "description": "Otherwise",
      "tooltip": "Otherwise",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {}
    },
    "pipeline": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Pipeline",
      "group": "Routing",
      "description": "Pipeline",
      "tooltip": "Pipeline",
      "icon": "pipeline24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {}
    },
    "policy": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Policy",
      "group": "Miscellaneous",
      "description": "Policy",
      "tooltip": "Policy",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "nextSiblingAddedAsChild": true,
      "properties": {
        "ref": {
          "type": "java.lang.String",
          "description": "ref",
          "tooltip": "ref",
          "title": "ref"
        }
      }
    },
    "pollEnrich": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Poll Enrich",
      "group": "Transformation",
      "description": "Poll Enrich",
      "tooltip": "Poll Enrich",
      "icon": "pollEnrich24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "resourceUri": {
          "type": "java.lang.String",
          "description": "resourceUri",
          "tooltip": "resourceUri",
          "title": "resourceUri"
        },
        "aggregationStrategyRef": {
          "type": "java.lang.String",
          "description": "aggregationStrategyRef",
          "tooltip": "aggregationStrategyRef",
          "title": "aggregationStrategyRef"
        },
        "timeout": {
          "type": "java.lang.Long",
          "description": "timeout",
          "tooltip": "timeout",
          "title": "timeout"
        }
      }
    },
    "process": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Process",
      "group": "Endpoints",
      "description": "Process",
      "tooltip": "Process",
      "icon": "process24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "ref": {
          "type": "java.lang.String",
          "description": "ref",
          "tooltip": "ref",
          "title": "ref"
        }
      }
    },
    "recipientList": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Recipient List",
      "group": "Routing",
      "description": "Recipient List",
      "tooltip": "Recipient List",
      "icon": "recipientList24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        },
        "delimiter": {
          "type": "java.lang.String",
          "description": "delimiter",
          "tooltip": "delimiter",
          "title": "delimiter"
        },
        "strategyRef": {
          "type": "java.lang.String",
          "description": "strategyRef",
          "tooltip": "strategyRef",
          "title": "strategyRef"
        },
        "executorServiceRef": {
          "type": "java.lang.String",
          "description": "executorServiceRef",
          "tooltip": "executorServiceRef",
          "title": "executorServiceRef"
        },
        "onPrepareRef": {
          "type": "java.lang.String",
          "description": "onPrepareRef",
          "tooltip": "onPrepareRef",
          "title": "onPrepareRef"
        },
        "parallelProcessing": {
          "type": "java.lang.Boolean",
          "description": "parallelProcessing",
          "tooltip": "parallelProcessing",
          "title": "parallelProcessing"
        },
        "stopOnException": {
          "type": "java.lang.Boolean",
          "description": "stopOnException",
          "tooltip": "stopOnException",
          "title": "stopOnException"
        },
        "ignoreInvalidEndpoints": {
          "type": "java.lang.Boolean",
          "description": "ignoreInvalidEndpoints",
          "tooltip": "ignoreInvalidEndpoints",
          "title": "ignoreInvalidEndpoints"
        },
        "streaming": {
          "type": "java.lang.Boolean",
          "description": "streaming",
          "tooltip": "streaming",
          "title": "streaming"
        },
        "timeout": {
          "type": "java.lang.Long",
          "description": "timeout",
          "tooltip": "timeout",
          "title": "timeout"
        },
        "shareUnitOfWork": {
          "type": "java.lang.Boolean",
          "description": "shareUnitOfWork",
          "tooltip": "shareUnitOfWork",
          "title": "shareUnitOfWork"
        }
      }
    },
    "removeHeader": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Remove Header",
      "group": "Transformation",
      "description": "Remove Header",
      "tooltip": "Remove Header",
      "icon": "transform24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "headerName": {
          "type": "java.lang.String",
          "description": "headerName",
          "tooltip": "headerName",
          "title": "headerName"
        }
      }
    },
    "removeHeaders": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Remove Headers",
      "group": "Transformation",
      "description": "Remove Headers",
      "tooltip": "Remove Headers",
      "icon": "transform24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "pattern": {
          "type": "java.lang.String",
          "description": "pattern",
          "tooltip": "pattern",
          "title": "pattern"
        },
        "excludePattern": {
          "type": "java.lang.String",
          "description": "excludePattern",
          "tooltip": "excludePattern",
          "title": "excludePattern"
        }
      }
    },
    "removeProperty": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Remove Property",
      "group": "Transformation",
      "description": "Remove Property",
      "tooltip": "Remove Property",
      "icon": "transform24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "propertyName": {
          "type": "java.lang.String",
          "description": "propertyName",
          "tooltip": "propertyName",
          "title": "propertyName"
        }
      }
    },
    "resequence": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Resequence",
      "group": "Routing",
      "description": "Resequence",
      "tooltip": "Resequence",
      "icon": "resequence24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        },
        "resequencerConfig": {
          "type": [
            "org.apache.camel.model.config.BatchResequencerConfig",
            "org.apache.camel.model.config.StreamResequencerConfig"
          ],
          "description": "resequencerConfig",
          "tooltip": "resequencerConfig",
          "title": "resequencerConfig"
        }
      }
    },
    "rollback": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Rollback",
      "group": "Control Flow",
      "description": "Rollback",
      "tooltip": "Rollback",
      "icon": "generic24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "message": {
          "type": "java.lang.String",
          "description": "message",
          "tooltip": "message",
          "title": "message"
        },
        "markRollbackOnly": {
          "type": "java.lang.Boolean",
          "description": "markRollbackOnly",
          "tooltip": "markRollbackOnly",
          "title": "markRollbackOnly"
        },
        "markRollbackOnlyLast": {
          "type": "java.lang.Boolean",
          "description": "markRollbackOnlyLast",
          "tooltip": "markRollbackOnlyLast",
          "title": "markRollbackOnlyLast"
        }
      }
    },
    "routingSlip": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Routing Slip",
      "group": "Routing",
      "description": "Routing Slip",
      "tooltip": "Routing Slip",
      "icon": "routingSlip24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        },
        "uriDelimiter": {
          "type": "java.lang.String",
          "description": "uriDelimiter",
          "tooltip": "uriDelimiter",
          "title": "uriDelimiter"
        },
        "ignoreInvalidEndpoints": {
          "type": "java.lang.Boolean",
          "description": "ignoreInvalidEndpoints",
          "tooltip": "ignoreInvalidEndpoints",
          "title": "ignoreInvalidEndpoints"
        }
      }
    },
    "sampling": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Sampling",
      "group": "Miscellaneous",
      "description": "Sampling",
      "tooltip": "Sampling",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "samplePeriod": {
          "type": "java.lang.Long",
          "description": "samplePeriod",
          "tooltip": "samplePeriod",
          "title": "samplePeriod"
        },
        "messageFrequency": {
          "type": "java.lang.Long",
          "description": "messageFrequency",
          "tooltip": "messageFrequency",
          "title": "messageFrequency"
        },
        "units": {
          "type": "java.util.concurrent.TimeUnit",
          "description": "units",
          "tooltip": "units",
          "title": "units"
        }
      }
    },
    "setBody": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Set Body",
      "group": "Transformation",
      "description": "Set Body",
      "tooltip": "Set Body",
      "icon": "setBody24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        }
      }
    },
    "setExchangePattern": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Set Exchange Pattern",
      "group": "Transformation",
      "description": "Set Exchange Pattern",
      "tooltip": "Set Exchange Pattern",
      "icon": "transform24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "pattern": {
          "type": "org.apache.camel.ExchangePattern",
          "description": "pattern",
          "tooltip": "pattern",
          "title": "pattern"
        }
      }
    },
    "setFaultBody": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Set Fault Body",
      "group": "Transformation",
      "description": "Set Fault Body",
      "tooltip": "Set Fault Body",
      "icon": "transform24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        }
      }
    },
    "setHeader": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Set Header",
      "group": "Transformation",
      "description": "Set Header",
      "tooltip": "Set Header",
      "icon": "transform24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        },
        "headerName": {
          "type": "java.lang.String",
          "description": "headerName",
          "tooltip": "headerName",
          "title": "headerName"
        }
      }
    },
    "setOutHeader": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Set Out Header",
      "group": "Transformation",
      "description": "Set Out Header",
      "tooltip": "Set Out Header",
      "icon": "transform24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        },
        "headerName": {
          "type": "java.lang.String",
          "description": "headerName",
          "tooltip": "headerName",
          "title": "headerName"
        }
      }
    },
    "setProperty": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Set Property",
      "group": "Transformation",
      "description": "Set Property",
      "tooltip": "Set Property",
      "icon": "transform24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        },
        "propertyName": {
          "type": "java.lang.String",
          "description": "propertyName",
          "tooltip": "propertyName",
          "title": "propertyName"
        }
      }
    },
    "sort": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Sort",
      "group": "Routing",
      "description": "Sort",
      "tooltip": "Sort",
      "icon": "generic24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        },
        "comparatorRef": {
          "type": "java.lang.String",
          "description": "comparatorRef",
          "tooltip": "comparatorRef",
          "title": "comparatorRef"
        }
      }
    },
    "split": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Split",
      "group": "Routing",
      "description": "Split",
      "tooltip": "Split",
      "icon": "split24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        },
        "strategyRef": {
          "type": "java.lang.String",
          "description": "strategyRef",
          "tooltip": "strategyRef",
          "title": "strategyRef"
        },
        "executorServiceRef": {
          "type": "java.lang.String",
          "description": "executorServiceRef",
          "tooltip": "executorServiceRef",
          "title": "executorServiceRef"
        },
        "onPrepareRef": {
          "type": "java.lang.String",
          "description": "onPrepareRef",
          "tooltip": "onPrepareRef",
          "title": "onPrepareRef"
        },
        "parallelProcessing": {
          "type": "java.lang.Boolean",
          "description": "parallelProcessing",
          "tooltip": "parallelProcessing",
          "title": "parallelProcessing"
        },
        "streaming": {
          "type": "java.lang.Boolean",
          "description": "streaming",
          "tooltip": "streaming",
          "title": "streaming"
        },
        "stopOnException": {
          "type": "java.lang.Boolean",
          "description": "stopOnException",
          "tooltip": "stopOnException",
          "title": "stopOnException"
        },
        "timeout": {
          "type": "java.lang.Long",
          "description": "timeout",
          "tooltip": "timeout",
          "title": "timeout"
        },
        "shareUnitOfWork": {
          "type": "java.lang.Boolean",
          "description": "shareUnitOfWork",
          "tooltip": "shareUnitOfWork",
          "title": "shareUnitOfWork"
        }
      }
    },
    "stop": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Stop",
      "group": "Miscellaneous",
      "description": "Stop",
      "tooltip": "Stop",
      "icon": "generic24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {}
    },
    "threads": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Threads",
      "group": "Miscellaneous",
      "description": "Threads",
      "tooltip": "Threads",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "executorServiceRef": {
          "type": "java.lang.String",
          "description": "executorServiceRef",
          "tooltip": "executorServiceRef",
          "title": "executorServiceRef"
        },
        "threadName": {
          "type": "java.lang.String",
          "description": "threadName",
          "tooltip": "threadName",
          "title": "threadName"
        },
        "poolSize": {
          "type": "java.lang.Integer",
          "description": "poolSize",
          "tooltip": "poolSize",
          "title": "poolSize"
        },
        "maxPoolSize": {
          "type": "java.lang.Integer",
          "description": "maxPoolSize",
          "tooltip": "maxPoolSize",
          "title": "maxPoolSize"
        },
        "keepAliveTime": {
          "type": "java.lang.Long",
          "description": "keepAliveTime",
          "tooltip": "keepAliveTime",
          "title": "keepAliveTime"
        },
        "timeUnit": {
          "type": "java.util.concurrent.TimeUnit",
          "description": "timeUnit",
          "tooltip": "timeUnit",
          "title": "timeUnit"
        },
        "maxQueueSize": {
          "type": "java.lang.Integer",
          "description": "maxQueueSize",
          "tooltip": "maxQueueSize",
          "title": "maxQueueSize"
        },
        "rejectedPolicy": {
          "type": "org.apache.camel.ThreadPoolRejectedPolicy",
          "description": "rejectedPolicy",
          "tooltip": "rejectedPolicy",
          "title": "rejectedPolicy"
        },
        "callerRunsWhenRejected": {
          "type": "java.lang.Boolean",
          "description": "callerRunsWhenRejected",
          "tooltip": "callerRunsWhenRejected",
          "title": "callerRunsWhenRejected"
        }
      }
    },
    "throttle": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Throttle",
      "group": "Control Flow",
      "description": "Throttle",
      "tooltip": "Throttle",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        },
        "executorServiceRef": {
          "type": "java.lang.String",
          "description": "executorServiceRef",
          "tooltip": "executorServiceRef",
          "title": "executorServiceRef"
        },
        "timePeriodMillis": {
          "type": "java.lang.Long",
          "description": "timePeriodMillis",
          "tooltip": "timePeriodMillis",
          "title": "timePeriodMillis"
        },
        "asyncDelayed": {
          "type": "java.lang.Boolean",
          "description": "asyncDelayed",
          "tooltip": "asyncDelayed",
          "title": "asyncDelayed"
        },
        "callerRunsWhenRejected": {
          "type": "java.lang.Boolean",
          "description": "callerRunsWhenRejected",
          "tooltip": "callerRunsWhenRejected",
          "title": "callerRunsWhenRejected"
        }
      }
    },
    "throwException": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Throw Exception",
      "group": "Control Flow",
      "description": "Throw Exception",
      "tooltip": "Throw Exception",
      "icon": "generic24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "ref": {
          "type": "java.lang.String",
          "description": "ref",
          "tooltip": "ref",
          "title": "ref"
        }
      }
    },
    "transacted": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Transacted",
      "group": "Control Flow",
      "description": "Transacted",
      "tooltip": "Transacted",
      "icon": "generic24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "ref": {
          "type": "java.lang.String",
          "description": "ref",
          "tooltip": "ref",
          "title": "ref"
        }
      }
    },
    "transform": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Transform",
      "group": "Transformation",
      "description": "Transform",
      "tooltip": "Transform",
      "icon": "transform24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        }
      }
    },
    "try": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Try",
      "group": "Control Flow",
      "description": "Try",
      "tooltip": "Try",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {}
    },
    "unmarshal": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Unmarshal",
      "group": "Transformation",
      "description": "Unmarshal",
      "tooltip": "Unmarshal",
      "icon": "unmarshal24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "ref": {
          "type": "java.lang.String",
          "description": "ref",
          "tooltip": "ref",
          "title": "ref"
        },
        "dataFormatType": {
          "type": [
            "org.apache.camel.model.dataformat.AvroDataFormat",
            "org.apache.camel.model.dataformat.Base64DataFormat",
            "org.apache.camel.model.dataformat.BeanioDataFormat",
            "org.apache.camel.model.dataformat.BindyDataFormat",
            "org.apache.camel.model.dataformat.CastorDataFormat",
            "org.apache.camel.model.dataformat.CryptoDataFormat",
            "org.apache.camel.model.dataformat.CsvDataFormat",
            "org.apache.camel.model.dataformat.CustomDataFormat",
            "org.apache.camel.model.dataformat.C24IODataFormat",
            "org.apache.camel.model.dataformat.FlatpackDataFormat",
            "org.apache.camel.model.dataformat.GzipDataFormat",
            "org.apache.camel.model.dataformat.HL7DataFormat",
            "org.apache.camel.model.dataformat.JaxbDataFormat",
            "org.apache.camel.model.dataformat.JibxDataFormat",
            "org.apache.camel.model.dataformat.JsonDataFormat",
            "org.apache.camel.model.dataformat.ProtobufDataFormat",
            "org.apache.camel.model.dataformat.RssDataFormat",
            "org.apache.camel.model.dataformat.XMLSecurityDataFormat",
            "org.apache.camel.model.dataformat.SerializationDataFormat",
            "org.apache.camel.model.dataformat.SoapJaxbDataFormat",
            "org.apache.camel.model.dataformat.StringDataFormat",
            "org.apache.camel.model.dataformat.SyslogDataFormat",
            "org.apache.camel.model.dataformat.TidyMarkupDataFormat",
            "org.apache.camel.model.dataformat.XMLBeansDataFormat",
            "org.apache.camel.model.dataformat.XmlJsonDataFormat",
            "org.apache.camel.model.dataformat.XmlRpcDataFormat",
            "org.apache.camel.model.dataformat.XStreamDataFormat",
            "org.apache.camel.model.dataformat.PGPDataFormat",
            "org.apache.camel.model.dataformat.ZipDataFormat",
            "org.apache.camel.model.dataformat.ZipFileDataFormat"
          ],
          "description": "dataFormatType",
          "tooltip": "dataFormatType",
          "title": "dataFormatType"
        }
      }
    },
    "validate": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Validate",
      "group": "Miscellaneous",
      "description": "Validate",
      "tooltip": "Validate",
      "icon": "generic24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        }
      }
    },
    "when": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "When",
      "group": "Routing",
      "description": "When",
      "tooltip": "When",
      "icon": "generic24.png",
      "acceptInput": true,
      "acceptOutput": true,
      "properties": {
        "expression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        }
      }
    },
    "wireTap": {
      "type": "object",
      "extends": {
        "type": "org.apache.camel.model.OptionalIdentifiedDefinition"
      },
      "title": "Wire Tap",
      "group": "Routing",
      "description": "Wire Tap",
      "tooltip": "Wire Tap",
      "icon": "wireTap24.png",
      "nextSiblingAddedAsChild": true,
      "properties": {
        "uri": {
          "type": "java.lang.String",
          "description": "uri",
          "tooltip": "uri",
          "title": "uri"
        },
        "newExchangeProcessorRef": {
          "type": "java.lang.String",
          "description": "newExchangeProcessorRef",
          "tooltip": "newExchangeProcessorRef",
          "title": "newExchangeProcessorRef"
        },
        "newExchangeExpression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "newExchangeExpression",
          "tooltip": "newExchangeExpression",
          "title": "newExchangeExpression"
        },
        "headers": {
          "type": "java.util.List",
          "description": "headers",
          "tooltip": "headers",
          "title": "headers"
        },
        "executorServiceRef": {
          "type": "java.lang.String",
          "description": "executorServiceRef",
          "tooltip": "executorServiceRef",
          "title": "executorServiceRef"
        },
        "onPrepareRef": {
          "type": "java.lang.String",
          "description": "onPrepareRef",
          "tooltip": "onPrepareRef",
          "title": "onPrepareRef"
        },
        "copy": {
          "type": "java.lang.Boolean",
          "description": "copy",
          "tooltip": "copy",
          "title": "copy"
        }
      }
    },
    "org.apache.camel.model.dataformat.StringDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.String Data Format",
      "tooltip": "org.apache.camel.model.dataformat.String Data Format",
      "properties": {
        "charset": {
          "type": "java.lang.String",
          "description": "charset",
          "tooltip": "charset",
          "title": "charset"
        }
      }
    },
    "org.apache.camel.model.dataformat.XMLBeansDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.XMLBeans Data Format",
      "tooltip": "org.apache.camel.model.dataformat.XMLBeans Data Format",
      "properties": {
        "prettyPrint": {
          "type": "java.lang.Boolean",
          "description": "prettyPrint",
          "tooltip": "prettyPrint",
          "title": "prettyPrint"
        }
      }
    },
    "org.apache.camel.model.config.StreamResequencerConfig": {
      "type": "object",
      "description": "org.apache.camel.model.config.Stream Resequencer Config",
      "tooltip": "org.apache.camel.model.config.Stream Resequencer Config",
      "properties": {
        "capacity": {
          "type": "java.lang.Integer",
          "description": "capacity",
          "tooltip": "capacity",
          "title": "capacity"
        },
        "timeout": {
          "type": "java.lang.Long",
          "description": "timeout",
          "tooltip": "timeout",
          "title": "timeout"
        },
        "ignoreInvalidExchanges": {
          "type": "java.lang.Boolean",
          "description": "ignoreInvalidExchanges",
          "tooltip": "ignoreInvalidExchanges",
          "title": "ignoreInvalidExchanges"
        },
        "rejectOld": {
          "type": "java.lang.Boolean",
          "description": "rejectOld",
          "tooltip": "rejectOld",
          "title": "rejectOld"
        }
      }
    },
    "org.apache.camel.model.dataformat.JibxDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Jibx Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Jibx Data Format",
      "properties": {
        "dataFormatName": {
          "type": "java.lang.String",
          "description": "dataFormatName",
          "tooltip": "dataFormatName",
          "optional": true,
          "title": "dataFormatName"
        },
        "unmarshallTypeName": {
          "type": "java.lang.String",
          "description": "unmarshallTypeName",
          "tooltip": "unmarshallTypeName",
          "optional": true,
          "title": "unmarshallTypeName"
        }
      }
    },
    "org.apache.camel.model.dataformat.BeanioDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Beanio Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Beanio Data Format",
      "properties": {
        "mapping": {
          "type": "java.lang.String",
          "description": "mapping",
          "tooltip": "mapping",
          "title": "mapping"
        },
        "streamName": {
          "type": "java.lang.String",
          "description": "streamName",
          "tooltip": "streamName",
          "title": "streamName"
        },
        "encoding": {
          "type": "java.lang.String",
          "description": "encoding",
          "tooltip": "encoding",
          "title": "encoding"
        },
        "ignoreUnidentifiedRecords": {
          "type": "java.lang.Boolean",
          "description": "ignoreUnidentifiedRecords",
          "tooltip": "ignoreUnidentifiedRecords",
          "title": "ignoreUnidentifiedRecords"
        },
        "ignoreUnexpectedRecords": {
          "type": "java.lang.Boolean",
          "description": "ignoreUnexpectedRecords",
          "tooltip": "ignoreUnexpectedRecords",
          "title": "ignoreUnexpectedRecords"
        },
        "ignoreInvalidRecords": {
          "type": "java.lang.Boolean",
          "description": "ignoreInvalidRecords",
          "tooltip": "ignoreInvalidRecords",
          "title": "ignoreInvalidRecords"
        }
      }
    },
    "org.apache.camel.model.loadbalancer.StickyLoadBalancer": {
      "type": "object",
      "description": "org.apache.camel.model.loadbalancer.Sticky Load Balancer",
      "tooltip": "org.apache.camel.model.loadbalancer.Sticky Load Balancer",
      "properties": {
        "correlationExpression": {
          "kind": "expression",
          "type": "org.apache.camel.model.language.ExpressionDefinition",
          "description": "correlationExpression",
          "tooltip": "correlationExpression",
          "title": "correlationExpression"
        }
      }
    },
    "org.apache.camel.model.language.Expression": {
      "type": "object",
      "description": "org.apache.camel.model.language.Expression",
      "tooltip": "org.apache.camel.model.language.Expression",
      "properties": {
        "expression": {
          "type": "java.lang.String",
          "description": "expression",
          "tooltip": "expression",
          "title": "expression"
        },
        "trim": {
          "type": "java.lang.Boolean",
          "description": "trim",
          "tooltip": "trim",
          "title": "trim"
        }
      }
    },
    "org.apache.camel.model.dataformat.XMLSecurityDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.XMLSecurity Data Format",
      "tooltip": "org.apache.camel.model.dataformat.XMLSecurity Data Format",
      "properties": {
        "xmlCipherAlgorithm": {
          "type": "java.lang.String",
          "description": "xmlCipherAlgorithm",
          "tooltip": "xmlCipherAlgorithm",
          "title": "xmlCipherAlgorithm"
        },
        "passPhrase": {
          "type": "java.lang.String",
          "description": "passPhrase",
          "tooltip": "passPhrase",
          "title": "passPhrase"
        },
        "secureTag": {
          "type": "java.lang.String",
          "description": "secureTag",
          "tooltip": "secureTag",
          "title": "secureTag"
        },
        "keyCipherAlgorithm": {
          "type": "java.lang.String",
          "description": "keyCipherAlgorithm",
          "tooltip": "keyCipherAlgorithm",
          "title": "keyCipherAlgorithm"
        },
        "recipientKeyAlias": {
          "type": "java.lang.String",
          "description": "recipientKeyAlias",
          "tooltip": "recipientKeyAlias",
          "title": "recipientKeyAlias"
        },
        "keyOrTrustStoreParametersId": {
          "type": "java.lang.String",
          "description": "keyOrTrustStoreParametersId",
          "tooltip": "keyOrTrustStoreParametersId",
          "title": "keyOrTrustStoreParametersId"
        },
        "keyPassword": {
          "type": "java.lang.String",
          "description": "keyPassword",
          "tooltip": "keyPassword",
          "title": "keyPassword"
        },
        "secureTagContents": {
          "type": "java.lang.Boolean",
          "description": "secureTagContents",
          "tooltip": "secureTagContents",
          "title": "secureTagContents"
        }
      }
    },
    "org.apache.camel.model.dataformat.CastorDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Castor Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Castor Data Format",
      "properties": {
        "mappingFile": {
          "type": "java.lang.String",
          "description": "mappingFile",
          "tooltip": "mappingFile",
          "title": "mappingFile"
        },
        "encoding": {
          "type": "java.lang.String",
          "description": "encoding",
          "tooltip": "encoding",
          "title": "encoding"
        },
        "validation": {
          "type": "java.lang.Boolean",
          "description": "validation",
          "tooltip": "validation",
          "title": "validation"
        },
        "packages": {
          "type": "[Ljava.lang.String;",
          "description": "packages",
          "tooltip": "packages",
          "title": "packages"
        },
        "classes": {
          "type": "[Ljava.lang.String;",
          "description": "classes",
          "tooltip": "classes",
          "title": "classes"
        }
      }
    },
    "org.apache.camel.model.Description": {
      "type": "object",
      "description": "org.apache.camel.model.Description",
      "tooltip": "org.apache.camel.model.Description",
      "properties": {
        "lang": {
          "type": "java.lang.String",
          "description": "lang",
          "tooltip": "lang",
          "title": "lang"
        },
        "text": {
          "type": "java.lang.String",
          "description": "text",
          "tooltip": "text",
          "title": "text"
        },
        "layoutX": {
          "type": "java.lang.Double",
          "description": "layoutX",
          "tooltip": "layoutX",
          "title": "layoutX"
        },
        "layoutY": {
          "type": "java.lang.Double",
          "description": "layoutY",
          "tooltip": "layoutY",
          "title": "layoutY"
        },
        "layoutWidth": {
          "type": "java.lang.Double",
          "description": "layoutWidth",
          "tooltip": "layoutWidth",
          "title": "layoutWidth"
        },
        "layoutHeight": {
          "type": "java.lang.Double",
          "description": "layoutHeight",
          "tooltip": "layoutHeight",
          "title": "layoutHeight"
        }
      }
    },
    "org.apache.camel.model.dataformat.FlatpackDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Flatpack Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Flatpack Data Format",
      "properties": {}
    },
    "org.apache.camel.model.dataformat.SyslogDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Syslog Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Syslog Data Format",
      "properties": {}
    },
    "org.apache.camel.model.dataformat.ZipDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Zip Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Zip Data Format",
      "properties": {
        "compressionLevel": {
          "type": "java.lang.Integer",
          "description": "compressionLevel",
          "tooltip": "compressionLevel",
          "title": "compressionLevel"
        }
      }
    },
    "org.apache.camel.model.dataformat.CryptoDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Crypto Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Crypto Data Format",
      "properties": {
        "algorithm": {
          "type": "java.lang.String",
          "description": "algorithm",
          "tooltip": "algorithm",
          "title": "algorithm"
        },
        "cryptoProvider": {
          "type": "java.lang.String",
          "description": "cryptoProvider",
          "tooltip": "cryptoProvider",
          "title": "cryptoProvider"
        },
        "keyRef": {
          "type": "java.lang.String",
          "description": "keyRef",
          "tooltip": "keyRef",
          "title": "keyRef"
        },
        "initVectorRef": {
          "type": "java.lang.String",
          "description": "initVectorRef",
          "tooltip": "initVectorRef",
          "title": "initVectorRef"
        },
        "algorithmParameterRef": {
          "type": "java.lang.String",
          "description": "algorithmParameterRef",
          "tooltip": "algorithmParameterRef",
          "title": "algorithmParameterRef"
        },
        "macAlgorithm": {
          "type": "java.lang.String",
          "description": "macAlgorithm",
          "tooltip": "macAlgorithm",
          "title": "macAlgorithm"
        },
        "buffersize": {
          "type": "java.lang.Integer",
          "description": "buffersize",
          "tooltip": "buffersize",
          "title": "buffersize"
        },
        "shouldAppendHMAC": {
          "type": "java.lang.Boolean",
          "description": "shouldAppendHMAC",
          "tooltip": "shouldAppendHMAC",
          "title": "shouldAppendHMAC"
        },
        "inline": {
          "type": "java.lang.Boolean",
          "description": "inline",
          "tooltip": "inline",
          "title": "inline"
        }
      }
    },
    "org.apache.camel.model.dataformat.HL7DataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.HL7Data Format",
      "tooltip": "org.apache.camel.model.dataformat.HL7Data Format",
      "properties": {
        "validate": {
          "type": "java.lang.Boolean",
          "description": "validate",
          "tooltip": "validate",
          "title": "validate"
        }
      }
    },
    "org.apache.camel.model.dataformat.Base64DataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Base64Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Base64Data Format",
      "properties": {
        "lineSeparator": {
          "type": "java.lang.String",
          "description": "lineSeparator",
          "tooltip": "lineSeparator",
          "title": "lineSeparator"
        },
        "lineLength": {
          "type": "java.lang.Integer",
          "description": "lineLength",
          "tooltip": "lineLength",
          "title": "lineLength"
        },
        "urlSafe": {
          "type": "java.lang.Boolean",
          "description": "urlSafe",
          "tooltip": "urlSafe",
          "title": "urlSafe"
        }
      }
    },
    "org.apache.camel.model.dataformat.XmlJsonDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Xml Json Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Xml Json Data Format",
      "properties": {
        "encoding": {
          "type": "java.lang.String",
          "description": "encoding",
          "tooltip": "encoding",
          "title": "encoding"
        },
        "elementName": {
          "type": "java.lang.String",
          "description": "elementName",
          "tooltip": "elementName",
          "title": "elementName"
        },
        "arrayName": {
          "type": "java.lang.String",
          "description": "arrayName",
          "tooltip": "arrayName",
          "title": "arrayName"
        },
        "rootName": {
          "type": "java.lang.String",
          "description": "rootName",
          "tooltip": "rootName",
          "title": "rootName"
        },
        "expandableProperties": {
          "type": "java.util.List",
          "description": "expandableProperties",
          "tooltip": "expandableProperties",
          "title": "expandableProperties"
        },
        "typeHints": {
          "type": "java.lang.String",
          "description": "typeHints",
          "tooltip": "typeHints",
          "title": "typeHints"
        },
        "forceTopLevelObject": {
          "type": "java.lang.Boolean",
          "description": "forceTopLevelObject",
          "tooltip": "forceTopLevelObject",
          "title": "forceTopLevelObject"
        },
        "namespaceLenient": {
          "type": "java.lang.Boolean",
          "description": "namespaceLenient",
          "tooltip": "namespaceLenient",
          "title": "namespaceLenient"
        },
        "skipWhitespace": {
          "type": "java.lang.Boolean",
          "description": "skipWhitespace",
          "tooltip": "skipWhitespace",
          "title": "skipWhitespace"
        },
        "trimSpaces": {
          "type": "java.lang.Boolean",
          "description": "trimSpaces",
          "tooltip": "trimSpaces",
          "title": "trimSpaces"
        },
        "skipNamespaces": {
          "type": "java.lang.Boolean",
          "description": "skipNamespaces",
          "tooltip": "skipNamespaces",
          "title": "skipNamespaces"
        },
        "removeNamespacePrefixes": {
          "type": "java.lang.Boolean",
          "description": "removeNamespacePrefixes",
          "tooltip": "removeNamespacePrefixes",
          "title": "removeNamespacePrefixes"
        }
      }
    },
    "org.apache.camel.model.dataformat.SerializationDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Serialization Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Serialization Data Format",
      "properties": {}
    },
    "org.apache.camel.model.dataformat.JsonDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Json Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Json Data Format",
      "properties": {
        "unmarshalTypeName": {
          "type": "java.lang.String",
          "description": "unmarshalTypeName",
          "tooltip": "unmarshalTypeName",
          "title": "unmarshalTypeName"
        },
        "prettyPrint": {
          "type": "java.lang.Boolean",
          "description": "prettyPrint",
          "tooltip": "prettyPrint",
          "title": "prettyPrint"
        },
        "library": {
          "type": "org.apache.camel.model.dataformat.JsonLibrary",
          "description": "library",
          "tooltip": "library",
          "title": "library"
        }
      }
    },
    "org.apache.camel.model.loadbalancer.CustomLoadBalancer": {
      "type": "object",
      "description": "org.apache.camel.model.loadbalancer.Custom Load Balancer",
      "tooltip": "org.apache.camel.model.loadbalancer.Custom Load Balancer",
      "properties": {
        "ref": {
          "type": "java.lang.String",
          "description": "ref",
          "tooltip": "ref",
          "title": "ref"
        }
      }
    },
    "org.apache.camel.model.dataformat.CustomDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Custom Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Custom Data Format",
      "properties": {
        "ref": {
          "type": "java.lang.String",
          "description": "ref",
          "tooltip": "ref",
          "title": "ref"
        }
      }
    },
    "org.apache.camel.model.config.BatchResequencerConfig": {
      "type": "object",
      "description": "org.apache.camel.model.config.Batch Resequencer Config",
      "tooltip": "org.apache.camel.model.config.Batch Resequencer Config",
      "properties": {
        "batchSize": {
          "type": "java.lang.Integer",
          "description": "batchSize",
          "tooltip": "batchSize",
          "title": "batchSize"
        },
        "batchTimeout": {
          "type": "java.lang.Long",
          "description": "batchTimeout",
          "tooltip": "batchTimeout",
          "title": "batchTimeout"
        },
        "allowDuplicates": {
          "type": "java.lang.Boolean",
          "description": "allowDuplicates",
          "tooltip": "allowDuplicates",
          "title": "allowDuplicates"
        },
        "reverse": {
          "type": "java.lang.Boolean",
          "description": "reverse",
          "tooltip": "reverse",
          "title": "reverse"
        },
        "ignoreInvalidExchanges": {
          "type": "java.lang.Boolean",
          "description": "ignoreInvalidExchanges",
          "tooltip": "ignoreInvalidExchanges",
          "title": "ignoreInvalidExchanges"
        }
      }
    },
    "org.apache.camel.model.dataformat.ZipFileDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Zip File Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Zip File Data Format",
      "properties": {
        "usingIterator": {
          "type": "java.lang.Boolean",
          "description": "usingIterator",
          "tooltip": "usingIterator",
          "title": "usingIterator"
        }
      }
    },
    "org.apache.camel.model.dataformat.TidyMarkupDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Tidy Markup Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Tidy Markup Data Format",
      "properties": {
        "dataObjectTypeName": {
          "type": "java.lang.String",
          "description": "dataObjectTypeName",
          "tooltip": "dataObjectTypeName",
          "title": "dataObjectTypeName"
        }
      }
    },
    "org.apache.camel.model.dataformat.XStreamDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.XStream Data Format",
      "tooltip": "org.apache.camel.model.dataformat.XStream Data Format",
      "properties": {
        "converters": {
          "type": "java.util.List",
          "description": "converters",
          "tooltip": "converters",
          "optional": true,
          "title": "converters"
        },
        "dataFormatName": {
          "type": "java.lang.String",
          "description": "dataFormatName",
          "tooltip": "dataFormatName",
          "optional": true,
          "title": "dataFormatName"
        },
        "driver": {
          "type": "java.lang.String",
          "description": "driver",
          "tooltip": "driver",
          "optional": true,
          "title": "driver"
        },
        "driverRef": {
          "type": "java.lang.String",
          "description": "driverRef",
          "tooltip": "driverRef",
          "optional": true,
          "title": "driverRef"
        },
        "encoding": {
          "type": "java.lang.String",
          "description": "encoding",
          "tooltip": "encoding",
          "optional": true,
          "title": "encoding"
        }
      }
    },
    "org.apache.camel.model.dataformat.JaxbDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Jaxb Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Jaxb Data Format",
      "properties": {
        "contextPath": {
          "type": "java.lang.String",
          "description": "contextPath",
          "tooltip": "contextPath",
          "title": "contextPath"
        },
        "schema": {
          "type": "java.lang.String",
          "description": "schema",
          "tooltip": "schema",
          "title": "schema"
        },
        "encoding": {
          "type": "java.lang.String",
          "description": "encoding",
          "tooltip": "encoding",
          "title": "encoding"
        },
        "partClass": {
          "type": "java.lang.String",
          "description": "partClass",
          "tooltip": "partClass",
          "title": "partClass"
        },
        "partNamespace": {
          "type": "java.lang.String",
          "description": "partNamespace",
          "tooltip": "partNamespace",
          "title": "partNamespace"
        },
        "namespacePrefixRef": {
          "type": "java.lang.String",
          "description": "namespacePrefixRef",
          "tooltip": "namespacePrefixRef",
          "title": "namespacePrefixRef"
        },
        "prettyPrint": {
          "type": "java.lang.Boolean",
          "description": "prettyPrint",
          "tooltip": "prettyPrint",
          "title": "prettyPrint"
        },
        "ignoreJAXBElement": {
          "type": "java.lang.Boolean",
          "description": "ignoreJAXBElement",
          "tooltip": "ignoreJAXBElement",
          "title": "ignoreJAXBElement"
        },
        "filterNonXmlChars": {
          "type": "java.lang.Boolean",
          "description": "filterNonXmlChars",
          "tooltip": "filterNonXmlChars",
          "title": "filterNonXmlChars"
        },
        "fragment": {
          "type": "java.lang.Boolean",
          "description": "fragment",
          "tooltip": "fragment",
          "title": "fragment"
        }
      }
    },
    "org.apache.camel.model.dataformat.ProtobufDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Protobuf Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Protobuf Data Format",
      "properties": {
        "instanceClass": {
          "type": "java.lang.String",
          "description": "instanceClass",
          "tooltip": "instanceClass",
          "title": "instanceClass"
        }
      }
    },
    "org.apache.camel.model.dataformat.BindyDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Bindy Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Bindy Data Format",
      "properties": {
        "classType": {
          "type": "java.lang.String",
          "description": "classType",
          "tooltip": "classType",
          "title": "classType"
        },
        "locale": {
          "type": "java.lang.String",
          "description": "locale",
          "tooltip": "locale",
          "title": "locale"
        },
        "type": {
          "type": "org.apache.camel.model.dataformat.BindyType",
          "description": "type",
          "tooltip": "type",
          "title": "type"
        },
        "packages": {
          "type": "[Ljava.lang.String;",
          "description": "packages",
          "tooltip": "packages",
          "title": "packages"
        }
      }
    },
    "org.apache.camel.model.dataformat.C24IODataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.C24IOData Format",
      "tooltip": "org.apache.camel.model.dataformat.C24IOData Format",
      "properties": {
        "elementTypeName": {
          "type": "java.lang.String",
          "description": "elementTypeName",
          "tooltip": "elementTypeName",
          "title": "elementTypeName"
        },
        "contentType": {
          "type": "org.apache.camel.model.dataformat.C24IOContentType",
          "description": "contentType",
          "tooltip": "contentType",
          "title": "contentType"
        }
      }
    },
    "org.apache.camel.model.dataformat.GzipDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Gzip Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Gzip Data Format",
      "properties": {}
    },
    "org.apache.camel.model.loadbalancer.FailoverLoadBalancer": {
      "type": "object",
      "description": "org.apache.camel.model.loadbalancer.Failover Load Balancer",
      "tooltip": "org.apache.camel.model.loadbalancer.Failover Load Balancer",
      "properties": {
        "exceptions": {
          "type": "java.util.List",
          "description": "exceptions",
          "tooltip": "exceptions",
          "title": "exceptions"
        },
        "roundRobin": {
          "type": "java.lang.Boolean",
          "description": "roundRobin",
          "tooltip": "roundRobin",
          "title": "roundRobin"
        },
        "maximumFailoverAttempts": {
          "type": "java.lang.Integer",
          "description": "maximumFailoverAttempts",
          "tooltip": "maximumFailoverAttempts",
          "title": "maximumFailoverAttempts"
        }
      }
    },
    "org.apache.camel.model.loadbalancer.TopicLoadBalancer": {
      "type": "object",
      "description": "org.apache.camel.model.loadbalancer.Topic Load Balancer",
      "tooltip": "org.apache.camel.model.loadbalancer.Topic Load Balancer",
      "properties": {}
    },
    "org.apache.camel.model.dataformat.XmlRpcDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Xml Rpc Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Xml Rpc Data Format",
      "properties": {
        "request": {
          "type": "java.lang.Boolean",
          "description": "request",
          "tooltip": "request",
          "title": "request"
        }
      }
    },
    "org.apache.camel.model.loadbalancer.RoundRobinLoadBalancer": {
      "type": "object",
      "description": "org.apache.camel.model.loadbalancer.Round Robin Load Balancer",
      "tooltip": "org.apache.camel.model.loadbalancer.Round Robin Load Balancer",
      "properties": {}
    },
    "org.apache.camel.model.dataformat.RssDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Rss Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Rss Data Format",
      "properties": {}
    },
    "org.apache.camel.model.dataformat.PGPDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.PGPData Format",
      "tooltip": "org.apache.camel.model.dataformat.PGPData Format",
      "properties": {
        "keyUserid": {
          "type": "java.lang.String",
          "description": "keyUserid",
          "tooltip": "keyUserid",
          "title": "keyUserid"
        },
        "password": {
          "type": "java.lang.String",
          "description": "password",
          "tooltip": "password",
          "title": "password"
        },
        "keyFileName": {
          "type": "java.lang.String",
          "description": "keyFileName",
          "tooltip": "keyFileName",
          "title": "keyFileName"
        },
        "armored": {
          "type": "java.lang.Boolean",
          "description": "armored",
          "tooltip": "armored",
          "title": "armored"
        },
        "integrity": {
          "type": "java.lang.Boolean",
          "description": "integrity",
          "tooltip": "integrity",
          "title": "integrity"
        }
      }
    },
    "org.apache.camel.model.dataformat.SoapJaxbDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Soap Jaxb Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Soap Jaxb Data Format",
      "properties": {
        "contextPath": {
          "type": "java.lang.String",
          "description": "contextPath",
          "tooltip": "contextPath",
          "title": "contextPath"
        },
        "encoding": {
          "type": "java.lang.String",
          "description": "encoding",
          "tooltip": "encoding",
          "title": "encoding"
        },
        "elementNameStrategyRef": {
          "type": "java.lang.String",
          "description": "elementNameStrategyRef",
          "tooltip": "elementNameStrategyRef",
          "title": "elementNameStrategyRef"
        },
        "version": {
          "type": "java.lang.String",
          "description": "version",
          "tooltip": "version",
          "title": "version"
        },
        "namespacePrefixRef": {
          "type": "java.lang.String",
          "description": "namespacePrefixRef",
          "tooltip": "namespacePrefixRef",
          "title": "namespacePrefixRef"
        }
      }
    },
    "org.apache.camel.model.loadbalancer.WeightedLoadBalancer": {
      "type": "object",
      "description": "org.apache.camel.model.loadbalancer.Weighted Load Balancer",
      "tooltip": "org.apache.camel.model.loadbalancer.Weighted Load Balancer",
      "properties": {
        "distributionRatio": {
          "type": "java.lang.String",
          "description": "distributionRatio",
          "tooltip": "distributionRatio",
          "title": "distributionRatio"
        },
        "distributionRatioDelimiter": {
          "type": "java.lang.String",
          "description": "distributionRatioDelimiter",
          "tooltip": "distributionRatioDelimiter",
          "title": "distributionRatioDelimiter"
        },
        "roundRobin": {
          "type": "java.lang.Boolean",
          "description": "roundRobin",
          "tooltip": "roundRobin",
          "title": "roundRobin"
        }
      }
    },
    "org.apache.camel.model.loadbalancer.RandomLoadBalancer": {
      "type": "object",
      "description": "org.apache.camel.model.loadbalancer.Random Load Balancer",
      "tooltip": "org.apache.camel.model.loadbalancer.Random Load Balancer",
      "properties": {}
    },
    "org.apache.camel.model.dataformat.AvroDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Avro Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Avro Data Format",
      "properties": {
        "instanceClassName": {
          "type": "java.lang.String",
          "description": "instanceClassName",
          "tooltip": "instanceClassName",
          "title": "instanceClassName"
        }
      }
    },
    "org.apache.camel.model.dataformat.CsvDataFormat": {
      "type": "object",
      "description": "org.apache.camel.model.dataformat.Csv Data Format",
      "tooltip": "org.apache.camel.model.dataformat.Csv Data Format",
      "properties": {
        "delimiter": {
          "type": "java.lang.String",
          "description": "delimiter",
          "tooltip": "delimiter",
          "title": "delimiter"
        },
        "configRef": {
          "type": "java.lang.String",
          "description": "configRef",
          "tooltip": "configRef",
          "title": "configRef"
        },
        "strategyRef": {
          "type": "java.lang.String",
          "description": "strategyRef",
          "tooltip": "strategyRef",
          "title": "strategyRef"
        },
        "autogenColumns": {
          "type": "java.lang.Boolean",
          "description": "autogenColumns",
          "tooltip": "autogenColumns",
          "title": "autogenColumns"
        },
        "skipFirstLine": {
          "type": "java.lang.Boolean",
          "description": "skipFirstLine",
          "tooltip": "skipFirstLine",
          "title": "skipFirstLine"
        }
      }
    }
  },
  "languages": {
    "constant": {
      "name": "Constant",
      "description": "Constant expression"
    },
    "el": {
      "name": "EL",
      "description": "Unified expression language from JSP / JSTL / JSF"
    },
    "header": {
      "name": "Header",
      "description": "Header value"
    },
    "javaScript": {
      "name": "JavaScript",
      "description": "JavaScript expression"
    },
    "jxpath": {
      "name": "JXPath",
      "description": "JXPath expression"
    },
    "method": {
      "name": "Method",
      "description": "Method call expression"
    },
    "mvel": {
      "name": "MVEL",
      "description": "MVEL expression"
    },
    "ognl": {
      "name": "OGNL",
      "description": "OGNL expression"
    },
    "groovy": {
      "name": "Groovy",
      "description": "Groovy expression"
    },
    "property": {
      "name": "Property",
      "description": "Property value"
    },
    "python": {
      "name": "Python",
      "description": "Python expression"
    },
    "php": {
      "name": "PHP",
      "description": "PHP expression"
    },
    "ref": {
      "name": "Ref",
      "description": "Reference to a bean expression"
    },
    "ruby": {
      "name": "Ruby",
      "description": "Ruby expression"
    },
    "simple": {
      "name": "Simple",
      "description": "Simple expression language from Camel"
    },
    "spel": {
      "name": "Spring EL",
      "description": "Spring expression language"
    },
    "sql": {
      "name": "SQL",
      "description": "SQL expression"
    },
    "tokenize": {
      "name": "Tokenizer",
      "description": "Tokenizing expression"
    },
    "xpath": {
      "name": "XPath",
      "description": "XPath expression"
    },
    "xquery": {
      "name": "XQuery",
      "description": "XQuery expression"
    }
  }
};