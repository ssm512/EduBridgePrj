package com.edu.domain.beacon.service;

import com.edu.domain.beacon.dto.request.BeaconRequest;
import com.edu.domain.beacon.vo.Beacon;

import java.util.List;

/** 비콘 관리 서비스 (BCN-01~03) */
public interface BeaconService {

    /** 목록 조회 (반 필터 선택) */
    List<Beacon> getList(Long classId);

    /** 단건 조회 */
    Beacon get(Long beaconId);

    /** 등록 → 생성된 비콘 반환 */
    Beacon create(BeaconRequest request);

    /** 수정 → 수정된 비콘 반환 */
    Beacon update(Long beaconId, BeaconRequest request);

    /** 삭제 */
    void delete(Long beaconId);
}
