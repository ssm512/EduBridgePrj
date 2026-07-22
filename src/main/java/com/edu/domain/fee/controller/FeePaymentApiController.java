package com.edu.domain.fee.controller;

import com.edu.domain.fee.dto.response.FeePaymentResponse;
import com.edu.domain.fee.dto.response.PaymentReceiptResponse;
import com.edu.domain.fee.service.FeeService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * 납부 이력 REST API
 * 리소스가 fee 가 아니라 fee-payment 이므로 base path 를 분리 (API 명세서 FEE-05 경로 기준)
 */
@RestController
@RequestMapping("/api/fee-payments")
public class FeePaymentApiController {

    private final FeeService feeService;
    private final ITemplateEngine templateEngine;

    public FeePaymentApiController(FeeService feeService, ITemplateEngine templateEngine) {
        this.feeService = feeService;
        this.templateEngine = templateEngine;
    }

    /**
     * PUT /api/fee-payments/{paymentId}/cancel - 납부 취소 (FEE-05)
     * 이력 삭제가 아닌 cancel_yn = 'Y' 처리 후 회비 상태 재계산
     * 결정사항(2026-07-21): 명세의 cancelReason 파라미터는 의도적으로 미구현.
     * DB에 저장할 컬럼도 없고, MVP 스코프에서 취소 사유 기록은 불필요하다고 판단해 뺌.
     * (감사 추적이 필요해지면 fee_discounts 처럼 별도 이력 테이블/컬럼 추가로 재검토)
     * FEE-20: 취소와 함께 연결된 영수증도 서비스 내부에서 자동으로 CANCELLED 처리된다 (고정 문구 사유,
     * 2026-07-22 결정 - 위 cancelReason 미구현 결정을 뒤집지 않기 위해 파라미터는 그대로 받지 않는다).
     */
    @PutMapping("/{paymentId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public FeePaymentResponse cancelPayment(@PathVariable Long paymentId) {
        return feeService.cancelPayment(paymentId);
    }

    /**
     * GET /api/fee-payments/{paymentId}/receipt - 영수증 상세 조회 (RCT-01)
     * 명세대로 ADMIN/PARENT + STUDENT 만 허용한다 (본인/본인 자녀 영수증만).
     * (2026-07-22 결정: TEACHER 를 잠시 허용했다가 같은 날 재논의 후 철회 - TEACHER 용 화면 자체가 없어서
     * 굳이 API 접근을 열어둘 이유가 없다고 판단. TEACHER 는 SecurityConfig 단계에서 403 으로 차단된다)
     */
    @GetMapping("/{paymentId}/receipt")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT')")
    public PaymentReceiptResponse getReceipt(@PathVariable Long paymentId,
                                              JwtAuthenticationToken authentication) {
        Long parentUserId = null;
        Long studentUserId = null;
        if (!isAdmin(authentication)) {
            if (isStudent(authentication)) {
                studentUserId = currentUserId(authentication);
            } else {
                parentUserId = currentUserId(authentication);
            }
        }
        return feeService.getReceipt(paymentId, parentUserId, studentUserId);
    }

    /**
     * GET /api/fee-payments/{paymentId}/receipt/pdf - 영수증 PDF 출력 (RCT-02)
     * fee/receiptPrint 템플릿을 렌더링한 HTML을 openhtmltopdf로 PDF 변환한다.
     * 브라우저가 PDF를 인라인으로 열면 뷰어 자체의 인쇄 버튼으로 "인쇄" 요구사항(FEE-11/20)도 함께 충족되므로
     * 별도의 인쇄 전용 화면을 따로 만들지 않는다. 접근 제어는 getReceipt()와 완전히 동일.
     */
    @GetMapping("/{paymentId}/receipt/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT')")
    public ResponseEntity<byte[]> getReceiptPdf(@PathVariable Long paymentId,
                                                 JwtAuthenticationToken authentication) {
        PaymentReceiptResponse receipt = getReceipt(paymentId, authentication);

        Context context = new Context();
        context.setVariable("receipt", receipt);
        String html = templateEngine.process("fee/receiptPrint", context);

        byte[] pdfBytes = renderPdf(html);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(receipt.getReceiptNo() + ".pdf")
                .build());
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    /** PDF에 임베드할 한글 폰트 이름 - receiptPrint.html 의 font-family 와 반드시 같은 문자열을 써야 한다 */
    private static final String RECEIPT_FONT_FAMILY = "NotoSansKR";

    /**
     * 위 폰트의 TTF 파일 classpath 경로. src/main/resources/fonts/ 밑에 실제 파일을 넣어야 동작한다.
     * 파일명이 NotoSansKR-Regular.ttf 가 아니라 NotoSansKR.ttf 인 이유(2026-07-22):
     * 원래 경로에 Google Fonts zip 압축을 통째로 풀어서 생긴 "NotoSansKR-Regular.ttf" 라는 이름의 디렉터리가
     * 이미 들어가 있었고(zip 안의 static/NotoSansKR-Regular.ttf 가 진짜 폰트 파일), 그 폴더를 지울 수 없는
     * 환경이라 같은 이름을 재사용하지 못하고 새 파일명으로 옮겨 등록함.
     */
    private static final String RECEIPT_FONT_RESOURCE_PATH = "/fonts/NotoSansKR.ttf";

    /**
     * HTML 문자열 -> PDF 바이트 변환. openhtmltopdf 는 엄격한 XML 파서라 템플릿이 well-formed XHTML 이어야 한다.
     * PDFBox 가 기본으로 쓰는 PDF 내장(Base-14) 폰트는 한글 글리프가 전혀 없어 CSS font-family 를 뭘로 지정해도
     * 한글은 깨진 문자(#, tofu box)로 나온다. 그래서 실제 한글 글리프가 있는 TTF 를 useFont() 로 직접 등록해야 한다
     * (시스템에 설치된 폰트를 이름으로 자동 인식하지 않음 - Windows 의 "맑은 고딕" 같은 이름을 CSS 에 적어도 무시된다).
     */
    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFont(() -> {
                InputStream fontStream = getClass().getResourceAsStream(RECEIPT_FONT_RESOURCE_PATH);
                if (fontStream == null) {
                    throw new IllegalStateException(
                            "한글 폰트 파일을 찾을 수 없습니다: classpath:" + RECEIPT_FONT_RESOURCE_PATH
                                    + " (src/main/resources" + RECEIPT_FONT_RESOURCE_PATH + " 에 TTF 파일을 추가해야 합니다)");
                }
                return fontStream;
            }, RECEIPT_FONT_FAMILY);
            builder.withHtmlContent(html, "");
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("영수증 PDF 생성에 실패했습니다", e);
        }
    }

    /**
     * JWT userId 클레임에서 로그인 사용자 PK 추출.
     * FeeApiController 의 동일한 헬퍼와 중복되지만, 공유 SecurityUtil 클래스가 없는 기존 컨벤션을 따른다.
     */
    private Long currentUserId(JwtAuthenticationToken authentication) {
        return ((Number) authentication.getToken().getClaim("userId")).longValue();
    }

    /**
     * ADMIN 여부 - ADMIN 만 스코핑 없이 전체 영수증을 조회할 수 있다.
     * TEACHER 는 이제 이 컨트롤러의 @PreAuthorize 대상에서 완전히 빠졌으므로(2026-07-22 재논의로 철회)
     * 여기서는 ADMIN/PARENT/STUDENT 만 분기하면 된다.
     */
    private boolean isAdmin(JwtAuthenticationToken authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
    }

    /** 학생 여부 - 학생 본인 영수증 스코핑용 */
    private boolean isStudent(JwtAuthenticationToken authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_STUDENT".equals(auth.getAuthority()));
    }
}
