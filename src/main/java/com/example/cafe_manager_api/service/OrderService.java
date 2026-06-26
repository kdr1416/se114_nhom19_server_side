package com.example.cafe_manager_api.service;

import com.example.cafe_manager_api.dto.*;
import com.example.cafe_manager_api.entity.*;
import com.example.cafe_manager_api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    private OrderResponse mapToOrderResponse(OrderEntity order) {
        String createdByFullName = null;
        if (order.getCreatedByUserId() != null) {
            createdByFullName = userRepository.findById(order.getCreatedByUserId())
                    .map(UserEntity::getFullName)
                    .orElse(null);
        }
        return new OrderResponse(
                order.getOrderId(),
                order.getTableId(),
                order.getOrderCode(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getNote(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getCreatedByUserId(),
                order.getCreatedShiftId(),
                createdByFullName
        );
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItemEntity item) {
        return new OrderItemResponse(
                item.getOrderItemId(),
                item.getOrderId(),
                item.getProductId(),
                item.getProductNameSnapshot(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal(),
                item.getNote()
        );
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(String status) {
        List<OrderEntity> orders;
        if (status != null && !status.trim().isEmpty()) {
            orders = orderRepository.findByStatus(status.trim().toUpperCase());
        } else {
            orders = orderRepository.findAll();
        }
        return orders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Integer orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(orderId);
        
        OrderResponse orderResponse = mapToOrderResponse(order);
        List<OrderItemResponse> itemResponses = items.stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());

        return new OrderDetailResponse(orderResponse, itemResponses);
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request, String username) {
        TableEntity table = tableRepository.findById(request.getTableId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bàn với ID: " + request.getTableId()));

        // Check if there is already an active order for this table
        List<OrderEntity> pendingOrders = orderRepository.findByTableIdAndStatus(table.getTableId(), "PENDING");
        List<OrderEntity> confirmedOrders = orderRepository.findByTableIdAndStatus(table.getTableId(), "CONFIRMED");
        if (!pendingOrders.isEmpty() || !confirmedOrders.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bàn đã có đơn hàng chưa thanh toán.");
        }

        // Set table status to OCCUPIED
        table.setStatus("OCCUPIED");
        tableRepository.save(table);

        // Retrieve creator user details
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại."));

        // Create OrderEntity
        OrderEntity order = new OrderEntity();
        order.setTableId(table.getTableId());
        order.setOrderCode("ORD-" + System.currentTimeMillis());
        order.setStatus("PENDING");
        order.setTotalAmount(0.0);
        order.setNote(request.getNote());
        order.setCreatedAt(System.currentTimeMillis());
        order.setCreatedByUserId(user.getUserId());
        List<ShiftEntity> activeShifts = shiftRepository.filterShifts(null, "IN_PROGRESS");
        Integer activeShiftId = activeShifts.isEmpty() ? null : activeShifts.get(0).getShiftId();
        order.setCreatedShiftId(activeShiftId);

        OrderEntity savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    @Transactional
    public OrderDetailResponse addItem(Integer orderId, OrderItemRequest request) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (!"PENDING".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể thêm món vào đơn hàng có trạng thái: " + order.getStatus());
        }

        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy món với ID: " + request.getProductId()));

        if (Boolean.FALSE.equals(product.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Món này đã ngừng bán.");
        }

        List<OrderItemEntity> existingItems = orderItemRepository.findByOrderId(orderId);
        OrderItemEntity targetItem = null;
        for (OrderItemEntity item : existingItems) {
            if (item.getProductId().equals(product.getProductId())) {
                targetItem = item;
                break;
            }
        }

        if (targetItem != null) {
            // Update quantity of existing item
            targetItem.setQuantity(targetItem.getQuantity() + request.getQuantity());
            targetItem.setSubtotal(targetItem.getQuantity() * targetItem.getUnitPrice());
            if (request.getNote() != null && !request.getNote().trim().isEmpty()) {
                targetItem.setNote(request.getNote());
            }
            orderItemRepository.save(targetItem);
        } else {
            // Create new order item
            OrderItemEntity newItem = new OrderItemEntity();
            newItem.setOrderId(orderId);
            newItem.setProductId(product.getProductId());
            newItem.setProductNameSnapshot(product.getProductName());
            newItem.setQuantity(request.getQuantity());
            newItem.setUnitPrice(product.getPrice());
            newItem.setSubtotal(request.getQuantity() * product.getPrice());
            newItem.setNote(request.getNote());
            orderItemRepository.save(newItem);
        }

        // Recalculate total amount
        recalculateAndSaveOrderTotal(order);

        return getOrderDetail(orderId);
    }

    @Transactional
    public OrderDetailResponse updateItem(Integer orderId, Integer itemId, Integer quantity) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (!"PENDING".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể cập nhật số lượng món của đơn hàng có trạng thái: " + order.getStatus());
        }

        OrderItemEntity item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy dòng món với ID: " + itemId));

        if (!item.getOrderId().equals(orderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dòng món không thuộc đơn hàng này.");
        }

        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng món phải lớn hơn 0.");
        }

        item.setQuantity(quantity);
        item.setSubtotal(quantity * item.getUnitPrice());
        orderItemRepository.save(item);

        // Recalculate total amount
        recalculateAndSaveOrderTotal(order);

        return getOrderDetail(orderId);
    }

    @Transactional
    public OrderDetailResponse removeItem(Integer orderId, Integer itemId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (!"PENDING".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa món khỏi đơn hàng có trạng thái: " + order.getStatus());
        }

        OrderItemEntity item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy dòng món với ID: " + itemId));

        if (!item.getOrderId().equals(orderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dòng món không thuộc đơn hàng này.");
        }

        orderItemRepository.delete(item);

        // Recalculate total amount
        recalculateAndSaveOrderTotal(order);

        return getOrderDetail(orderId);
    }

    @Transactional
    public OrderResponse confirmOrder(Integer orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (!"PENDING".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng phải ở trạng thái PENDING mới có thể xác nhận.");
        }

        order.setStatus("CONFIRMED");
        OrderEntity savedOrder = orderRepository.save(order);
        return mapToOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Integer orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        if ("PAID".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hủy đơn hàng đã thanh toán.");
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng đã được hủy trước đó.");
        }

        order.setStatus("CANCELLED");
        OrderEntity savedOrder = orderRepository.save(order);

        // Release Table
        TableEntity table = tableRepository.findById(order.getTableId()).orElse(null);
        if (table != null) {
            table.setStatus("AVAILABLE");
            tableRepository.save(table);
        }

        return mapToOrderResponse(savedOrder);
    }

    private void recalculateAndSaveOrderTotal(OrderEntity order) {
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getOrderId());
        double total = items.stream().mapToDouble(OrderItemEntity::getSubtotal).sum();
        order.setTotalAmount(total);
        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getPaidOrdersHistory(Long fromMs, Long toMs) {
        List<OrderEntity> orders = orderRepository.findByStatusAndPaidAtBetween("PAID", fromMs, toMs);
        if (orders.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Integer> orderIds = orders.stream().map(OrderEntity::getOrderId).collect(Collectors.toList());
        List<OrderItemEntity> allItems = orderItemRepository.findByOrderIdIn(orderIds);
        java.util.Map<Integer, List<OrderItemEntity>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));

        java.util.Set<Integer> userIds = orders.stream()
                .map(OrderEntity::getCreatedByUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        java.util.Map<Integer, String> userFullNameMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            List<UserEntity> users = userRepository.findAllByUserIdIn(new java.util.ArrayList<>(userIds));
            for (UserEntity u : users) {
                userFullNameMap.put(u.getUserId(), u.getFullName());
            }
        }

        return orders.stream().map(order -> {
            String createdByFullName = null;
            if (order.getCreatedByUserId() != null) {
                createdByFullName = userFullNameMap.get(order.getCreatedByUserId());
            }

            OrderResponse orderResponse = new OrderResponse(
                    order.getOrderId(),
                    order.getTableId(),
                    order.getOrderCode(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    order.getNote(),
                    order.getCreatedAt(),
                    order.getPaidAt(),
                    order.getCreatedByUserId(),
                    order.getCreatedShiftId(),
                    createdByFullName
            );

            List<OrderItemEntity> items = itemsByOrderId.getOrDefault(order.getOrderId(), java.util.Collections.emptyList());
            List<OrderItemResponse> itemResponses = items.stream()
                    .map(this::mapToOrderItemResponse)
                    .collect(Collectors.toList());

            return new OrderDetailResponse(orderResponse, itemResponses);
        }).collect(Collectors.toList());
    }
}
