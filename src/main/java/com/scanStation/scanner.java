package com.scanStation;

import com.scanStation.bean.ruleBean;
import com.scanStation.bean.scannerBean;
import com.scanStation.bean.vulBean;
import com.scanStation.commonOkHttp.CommonOkHttpClient;
import com.scanStation.commonOkHttp.CommonOkHttpClientBuilder;
import com.scanStation.tools.avitorTools;
import com.scanStation.tools.yamlTools;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class scanner {

    public static void main(String[] args) {
        String url = args[0];
        String path = args[1];
        String cookie;
        String param;

        if (args.length >= 3) {
            cookie = args[2];
        } else {
            cookie = "";
        }
        if (args.length >= 4) {
            param = args[3];
        } else {
            param = "";
        }
        scanner scanner = new scanner();
        ArrayList<String> re = new ArrayList<>();
        File dir = new File(path);
        String[] children = dir.list();
        for (String file : children) {
            if (file.endsWith(".yaml")) {
                scanner.scan(url, path + "/" + file, cookie, param,re);
            }
        }
        for (String r : re){
            System.out.println(r);
        }

    }


    public void scan(String url, String filePath, String cookie, String param,ArrayList<String> re) {
        log.info("------检测开始");
        //初始化request
        CommonOkHttpClient httpClientNotSafe = new CommonOkHttpClientBuilder().unSafe(true).build();
        if (cookie != null && !"".equals(cookie)) {
            //设置全局cookie
            httpClientNotSafe.setCookie(cookie);
            log.info("设置全局cookie值为:"+cookie);
        }
        if (param != null && !"".equals(param)) {
            //通过String方式设置全局参数
            httpClientNotSafe.setGlobalParam(param);
            log.info("设置全局参数值为:"+param);
        }
        log.info("---初始化request完成");
        //加载规则
        vulBean vul = new yamlTools(filePath).load();
        ruleBean rule = vul.getRules();
        rule.setUrl(url);
//        rule.setOob("q3fljw.dnslog.cn"); //设置dnslog
        log.info(rule.toString());

        log.info("加载规则:"+vul.getName());
        log.info("---规则加载完成");

        ArrayList<scannerBean> payloadAndExpression = rule.Generatepayload();
        Map<String, Object> expressionsEnv = new HashMap<>();
        for (scannerBean scb : payloadAndExpression) {
            Map response = httpClientNotSafe.request(scb.getUrl(), scb.getParam(), rule.getMethod());
            System.out.println(scb.toString());
            scb.setResult(judgment(vul, rule, scb, response));
            expressionsEnv.put(scb.getName(), scb.getResult());
        //System.out.println(scb.toString());
            log.info("单一payload检测完成");
            log.info("payload信息:"+scb.toString()+rule.getMethod());

        }
        log.info("---payload检测完成");
        //expressions判断
        avitorTools avitor = new avitorTools();
        avitor.setEnv(expressionsEnv);
        avitor.setExpression(rule.getExpressions());
        Boolean expressionsRe = avitor.execAvitor();
        if (expressionsRe) {
            for (scannerBean scb : payloadAndExpression) {
                if (scb.getResult()) {
                    re.add("++++++++++++vulFind:"+vul.getName()+"-"+scb.toString()+" "+vul.getDetail());
                    log.info("++++++++++++vulFind:"+vul.getName()+"-"+scb.toString()+" "+vul.getDetail());
                }
            }
        }
        log.info("---expressions判断完成");
        log.info("------检测完成");
    }

    private Boolean judgment(vulBean vul, ruleBean rule, scannerBean scb, Map response) {
        avitorTools avitor = new avitorTools();
        response.put("param", rule.getParams());
        response.put("method", rule.getMethod());
        response.put("url", scb.getUrl());
        response.put("dnslog",rule.getOob());
        response.put("flag",rule.getOobflag());

        avitor.setEnv(response);

        avitor.setExpression(scb.getExpression());
        Boolean re = avitor.execAvitor();
        return re;
    }
}
