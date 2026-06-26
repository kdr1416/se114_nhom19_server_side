package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.PaymentRequest;
import com.example.cafe_manager_api.dto.PaymentResponse;
import com.example.cafe_manager_api.entity.OrderEntity;
import com.example.cafe_manager_api.entity.OrderItemEntity;
import com.example.cafe_manager_api.entity.PaymentEntity;
import com.example.cafe_manager_api.entity.PromotionEntity;
import com.example.cafe_manager_api.entity.TableEntity;
import com.example.cafe_manager_api.entity.UserEntity;
import com.example.cafe_manager_api.entity.ShiftEntity;
import com.example.cafe_manager_api.repository.OrderRepository;
import com.example.cafe_manager_api.repository.OrderItemRepository;
import com.example.cafe_manager_api.repository.PaymentRepository;
import com.example.cafe_manager_api.repository.PromotionRepository;
import com.example.cafe_manager_api.repository.TableRepository;
import com.example.cafe_manager_api.repository.UserRepository;
import com.example.cafe_manager_api.repository.ShiftRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, String username) {
        // 1. Verify order exists and is CONFIRMED
        OrderEntity order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + request.getOrderId()));

        if (!"CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Chỉ đơn hàng ở trạng thái CONFIRMED mới có thể thanh toán. Trạng thái hiện tại: " + order.getStatus());
        }

        // 2. Calculate subtotal from OrderItems
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getOrderId());
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng chưa có món ăn nào.");
        }
        double subtotal = items.stream().mapToDouble(OrderItemEntity::getSubtotal).sum();

        // 3. Apply promotion if provided (validate code, check validity dates, calculate discount)
        double discountAmount = 0.0;
        if (request.getDiscountAmount() != null) {
            discountAmount = request.getDiscountAmount();
        } else if (request.getPromotionCode() != null && !request.getPromotionCode().trim().isEmpty()) {
            String code = request.getPromotionCode().trim();
            PromotionEntity promotion = promotionRepository.findByCode(code)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không hợp lệ."));

            if (Boolean.FALSE.equals(promotion.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá này đã ngừng hoạt động.");
            }

            long now = System.currentTimeMillis();
            if (promotion.getExpiresAt() != null && promotion.getExpiresAt() > 0 && promotion.getExpiresAt() < now) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá đã hết hạn.");
            }

            // FUTURE: bulk discount, product-specific, loyalty points
            if ("PERCENTAGE".equalsIgnoreCase(promotion.getType())) {
                discountAmount = subtotal * (promotion.getValue() / 100.0);
            } else {
                discountAmount = promotion.getValue();
            }

            if (discountAmount > subtotal) {
                discountAmount = subtotal;
            }
        }

        double totalAmount = subtotal - discountAmount;

        // Check if amountReceived is sufficient
        if (request.getAmountReceived() < totalAmount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    String.format("Số tiền khách đưa không đủ. Cần trả: %,.0f, Nhận: %,.0f", totalAmount, request.getAmountReceived()));
        }

        double change = request.getAmountReceived() - totalAmount;

        // Find cashier user
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng thanh toán không tồn tại."));

        // 4. Insert PaymentEntity
        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(order.getOrderId());
        payment.setPaymentMethod(request.getPaymentMethod().trim().toUpperCase());
        payment.setSubtotal(subtotal);
        payment.setDiscountAmount(discountAmount);
        payment.setFinalAmount(totalAmount);
        payment.setPaidAt(System.currentTimeMillis());
        payment.setStatus("PAID");
        payment.setCashierUserId(user.getUserId());
        List<ShiftEntity> activeShifts = shiftRepository.filterShifts(null, "IN_PROGRESS");
        Integer activeShiftId = activeShifts.isEmpty() ? null : activeShifts.get(0).getShiftId();
        payment.setPaidShiftId(activeShiftId);

        PaymentEntity savedPayment = paymentRepository.save(payment);

        // 5. Update OrderEntity status -> PAID, total_amount -> totalAmount
        order.setStatus("PAID");
        order.setPaidAt(System.currentTimeMillis());
        order.setTotalAmount(totalAmount);
        orderRepository.save(order);

        // 6. Update TableEntity status -> AVAILABLE
        TableEntity table = tableRepository.findById(order.getTableId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bàn của đơn hàng với ID: " + order.getTableId()));
        table.setStatus("AVAILABLE");
        tableRepository.save(table);

        return new PaymentResponse(
                savedPayment.getPaymentId(),
                savedPayment.getSubtotal(),
                savedPayment.getDiscountAmount(),
                savedPayment.getFinalAmount(),
                change,
                user.getUserId(),
                user.getFullName(),
                savedPayment.getPaymentMethod(),
                savedPayment.getPaidAt()
        );
    }

    @Transactional(readOnly = true)
    public PaymentEntity getPaymentByOrderId(Integer orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hóa đơn thanh toán cho đơn hàng ID: " + orderId));
    }

    @Transactional(readOnly = true)
    public Double getRevenueInRange(Long start, Long end) {
        Double revenue = paymentRepository.getRevenueInRange(start, end);
        return revenue != null ? revenue : 0.0;
    }

    @Transactional(readOnly = true)
    public Long countPaymentsInRange(Long start, Long end) {
        Long count = paymentRepository.countPaymentsInRange(start, end);
        return count != null ? count : 0L;
    }
}
