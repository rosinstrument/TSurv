/*
 * Copyright 2007 Sxip Identity Corporation
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
package net.oauth.j2me;

import com.substanceofcode.utils.Log;
import com.substanceofcode.utils.StringUtil;
import java.io.IOException;
import java.util.Hashtable;
import javax.microedition.io.HttpConnection;
import net.oauth.j2me.token.*;

public class Consumer {

    private ConsumerConfig config;
    private String signatureMethod;

    public Consumer() {
        config = new ConsumerConfig();
        signatureMethod = "PLAINTEXT";
    }

    /**
     * Consumer constructor
     *
     * @param key consumer key (provided by your OAuth service provider)
     * @param secret consumer secret (from your Oauth service provider)
     *
     */
    public Consumer(String key, String secret) {
        config = new ConsumerConfig(key, secret);
        signatureMethod = "PLAINTEXT";
    }

    public Consumer(String key, String secret, String callbackEndpoint) {
        config = new ConsumerConfig(key, secret, callbackEndpoint);
        signatureMethod = "PLAINTEXT";
    }

    //
    // Getters and setters
    //
    public ConsumerConfig getConfig() {
        return config;
    }

    public void setConfig(ConsumerConfig config) {
        this.config = config;
    }

    public String getSignatureMethod() {
        return signatureMethod;
    }

    public void setSignatureMethod(String signatureMethod) {
        this.signatureMethod = signatureMethod;
    }

    //
    // Token request messages
    //
    public RequestToken getRequestToken(String endpoint, String callback) throws OAuthServiceProviderException {
        RequestToken token = null;

        OAuthMessage requestMessage = new OAuthMessage();
        requestMessage.setRequestURL(endpoint);
        requestMessage.setConsumerKey(config.getKey());
        if (callback != null) {
            requestMessage.setCallback(callback);
        }
        requestMessage.createSignature(signatureMethod, config.getSecret());
        String url = endpoint + "?" + requestMessage.convertToUrlParameters();
        Log.info("Attempting to get RT from " + url);
        String responseString = null;
        try {
            responseString = Util.getViaHttpsConnection(url);
        } catch (IOException e) { // 
            Log.info(e.toString());
            return null;
        }
        Log.info("Consumer got responseString when requesting RequestToken..." + responseString);
        OAuthMessage responseMessage = new OAuthMessage();
        try {
            responseMessage.parseResponseStringForToken(responseString); // can throw an exception or return null if bad data...
        } catch (OAuthBadDataException e) {
            Log.info(e.toString());
            return null;
        }
        token = new RequestToken(responseMessage.getToken(), responseMessage.getTokenSecret());
        return token;
    }

    // returns null if something goes wrong
    public AccessToken getAccessToken(String endpoint, String verifier, RequestToken requestToken) throws OAuthServiceProviderException, BadTokenStateException {
        if (requestToken.getExchanged()) {
            throw new BadTokenStateException("Request token already used");
        }
        AccessToken token = null;
        OAuthMessage requestMessage = new OAuthMessage();
        requestMessage.setRequestURL(endpoint);
        requestMessage.setConsumerKey(config.getKey());
        requestMessage.setToken(requestToken.getToken());
        requestMessage.setTokenSecret(requestToken.getSecret());
        requestMessage.setCallback(config.getCallbackEndpoint());
        requestMessage.setVerifier(verifier);
        requestMessage.createSignature(signatureMethod, config.getSecret());
        String url = endpoint + "?" + requestMessage.convertToUrlParameters();
        Log.info("Attempting to get AT from " + url);
        String responseString = null;
        try {
            responseString = Util.getViaHttpsConnection(url);
        } catch (IOException e) {
            Log.info(e.toString());
            return null;
        }
        Log.info("Consumer got responseString when requesting AT..." + responseString);
        OAuthMessage responseMessage = new OAuthMessage();
        try {
            responseMessage.parseResponseStringForToken(responseString);
        } catch (OAuthBadDataException e) {
            Log.info(e.toString());
            return null;
        }
        token = new AccessToken(responseMessage.getToken(), responseMessage.getTokenSecret());
        token.setAdditionalParams(responseMessage.getRequestParameters());
        requestToken.setExchanged(true);
        return token;
    }

    public void sign(HttpConnection con, String body, AccessToken accessToken) throws IOException {
        String endpoint = con.getURL();
        Hashtable qp = new Hashtable();
        int i = endpoint.indexOf('?');
        if (i >= 0) {
            endpoint = endpoint.substring(0, i);
            String[] queries = Util.split(con.getQuery(), "&");
            String[] kv;
            for (i = 0; i < queries.length; i++) {
                kv = Util.split(queries[i], "=");
                if (kv != null && kv.length == 2) {
                    qp.put(kv[0], kv[1]);
                }
            }
        }
        if (body.length() > 0) {
            String[] queries = Util.split(body, "&");
            String[] kv;
            for (i = 0; i < queries.length; i++) {
                kv = Util.split(queries[i], "=");
                if (kv != null && kv.length == 2) {
                    qp.put(kv[0], StringUtil.urlDecode(kv[1]));
                }
            }
        }
        OAuthMessage req = new OAuthMessage();
        req.setRequestMethod(con.getRequestMethod());
        req.setRequestURL(endpoint);
        req.setConsumerKey(config.getKey());
        req.setToken(accessToken.getToken());
        req.setTokenSecret(accessToken.getSecret());
        req.setAdditionalProperties(qp);
        req.createSignature(signatureMethod, config.getSecret());
        String header = req.convertToAuthorizationHeader();
        con.setRequestProperty("Authorization", header);
    }

    public String accessProtectedResource(String endpoint, AccessToken accessToken, Hashtable queryParams) throws OAuthServiceProviderException, IOException {
        return accessProtectedResource(endpoint, accessToken, queryParams, OAuthMessage.METHOD_GET);
    }

    public String accessProtectedResource(String endpoint, AccessToken accessToken, Hashtable queryParams, String httpMethod) throws OAuthServiceProviderException, IOException {
        String responseString = null;
        OAuthMessage requestMessage = new OAuthMessage();
        requestMessage.setRequestMethod(httpMethod);
        requestMessage.setRequestURL(endpoint);
        requestMessage.setConsumerKey(config.getKey());
        requestMessage.setToken(accessToken.getToken());
        requestMessage.setTokenSecret(accessToken.getSecret());
        if (queryParams == null) {
            requestMessage.setAdditionalProperties(new Hashtable());
        } else {
            requestMessage.setAdditionalProperties(queryParams);
        }
        requestMessage.createSignature(signatureMethod, config.getSecret());

        String url = endpoint + "?" + requestMessage.convertToUrlParameters();
        Log.info("Attempting to access " + url);

        if (OAuthMessage.METHOD_POST.equals(httpMethod)) {
            responseString = Util.postViaHttpsConnection(url);
        } else {
            responseString = Util.getViaHttpsConnection(url);
        }
        Log.info("Consumer got responseString " + responseString);
        return responseString;
    }

    public RequestToken markRequestTokenAuthorized(RequestToken token) {
        token.setAuthorized(true);
        return token;
    }

    public RequestToken markRequestTokenExchanged(RequestToken token) {
        token.setExchanged(true);
        return token;
    }
}