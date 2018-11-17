package com.example.administrator.myapplication;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Earlybro_DaeGuen on 2017-11-06.
 */
public class DownloadTips extends StringRequest {

    final static private String URL = "ServerImageURL";

    private Map<String, String> parameters;

    public DownloadTips(String Dir, Response.Listener<String> listener){

        super(Method.POST, URL, listener, null);
        parameters = new HashMap<>();
        parameters.put("dir", Dir);
    }

    @Override
    public Map<String, String> getParams(){
        return parameters;
    }
}