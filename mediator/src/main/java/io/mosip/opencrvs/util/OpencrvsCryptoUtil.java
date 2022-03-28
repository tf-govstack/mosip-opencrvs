package io.mosip.opencrvs.util;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
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
import java.io.FileReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;

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
            decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decryptCipher.init(Cipher.DECRYPT_MODE,mosipPrivateKey);
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

    public byte[] decrypt(byte[] encryptedData) throws BaseCheckedException{
        try{
            return decryptCipher.doFinal(encryptedData);
        }
        catch (Exception e){
            throw new BaseCheckedException(ErrorCode.CRYPTO_DECRYPT_EXCEPTION_CODE,ErrorCode.CRYPTO_DECRYPT_EXCEPTION_MESSAGE,e);
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
}
