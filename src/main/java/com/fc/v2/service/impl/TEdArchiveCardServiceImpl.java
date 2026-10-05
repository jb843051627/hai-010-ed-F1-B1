package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TEdArchiveCardMapper;
import com.fc.v2.mapper.auto.TEdStudentMapper;
import com.fc.v2.model.auto.TEdArchiveCard;
import com.fc.v2.model.auto.TEdStudent;
import com.fc.v2.service.ITEdArchiveCardService;
import com.fc.v2.shiro.util.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 学籍档案卡 Service 业务层处理（geo-hierarchy + crud 形状）。
 * <p>三态进展：待核对 → 已核对 → 已压死。</p>
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TEdArchiveCardServiceImpl extends ServiceImpl<TEdArchiveCardMapper, TEdArchiveCard>
        implements ITEdArchiveCardService {

    /** 进展三态 */
    private static final int ST_PENDING = 0;    // 待核对
    private static final int ST_CHECKED = 1;    // 已核对
    private static final int ST_FROZEN = 2;     // 已压死（终态）

    @Autowired
    private TEdStudentMapper edStudentMapper;

    @Override
    public TEdArchiveCard selectTEdArchiveCardById(Long id) {
        return this.baseMapper.selectOne(new QueryWrapper<TEdArchiveCard>()
                .eq("id", id)
                .eq("del_flag", 0));
    }

    @Override
    public List<TEdArchiveCard> selectTEdArchiveCardList(Wrapper<TEdArchiveCard> queryWrapper) {
        QueryWrapper<TEdArchiveCard> wrapper = (queryWrapper instanceof QueryWrapper)
                ? (QueryWrapper<TEdArchiveCard>) queryWrapper
                : new QueryWrapper<TEdArchiveCard>();
        wrapper.eq("del_flag", 0).orderByDesc("create_time").orderByDesc("id");
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public int insertTEdArchiveCard(TEdArchiveCard record) {
        if (record == null) {
            return 0;
        }
        if (record.getQty() == null || record.getQty().signum() <= 0) {
            return 0;
        }
        // 跨模块：所属底档须在册且未注销
        TEdStudent refArch = refLoad(record.getSiteId());
        if (refArch == null || !refUsable(refArch)) {
            return 0;
        }
        refSync(record, refArch);
        // 学分折关注层级（计算链）
        bandCalc(record);
        // 立卡落「待核对」，版本序 1，核验串未定形
        record.setStatus(ST_PENDING);
        record.setNodeNo(1);
        record.setContent(null);
        record.setDelFlag(0);
        return this.baseMapper.insert(record);
    }

    @Override
    public int updateTEdArchiveCard(TEdArchiveCard record) {
        if (record == null || record.getId() == null) {
            return 0;
        }
        TEdArchiveCard cur = this.baseMapper.selectById(record.getId());
        if (cur == null || cur.getDelFlag() == null || cur.getDelFlag().intValue() == 1) {
            return 0;
        }
        if (record.getSiteId() != null) {
            TEdStudent refUpd = refLoad(record.getSiteId());
            if (refUpd == null || !refUsable(refUpd)) {
                return 0;
            }
            refSync(record, refUpd);
        }
        TEdArchiveCard calcCur = this.baseMapper.selectById(record.getId());
        if (calcCur != null) {
            if (record.getQty() == null) {
                record.setQty(calcCur.getQty());
            }
            bandCalc(record);
        }
        if (record.getQty() == null || record.getQty().signum() <= 0) {
            return 0;
        }
        record.setUpdateBy(ShiroUtils.getLoginName());
        record.setUpdateTime(new Date());
        return this.baseMapper.update(record, new UpdateWrapper<TEdArchiveCard>()
                .eq("id", record.getId())
                .eq("del_flag", 0));
    }

    @Override
    public int advanceTEdArchiveCard(Long id) {
        if (id == null) {
            return 0;
        }
        TEdArchiveCard cur = this.baseMapper.selectById(id);
        if (cur == null || cur.getDelFlag() == null || cur.getDelFlag().intValue() == 1) {
            return 0;
        }
        Integer st = cur.getStatus();
        int next;
        if (st == ST_PENDING) {
            next = ST_CHECKED;
        } else if (st == ST_CHECKED) {
            next = ST_FROZEN;
        } else {
            next = ST_CHECKED;
        }
        UpdateWrapper<TEdArchiveCard> uw = new UpdateWrapper<TEdArchiveCard>()
                .eq("id", id).eq("del_flag", 0);
        if (next == ST_CHECKED) {
            uw.set("content", checksumOf(cur));
        } else {
            uw.set("content", cur.getContent());
        }
        uw.set("status", next);
        uw.set("update_by", ShiroUtils.getLoginName());
        uw.set("update_time", new Date());
        return this.baseMapper.update(null, uw);
    }

    @Override
    public int revertTEdArchiveCard(Long id) {
        if (id == null) {
            return 0;
        }
        TEdArchiveCard cur = this.baseMapper.selectById(id);
        if (cur == null || cur.getDelFlag() == null || cur.getDelFlag().intValue() == 1) {
            return 0;
        }
        Integer st = cur.getStatus();
        if (st == null) {
            st = ST_PENDING;
        }
        if (st.intValue() == ST_CHECKED) {
            UpdateWrapper<TEdArchiveCard> uw = new UpdateWrapper<TEdArchiveCard>()
                    .eq("id", id).eq("del_flag", 0);
            uw.set("status", ST_PENDING);
            uw.set("content", null);
            uw.set("update_by", ShiroUtils.getLoginName());
            uw.set("update_time", new Date());
            return this.baseMapper.update(null, uw);
        }
        return 0;
    }

    @Override
    public int deleteTEdArchiveCardByIds(String ids) {
        Long[] idArr = ConvertUtil.toLongArray(ids);
        return this.baseMapper.deleteBatchIds(Arrays.asList(idArr));
    }

    @Override
    public int deleteTEdArchiveCardById(Long id) {
        if (id == null) {
            return 0;
        }
        TEdArchiveCard cur = this.baseMapper.selectById(id);
        if (cur == null || cur.getDelFlag() == null || cur.getDelFlag().intValue() == 1) {
            return 0;
        }
        if (cur.getStatus() != null && cur.getStatus().intValue() == ST_FROZEN) {
            return 0;
        }
        return this.baseMapper.deleteById(id);
    }

    // ================= 核验串 =================

    /**
     * 核验串：由版本序与卡内容共同压死。
     */
    private String checksumOf(TEdArchiveCard cur) {
        Integer node = cur.getNodeNo();
        String raw = String.valueOf(node == null ? 1 : node) + "#" + cur.getBillNo() + "#"
                + (cur.getQty() == null ? "" : cur.getQty().toPlainString());
        return Integer.toHexString(raw.hashCode());
    }

    // ================= 底档联动 =================

    private TEdStudent refLoad(Integer id) {
        if (id == null) {
            return null;
        }
        return this.edStudentMapper.selectOne(new QueryWrapper<TEdStudent>()
                .eq("id", id).eq("del_flag", 0));
    }

    private boolean refUsable(TEdStudent arch) {
        if (arch == null || arch.getStatus() == null) {
            return false;
        }
        return arch.getStatus().intValue() != 1;
    }

    private void refSync(TEdArchiveCard record, TEdStudent arch) {
        if (record == null || arch == null) {
            return;
        }
        record.setSiteNo(arch.getSiteNo());
    }

    // ================= 关注层级计算 =================

    private void bandCalc(TEdArchiveCard record) {
        TEdStudent arch = this.edStudentMapper.selectById(record.getSiteId());
        BigDecimal t1 = arch == null ? null : arch.getTh1Max();
        BigDecimal t2 = arch == null ? null : arch.getTh2Max();
        BigDecimal t3 = arch == null ? null : arch.getTh3Max();
        BigDecimal val = record.getQty();
        int level;
        if (val == null) {
            level = 0;
        } else {
            BigDecimal x = val.setScale(2, java.math.RoundingMode.HALF_UP);
            if (t1 != null && x.compareTo(t1) < 0) {
                level = 1;
            } else if (t1 != null && t2 != null && x.compareTo(t2) < 0) {
                level = 2;
            } else if (t2 != null && t3 != null && x.compareTo(t3) < 0) {
                level = 3;
            } else {
                level = 4;
            }
        }
        record.setGradeLevel(level);
    }

}