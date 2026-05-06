package com.pengcheng.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pengcheng.file.entity.SysFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件信息 Mapper
 */
@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {
}
