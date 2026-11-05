package com.mrworld.yaho.customer;

import com.mrworld.yaho.common.dto.PageResponse;
import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.common.exception.BadRequestException;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.customer.dto.CustomerListRequest;
import com.mrworld.yaho.customer.dto.CustomerSummaryResponse;
import com.mrworld.yaho.customer.dto.LoyaltyGradeBatchUpdateRequest;
import com.mrworld.yaho.customer.dto.LoyaltyGradeBatchUpdateResponse;
import com.mrworld.yaho.customer.dto.LoyaltyPolicyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 직원용 고객 조회, 단골 등급 일괄 변경, 단골 등급 기준 조회
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {
    private static final Sort CUSTOMER_SORT = Sort.by("memberId");

    private final CustomerRepository customerRepository;
    public LoyaltyPolicyResponse getLoyaltyPolicy() {
        return LoyaltyPolicyResponse.from(LoyaltyGrade.values());
    }

    // 고객 목록에는 등급 변경에 필요한 최소 정보만 반환한다.
    public PageResponse<CustomerSummaryResponse> getCustomers(CustomerListRequest request) {
        String keyword = request.getKeyword() == null || request.getKeyword().isBlank()
                ? null
                : request.getKeyword().trim();
        Page<Customer> page = customerRepository.search(
                keyword, request.getLoyaltyGrade(), request.toPageable(CUSTOMER_SORT));
        return PageResponse.of(page, CustomerSummaryResponse::from);
    }

    @Transactional
    public LoyaltyGradeBatchUpdateResponse updateLoyaltyGrades(LoyaltyGradeBatchUpdateRequest request) {
        List<LoyaltyGradeBatchUpdateRequest.Change> changes = request.getChanges();
        HashSet<Long> uniqueIds = new HashSet<>();
        if (changes.stream().anyMatch(change -> !uniqueIds.add(change.getCustomerId()))) {
            throw new BadRequestException("같은 고객의 등급을 두 번 변경할 수 없습니다.");
        }
        Map<Long, Customer> customers = customerRepository.findAllById(uniqueIds).stream()
                .collect(Collectors.toMap(Customer::getMemberId, Function.identity()));
        if (customers.size() != uniqueIds.size()) {
            throw new NotFoundException("존재하지 않는 고객이 포함되어 있습니다.");
        }
        List<CustomerSummaryResponse> updated = changes.stream().map(change -> {
            Customer customer = customers.get(change.getCustomerId());
            customer.updateLoyaltyGrade(change.getLoyaltyGrade());
            return CustomerSummaryResponse.from(customer);
        }).toList();
        return LoyaltyGradeBatchUpdateResponse.of(updated);
    }
}
