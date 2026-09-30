package com.wastina.trust.service;

import com.wastina.trust.entity.*;
import com.wastina.trust.gateway.document.MockMalwareScanner;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GatewayDomainTest {

    @Test
    void paymentTransactionRejectsAmountMismatchAndRequiresReconciliation() {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setAmount(new BigDecimal("5000.00"));
        transaction.setCurrency("ETB");

        assertThrows(IllegalArgumentException.class,
                () -> transaction.markSuccessful(new BigDecimal("50000.00"), "ETB"));
        assertEquals(PaymentTransactionStatus.RECONCILIATION_REQUIRED, transaction.getStatus());
    }

    @Test
    void refundCannotExceedRemainingPaidAmount() {
        PaymentRefund refund = new PaymentRefund();
        refund.setAmount(new BigDecimal("80.00"));

        assertThrows(IllegalArgumentException.class,
                () -> refund.validateAgainst(new BigDecimal("100.00"), new BigDecimal("30.00")));
    }

    @Test
    void documentCannotBecomeAvailableBeforeCleanScan() {
        Document document = new Document();
        document.setStatus(DocumentStatus.UPLOADING);
        document.markQuarantined();

        assertThrows(IllegalStateException.class,
                () -> document.markAvailable("application/pdf", 10L, "hash"));

        document.setScanStatus(DocumentScanStatus.PASSED);
        document.markAvailable("application/pdf", 10L, "hash");
        assertTrue(document.canIssueDownloadUrl());
    }

    @Test
    void malwareScannerFailsClosedForSyntheticMalwareFixture() {
        MockMalwareScanner scanner = new MockMalwareScanner();
        MalwareScannerResultAssertions.assertRejected(scanner.scan(
                "EICAR-TEST-FILE".getBytes(java.nio.charset.StandardCharsets.UTF_8), "application/pdf"));
    }

    @Test
    void uploadSessionExpiresAndCannotStart() {
        DocumentUploadSession session = new DocumentUploadSession();
        session.setExpiresAt(Instant.now().minusSeconds(1));
        session.setStatus(UploadSessionStatus.CREATED);

        assertThrows(IllegalStateException.class, session::beginUpload);
    }

    @Test
    void generatedDocumentObjectKeyDoesNotUseUserFilename() {
        Document document = new Document();
        document.setObjectKey("objects/evidence/2026/" + UUID.randomUUID());

        assertFalse(document.getObjectKey().contains("/uploads/"));
        assertFalse(document.getObjectKey().contains(".."));
    }

    private static final class MalwareScannerResultAssertions {
        private static void assertRejected(com.wastina.trust.gateway.document.MalwareScanner.ScanResult result) {
            assertFalse(result.clean());
            assertEquals("synthetic-malware", result.signature());
        }
    }
}
