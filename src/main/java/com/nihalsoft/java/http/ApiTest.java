package com.nihalsoft.java.http;

public class ApiTest {

    public static void main(String[] args) throws Exception {

        ApiResponse res = ApiClient.url("https://api.publicapis.org/entries").get();
        System.out.println(res.getContentAsString());
    }

}
