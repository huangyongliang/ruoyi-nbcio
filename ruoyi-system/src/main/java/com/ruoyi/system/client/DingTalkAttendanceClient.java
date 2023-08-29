package com.ruoyi.system.client;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * 钉钉考勤开放接口客户端。
 *
 * @author codex
 */
@Component
public class DingTalkAttendanceClient {

    private static final String ACCESS_TOKEN_URL = "https://oapi.dingtalk.com/gettoken";
    private static final String LIST_RECORD_URL = "https://oapi.dingtalk.com/attendance/listRecord";
    private static final int TIMEOUT = 30000;

    public String getAccessToken(String appKey, String appSecret) {
        String body = HttpRequest.get(ACCESS_TOKEN_URL + "?appkey=" + encode(appKey) + "&appsecret=" + encode(appSecret))
            .timeout(TIMEOUT)
            .execute()
            .body();
        JSONObject json = parseResponse(body);
        String token = json.getString("access_token");
        if (token == null || token.trim().isEmpty()) {
            throw new ServiceException("钉钉 access_token 获取失败");
        }
        return token;
    }

    public JSONArray listRecord(String accessToken, List<String> userIds, String checkDateFrom, String checkDateTo) {
        JSONObject payload = new JSONObject();
        payload.put("userIds", userIds);
        payload.put("checkDateFrom", checkDateFrom);
        payload.put("checkDateTo", checkDateTo);
        payload.put("isI18n", false);

        String body = HttpRequest.post(LIST_RECORD_URL + "?access_token=" + encode(accessToken))
            .timeout(TIMEOUT)
            .contentType("application/json")
            .body(payload.toJSONString())
            .execute()
            .body();
        JSONObject json = parseResponse(body);
        JSONArray result = json.getJSONArray("recordresult");
        return result == null ? new JSONArray() : result;
    }

    private JSONObject parseResponse(String body) {
        JSONObject json = JSONObject.parseObject(body);
        Integer errCode = json.getInteger("errcode");
        if (errCode != null && errCode != 0) {
            String errMsg = json.getString("errmsg");
            throw new ServiceException("钉钉接口返回异常：" + errCode + " " + (errMsg == null ? "" : errMsg));
        }
        return json;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ServiceException("URL编码失败");
        }
    }

}
