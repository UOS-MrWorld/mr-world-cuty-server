package com.mrworld.yaho.customer;

import com.mrworld.yaho.booking.BookingRepository;
import com.mrworld.yaho.common.dto.IdTotal;
import com.mrworld.yaho.common.dto.PageResponse;
import com.mrworld.yaho.common.enums.LoyaltyGrade;
import com.mrworld.yaho.common.exception.BadRequestException;
import com.mrworld.yaho.common.exception.NotFoundException;
import com.mrworld.yaho.customer.dto.CustomerDetailResponse;
import com.mrworld.yaho.customer.dto.CustomerListRequest;
import com.mrworld.yaho.customer.dto.CustomerSummaryResponse;
import com.mrworld.yaho.customer.dto.DiscountResponse;
import com.mrworld.yaho.customer.dto.LoyaltyPolicyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 직원용 고객 조회, 단골 할인 일괄 적용, 단골 등급 기준 조회
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {
    private static final Sort CUSTOMER_SORT = Sort.by("memberId");

    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    public LoyaltyPolicyResponse getLoyaltyPolicy() {
        return LoyaltyPolicyResponse.from(LoyaltyGrade.values());
    }

    // 고객 목록. 페이지의 고객들 완료 여행 횟수를 한 번의 쿼리로 함께 센다.
    public PageResponse<CustomerSummaryResponse> getCustomers(CustomerListRequest request) {
        String keyword = request.getKeyword() == null || request.getKeyword().isBlank()
                ? null
                : request.getKeyword().trim();
        Page<Customer> page = customerRepository.search(
                keyword, request.getLoyaltyGrade(), request.getDiscountApplied(), request.toPageable(CUSTOMER_SORT));

        List<Long> customerIds = page.getContent().stream().map(Customer::getMemberId).toList();
        Map<Long, Long> tourCounts = customerIds.isEmpty() ? Map.of()
                : IdTotal.toMap(bookingRepository.countCompletedToursByCustomerIds(customerIds));

        return PageResponse.of(page, customer -> CustomerSummaryResponse.from(
                customer, tourCounts.getOrDefault(customer.getMemberId(), 0L)));
    }

    public CustomerDetailResponse getCustomerDetail(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 고객입니다."));
        return CustomerDetailResponse.from(customer, bookingRepository.countCompletedToursByCustomerId(customerId));
    }

    // 해당 등급 고객 중 단골 할인이 아직 적용되지 않은 고객에게 할인을 일괄로 켠다. 이미 적용된 고객은 건너뛴다.
    // 할인율이 0인 등급(NORMAL)은 켜도 의미가 없으므로 적용 가능한 고객이 없는 것으로 본다.
    @Transactional
    public DiscountResponse activateDiscountByGrade(LoyaltyGrade loyaltyGrade) {
        int targetCount = (int) customerRepository.countByLoyaltyGrade(loyaltyGrade);
        List<Customer> eligible = loyaltyGrade.getDiscountRate() == 0
                ? List.of()
                : customerRepository.findAllByLoyaltyGradeAndLoyaltyDiscountAppliedFalse(loyaltyGrade);
        if (eligible.isEmpty()) {
            throw new BadRequestException("할인을 적용할 수 있는 고객이 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        eligible.forEach(customer -> customer.activateDiscount(now));

        return DiscountResponse.of(targetCount, eligible.size());
    }
}
