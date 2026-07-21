package com.edu.domain.fee.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.common.exception.ApiException;
import com.edu.domain.fee.dto.request.DiscountPolicyRequest;
import com.edu.domain.fee.dto.response.DiscountPolicyResponse;
import com.edu.domain.fee.mapper.DiscountPolicyMapper;
import com.edu.domain.fee.service.DiscountPolicyService;
import com.edu.domain.fee.vo.DiscountPolicyVo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DiscountPolicyServiceImpl implements DiscountPolicyService {

    private static final BigDecimal MAX_RATE = BigDecimal.valueOf(100);

    private final DiscountPolicyMapper discountPolicyMapper;

    public DiscountPolicyServiceImpl(DiscountPolicyMapper discountPolicyMapper) {
        this.discountPolicyMapper = discountPolicyMapper;
    }

    @Override
    public PageResponse<DiscountPolicyResponse> getList(boolean activeOnly, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;

        long totalCount = discountPolicyMapper.countList(activeOnly);
        if (totalCount == 0) {
            return PageResponse.of(List.of(), safePage, safeSize, 0);
        }

        List<DiscountPolicyResponse> content = discountPolicyMapper.findList(activeOnly, safeSize, offset)
                .stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(content, safePage, safeSize, totalCount);
    }

    @Override
    public DiscountPolicyResponse get(Long policyId) {
        return toResponse(getVo(policyId));
    }

    @Override
    @Transactional
    public DiscountPolicyResponse create(DiscountPolicyRequest request) {
        validate(request);
        DiscountPolicyVo policy = toEntity(new DiscountPolicyVo(), request);
        discountPolicyMapper.insert(policy);
        return toResponse(getVo(policy.getPolicyId()));
    }

    @Override
    @Transactional
    public DiscountPolicyResponse update(Long policyId, DiscountPolicyRequest request) {
        DiscountPolicyVo policy = getVo(policyId);   // 없으면 404
        validate(request);
        toEntity(policy, request);
        discountPolicyMapper.update(policy);
        return toResponse(getVo(policyId));
    }

    /** PK 단건 조회 (없으면 404) - create/update 응답 재조회에도 재사용 */
    private DiscountPolicyVo getVo(Long policyId) {
        DiscountPolicyVo policy = discountPolicyMapper.findById(policyId);
        if (policy == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 할인정책입니다");
        }
        return policy;
    }

    /** 필드 단위 검증(@Valid)으로 못 잡는 필드 간 규칙 검증 */
    private void validate(DiscountPolicyRequest request) {
        if ("RATE".equals(request.getDiscountType()) && request.getDiscountValue().compareTo(MAX_RATE) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "정률 할인은 100%를 초과할 수 없습니다");
        }
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "적용 시작일은 종료일보다 늦을 수 없습니다");
        }
    }

    /** 요청 값을 엔티티에 반영 (create/update 공용) */
    private DiscountPolicyVo toEntity(DiscountPolicyVo policy, DiscountPolicyRequest r) {
        policy.setPolicyName(r.getPolicyName());
        policy.setDiscountType(r.getDiscountType());
        policy.setDiscountValue(r.getDiscountValue());
        policy.setConditionType(r.getConditionType());
        policy.setStartDate(r.getStartDate());
        policy.setEndDate(r.getEndDate());
        policy.setActiveYn("N".equalsIgnoreCase(r.getActiveYn()) ? "N" : "Y");
        policy.setDescription(r.getDescription());
        return policy;
    }

    private DiscountPolicyResponse toResponse(DiscountPolicyVo v) {
        return DiscountPolicyResponse.builder()
                .policyId(v.getPolicyId())
                .policyName(v.getPolicyName())
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .conditionType(v.getConditionType())
                .startDate(v.getStartDate())
                .endDate(v.getEndDate())
                .activeYn(v.getActiveYn())
                .description(v.getDescription())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
