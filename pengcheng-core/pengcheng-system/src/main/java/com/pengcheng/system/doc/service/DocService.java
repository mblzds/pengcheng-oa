package com.pengcheng.system.doc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.doc.entity.Doc;
import com.pengcheng.system.doc.entity.DocSpace;
import com.pengcheng.system.doc.entity.DocVersion;
import com.pengcheng.system.doc.mapper.DocMapper;
import com.pengcheng.system.doc.mapper.DocSpaceMapper;
import com.pengcheng.system.doc.mapper.DocVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 云文档服务
 * 提供文档空间管理、文档CRUD、版本历史、全文搜索等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocService {

    private final DocSpaceMapper spaceMapper;
    private final DocMapper docMapper;
    private final DocVersionMapper versionMapper;

    // ========== 空间管理 ==========

    public List<DocSpace> getUserSpaces(Long userId) {
        return spaceMapper.selectList(
            new LambdaQueryWrapper<DocSpace>()
                .eq(DocSpace::getDeleted, 0)
                .and(w -> w.eq(DocSpace::getOwnerId, userId)
                    .or().eq(DocSpace::getVisibility, "all"))
                .orderByDesc(DocSpace::getUpdatedAt)
        );
    }

    public DocSpace createSpace(DocSpace space) {
        spaceMapper.insert(space);
        return space;
    }

    public void updateSpace(DocSpace space) {
        spaceMapper.updateById(space);
    }

    public void deleteSpace(Long id) {
        DocSpace space = new DocSpace();
        space.setId(id);
        space.setDeleted(1);
        spaceMapper.updateById(space);
    }

    // ========== 文档管理 ==========

    public List<Doc> getDocTree(Long spaceId) {
        return docMapper.selectList(
            new LambdaQueryWrapper<Doc>()
                .eq(Doc::getSpaceId, spaceId)
                .eq(Doc::getDeleted, 0)
                .orderByAsc(Doc::getSortOrder)
                .orderByDesc(Doc::getUpdatedAt)
        );
    }

    public Doc getDoc(Long id) {
        return docMapper.selectById(id);
    }

    @Transactional
    public Doc createDoc(Doc doc) {
        if (doc.getContent() != null) {
            doc.setWordCount(doc.getContent().length());
        }
        doc.setVersion(1);
        docMapper.insert(doc);
        return doc;
    }

    @Transactional
    public void updateDoc(Doc doc, Long editorId) {
        Doc existing = docMapper.selectById(doc.getId());
        if (existing == null || existing.getDeleted() == 1) return;

        saveVersion(existing);

        doc.setLastEditorId(editorId);
        doc.setVersion(existing.getVersion() + 1);
        if (doc.getContent() != null) {
            doc.setWordCount(doc.getContent().length());
        }
        docMapper.updateById(doc);
    }

    public void deleteDoc(Long id) {
        Doc doc = new Doc();
        doc.setId(id);
        doc.setDeleted(1);
        docMapper.updateById(doc);
    }

    public void moveDoc(Long docId, Long newParentId, Integer sortOrder) {
        Doc doc = new Doc();
        doc.setId(docId);
        doc.setParentId(newParentId);
        doc.setSortOrder(sortOrder);
        docMapper.updateById(doc);
    }

    // ========== 版本管理 ==========

    private void saveVersion(Doc doc) {
        DocVersion version = new DocVersion();
        version.setDocId(doc.getId());
        version.setVersion(doc.getVersion());
        version.setTitle(doc.getTitle());
        version.setContent(doc.getContent());
        version.setEditorId(doc.getLastEditorId() != null ? doc.getLastEditorId() : doc.getCreatorId());
        versionMapper.insert(version);
    }

    public List<DocVersion> getVersionHistory(Long docId) {
        return versionMapper.selectList(
            new LambdaQueryWrapper<DocVersion>()
                .eq(DocVersion::getDocId, docId)
                .orderByDesc(DocVersion::getVersion)
        );
    }

    @Transactional
    public void restoreVersion(Long docId, Integer targetVersion, Long editorId) {
        DocVersion version = versionMapper.selectOne(
            new LambdaQueryWrapper<DocVersion>()
                .eq(DocVersion::getDocId, docId)
                .eq(DocVersion::getVersion, targetVersion)
        );
        if (version == null) return;

        Doc existing = docMapper.selectById(docId);
        if (existing == null) return;

        saveVersion(existing);

        Doc update = new Doc();
        update.setId(docId);
        update.setTitle(version.getTitle());
        update.setContent(version.getContent());
        update.setLastEditorId(editorId);
        update.setVersion(existing.getVersion() + 1);
        if (version.getContent() != null) {
            update.setWordCount(version.getContent().length());
        }
        docMapper.updateById(update);
    }

    // ========== 搜索 ==========

    public List<Doc> searchDocs(Long spaceId, String keyword) {
        return docMapper.searchInSpace(spaceId, keyword, 50);
    }
}
