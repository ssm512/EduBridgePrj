package com.edu.domain.notice.mapper;

import com.edu.domain.notice.vo.NoticeFileVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeFileMapper {

    /** NOT-06 첨부파일 메타데이터 등록. useGeneratedKeys로 fileId가 채워진다 */
    int insertFile(NoticeFileVo file);

    /** NOT-07 첨부파일 단건 조회 (다운로드용) */
    NoticeFileVo selectByFileId(@Param("fileId") Long fileId);

    /** 공지의 첨부파일 목록 조회 */
    List<NoticeFileVo> selectFilesByNoticeId(@Param("noticeId") Long noticeId);

    /** 첨부파일 메타데이터 삭제 (명세서 외 - 첨부 관리용) */
    int deleteFile(@Param("fileId") Long fileId);
}
