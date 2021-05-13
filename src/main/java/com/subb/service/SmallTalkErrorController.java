package com.subb.service;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Controller
public class SmallTalkErrorController implements ErrorController {
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            switch (HttpStatus.valueOf(statusCode)) {
                case BAD_REQUEST:
                    return "error-400";
                case FORBIDDEN:
                    return "error-403";
                case NOT_FOUND:
                    return "error-404";
                case METHOD_NOT_ALLOWED:
                    return "error-405";
                case INTERNAL_SERVER_ERROR:
                    return "error-500";
                default:
                    return "error";
            }
        }
        return "error";
    }

    @Override
    public String getErrorPath() {
        return null;
    }
}
