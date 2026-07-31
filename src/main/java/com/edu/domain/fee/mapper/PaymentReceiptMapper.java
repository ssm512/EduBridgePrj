package com.edu.domain.fee.mapper;

import com.edu.domain.fee.dto.response.PaymentReceiptResponse;
import com.edu.domain.fee.vo.PaymentReceiptVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentReceiptMapper {

    /** 영수증 발급 (FEE-19) - payFee() 내부에서 납부 처리 직후 자동 호출 */
    int insert(PaymentReceiptVo receipt);

    /** 납부 이력 1건에 연결된 영수증 원본 조회 - payment_id 는 UNIQUE 라 0~1건 */
    PaymentReceiptVo selectByPaymentId(@Param("paymentId") Long paymentId);

    /**
     * 영수증 취소 (FEE-20) - cancelPayment() 내부에서 납부 취소 직후 자동 호출.
     * status_code = 'ISSUED' 인 것만 대상으로 해서, 이미 취소된 영수증을 다시 취소하거나
     * 영수증 자체가 없는 결제(이 기능 배포 전 이력)를 건드리지 않도록 방어한다.
     */
    int cancelByPaymentId(@Param("paymentId") Long paymentId, @Param("cancelReason") String cancelReason);

    /**
     * 영수증 상세 조회 (RCT-01/02) - payment_receipts + fee_payments + fees + students(users)
     * + classes + issuer(users) JOIN. 없으면 null (이 기능 배포 전 납부 이력이면 영수증이 없을 수 있음).
     */
    PaymentReceiptResponse selectDetailByPaymentId(@Param("paymentId") Long paymentId);
}
