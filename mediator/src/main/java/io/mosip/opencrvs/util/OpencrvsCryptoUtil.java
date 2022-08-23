package io.mosip.opencrvs.util;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.dto.BaseEventRequest;
import io.mosip.opencrvs.error.ErrorCode;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileReader;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.util.Arrays;

@Component
public class OpencrvsCryptoUtil {

    private static final Logger LOGGER = LogUtil.getLogger(OpencrvsCryptoUtil.class);

    @Value("${mosip.kernel.crypto.asymmetric-algorithm-name}")
    private String asymmetricAlgoName;
    @Value("${mosip.kernel.crypto.symmetric-algorithm-name}")
    private String symmetricAlgoName;
    @Value("${mosip.kernel.crypto.sign-algorithm-name}")
    private String signAlgoName;
    @Value("${mosip.kernel.crypto.gcm-tag-length}")
    private int symmetricGcmTagLength;
    @Value("${mosip.kernel.keygenerator.symmetric-key-length}")
    private int symmetricKeyLength;
    @Value("${mosip.kernel.crypto.thumbprint.present}")
    private boolean thumbprintPresent;
    @Value("${mosip.kernel.crypto.thumbprint.length}")
    private int thumbprintLength;
    @Value("${mosip.kernel.data-key-splitter}")
    private String keySplitter;
    private final int nonceLength = 96;
    private final int aadLength = 256;

    @Value("${mosip.opencrvs.privkey.path}")
    private String mosipPrivKeyPath;
    private PrivateKey mosipPrivateKey;
    private Cipher decryptCipher;

    @Value("${opencrvs.mosip.pubkey.path}")
    private String opencrvsPublicKeyPath;
    private PublicKey opencrvsPublicKey;
    private Cipher encryptCipher;
    private Signature verifier;
    private Signature signer;

    private KeyGenerator symmetrickeyGen;

    @PostConstruct
    public void init(){
        try{
            PEMParser parser = new PEMParser(new FileReader(FileUtils.getFile(mosipPrivKeyPath)));
            mosipPrivateKey = new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) parser.readObject());
        } catch(Exception e) {
            throw ErrorCode.CRYPTO_READ_PRIVATE_KEY_EXCEPTION.throwUnchecked(e);
        }

        try {
            decryptCipher = Cipher.getInstance(asymmetricAlgoName);
            OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            decryptCipher.init(Cipher.DECRYPT_MODE,mosipPrivateKey,oaepParams);

            signer = Signature.getInstance(signAlgoName);
            signer.initSign(mosipPrivateKey);
        } catch (Exception e){
            throw ErrorCode.CRYPTO_INIT_PRIVATE_KEY_EXCEPTION.throwUnchecked(e);
        }

        try{
            PEMParser parser = new PEMParser(new FileReader(FileUtils.getFile(opencrvsPublicKeyPath)));
            opencrvsPublicKey = new JcaPEMKeyConverter().getPublicKey(((X509CertificateHolder)parser.readObject()).getSubjectPublicKeyInfo());
        } catch(Exception e) {
            throw ErrorCode.CRYPTO_READ_PUBLIC_KEY_EXCEPTION.throwUnchecked(e);
        }

        try {
            encryptCipher = Cipher.getInstance(asymmetricAlgoName);
            OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            encryptCipher.init(Cipher.ENCRYPT_MODE, opencrvsPublicKey, oaepParams);

            symmetrickeyGen = KeyGenerator.getInstance(symmetricAlgoName.split("/")[0]);
            symmetrickeyGen.init(symmetricKeyLength);

            verifier = Signature.getInstance(signAlgoName);
            verifier.initVerify(opencrvsPublicKey);
        } catch (Exception e){
            throw ErrorCode.CRYPTO_INIT_PUBLIC_KEY_EXCEPTION.throwUnchecked(e);
        }
    }

    public boolean verify(byte[] data, byte[] signature) throws BaseCheckedException{
        try{
            verifier.update(data);
            return verifier.verify(signature);
        }
        catch (Exception e){
            throw ErrorCode.CRYPTO_SIGN_VERIFY_EXCEPTION.throwChecked(e);
        }
    }
    public void verifyThrowException(byte[] data, byte[] signature) throws BaseCheckedException{
        if(!this.verify(data, signature)){
            throw ErrorCode.CRYPTO_SIGN_VERIFY_EXCEPTION.throwChecked();
        }
    }

    public byte[] aSymmetricDecrypt(byte[] encryptedData) throws BaseCheckedException{
        try{
            return decryptCipher.doFinal(encryptedData);
        }
        catch (Exception e){
            throw ErrorCode.CRYPTO_DECRYPT_EXCEPTION.throwChecked(e);
        }
    }

    public byte[] symmetricDecrypt(byte[] encryptedData, byte[] key, byte[] nonce, byte[] aad) throws BaseCheckedException{
        try{
            SecretKeySpec secretKeySpec = new SecretKeySpec(key,0,key.length,symmetricAlgoName.split("/")[0]);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(symmetricGcmTagLength, nonce);
            Cipher symmetricDecryptCipher = Cipher.getInstance(symmetricAlgoName);
            symmetricDecryptCipher.init(Cipher.DECRYPT_MODE,secretKeySpec,gcmParameterSpec);
            symmetricDecryptCipher.updateAAD(aad);

            return symmetricDecryptCipher.doFinal(encryptedData);
        }
        catch (Exception e){
            throw ErrorCode.CRYPTO_DECRYPT_EXCEPTION.throwChecked(e);
        }
    }

    public byte[] decrypt(byte[] encryptedHybridData) throws BaseCheckedException{
        int keyHeaderLength = "VER_R2".length();

        int keyDemiliterIndex = CryptoUtil.getSplitterIndex(encryptedHybridData, 0, keySplitter);
        byte[] encryptedKey = Arrays.copyOfRange(encryptedHybridData,
                thumbprintPresent ? keyHeaderLength + (thumbprintLength/8) : keyHeaderLength,
                keyDemiliterIndex);
        byte[] nonce = Arrays.copyOfRange(encryptedHybridData,
                keyDemiliterIndex + keySplitter.length(),
                keyDemiliterIndex + keySplitter.length() + (nonceLength/8));
        byte[] aad = Arrays.copyOfRange(encryptedHybridData,
                keyDemiliterIndex + keySplitter.length(),
                keyDemiliterIndex + keySplitter.length() + (aadLength/8));
        byte[] encryptedData = Arrays.copyOfRange(encryptedHybridData,
                keyDemiliterIndex + keySplitter.length() + (aadLength/8),
                encryptedHybridData.length);

        byte[] decryptedSymmetricKey = aSymmetricDecrypt(encryptedKey);

        return symmetricDecrypt(encryptedData, decryptedSymmetricKey, nonce, aad);
    }

    public byte[] asymmetricEncrypt(byte[] data) throws BaseCheckedException{
        try{
            return encryptCipher.doFinal(data);
        } catch (Exception e){
            throw ErrorCode.CRYPTO_ENCRYPT_EXCEPTION.throwChecked(e);
        }
    }

    private byte[] symmetricEncrypt(byte[] input, SecretKey secretKey, byte[] nonce, byte[] aad) throws BaseCheckedException{
        try{
            Cipher cipher = Cipher.getInstance(symmetricAlgoName);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(symmetricGcmTagLength, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
            cipher.updateAAD(aad);
            return cipher.doFinal(input);
        } catch(Exception e) {
            throw ErrorCode.CRYPTO_ENCRYPT_EXCEPTION.throwChecked(e);
        }
    }


    public byte[] sign(byte[] data) throws BaseCheckedException{
        try{
            signer.update(data);
            return signer.sign();
        } catch (Exception e){
            throw ErrorCode.CRYPTO_SIGN_VERIFY_EXCEPTION.throwChecked(e);
        }
    }

    public BaseEventRequest encryptSign(String request) throws BaseCheckedException {
        BaseEventRequest res = new BaseEventRequest();
        SecretKey secretKey = symmetrickeyGen.generateKey();
        byte[] randomIV = generateBytes(nonceLength/8);
        byte[] randomAAD = generateBytes((aadLength-nonceLength)/8);
        byte[] randomThumbprint = generateBytes(thumbprintLength/8);

        res.setData(
            CryptoUtil.encodeToURLSafeBase64(
                combineByteArrays(
                    "VER_R2".getBytes(),
                    thumbprintPresent ? randomThumbprint : new byte[0],
                    asymmetricEncrypt(
                        secretKey.getEncoded()
                    ),
                    keySplitter.getBytes(),
                    randomIV,
                    randomAAD,
                    symmetricEncrypt(
                        request.getBytes(),
                        secretKey,
                        randomIV,
                        combineByteArrays(randomIV,randomAAD)
                    )
                )
            )
        );
        res.setSignature(
            CryptoUtil.encodeToURLSafeBase64(
                sign(
                    request.getBytes()
                )
            )
        );
        return res;
    }

    private byte[] generateBytes(int size){
        SecureRandom secureRandom = new SecureRandom();
        byte[] byteArr = new byte[size];
        secureRandom.nextBytes(byteArr);
        return byteArr;
    }

    private byte[] combineByteArrays(byte[]... arrays){
        int totalSize = 0;
        for(byte[] array: arrays){
            totalSize+=array.length;
        }
        byte[] output = new byte[totalSize];
        int carry = 0;
        for(byte[] array: arrays){
            System.arraycopy(array,0, output, carry, array.length);
            carry += array.length;
        }
        return output;
    }

}
