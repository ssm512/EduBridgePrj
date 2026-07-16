package com.edu.domain.notice.mapper;

import com.edu.domain.notice.dto.request.NoticeSearchRequest;
import com.edu.domain.notice.vo.NoticeQueryContext;
import com.edu.domain.notice.vo.NoticeTargetVo;
import com.edu.domain.notice.vo.NoticeVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeMapper {

    // ===== 공지 (notices) =====

    /** NOT-01 공지 등록. useGeneratedKeys로 noticeId가 채워진다 */
    int insertNotice(NoticeVo notice);

    /** 공지 PK로 단건 조회 (상태 무관, 권한 검증용) */
    NoticeVo selectByNoticeId(@Param("noticeId") Long noticeId);

    /** NOT-03 공지 상세 조회 (본문 + 작성자 + 현재 사용자 읽음 여부 + 읽음 수) */
    NoticeVo selectDetail(@Param("noticeId") Long noticeId,
                          @Param("ctx") NoticeQueryContext ctx);

    /** NOT-02 대상자별 공지 목록 조회 (롤별 가시성 + 필터 + 페이징) */
    List<NoticeVo> selectNotices(@Param("cond") NoticeSearchRequest cond,
                                 @Param("ctx") NoticeQueryContext ctx);

    /** NOT-02 목록 전체 건수 (페이징용) */
    long countNotices(@Param("cond") NoticeSearchRequest cond,
                      @Param("ctx") NoticeQueryContext ctx);

    /** 현재 사용자가 볼 수 있는 공지인지 검증 (상세/첨부/읽음 처리 전 공통 체크) */
    boolean existsVisibleNotice(@Param("noticeId") Long noticeId,
                                @Param("ctx") NoticeQueryContext ctx);

    /** NOT-04 공지 수정 (제목/내용/대상유형) */
    int updateNotice(NoticeVo notice);

    /** NOT-05 공지 소프트 삭제 (notice_status = DELETED) */
    int updateStatus(@Param("noticeId") Long noticeId,
                     @Param("noticeStatus") String noticeStatus);

    // ===== 공지 대상 (notice_targets) =====

    /** NOT-01/04 공지 대상 일괄 등록 */
    int insertTargets(@Param("noticeId") Long noticeId,
                      @Param("targetType") String targetType,
                      @Param("targetIds") List<Long> targetIds);

    /** NOT-04 공지 대상 전체 삭제 (수정 시 교체용) */
    int deleteTargets(@Param("noticeId") Long noticeId);

    /** 공지 대상 목록 조회 (대상 이름 포함) */
    List<NoticeTargetVo> selectTargets(@Param("noticeId") Long noticeId);

    /**
     * [추가 2026-07-16] targetIds 중 실제로 존재하는 대상(class/student/parent/teacher) 개수
     * 등록/수정 전 유효성 검증용 (반환값이 distinct targetIds 크기와 다르면 존재하지 않는 ID 포함)
     */
    long countExistingTargets(@Param("targetType") String targetType,
                              @Param("targetIds") List<Long> targetIds);

    // ===== 읽음 (notice_reads) =====

    /** NOT-08 읽음 처리 (INSERT, 이미 있으면 UPDATE — UPSERT) */
    int upsertRead(@Param("noticeId") Long noticeId, @Param("userId") Long userId);

    // ===== 현재 사용자 컨텍스트 조회 (NoticeQueryContext 구성용) =====

    /** users PK로 students PK 조회 (STUDENT 롤) */
    Long selectStudentIdByUserId(@Param("userId") Long userId);

    /** users PK로 parents PK 조회 (PARENT 롤) */
    Long selectParentIdByUserId(@Param("userId") Long userId);

    /** users PK로 teachers PK 조회 (TEACHER 롤) */
    Long selectTeacherIdByUserId(@Param("userId") Long userId);

    // ===== 알림 연동 (공지 대상 → 실제 알림 수신자 user_id) =====

    /**
     * 공지 대상(targetType)에 해당하는 알림 수신자 user_id 목록 조회.
     * ALL: 활성 회원 전체. CLASS: 수강생 + 학부모 + 담당강사. 그 외: 해당 대상 본인.
     */
    List<Long> selectTargetUserIds(@Param("noticeId") Long noticeId,
                                   @Param("targetType") String targetType);
}
