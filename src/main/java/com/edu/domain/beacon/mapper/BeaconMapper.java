package com.edu.domain.beacon.mapper;

import com.edu.domain.beacon.vo.Beacon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 비콘 매퍼 (BCN-01~03). XML: resources/mapper/beacon/BeaconMapper.xml
 */
@Mapper
public interface BeaconMapper {

    /** 목록 조회 (반 필터 선택). className 조인 */
    List<Beacon> findList(@Param("classId") Long classId);

    /** PK 단건 조회 */
    Beacon findById(@Param("beaconId") Long beaconId);

    /** 등록 (useGeneratedKeys로 beaconId 채움) */
    int insert(Beacon beacon);

    /** 수정 */
    int update(Beacon beacon);

    /** 삭제 */
    int deleteById(@Param("beaconId") Long beaconId);
}
