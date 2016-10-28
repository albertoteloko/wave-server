package com.acs.waveserver.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RouterBuilder {

    private final List<RequestFilter> filters = new ArrayList<>();
    private final List<RequestHandler> handlers = new ArrayList<>();
    private final Map<ResponseCodes, ErrorCodeHandler> errorCodeHandlers = getDefaultErrorHandlers();
    private ExceptionHandler exceptionHandler = getDefaultExceptionHandler();


    public Router build() {
        return new Router(filters, handlers, errorCodeHandlers, exceptionHandler);
    }

    private ExceptionHandler getDefaultExceptionHandler() {
        return null;
    }

    private Map<ResponseCodes, ErrorCodeHandler> getDefaultErrorHandlers() {
        return null;
    }
}