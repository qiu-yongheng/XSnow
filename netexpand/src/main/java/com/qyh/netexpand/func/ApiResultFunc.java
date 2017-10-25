package com.qyh.netexpand.func;

import android.text.TextUtils;

import com.qyh.netexpand.mode.ApiResult;
import com.qyh.netexpand.mode.ResponseCode;
import com.qyh.litemvp.common.GsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;

import io.reactivex.functions.Function;
import okhttp3.ResponseBody;

/**
 * @Description: ResponseBody转ApiResult<T>
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-30 17:55
 */
public class ApiResultFunc<T> implements Function<ResponseBody, ApiResult<T>> {
    protected Type type;

    public ApiResultFunc(Type type) {
        this.type = type;
    }

    @Override
    public ApiResult<T> apply(ResponseBody responseBody) throws Exception {
        ApiResult<T> apiResult = new ApiResult<>();
        apiResult.setCode(-1);
        try {
            String json = responseBody.string();
            // 将json解析成ApiResult
            ApiResult result = parseApiResult(json, apiResult);
            if (result != null) {
                apiResult = result;
                if (apiResult.getData() != null) {
                    T data = GsonUtil.gson().fromJson(apiResult.getData().toString(), type);
                    apiResult.setData(data);
                    apiResult.setCode(ResponseCode.HTTP_SUCCESS);
                } else {
                    apiResult.setMsg("ApiResult's data is null");
                }
            } else {
                apiResult.setMsg("json is null");
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            apiResult.setMsg(e.getMessage());
        } finally {
            responseBody.close();
        }
        return apiResult;
    }

    /**
     * 将json解析成ApiResult
     * @param json
     * @param apiResult
     * @return
     * @throws JSONException
     */
    private ApiResult parseApiResult(String json, ApiResult apiResult) throws JSONException {
        if (TextUtils.isEmpty(json)) return null;
        JSONObject jsonObject = new JSONObject(json);
        if (jsonObject.has("code")) {
            apiResult.setCode(jsonObject.getInt("code"));
        }
        if (jsonObject.has("data")) {
            apiResult.setData(jsonObject.getString("data"));
        }
        if (jsonObject.has("msg")) {
            apiResult.setMsg(jsonObject.getString("msg"));
        }
        return apiResult;
    }
}
