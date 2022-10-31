package com.beepsoft.hasuraconf.actions

import com.beepsoft.hasuraconf.annotation.*

/**
 * This action maps our /api/auth/signin REST API to the hasura 
 */
@HasuraAction(
    handler = "{{SERVER_BASE_URL}}",
    comment = "Signs in a user.",
    permissions = ["public"],
    requestTransform = HasuraRequestTransform(
        url = "{{\$base_url}}/api/auth/signin",
        method = HasuraHttpMethod.POST,
        body = """
            {
                "usernameOrEmail": {{${"$"}body.input.args.usernameOrEmail}},
                "password": {{${"$"}body.input.args.password}}
            }      
        """,
    ),
)
fun signIn(args: SignInInput): SignInOutput = TODO()

data class SignInInput(
    /**
     * Username
     */
    @HasuraField(description = "Username or email")
    val usernameOrEmail: String,

    /**
     * Password
     */
    @HasuraField(description = "PIN code")
    val password: String,
)


data class SignInOutput(
    @HasuraField(description = "Access token")
    val accessToken: String?,

    @HasuraField(description = "Type of token (Bearer for now)")
    val tokenType: String?,
)
