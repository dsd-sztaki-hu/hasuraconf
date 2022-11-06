package com.beepsoft.hasura.actions

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * This is the endpoint, which accepts any action calls and forward it the actual specific controller implementing
 * the action
 *
 * 1. ActionsFilter looks for /actions paths. If matches, passes the request wrapped in ActionRequestWrapper
 * 2. The request reaches HasuraActionController, which forwards it to the actual controller matching the
 *    actionName in ActionRequestWrapper
 * 3. The actual action handler handles the request
 */
@RestController
//@RequestMapping("/actions")
@RequestMapping("\${hasuraconf.action-controller.path:/actions}")
class HasuraActionController {

    @Value("\${hasuraconf.action-controller.path:/actions}")
    lateinit var path: String

    /**
     * Forwards the request to the actual action implementation, which must be handle /actions/<req.actionName>
     */
    @PostMapping
    fun actionProxy(request: HttpServletRequest, response: HttpServletResponse) : ModelAndView {
        val req: ActionRequestWrapper = request as ActionRequestWrapper
        // We expect  to have a controller method responding to /actions/<req.actionName> to which we forward this
        // request. The ActionFilter already prepared the request body to match the argument of the action to be
        // called now:
        return ModelAndView("forward:$path/"+req.actionName)
    }

}
