package com.example.administrator.myapplication;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Earlybro_DaeGuen on 2017-11-06.
 */
public class DownloadTip extends StringRequest {

    final static private String URL = "ServerImageURL";

    private Map<String, String> parameters;

    public DownloadTip(String shopName, int ItemNum, Response.Listener<String> listener){

        super(Method.POST, URL, listener, null);
        parameters = new HashMap<>();
        parameters.put("shopName", shopName);
        String ItemnumtoStr = String.valueOf(ItemNum);
        parameters.put("ItemNum", ItemnumtoStr);
    }

    @Override
    public Map<String, String> getParams(){
        return parameters;
    }
}