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
import net.oauth.j2me.Util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Date;
import java.util.Random;
import net.oauth.j2me.signature.OAuthSignature;
import com.substanceofcode.utils.Log;


public class OAuthMessage {
    private String requestMethod;
    private String requestURL;
    private String consumerKey;
    private String version;
    private String signatureMethod;
    private String signature;
    private String timestamp;
    private String nonce;
    private String token;
    private String tokenSecret;
    private String callback;
    private String verifier;
    private Hashtable requestParameters;
    
    public static final String METHOD_GET="GET";
    public static final String METHOD_POST="POST";

    public OAuthMessage() {
        requestMethod = "GET";
        version = "1.0";
        signatureMethod = "PLAINTEXT";

        Date d = new Date();
        timestamp = Long.toString(d.getTime()/1000); // long timestamp instead of double

        Nonce n = new Nonce();
        nonce = n.getNonce();
        requestParameters = new Hashtable(); //start with empty rather than null
        tokenSecret=""; //empty string better than null
    }
/*
    public OAuthMessage(HttpServletRequest request) omitted
*/
    
    /*
    public OAuthMessage(String response) {
        requestParameters = new Hashtable(); //start with empty rather than null
        parseResponseString(response);
    }
    */
    
    //
    // Standard getters and setters
    //
    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestURL() {
        return requestURL;
    }

    public void setRequestURL(String requestURL) {
        this.requestURL = requestURL;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSignatureMethod() {
        return signatureMethod;
    }

    public void setSignatureMethod(String signatureMethod) {
        this.signatureMethod = signatureMethod;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public String getVerifier() {
        return verifier;
    }

    public void setVerifier(String verifier) {
        this.verifier = verifier;
    }

    public Hashtable getRequestParameters() {
        return requestParameters;
    }

    public void setAdditionalProperties(Hashtable requestParameters) {
        this.requestParameters = requestParameters;
    }

    //
    // Other Methods
    //
    public String getAdditionalProperty(String key) {
        return (String) requestParameters.get(key);
    }

    public void setAdditionalProperty(String key, String value) {
        if (!"oauth_signature".equals(key)) {
            requestParameters.put(key, value);
        }
    }

    public void parseResponseStringForToken(String response) throws OAuthBadDataException {
        if (response.indexOf("&") < 0) {
            throw new OAuthBadDataException("expected response to contain &");
        }
        String[] nameValuePairs = Util.split(response, "&"); //response.split("&");
        for (int i = 0; i < nameValuePairs.length; i++) {
            String[] singleNameValuePair = Util.split(nameValuePairs[i],"=");// TODO -- watch out for maxsplit
            // TODO: decode values with URLDecoder

            String key = singleNameValuePair[0].intern();
            // Check for known oauth keys
            // if ("oauth_consumer_key" == snvp) {
            // if ("oauth_consumer_key".intern() == snvp) {
            /*
            if ("oauth_consumer_key".equals(singleNameValuePair[0])) {
                consumerKey = singleNameValuePair[1];
            } else if ("oauth_version".equals(singleNameValuePair[0])) {
                version = singleNameValuePair[1];
            } else if ("oauth_signature_method".equals(singleNameValuePair[0])) {
                signatureMethod = singleNameValuePair[1];
            } else if ("oauth_signature".equals(singleNameValuePair[0])) {
                signature = singleNameValuePair[1];
            } else if ("oauth_timestamp".equals(singleNameValuePair[0])) {
                timestamp = singleNameValuePair[1];
            } else if ("oauth_nonce".equals(singleNameValuePair[0])) {
                nonce = singleNameValuePair[1];
            } else if ("oauth_token".equals(singleNameValuePair[0])) {
                token = singleNameValuePair[1];
            } else if ("oauth_token_secret".equals(singleNameValuePair[0])) {
                tokenSecret = singleNameValuePair[1];
            } else if ("oauth_callback".equals(singleNameValuePair[0])) {
                callback = singleNameValuePair[1];
            }
            
            if (!"oauth_signature".equals(singleNameValuePair[0])) {
             */
            /*
            if ("oauth_consumer_key"==key) {
                consumerKey = singleNameValuePair[1];
            } else if ("oauth_version"==key) {
                version = singleNameValuePair[1];
            } else if ("oauth_signature_method"==key) {
                signatureMethod = singleNameValuePair[1];
            } else if ("oauth_signature"==key) {
                signature = singleNameValuePair[1];
            } else if ("oauth_timestamp"==key) {
                timestamp = singleNameValuePair[1];
            } else if ("oauth_nonce"==key) {
                nonce = singleNameValuePair[1];
            } else
            */
            if ("oauth_token"==key) {
                token = singleNameValuePair[1];
            } else if ("oauth_token_secret"==key) {
                tokenSecret = singleNameValuePair[1];
            } 
            /* else if ("oauth_callback"==key) {
                callback = singleNameValuePair[1];
            }
            */
            if ("oauth_signature"!=key) {
                requestParameters.put(singleNameValuePair[0], singleNameValuePair[1]);
            }
        }
        if ((token==null) && (tokenSecret==null)) {
            throw new OAuthBadDataException("expected response token and token secret");
        }
    }

    public String normalizeRequestParameters() {
        //Log.verbose("in normalizeRequestParameters");
        String normalizedRequestParameters = "";
        Hashtable h=Util.hashtableMerge(this.convertToKeyValuePairs(), requestParameters);
        //Log.verbose("made it past hmerge");
        Enumeration keys=Util.sort(h.keys());
        //Log.verbose("made it past sort");
        OAuthParameterEncoder encoder = new OAuthParameterEncoder();
        try {
            String key = "";
            while (keys.hasMoreElements()) {
                try {
                    key = (String) keys.nextElement();
                    if (!"".equals(normalizedRequestParameters)) {
                        normalizedRequestParameters += "&";
                    }
                    normalizedRequestParameters += encoder.encode(key) + "=" + encoder.encode((String) h.get(key));
                    //normalizedRequestParameters += key + "=" + (String) h.get(key);
                } catch (java.util.NoSuchElementException e) {
                    ;
                }
            }
        } catch (NullPointerException e) {
            ;
        } catch (Exception e) {
            ;
        }
        //Log.verbose("normalized params="+normalizedRequestParameters);
        return normalizedRequestParameters;
    }

    public String signatureBaseString(String consumerSecret) {
        //Log.verbose("in signatureBaseString");
        String signatureBaseString = "";
        OAuthParameterEncoder encoder = new OAuthParameterEncoder();

        signatureBaseString += encoder.encode(requestMethod);
        signatureBaseString += "&" + encoder.encode(requestURL);
        signatureBaseString += "&" + encoder.encode(this.normalizeRequestParameters());
        //signatureBaseString += "&" + encoder.encode(consumerSecret); // don't want this in sig base string
        // not sure if we want the token secret stuff either
        /*
        if (tokenSecret != null) {
            signatureBaseString += "&" + encoder.encode(tokenSecret);
        } else {
            signatureBaseString += "&";
        }
         **/
        //Log.verbose("sig base string="+signatureBaseString);
        return signatureBaseString;
    }

    public String concatConsumerAndTokenSecrets(String consumerSecret, String tokenSecret) {
        OAuthParameterEncoder encoder = new OAuthParameterEncoder();
        return encoder.encode(consumerSecret)
            + "&" + encoder.encode(tokenSecret);
    }

    public void createSignature() {
        if (!"".equals(signatureMethod)) {
            createSignature(signatureMethod, "");
        }
    }

    public void createSignature(String signatureMethod, String consumerSecret) {
        this.signatureMethod = signatureMethod;
        String signatureClassName = "";
        if ("PLAINTEXT".equals(signatureMethod)) {
            signatureClassName = "net.oauth.j2me.signature.PLAINTEXTSignature";
            createSignature(new net.oauth.j2me.signature.PLAINTEXTSignature(), consumerSecret);
        } else if ("HMAC-SHA1".equals(signatureMethod)) {
            signatureClassName = "net.oauth.j2me.signature.HMACSHA1Signature";
            createSignature(new net.oauth.j2me.signature.HMACSHA1Signature(), consumerSecret);
        }
        //Log.verbose("sig method="+signatureMethod+", sig class name="+signatureClassName);
		/*
        if (!"".equals(signatureClassName)) {
            try {
                createSignature((OAuthSignature) Class.forName(signatureClassName).newInstance(), consumerSecret);
            } catch (java.lang.InstantiationException e) {
                Log.verbose(e.toString());
            } catch (java.lang.ClassNotFoundException e) {
                Log.verbose(e.toString());;
            } catch (Exception e) {
                Log.verbose(e.toString());;
            }
        }
		*/
    }

    public void createSignature(OAuthSignature sigGenerator, String consumerSecret) {
        signatureMethod = sigGenerator.getMethod();
        //Log.verbose("Sig method="+signatureMethod);
        if (!"PLAINTEXT".equals(signatureMethod)) {
            sigGenerator.setMessage(this.signatureBaseString(consumerSecret));
            sigGenerator.setKey(concatConsumerAndTokenSecrets(consumerSecret, tokenSecret));
        } else {
            sigGenerator.setMessage(consumerSecret);
            sigGenerator.setKey(tokenSecret);
        }
        signature = sigGenerator.getSignature();
    }

    public String convertToUrlParameters() {
        OAuthParameterEncoder encoder = new OAuthParameterEncoder();

        String encodedMessage = "";
        
        if (!"".equals(callback) && callback!=null) {
            encodedMessage += "oauth_callback=" + encoder.encode(callback) + "&";
        }
        if (!"".equals(consumerKey)) {
            encodedMessage += "oauth_consumer_key=" + encoder.encode(consumerKey);
        }
        if (!"".equals(nonce)) {
            encodedMessage += "&oauth_nonce=" + encoder.encode(nonce);
        }
        if (!"".equals(signature)) {
            encodedMessage += "&oauth_signature=" + encoder.encode(signature);
        }
        if (!"".equals(signatureMethod)) {
            encodedMessage += "&oauth_signature_method=" + encoder.encode(signatureMethod);
        }
        if (!"".equals(timestamp)) {
            encodedMessage += "&oauth_timestamp=" + encoder.encode(timestamp);
        }
        if (!"".equals(token) && token!=null) {
            encodedMessage += "&oauth_token=" + encoder.encode(token);
        }
        if (!"".equals(verifier) && verifier!=null) {
            encodedMessage += "&oauth_verifier=" + encoder.encode(verifier);
        }
        if (!"".equals(version)) {
            encodedMessage += "&oauth_version=" + encoder.encode(version);
        }
        // do we really want to send token secret?
        /*
        if (!"".equals(tokenSecret) && tokenSecret!=null) {
            encodedMessage += "&oauth_token_secret=" + encoder.encode(tokenSecret);
        }
        */
        try {
            String key = "";
            Enumeration keys=requestParameters.keys();
            while (keys.hasMoreElements()) {
                try {
                    key = (String) keys.nextElement();
                    if (key.indexOf("oauth_") == -1) {
                        encodedMessage += "&" + key + "="
                            + encoder.encode((String) requestParameters.get(key));
                    }
                } catch (java.util.NoSuchElementException e) {
                    ;
                }
            }
        } catch (NullPointerException e) {
            ;
        } catch (Exception e) {
            ;
        }

        return encodedMessage;
    }

    public Hashtable convertToKeyValuePairs() {
        //Log.verbose("in convertToKeyValuePairs");
        Hashtable keyValuePairs = new Hashtable();

        if (!"".equals(consumerKey) && consumerKey!=null) {
            keyValuePairs.put("oauth_consumer_key", consumerKey);
        }
        if (!"".equals(version) && version!=null) {
            keyValuePairs.put("oauth_version", version);
        }
        if (!"".equals(signatureMethod) && signatureMethod!=null) {
            keyValuePairs.put("oauth_signature_method", signatureMethod);
        }
        if (!"".equals(signature) && signature!=null) {
            keyValuePairs.put("oauth_signature", signature);
        }
        if (!"".equals(timestamp) && timestamp!=null) {
            keyValuePairs.put("oauth_timestamp", timestamp);
        }
        if (!"".equals(nonce) && nonce!=null) {
            keyValuePairs.put("oauth_nonce", nonce);
        }
        if (!"".equals(token) && token!=null) {
            keyValuePairs.put("oauth_token", token);
        }
        // do we want token secret in there?
        /*
        if (!"".equals(tokenSecret) && tokenSecret!=null) {
            keyValuePairs.put("oauth_token_secret", tokenSecret);
        }
         **/
        if (!"".equals(callback) && callback != null) {
            keyValuePairs.put("oauth_callback", callback);
        }
 //TODO -- why is this commented out?
        try {
            //Iterator> iter =
            //    requestParameters.entrySet().iterator();
            Enumeration keys=requestParameters.keys();
            String key = "";
            while (keys.hasMoreElements()) {
                try {
                    key = (String) keys.nextElement();
                    if (key.indexOf("oauth_") == -1) {
                        keyValuePairs.put(key, (String) requestParameters.get(key));
                    }
                } catch (java.util.NoSuchElementException e) {
                    ;
                }
            }
        } catch (NullPointerException e) {
            ;
        } catch (Exception e) {
            ;
        }
        //Log.verbose("done convertToKeyValuePairs");
        return keyValuePairs;
    }

	public String convertToAuthorizationHeader() {
        OAuthParameterEncoder encoder = new OAuthParameterEncoder();
		String ret = "OAuth ";

		if (!"".equals(nonce) && nonce!=null) {
			ret += "oauth_nonce=\""+encoder.encode(nonce)+"\", ";
		}
		if (!"".equals(callback) && callback != null) {
			ret += "oauth_callback=\""+encoder.encode(callback)+"\", ";
		}
		if (!"".equals(signatureMethod) && signatureMethod!=null) {
			ret += "oauth_signature_method=\""+encoder.encode(signatureMethod)+"\", ";
		}
		if (!"".equals(timestamp) && timestamp!=null) {
			ret += "oauth_timestamp=\""+encoder.encode(timestamp)+"\", ";
		}
		if (!"".equals(consumerKey) && consumerKey!=null) {
			ret += "oauth_consumer_key=\""+encoder.encode(consumerKey)+"\", ";
		}
		if (!"".equals(token) && token!=null) {
			ret += "oauth_token=\""+encoder.encode(token)+"\", ";
		}
		if (!"".equals(signature) && signature!=null) {
			ret += "oauth_signature=\""+encoder.encode(signature)+"\", ";
		}
		if (!"".equals(version) && version!=null) {
			ret += "oauth_version=\""+encoder.encode(version)+"\"";
		}
		return ret;
	}
}
