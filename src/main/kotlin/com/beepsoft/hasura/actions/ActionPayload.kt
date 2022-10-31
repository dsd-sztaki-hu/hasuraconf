package com.beepsoft.hasura.actions

import com.fasterxml.jackson.annotation.JsonProperty

// {
//  "request_query": "mutation {\n  uploadDefinitions(args:{\n    content:\"foo\"\n  }) {\n    result\n  }\n}",
//  "session_variables": {
//    "x-hasura-role": "admin"
//  },
//  "input": {
//    "args": {
//      "content": "foo"
//    }
//  },
//  "action": {
//    "name": "uploadDefinitions"
//  }
data class ActionPayload (
    /**
     * The graphql query
     */
    @JsonProperty("request_query")
    val requestQuery: String,

    /**
     * x-hasura-role, x-hasura-user-id, etc.
     */
    @JsonProperty("session_variables")
    val sessionVariables: Map<String, String>,

    /**
     * The action arguments. We only have a single arg for all our actions.
     */
    val input: Map<String, Any>,

    /**
     * Action's name, etc.
     */
    val action: ActionData,
)

data class ActionData (
    @JsonProperty("name")
    val name: String
)

//   "actionPayload": {
//    "action": {
//      "name": "startTask"
//    },
//    "input": {
//      "args": {
//        "taskId": 123
//      }
//    },
//    "request_query": "mutation {\n  startTask(args:{taskId:123}) {\n    executedTaskId\n  }\n}",
//    "session_variables": {
//      "x-hasura-role": "admin"
//    }
//  }
