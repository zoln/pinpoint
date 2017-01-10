/*
 * Copyright 2015 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.web.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.navercorp.pinpoint.web.config.ConfigProperties;
import com.navercorp.pinpoint.web.service.UserService;
import com.navercorp.pinpoint.web.vo.User;

/**
 * @author HyunGil Jeong
 */
@Controller
public class ConfigController {
    
    private final static String SSO_USER = "SSO_USER";

    @Autowired
    private ConfigProperties webProperties;
    
    @Autowired
    private UserService userService;
    
    @RequestMapping(value="/configuration", method=RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getProperties(@RequestHeader(value=SSO_USER, required=false) String userId) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("sendUsage", webProperties.getSendUsage());
        result.put("editUserInfo", webProperties.getEditUserInfo());
        result.put("showActiveThread", webProperties.isShowActiveThread());
        result.put("openSource", webProperties.isOpenSource());
        
        if (!StringUtils.isEmpty(userId)) {
            User user = userService.selectUserByUserId(userId);
            result.put("userId", user.getUserId());
            result.put("userName", user.getName());
            result.put("userDepartment", user.getDepartment());
        }
        
        if(!StringUtils.isEmpty(webProperties.getSecurityGuideUrl())) {
            result.put("securityGuideUrl", webProperties.getSecurityGuideUrl());
        }
        
        return result;
    }
}
