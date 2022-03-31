package io.mosip.opencrvs.util;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.FileUtils;
import io.mosip.opencrvs.error.ErrorCode;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.io.pem.PemReader;
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
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

@Component
public class OpencrvsCryptoUtil {

    private static final Logger LOGGER = LogUtil.getLogger(OpencrvsCryptoUtil.class);

    @Value("${mosip.opencrvs.decrypt.privkey.path}")
    private String mosipPrivKeyPath;
    PrivateKey mosipPrivateKey;
    Cipher decryptCipher;

    @Value("${mosip.opencrvs.signverify.pubkey.path}")
    private String opencrvsPublicKeyPath;
    PublicKey opencrvsPublicKey;
    Signature signer;

    @PostConstruct
    public void init(){
        try{
            PEMParser parser = new PEMParser(new FileReader(FileUtils.getFile(mosipPrivKeyPath)));
            mosipPrivateKey = new JcaPEMKeyConverter().getPrivateKey(((PEMKeyPair)parser.readObject()).getPrivateKeyInfo());
        } catch(Exception e) {
            throw new BaseUncheckedException(ErrorCode.CRYPTO_READ_PRIVATE_KEY_EXCEPTION_CODE,ErrorCode.CRYPTO_READ_PRIVATE_KEY_EXCEPTION_MESSAGE,e);
        }

        try {
            decryptCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            decryptCipher.init(Cipher.DECRYPT_MODE,mosipPrivateKey,oaepParams);
        } catch (Exception e){
            throw new BaseUncheckedException(ErrorCode.CRYPTO_INIT_PRIVATE_KEY_EXCEPTION_CODE,ErrorCode.CRYPTO_INIT_PRIVATE_KEY_EXCEPTION_MESSAGE,e);
        }

        try{
            PEMParser parser = new PEMParser(new FileReader(FileUtils.getFile(opencrvsPublicKeyPath)));
            opencrvsPublicKey = new JcaPEMKeyConverter().getPublicKey(((X509CertificateHolder)parser.readObject()).getSubjectPublicKeyInfo());
        } catch(Exception e) {
            throw new BaseUncheckedException(ErrorCode.CRYPTO_READ_PUBLIC_KEY_EXCEPTION_CODE,ErrorCode.CRYPTO_READ_PUBLIC_KEY_EXCEPTION_MESSAGE,e);
        }

        try {
            signer = Signature.getInstance("SHA256withRSA");
            signer.initVerify(opencrvsPublicKey);
        } catch (Exception e){
            throw new BaseUncheckedException(ErrorCode.CRYPTO_INIT_PUBLIC_KEY_EXCEPTION_CODE,ErrorCode.CRYPTO_INIT_PUBLIC_KEY_EXCEPTION_MESSAGE,e);
        }
    }

    public boolean verify(byte[] data, byte[] signature) throws BaseCheckedException{
        try{
            signer.update(data);
            return signer.verify(signature);
        }
        catch (Exception e){
            throw new BaseCheckedException(ErrorCode.CRYPTO_SIGN_VERIFY_EXCEPTION_CODE,ErrorCode.CRYPTO_SIGN_VERIFY_EXCEPTION_MESSAGE,e);
        }
    }

    public byte[] aSymmetricDecrypt(byte[] encryptedData) throws BaseCheckedException{
        try{
            return decryptCipher.doFinal(encryptedData);
        }
        catch (Exception e){
            throw new BaseCheckedException(ErrorCode.CRYPTO_DECRYPT_EXCEPTION_CODE,ErrorCode.CRYPTO_DECRYPT_EXCEPTION_MESSAGE,e);
        }
    }

    public byte[] symmetricDecrypt(byte[] encryptedData, byte[] key, byte[] nonce, byte[] aad) throws BaseCheckedException{
        String symmetricAlgorithmName = "AES/GCM/PKCS5Padding";
        int gcmTagLength = 16 * Byte.SIZE;
        try{
            SecretKeySpec secretKeySpec = new SecretKeySpec(key,0,key.length,symmetricAlgorithmName.split("/")[0]);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(gcmTagLength, nonce);
            Cipher symmetricDecryptCipher = Cipher.getInstance(symmetricAlgorithmName);
            symmetricDecryptCipher.init(Cipher.DECRYPT_MODE,secretKeySpec,gcmParameterSpec);
            symmetricDecryptCipher.updateAAD(aad);

            return symmetricDecryptCipher.doFinal(encryptedData);
        }
        catch (Exception e){
            throw new BaseCheckedException(ErrorCode.CRYPTO_DECRYPT_EXCEPTION_CODE,ErrorCode.CRYPTO_DECRYPT_EXCEPTION_MESSAGE,e);
        }
    }

    public byte[] decrypt(byte[] encryptedHybridData) throws BaseCheckedException{
        String keySplitter = "#KEY_SPLITTER#";
        int keyHeaderLength = "VER_R2".length();
        int aadLength = 32;
        int nonceLength = 12;

        int keyDemiliterIndex = CryptoUtil.getSplitterIndex(encryptedHybridData, 0, keySplitter);
        byte[] encryptedKey = Arrays.copyOfRange(encryptedHybridData,
                keyHeaderLength,
                keyDemiliterIndex);
        byte[] nonce = Arrays.copyOfRange(encryptedHybridData,
                keyDemiliterIndex + keySplitter.length(),
                keyDemiliterIndex + keySplitter.length() + nonceLength);
        byte[] aad = Arrays.copyOfRange(encryptedHybridData,
                keyDemiliterIndex + keySplitter.length(),
                keyDemiliterIndex + keySplitter.length() + aadLength);
        byte[] encryptedData = Arrays.copyOfRange(encryptedHybridData,
                keyDemiliterIndex + keySplitter.length() + aadLength,
                encryptedHybridData.length);

        byte[] decryptedSymmetricKey = aSymmetricDecrypt(encryptedKey);

        return symmetricDecrypt(encryptedData, decryptedSymmetricKey, nonce, aad);
    }

}
