
package ch.protonmail.android.utils.crypto;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;


public class TextVerificationResult extends AbstractDecryptionResult {

    private final String data;
    private final Long verifiedSignatureTimestamp;

    public TextVerificationResult(@NonNull String data, boolean signatureIsValid, @Nullable Long verifiedSignatureTimestamp) {
        super(true, signatureIsValid);
        this.data = data;

        if (signatureIsValid) {
            this.verifiedSignatureTimestamp = Objects.requireNonNull(verifiedSignatureTimestamp, "verifiedSignatureTimestamp must not be null for valid signatures");
        } else if (verifiedSignatureTimestamp != null) {
            throw new IllegalArgumentException("verifiedSignatureTimestamp must be null for invalid signatures");
        } else {
            this.verifiedSignatureTimestamp = null;
        }
    }

    /**
     * @return creation time of the verified signature, or null for invalid signatures
     */
    @Nullable
    public Long getVerifiedSignatureTimestamp() {
        return verifiedSignatureTimestamp;
    }

    /**
     * @return signed data. Call `isVerified` to confirm signature validity.
     */
    public String getData() {
        return data;
    }
}
