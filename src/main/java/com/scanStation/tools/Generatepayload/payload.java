package com.scanStation.tools.Generatepayload;

import com.scanStation.bean.ruleBean;
import com.scanStation.bean.scannerBean;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class payload {
    private ruleBean rule;

    public payload(ruleBean rule) {
        this.rule = rule;
    }

    public ArrayList<scannerBean> Generatepayload() {
        ArrayList<scannerBean> scanner = new ArrayList<>();
        if ("Form".equals(rule.getType())) {
            getformPayloads(scanner);
        }if ("Json".equals(rule.getType())){
            getjsonPayload(scanner);
        }
        getheaderPayload(scanner);
        return scanner;
    }

    private ArrayList<scannerBean> getformPayloads(ArrayList<scannerBean> scanner) {

        int i = scanner.size();
        for (String vul : rule.getVulParam().split("&")) {
            for (Map<String, String> payload : rule.getPayloads()) {
                Map<String, Object> params = rule.getParams();
                String[] var = vul.split("=");
                String tmp = payload.get("payload");
                //存在带外等替换
                tmp = replaceSpecialParam(tmp, "{{dnslog}}", rule.getOob());
                //组装payload放入
                params.put(var[0], var.length >= 2 ? var[1] + tmp : tmp); //暂时直接加入

                scannerBean scb = new scannerBean();
                scb.setUrl(rule.getUrl());
                scb.setName("payload" + i);
                scb.setParam(params);
                scb.setExpression(payload.get("expression"));
                scb.setMethod(rule.getMethod());
                scb.setType("Form");
                scanner.add(scb);
//                log.debug(scb.toString());
                i++;
            }
        }

        return scanner;
    }

    private ArrayList<scannerBean> getheaderPayload(ArrayList<scannerBean> scanner) {
        if (rule.isHeaderscan()) {
            int i = scanner.size();
            for (Map.Entry<String, String> header : rule.getHeader().entrySet()) {
                for (Map<String, String> payload : rule.getPayloads()) {
                    scannerBean scb = new scannerBean();
                    Map<String, String> headers = new HashMap<>();
                    Map<String, Object> params = new HashMap<>();
                    headers.put(header.getKey(), payload.get("payload"));
                    if(rule.getType().equals("Form")) {
                        params = rule.getParams(); //原始参数
                    }else if (rule.getType().equals("Json")){
                        replaceJson replaceJson = new replaceJson();
                        params= replaceJson.replace(rule.getOriginalParam(),"","");
                    }
                    scb.setHeader(headers);
                    scb.setUrl(rule.getUrl());
                    scb.setName("payload" + i);
                    scb.setParam(params);
                    scb.setExpression(payload.get("expression"));
                    scb.setMethod(rule.getMethod());
                    scb.setType(rule.getType());
                    scb.setHeaderscan(true);
                    scanner.add(scb);
                    i++;
                }
            }
        } else {
            for (scannerBean scb : scanner) {
                scb.setHeader(rule.getHeader());
            }
        }
        return scanner;
    }

    private ArrayList<scannerBean> getjsonPayload(ArrayList<scannerBean> scanner) {
        int i = scanner.size();
        for (String vul : rule.getVulParam().split("&")) {
            for (Map<String, String> payload : rule.getPayloads()) {
                replaceJson replaceJson = new replaceJson();
                Map params = replaceJson.replace(rule.getOriginalParam(),vul,payload.get("payload"));

                scannerBean scb = new scannerBean();
                scb.setUrl(rule.getUrl());
                scb.setName("payload" + i);
                scb.setParam(params);
                scb.setExpression(payload.get("expression"));
                scb.setMethod(rule.getMethod());
                scanner.add(scb);
                scb.setType("Json");
                i++;
            }
        }
        return scanner;
    }

    private ArrayList<scannerBean> getMultiPayload(ArrayList<scannerBean> scanner) {
        return scanner;
    }

    @NotNull
    private String replaceSpecialParam(String tmp, String Special, String Param) {
        if (tmp.contains("Special")) {
            tmp = tmp.replace(Special, Param);//带外地址
        }
        return tmp;
    }


}