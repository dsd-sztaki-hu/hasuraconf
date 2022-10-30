package com.beepsoft.hasura.actions;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * HttpRequest wrapper to provide the actual action body to the action handler also taking care of passing the
 * raw action payload to the action handler in its input arg in a field called "actionPayload". This actionPayload
 * field must be marked with @HasuraIgnoreField.
 */
public class ActionRequestWrapper extends HttpServletRequestWrapper {

    private String body;
    private String actionBody;
    private String actionName;
    private static ObjectMapper mapper = new ObjectMapper();

    public ActionRequestWrapper(HttpServletRequest request) throws IOException
    {
        super(request);
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                stringBuilder.append("");
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }
        body = stringBuilder.toString();
        // {
        //  "request_query": "mutation {\n  uploadDefinitions(args:{\n    content:\"qweqwe\"\n  }) {\n    result\n  }\n}",
        //  "session_variables": {
        //    "x-hasura-role": "admin"
        //  },
        //  "input": {
        //    "args": {
        //      "content": "qweqwe"
        //    }
        //  },
        //  "action": {
        //    "name": "uploadDefinitions"
        //  }
        //}
        Map<String, Object> actionPayload = mapper.readValue(body, Map.class);
        Map<String, Object> input = (Map<String, Object>)actionPayload.get("input");
        if (input.keySet().size() > 1) {
            throw new ActionException("Action `"+((Map)actionPayload.get("action")).get("name")+"` has more than 1 arguments. You need to define actions with a single argument of an object type.");
        }

        Object arg = input.size() == 0 ? new HashMap() : input.values().stream().findFirst().get();
        if (!(arg instanceof Map)) {
            throw new ActionException("Action `"+((Map)actionPayload.get("action")).get("name")+"` must have a single argument of an object type.");
        }
        // Set the action payload in the arg even if it is not contained in the input type. If it exist it will be
        // parsed and made available to the action
        Map actionPayloadCopy = mapper.readValue(body, Map.class);
        ((Map) arg).put("actionPayload", actionPayloadCopy);
        // This will be passed to the action action handler
        actionBody = mapper.writeValueAsString(arg);
        // THis will be used by the HasuraActionController to figure out where to forward the request to
        actionName = ((Map<String, String>)actionPayload.get("action")).get("name");
    }

    /**
     * THe input stream will allow reading the contents of actionBody
     * @return
     * @throws IOException
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(actionBody.getBytes());
        ServletInputStream servletInputStream = new ServletInputStream() {
            @Override
            public boolean isFinished()
            {
                return false;
            }

            @Override
            public boolean isReady()
            {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener)
            {
                throw new UnsupportedOperationException();
            }

            public int read() throws IOException {
                return byteArrayInputStream.read();
            }
        };
        return servletInputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(this.getInputStream()));
    }

    public String getBody() {
        return this.body;
    }

    public String getActionBody() {
        return this.actionBody;
    }

    public String getActionName() {
        return this.actionName;
    }

}
