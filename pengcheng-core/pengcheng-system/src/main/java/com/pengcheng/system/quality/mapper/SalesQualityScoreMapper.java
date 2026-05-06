package com.pengcheng.system.quality.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.system.quality.entity.SalesQualityScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SalesQualityScoreMapper extends BaseMapper<SalesQualityScore> {

    @Select("SELECT * FROM sys_sales_quality_score WHERE user_id = #{userId} ORDER BY score_date DESC LIMIT #{limit}")
    List<SalesQualityScore> getRecentScores(Long userId, int limit);

    @Select("SELECT s.*, u.nick_name AS userName FROM sys_sales_quality_score s LEFT JOIN sys_user u ON s.user_id = u.user_id WHERE s.score_date = #{date} ORDER BY s.overall_score DESC")
    List<SalesQualityScore> getRankingByDate(String date);
}
