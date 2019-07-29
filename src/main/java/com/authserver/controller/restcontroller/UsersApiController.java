package com.authserver.controller.restcontroller;

import java.util.Map;

import com.authserver.data.ApiResult;
import com.authserver.data.vo.IssueUserJwtVO;
import com.authserver.service.UsersService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@AllArgsConstructor
@RestController
public class UsersApiController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private UsersService usersSvc;
   
    @GetMapping("/users/api/duplicated/{nickname}")
    public Map<String, Object> userApiCheckEmailDuplicated(@PathVariable String nickname) {
        ApiResult selectResult = usersSvc.selectUserByKey("nickname", nickname);

        if (selectResult.getResult() && selectResult.getDataAsStr("selectedUser") != null) {
            return ApiResult.make(false).toMap(); // 이미 회원정보 있음
        }
        
        return ApiResult.make(true).toMap();
    }

    @RequestMapping(value="/users/api/jwt", method={ RequestMethod.GET, RequestMethod.POST })
    public Map<String, Object> userApiGetJwt(@ModelAttribute IssueUserJwtVO issueUserJwtVo) {
        return usersSvc.issueUserJwt(issueUserJwtVo.getAudience(), issueUserJwtVo.getEmail(),
                                     issueUserJwtVo.getPassword(), issueUserJwtVo.getDuration()).toMap();
    }
}