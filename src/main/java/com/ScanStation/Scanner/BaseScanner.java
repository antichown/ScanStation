package com.ScanStation.Scanner;

import com.ScanStation.Bean.PayloadBean;
import com.ScanStation.Bean.ResultBean;
import com.ScanStation.Bean.ScanBean;
import com.ScanStation.Tools.Avitor.avitorTools;
import com.commonOkHttp.CommonOkHttpClient;
import com.commonOkHttp.CommonOkHttpClientBuilder;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@Log4j2
public class BaseScanner extends Thread implements Scanner{
    PayloadBean payload;

    @Override
    public Callable<ResultBean> scan(PayloadBean payload) {
        avitorTools avitor = new avitorTools();
        for (ScanBean scanBean : payload.getScanList()) {
            CommonOkHttpClient httpClientNotSafe = new CommonOkHttpClientBuilder().unSafe(true).build();
            Map<String, String> response = httpClientNotSafe.request(scanBean);
            scanBean.setResponse(response);

            Map<String, Object> env = new HashMap<>();
            env.put("normalrequest", payload.getNormalRequest());
            env.put("scaned", scanBean);
            env.putAll(scanBean.getResponse());
            log.debug(scanBean.getResponse().get("body"));
            log.debug("表达式环境:"+env);
            Boolean result = avitor.execAvitor(scanBean.getExpression(), env);
            scanBean.setResult(result);
        }
        return new ResultBeanCallable(getResult(payload));
    }

    @Override
    public ResultBean getResult(PayloadBean payload) {
        ResultBean resultBean = new ResultBean();
        Map<String, Object> env = new HashMap<>();
        for (ScanBean scanBean : payload.getScanList()) {
            env.put(scanBean.getName(), scanBean.getResult());
        }

        avitorTools avitor = new avitorTools();
        Boolean result = avitor.execAvitor(payload.getExpressions(), env);
        ArrayList<ScanBean> vulRequest = new ArrayList<>();
        if (result) {
            for (ScanBean scanBean : payload.getScanList()) {
                if (scanBean.getResult()) {
                    vulRequest.add(scanBean);
                }
            }

            if (!vulRequest.isEmpty()) {
                resultBean.setVulRequest(vulRequest);
                resultBean.setRuleName(payload.getRuleName());
                resultBean.setOriginalRequest(payload.getNormalRequest());
                resultBean.setDetail(payload.getDetail());
                return resultBean;
            }
        }
        return resultBean;
    }

}
