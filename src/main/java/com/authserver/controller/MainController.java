package com.authserver.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.authserver.service.GoogleOAuthService;
import com.robi.data.ApiResult;
import com.robi.util.MapUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@PropertySource("classpath:config.properties")
@Controller
public class MainController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private GoogleOAuthService googleOauthSvc;
    private Environment env;

    @RequestMapping("/test")
    public ModelAndView testPage() {
        return new ModelAndView("test");
    }

    @RequestMapping("/main")
    public ModelAndView mainPage(
        @RequestParam(name = "audience", required = false) String audience,
        @RequestParam(name = "duration", required = false) String duration,
        @RequestParam(name = "afterIssueParam", required = false) String afterIssueParam,
        @RequestParam(name = "userJwt", required = false) String userJwt,
        @RequestParam(name = "keepLoggedIn", required = false) String keepLoggedIn,
        HttpServletResponse response)
    {
        // Data for cookie
        if (userJwt != null) {
            Cookie userJwtCookie = new Cookie("userJwt", userJwt);
            int userJwtCookieMaxAgeMin = 0;

            if (keepLoggedIn != null && keepLoggedIn.equals("true")) {
                userJwtCookieMaxAgeMin = Integer.parseInt(env.getProperty("users.keepLoggedInCookieLifeSec")) * 60;
            }
            else {
                userJwtCookieMaxAgeMin = Integer.parseInt(env.getProperty("users.defaultCookieLifeSec"));
            }

            userJwtCookie.setMaxAge(userJwtCookieMaxAgeMin);
            response.addCookie(userJwtCookie);
        }

        // Data for view
        Map<String, Object> modelMap = new HashMap<String, Object>();
        modelMap.put("audience", audience == null ? env.getProperty("auths.audienceName") : audience);
        modelMap.put("duration", duration);
        /**
         *  <afterIssueParam>
         *  1. popup
         *   ex) afterIssueParam=popup
         *       -> call parent's javascript function : opener.d4r_login_return(userJwt, keepLoggedIn);
         *  2. iframe
         *   ex) afterIssueParam=iframe
         *       -> call parent's javascript function : $(parent.document).d4r_login_return(userJwt, keepLoggedIn);
         *  3. redirection
         *   ex) afterIssueParam=https://www.de4bi.net?userJwt={userJwt}&keepLoggedIn={true|false}
         **/
        modelMap.put("afterIssueParam", afterIssueParam);
        modelMap.put("clientSalt", env.getProperty("users.password.clientSalt"));

        // - Google Oauth ---------------------------------------------------------------
        ApiResult codeUrlRst = googleOauthSvc.makeCodeUrl();
        String googleOauthCodeUrl = null;

        if (codeUrlRst != null && codeUrlRst.getResult()) {
            googleOauthCodeUrl = codeUrlRst.getDataAsStr("codeUrl");
        }
        
        modelMap.put("googleLoginUrl", googleOauthCodeUrl);
        codeUrlRst = null;

        // - Kakao Oauth ---------------------------------------------------------------
        // codeUrlRst = kakaoOauthSvc.makeCodeUrl();
        String kakaoOauthCodeUrl = null;

        if (codeUrlRst != null && codeUrlRst.getResult()) {
            kakaoOauthCodeUrl = codeUrlRst.getDataAsStr("codeUrl");
        }

        modelMap.put("kakaoLoginUrl", kakaoOauthCodeUrl);

        // - Naver Oauth ---------------------------------------------------------------
        // codeUrlRst = naverOauthSvc.makeCodeUrl();
        String naverOauthCodeUrl = null;

        if (codeUrlRst != null && codeUrlRst.getResult()) {
            naverOauthCodeUrl = codeUrlRst.getDataAsStr("codeUrl");
        }

        modelMap.put("naverLoginUrl", naverOauthCodeUrl);

        // - Return view ---------------------------------------------------------------
        return new ModelAndView("main", modelMap); // main.jsp
    }

    @RequestMapping("/register")
    public ModelAndView registerPage(
        @RequestParam String email, @RequestParam String sign, @RequestParam String nonce) {
        return new ModelAndView("register", MapUtil.toMap("email", email, "sign", sign, "nonce", nonce,
                                                          "clientSalt", env.getProperty("users.password.clientSalt")));
    }

    @RequestMapping("/errors")
    public ModelAndView errorPage(
        @RequestParam(required = false) String alertMsg, @RequestParam(required = false) String errorMsg) {
        return new ModelAndView("error", MapUtil.toMap("alertMsg", alertMsg, "errorMsg", errorMsg));
    }
}