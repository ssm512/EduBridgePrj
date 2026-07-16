package com.edu.domain.setting.mapper;

import com.edu.domain.setting.vo.SystemSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 시스템설정 매퍼. XML: resources/mapper/setting/SettingMapper.xml
 */
@Mapper
public interface SettingMapper {

    /** 전체 설정 목록 (키 순) */
    List<SystemSetting> findAll();

    /** 단건 조회 */
    SystemSetting findByKey(@Param("key") String key);

    /** 값 조회 (설정 소비용, 없으면 null) */
    String getValue(@Param("key") String key);

    /** 값/설명 수정 (description이 null이면 값만 갱신) */
    int updateValue(@Param("key") String key,
                    @Param("value") String value,
                    @Param("description") String description,
                    @Param("updatedBy") Long updatedBy);

    /** 로그인 ID → user_id (updated_by 기록용) */
    Long findUserIdByLoginId(@Param("loginId") String loginId);

    int insertActivityLog(@Param("userId") Long updatedBy,
                          @Param("actionType") String actionType,
                          @Param("targetTable") String targetTable,
                          @Param("description") String description);

    /** 활성 관리자(ADMIN) user_id 목록 — 공휴일 등록 리마인더 수신자 */
    List<Long> findAdminUserIds();
}
