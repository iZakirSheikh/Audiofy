package com.prime.player.billing;

/*
 * This class is an sample of how you can check to make sure your purchases on the device came from
 * Google Play. Putting code like this on your server will provide additional protection.
 * <p>
 * One thing that you may also wish to consider doing is caching purchase IDs to make replay attacks
 * harder. The reason this code isn't just part of the library is to allow you to customize it (and
 * rename it!) to make generic patching exploits more difficult.
 */

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * Security-related methods. For a secure implementation, all of this code should be implemented on
 * a server that communicates with the application on the device.
 */
class Security {
    static final private String TAG = "IABUtil/Security";
    static final private String KEY_FACTORY_ALGORITHM = "RSA";
    static final private String SIGNATURE_ALGORITHM = "SHA1withRSA";

    /**
     * BASE_64_ENCODED_PUBLIC_KEY should be YOUR APPLICATION PUBLIC KEY. You currently get this
     * from the Google Play developer console under the "Monetization Setup" category in the
     * Licensing area. This build has been setup so that if you define base64EncodedPublicKey in
     * your local.properties, it will be echoed into BuildConfig.
     */

    final private static String BASE_64_ENCODED_PUBLIC_KEY = Private.PLAY_CONSOLE_PUBLIC_KEY;

    /**
     * Verifies that the data was signed with the given signature
     *
     * @param signedData the signed JSON string (signed, not encrypted)
     * @param signature  the signature for the data, signed with the private key
     */
    static public boolean verifyPurchase(String signedData, String signature) {
        if ((TextUtils.isEmpty(signedData) || TextUtils.isEmpty(BASE_64_ENCODED_PUBLIC_KEY)
                || TextUtils.isEmpty(signature))
        ) {
            Log.w(TAG, "Purchase verification failed: missing data.");
            return false;
        }
        try {
            PublicKey key = generatePublicKey(BASE_64_ENCODED_PUBLIC_KEY);
            return verify(key, signedData, signature);
        } catch (IOException e) {
            Log.e(TAG, "Error generating PublicKey from encoded key: " + e.getMessage());
            return false;
        }
    }

    /**
     * Generates a PublicKey instance from a string containing the Base64-encoded public key.
     *
     * @param encodedPublicKey Base64-encoded public key
     * @throws IOException if encoding algorithm is not supported or key specification
     *                     is invalid
     */
    static private PublicKey generatePublicKey(String encodedPublicKey) throws IOException {
        try {
            byte[] decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
            return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
        } catch (NoSuchAlgorithmException e) {
            // "RSA" is guaranteed to be available.
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            String msg = "Invalid key specification: " + e;
            Log.w(TAG, msg);
            throw new IOException(msg);
        }
    }

    /**
     * Verifies that the signature from the server matches the computed signature on the data.
     * Returns true if the data is correctly signed.
     *
     * @param publicKey  public key associated with the developer account
     * @param signedData signed data from server
     * @param signature  server signature
     * @return true if the data and signature match
     */
    static private Boolean verify(PublicKey publicKey, String signedData, String signature) {
        byte[] signatureBytes;
        try {
            signatureBytes = Base64.decode(signature, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Base64 decoding failed.");
            return false;
        }
        try {
            Signature signatureAlgorithm = Signature.getInstance(SIGNATURE_ALGORITHM);
            signatureAlgorithm.initVerify(publicKey);
            signatureAlgorithm.update(signedData.getBytes());
            if (!signatureAlgorithm.verify(signatureBytes)) {
                Log.w(TAG, "Signature verification failed...");
                return false;
            }
            return true;
        } catch (NoSuchAlgorithmException e) {
            // "RSA" is guaranteed to be available.
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "Invalid key specification.");
        } catch (SignatureException e) {
            Log.e(TAG, "Signature exception.");
        }
        return false;
    }
}