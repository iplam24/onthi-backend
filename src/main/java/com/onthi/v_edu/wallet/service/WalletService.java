package com.onthi.v_edu.wallet.service;

import com.onthi.v_edu.common.constant.TransactionStatus;
import com.onthi.v_edu.common.constant.TransactionType;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.wallet.entity.Transaction;
import com.onthi.v_edu.wallet.entity.Wallet;
import com.onthi.v_edu.wallet.repository.TransactionRepository;
import com.onthi.v_edu.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.webhooks.WebhookData;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final PayOSService payOSService;

    @Transactional
    public CreatePaymentLinkResponse initiateDeposit(User user, BigDecimal amount) throws Exception {
        // 1. Tạo Transaction mới ở trạng thái PENDING
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setDescription("Nạp tiền vào ví qua PayOS");
        transaction.setCreatedAt(LocalDateTime.now());
        
        logger.info("[PAYMENT] Khởi tạo yêu cầu nạp tiền cho user: {}, số tiền: {}", user.getUsername(), amount);
        
        // Tạo orderCode duy nhất
        transaction = transactionRepository.save(transaction);
        long orderCode = Long.parseLong(String.valueOf(System.currentTimeMillis()).substring(2) + transaction.getId());
        transaction.setOrderCode(orderCode);
        transaction = transactionRepository.save(transaction);

        // 2. Gọi PayOS để lấy link thanh toán và thông tin QR
        CreatePaymentLinkResponse paymentInfo = payOSService.createPaymentLink(transaction);
        
        logger.info("[PAYMENT] PayOS Response - Bin: {}, Acc: {}, Name: {}, Desc: {}", 
            paymentInfo.getBin(), paymentInfo.getAccountNumber(), paymentInfo.getAccountName(), paymentInfo.getDescription());

        // 3. Lưu paymentLinkId
        transaction.setPaymentLinkId(paymentInfo.getPaymentLinkId());
        transactionRepository.save(transaction);

        return paymentInfo;
    }

    @Transactional
    public void processPaymentWebhook(WebhookData webhookData) {
        long orderCode = webhookData.getOrderCode();
        logger.info("[PAYMENT WEBHOOK] Nhận dữ liệu phản hồi cho OrderCode: {}, Mã trạng thái: {}", orderCode, webhookData.getCode());
        
        Transaction transaction = transactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Transaction not found for orderCode: " + orderCode));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            logger.warn("[PAYMENT WEBHOOK] Giao dịch {} đã được xử lý trước đó (Status: {}), bỏ qua.", orderCode, transaction.getStatus());
            return; // Đã xử lý rồi
        }

        if ("00".equals(webhookData.getCode())) {
            // Thanh toán thành công
            transaction.setStatus(TransactionStatus.SUCCESS);
            
            // Cập nhật số dư ví
            Wallet wallet = walletRepository.findByUserId(transaction.getUser().getId())
                    .orElseGet(() -> {
                        Wallet newWallet = new Wallet();
                        newWallet.setUser(transaction.getUser());
                        newWallet.setBalance(BigDecimal.ZERO);
                        return newWallet;
                    });
            
            wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
            walletRepository.save(wallet);
            logger.info("[PAYMENT SUCCESS] Đã cộng {} vào ví của user: {}. Số dư mới: {}", 
                    transaction.getAmount(), transaction.getUser().getUsername(), wallet.getBalance());
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            logger.warn("[PAYMENT FAILED] Giao dịch {} thất bại hoặc bị hủy bởi người dùng.", orderCode);
        }
        
        transactionRepository.save(transaction);
    }

    @Transactional
    public void syncPaymentStatus(long orderCode, PaymentLink paymentLink) {
        logger.info("[PAYMENT SYNC] Đang đồng bộ trạng thái cho OrderCode: {} từ PayOS Information", orderCode);
        
        Transaction transaction = transactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Transaction not found for orderCode: " + orderCode));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            return; // Đã xử lý rồi
        }

        String status = paymentLink.getStatus() != null ? paymentLink.getStatus().toString().trim() : "";
        logger.info("[PAYMENT SYNC] Trạng thái sau khi toString và trim: '{}' (Độ dài: {})", status, status.length());

        if ("PAID".equalsIgnoreCase(status)) {
            logger.info("[PAYMENT SYNC] PHÁT HIỆN TRẠNG THÁI PAID! Bắt đầu quy trình cộng tiền...");
            transaction.setStatus(TransactionStatus.SUCCESS);
            
            Wallet wallet = walletRepository.findByUserId(transaction.getUser().getId())
                    .orElseGet(() -> {
                        logger.info("[PAYMENT SYNC] Người dùng {} chưa có ví, đang khởi tạo ví mới...", transaction.getUser().getUsername());
                        Wallet newWallet = new Wallet();
                        newWallet.setUser(transaction.getUser());
                        newWallet.setBalance(java.math.BigDecimal.ZERO);
                        return newWallet;
                    });
            
            wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
            walletRepository.save(wallet);
            transactionRepository.save(transaction);
            
            logger.info("[PAYMENT SYNC SUCCESS] Đã lưu Ví thành công. User: {}, Số dư: {}", 
                    transaction.getUser().getUsername(), wallet.getBalance());
        } else if ("CANCELLED".equals(paymentLink.getStatus())) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            logger.warn("[PAYMENT SYNC] Giao dịch {} đã bị hủy.", orderCode);
        } else {
            logger.info("[PAYMENT SYNC] Bỏ qua vì trạng thái '{}' không phải là PAID.", status);
        }
    }

    @Transactional
    public void deductBalance(User user, BigDecimal amount, String reason) {
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + user.getUsername()));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Số dư không đủ để thực hiện giao dịch.");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        // Tạo Transaction bản ghi cho việc trừ tiền
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(amount.negate()); // Số tiền âm
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setDescription(reason);
        transaction.setPaymentLinkId("INTERNAL_" + reason.toUpperCase().replace(" ", "_"));
        transactionRepository.save(transaction);

        logger.info("[WALLET DEDUCT] Đã trừ {} từ ví user: {}. Lý do: {}. Số dư mới: {}", 
                amount, user.getUsername(), reason, wallet.getBalance());
    }

    @Transactional
    public void transfer(User sender, User receiver, BigDecimal amount, String message) {
        if (sender.getId().equals(receiver.getId())) {
            throw new RuntimeException("Bạn không thể tặng quà cho chính mình!");
        }

        Wallet senderWallet = walletRepository.findByUserId(sender.getId())
                .orElseThrow(() -> new RuntimeException("Ví của người gửi không tồn tại"));

        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Số dư ví không đủ để tặng quà!");
        }

        Wallet receiverWallet = walletRepository.findByUserId(receiver.getId())
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUser(receiver);
                    w.setBalance(BigDecimal.ZERO);
                    return walletRepository.save(w);
                });

        // Trừ tiền người gửi
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        walletRepository.save(senderWallet);

        // Cộng tiền người nhận
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
        walletRepository.save(receiverWallet);

        // Lưu transaction cho người gửi (tiền ra)
        Transaction senderTx = new Transaction();
        senderTx.setUser(sender);
        senderTx.setAmount(amount.negate());
        senderTx.setType(TransactionType.GIFT);
        senderTx.setStatus(TransactionStatus.SUCCESS);
        senderTx.setCreatedAt(LocalDateTime.now());
        senderTx.setDescription("Tặng quà cho " + (receiver.getFullName() != null ? receiver.getFullName() : receiver.getUsername()) + ": " + message);
        senderTx.setPaymentLinkId("GIFT_TO_" + receiver.getId());
        transactionRepository.save(senderTx);

        // Lưu transaction cho người nhận (tiền vào)
        Transaction receiverTx = new Transaction();
        receiverTx.setUser(receiver);
        receiverTx.setAmount(amount);
        receiverTx.setType(TransactionType.GIFT);
        receiverTx.setStatus(TransactionStatus.SUCCESS);
        receiverTx.setCreatedAt(LocalDateTime.now());
        receiverTx.setDescription("Nhận quà từ " + (sender.getFullName() != null ? sender.getFullName() : sender.getUsername()) + ": " + message);
        receiverTx.setPaymentLinkId("GIFT_FROM_" + sender.getId());
        transactionRepository.save(receiverTx);
        
        logger.info("[GIFT] User {} tặng {} cho user {}. Message: {}", 
                sender.getUsername(), amount, receiver.getUsername(), message);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByUser(User user, Pageable pageable) {
        logger.info("[DB] Đang tìm giao dịch cho user ID: {}, page: {}", user.getId(), pageable.getPageNumber());
        Page<Transaction> txs = transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        logger.info("[DB] Tìm thấy {} giao dịch trang {}.", txs.getNumberOfElements(), txs.getNumber());
        return txs;
    }
}
