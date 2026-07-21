package com.edu.domain.fee.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.common.exception.ApiException;
import com.edu.domain.fee.dto.request.DiscountPolicyRequest;
import com.edu.domain.fee.dto.response.DiscountPolicyResponse;
import com.edu.domain.fee.dto.response.DiscountPreviewResponse;
import com.edu.domain.fee.mapper.DiscountPolicyMapper;
import com.edu.domain.fee.service.DiscountPolicyService;
import com.edu.domain.fee.vo.DiscountPolicyVo;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DiscountPolicyServiceImpl implements DiscountPolicyService {

    private static final BigDecimal MAX_RATE = BigDecimal.valueOf(100);

    private final DiscountPolicyMapper discountPolicyMapper;
    private final UserMapper userMapper;

    public DiscountPolicyServiceImpl(DiscountPolicyMapper discountPolicyMapper, UserMapper userMapper) {
        this.discountPolicyMapper = discountPolicyMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResponse<DiscountPolicyResponse> getList(String activeYn, LocalDate targetDate, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;
        String normalizedActiveYn = normalizeActiveYn(activeYn);

        long totalCount = discountPolicyMapper.countList(normalizedActiveYn, targetDate);
        if (totalCount == 0) {
            return PageResponse.of(List.of(), safePage, safeSize, 0);
        }

        List<DiscountPolicyResponse> content = discountPolicyMapper
                .findList(normalizedActiveYn, targetDate, safeSize, offset)
                .stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.of(content, safePage, safeSize, totalCount);
    }

    /** 빈 문자열/소문자 등 잡값을 정리 - Y/N 이 아니면 필터 없음(null)으로 취급 */
    private String normalizeActiveYn(String activeYn) {
        if (activeYn == null || activeYn.isBlank()) {
            return null;
        }
        String upper = activeYn.trim().toUpperCase();
        return ("Y".equals(upper) || "N".equals(upper)) ? upper : null;
    }

    @Override
    public DiscountPolicyResponse get(Long discountPolicyId) {
        return toResponse(getVo(discountPolicyId));
    }

    @Override
    @Transactional
    public DiscountPolicyResponse create(DiscountPolicyRequest request, Authentication authentication) {
        validate(request);
        DiscountPolicyVo policy = toEntity(new DiscountPolicyVo(), request);
        policy.setCreatedBy(resolveCurrentUserId(authentication));
        discountPolicyMapper.insert(policy);
        return toResponse(getVo(policy.getDiscountPolicyId()));
    }

    @Override
    @Transactional
    public DiscountPolicyResponse update(Long discountPolicyId, DiscountPolicyRequest request) {
        DiscountPolicyVo policy = getVo(discountPolicyId);   // 없으면 404
        validate(request);
        toEntity(policy, request);
        discountPolicyMapper.update(policy);
        return toResponse(getVo(discountPolicyId));
    }

    @Override
    public DiscountPreviewResponse preview(Long discountPolicyId, long feeAmount) {
        DiscountPolicyVo policy = getVo(discountPolicyId);   // 없으면 404
        if (!policy.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "비활성화된 할인정책은 적용할 수 없습니다");
        }
        if (!policy.isWithinPeriod(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "할인정책 적용 기간이 아닙니다");
        }

        long discountAmount = Math.min(policy.calculateDiscountAmount(feeAmount), feeAmount);
        return DiscountPreviewResponse.builder()
                .discountPolicyId(discountPolicyId)
                .feeAmount(feeAmount)
                .discountAmount(discountAmount)
                .billableAmount(feeAmount - discountAmount)
                .build();
    }

    /** PK 단건 조회 (없으면 404) - create/update 응답 재조회에도 재사용 */
    private DiscountPolicyVo getVo(Long discountPolicyId) {
        DiscountPolicyVo policy = discountPolicyMapper.findById(discountPolicyId);
        if (policy == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 할인정책입니다");
        }
        return policy;
    }

    /** 로그인 ID(Authentication.getName())로 등록자 PK 조회 - NoticeServiceImpl과 동일 패턴 */
    private Long resolveCurrentUserId(Authentication authentication) {
        UserDto user = userMapper.selectByLoginId(authentication.getName());
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다");
        }
        return user.getUserId();
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
        policy.setStartDate(r.getStartDate());
        policy.setEndDate(r.getEndDate());
        policy.setActiveYn("N".equalsIgnoreCase(r.getActiveYn()) ? "N" : "Y");
        policy.setDescription(r.getDescription());
        return policy;
    }

    private DiscountPolicyResponse toResponse(DiscountPolicyVo v) {
        return DiscountPolicyResponse.builder()
                .discountPolicyId(v.getDiscountPolicyId())
                .policyName(v.getPolicyName())
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .startDate(v.getStartDate())
                .endDate(v.getEndDate())
                .activeYn(v.getActiveYn())
                .description(v.getDescription())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}